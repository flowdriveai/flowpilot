package ai.flow.vision;

import ai.flow.common.ParamsInterface;
import ai.flow.definitions.Definitions;
import ai.flow.vision.messages.*;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OrtSession.SessionOptions;
import ai.onnxruntime.OrtSession.SessionOptions.OptLevel;
import messaging.ZMQPubHandler;
import messaging.ZMQSubHandler;
import org.capnproto.PrimitiveList;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ModelExecutor implements Runnable, ModelExecutorInterface {

    public boolean stopped = false;
    public Thread thread;
    public final String threadName = "modeld";
    public boolean initialized = false;
    public final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final String modelPath = "models/supercombo_simple.onnx";
    public static boolean CUDAAvailable = false;

    public float[][] netOutputs;
    public ParsedOutputs outs;
    public long timePerIt = 0;
    public long iterationNum = 1;
    public int warmupIters = 50;

    public static final long[] imgTensorShape = {1, 12, 128, 256};
    public static final long[] desireTensorShape = {1, 8};
    public static final long[] trafficTensorShape = {1, 2};
    public static final long[] stateTensorShape = {1, 512};
    public static final long[] YUVimgShape = {384, 512, 1};

    public final INDArray imgTensorSequence = Nd4j.zeros(imgTensorShape);
    public final INDArray transformedYUVNDArr = Nd4j.zeros(DataType.UINT8, YUVimgShape);

    public final INDArray augmentRot = Nd4j.zeros(1, 3);
    public final INDArray augmentTrans = Nd4j.zeros(1, 3);

    public final float[][] pulseDesire = new float[1][8];
    public final float[][] prevDesire = new float[1][8];
    public final float[][] traffic = new float[1][2];
    public float[][] state = new float[1][512];

    public final Parser parser = new Parser();
    public final ParamsInterface params = ParamsInterface.getInstance();

    public final  Map<String, OnnxTensor> container = new HashMap<>();

    public final INDArrayIndex[] imgTensor0Slices = new INDArrayIndex[]{NDArrayIndex.point(0), NDArrayIndex.interval(0,6),
                                                                                NDArrayIndex.all(), NDArrayIndex.all()};
    public final INDArrayIndex[] imgTensor1Slices = new INDArrayIndex[]{NDArrayIndex.point(0), NDArrayIndex.interval(6,12),
                                                                             NDArrayIndex.all(), NDArrayIndex.all()};

    public static final float[] FULL_FRAME_SIZE = {874, 1164};
    public static final float FOCAL = 910.0f;

    public static float[][] cameraIntrinsicBuffer = {
        {FOCAL, 0.00f, FULL_FRAME_SIZE[1]/2},
        {0.00f, FOCAL, FULL_FRAME_SIZE[0]/2},
        {0.00f, 0.00f, 1.00f}
    };

    public static final long[] MEDMODEL_INPUT_SIZE = {512, 256};
    public static final float MEDMODEL_CY = 47.6f;
    public static final float medmodel_zoom = 1f;

    public static float[][] medmodel_intrinsics_buffer = {
        {FOCAL/medmodel_zoom, 0f, 0.5f*MEDMODEL_INPUT_SIZE[0]},
        {0f, FOCAL/medmodel_zoom, MEDMODEL_CY},
        {0f, 0f, 1f}
    };
    public static final INDArray medmodel_intrinsics = Nd4j.createFromArray(medmodel_intrinsics_buffer);
    public static INDArray cameraIntrinsics = Nd4j.createFromArray(cameraIntrinsicBuffer);

    public final ZMQPubHandler ph = new ZMQPubHandler();
    public final ZMQSubHandler sh = new ZMQSubHandler(true);

    public MsgDesire msgDesire = new MsgDesire();
    public MsgCameraOdometery msgCameraOdometery = new MsgCameraOdometery();
    public MsgModelDataV2 msgModelDataV2 = new MsgModelDataV2();
    public MsgFrameData msgFrameData = new MsgFrameData(1164*874*3);
    public Definitions.FrameData.Reader frameData;
    public ByteBuffer msgFrameDataBuffer = msgFrameData.getSerializedBuffer();
    public MsgLiveCalibrationData msgLiveCalibrationData = new MsgLiveCalibrationData();
    public ByteBuffer msgLiveCalibrationDataBuffer = msgLiveCalibrationData.getSerializedBuffer();
    public Definitions.LiveCalibrationData.Reader liveCalib;

    public long start, end, timestamp;
    public int lastFrameID = -1;
    public int firstFrameID = -1;
    public int frameDrops = 0;
    public int pulseDesireInput;

    Mat deviceToCalibTransform;

    public ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    Runnable serializeAndPublish = new Runnable()
    {
        @Override
        public void run()
        {
            msgModelDataV2.fill(outs, timestamp, frameData.getFrameId(), -1, getFrameDropPercent(), getIterationRate(), -1);
            msgDesire.fill(outs, timestamp);
            msgCameraOdometery.fill(outs, timestamp, frameData.getFrameId());

            ph.publishBuffer("modelV2", msgModelDataV2.serialize());
            ph.publishBuffer("desire", msgDesire.serialize());
            ph.publishBuffer("cameraOdometry", msgCameraOdometery.serialize());
        }
    };

    public void warmup(OrtSession network, Map<String, OnnxTensor> dummyInputs, int warmupIters) throws OrtException {
        for (int i=0; i<warmupIters; i++) {
            try (OrtSession.Result netOutputsTensor = network.run(container);) {
            }
        }
    }

    public boolean isIntrinsicsValid(){
        // PS: find better ways to check validity.
        if (!frameData.hasIntrinsics())
            return false;
        PrimitiveList.Float.Reader intrinsics = frameData.getIntrinsics();
        return intrinsics.get(0)!=0 & intrinsics.get(2)!=0 & intrinsics.get(4)!=0 & intrinsics.get(5)!=0 & intrinsics.get(8)!=0;
    }

    public void updateCameraMatrix(){
        if (!isIntrinsicsValid())
            return;
        PrimitiveList.Float.Reader intrinsics = frameData.getIntrinsics();
        for (int i=0; i<3; i++){
            for (int j=0; j<3; j++){
                cameraIntrinsicBuffer[i][j] = intrinsics.get(i*3 + j);
                cameraIntrinsics.put(i, j, intrinsics.get(i*3 + j));
            }
        }
    }

    public void handleLiveCalib(Definitions.LiveCalibrationData.Reader liveCalib){
        PrimitiveList.Float.Reader rpy = liveCalib.getRpyCalib();
        for (int i=0; i<3; i++) {
            augmentRot.put(0, i, rpy.get(i));
        }
        deviceToCalibTransform = Preprocess.deviceToCalibratedFrame(1.22f, new float[]{874.0f, 1164.0f}, cameraIntrinsics, medmodel_intrinsics, augmentRot, augmentTrans);
    }

	public void run(){
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        ph.createPublishers(Arrays.asList("modelV2", "desire", "cameraOdometry"));
        sh.createSubscribers(Arrays.asList("roadCameraState", "pulseDesire", "liveCalibration"));

        Mat transformed = new Mat(256, 512, CvType.CV_8UC3);
        Mat transformedYUV = new Mat(384, 512, CvType.CV_8UC1, transformedYUVNDArr.data().asNio());

        final Size outputSize = new Size(512, 256);

		try{
		    final OrtEnvironment env = OrtEnvironment.getEnvironment();
            final OrtSession.SessionOptions opts = new SessionOptions();

            opts.setOptimizationLevel(OptLevel.ALL_OPT);
            opts.setInterOpNumThreads(8);
            opts.setIntraOpNumThreads(4);
            opts.setExecutionMode(OrtSession.SessionOptions.ExecutionMode.SEQUENTIAL);
            if (System.getenv("USE_CUDA") != null) {
                CUDAAvailable = true;
                opts.addCUDA();
                logger.info("To use CPU, unset the 'USE_CUDA' environment variable");
            }
            final OrtSession session = env.createSession(modelPath, opts);

            OnnxTensor inputImgTensor = OnnxTensor.createTensor(env, imgTensorSequence.data().asNioFloat(), imgTensorShape);

            try (OnnxTensor inputStateTensor = OnnxTensor.createTensor(env, state);
                 OnnxTensor inputDesireTensor = OnnxTensor.createTensor(env, pulseDesire);
                 OnnxTensor inputTrafficTensor = OnnxTensor.createTensor(env, traffic);){
                container.put("input_imgs", inputImgTensor);
                container.put("initial_state", inputStateTensor);
                container.put("desire", inputDesireTensor);
                container.put("traffic_convention", inputTrafficTensor);
                warmup(session, container, warmupIters);
            }

            ByteBuffer imgBuffer;
            sh.recvBuffer("roadCameraState", msgFrameDataBuffer);
            frameData = msgFrameData.deserialize().getFrameData();
            if (frameData.getNativeImageAddr() != 0)
                imgBuffer = MsgFrameData.getImgBufferFromAddr(frameData.getNativeImageAddr());
            else
                imgBuffer = ByteBuffer.allocateDirect(1164*874*3);
            Mat imageCurr = new Mat(874, 1164, CvType.CV_8UC3, imgBuffer);
            updateCameraMatrix();
            deviceToCalibTransform = Preprocess.deviceToCalibratedFrame(1.22f, new float[]{874.0f, 1164.0f}, cameraIntrinsics, medmodel_intrinsics, augmentRot, augmentTrans);
            lastFrameID = frameData.getFrameId();

            Preprocess.TransformImg(imageCurr, transformed, deviceToCalibTransform, outputSize);
            Preprocess.RGB888toYUV420(transformed, transformedYUV);
            Preprocess.YUV420toTensor(transformedYUVNDArr, imgTensorSequence, 0);

            initialized = true;
            params.putBool("ModelDReady", true);
	        while (!stopped) {

                sh.recvBuffer("roadCameraState", msgFrameDataBuffer);
                frameData = msgFrameData.deserialize().getFrameData();
                if (frameData.getNativeImageAddr() == 0) {
                    imgBuffer.put(frameData.getImage().asByteBuffer());
                    imgBuffer.rewind();
                }

                start = System.currentTimeMillis();
                if (sh.updated("pulseDesire")){
                    pulseDesireInput = Integer.parseInt(new String(sh.getData("pulseDesire")));
                    pulseDesire[0][pulseDesireInput] = 1.0f;
                }

                if (sh.updated("liveCalibration")) {
                    sh.recvBuffer("liveCalibration", msgLiveCalibrationDataBuffer);
                    liveCalib = msgLiveCalibrationData.deserialize().getLiveCalibration();
                    handleLiveCalib(liveCalib);
                }

                Preprocess.TransformImg(imageCurr, transformed, deviceToCalibTransform, outputSize);
                Preprocess.RGB888toYUV420(transformed, transformedYUV);
                Preprocess.YUV420toTensor(transformedYUVNDArr, imgTensorSequence, 1);

		        try (OnnxTensor inputStateTensor = OnnxTensor.createTensor(env, state);
		             OnnxTensor inputDesireTensor = OnnxTensor.createTensor(env, pulseDesire);
                     OnnxTensor inputTrafficTensor = OnnxTensor.createTensor(env, traffic);){
		                 container.put("input_imgs", inputImgTensor);
                         container.put("initial_state", inputStateTensor);
                         container.put("desire", inputDesireTensor);
                         container.put("traffic_convention", inputTrafficTensor);
                         try (OrtSession.Result netOutputsTensor = session.run(container);){
		                     netOutputs = (float[][]) netOutputsTensor.get(0).getValue();
		                     outs = parser.parser(netOutputs[0]);
		                 }
		        }
                
                pulseDesire[0][0] = 1.0f;
		        for (int i=1; i<outs.metaData.desireState.length; i++){
                    if (outs.metaData.desireState[i] - prevDesire[0][i] > 0.99f)
                        pulseDesire[0][i] = outs.metaData.desireState[i];
                    else
                        pulseDesire[0][i] = 0.0f;
                    prevDesire[0][i] = outs.metaData.desireState[i];
                }

		        state = outs.state;
                imgTensorSequence.put(imgTensor0Slices, imgTensorSequence.get(imgTensor1Slices));

                // publish outputs
                timestamp = System.currentTimeMillis();
                backgroundExecutor.execute(serializeAndPublish);

                end = System.currentTimeMillis();
                // compute runtime stats.
                // skip 1st 10 reading to let it warm up.
                if (iterationNum > 10) {
                    timePerIt += end - start;
                    frameDrops += (frameData.getFrameId() - lastFrameID) - 1;
                }
                else
                    firstFrameID = lastFrameID;

                lastFrameID = frameData.getFrameId();
                iterationNum++;
		    }
            deviceToCalibTransform.release();
        }catch(OrtException e){
               System.out.println(e);
         }

		// dispose
        backgroundExecutor.shutdown();
        imgTensorSequence.close();
        transformedYUVNDArr.close();
        ph.releaseAll();
        transformed.release();
        transformedYUV.release();
	}

    public void start() {
        if (thread == null) {
            thread = new Thread(this, threadName);
            thread.setDaemon(false);
            thread.start();
        }
    }

    public long getIterationRate() {
        return timePerIt/iterationNum;
    }

    public float getFrameDropPercent() {
        return (float)100*frameDrops/(lastFrameID-firstFrameID);
    }

    public boolean isRunning() {
        return initialized;
    }

    public void stop() {
        stopped = true;
        params.putBool("ModelDReady", false);
    }
}
