package ai.flow.modeld;

import ai.flow.common.ParamsInterface;
import ai.flow.common.transformatons.Camera;
import ai.flow.definitions.Definitions;
import ai.flow.modeld.messages.MsgFrameData;
import ai.flow.modeld.messages.MsgLiveCalibrationData;
import ai.flow.modeld.messages.MsgModelRaw;
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

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ModelExecutor implements Runnable{

    public boolean stopped = false;
    public Thread thread;
    public final String threadName = "modeld";
    public boolean initialized = false;
    public long timePerIt = 0;
    public long iterationNum = 1;

    public static final int[] imgTensorShape = {1, 12, 128, 256};
    public static final int[] desireTensorShape = {1, 100, CommonModel.DESIRE_LEN};
    public static final int[] trafficTensorShape = {1, CommonModel.TRAFFIC_CONVENTION_LEN};
    public static final int[] stateTensorShape = {1, CommonModel.HISTORY_BUFFER_LEN, CommonModel.FEATURE_LEN};
    public static final int[] outputTensorShape = {1, CommonModel.NET_OUTPUT_SIZE};

    public static final Map<String, int[]> shapeMap = new HashMap<>();
    public static final long[] YUVimgShape = {384, 512, 1};

    public final INDArray imgTensorSequence = Nd4j.zeros(imgTensorShape);
    public final INDArray imgWideTensorSequence = Nd4j.zeros(imgTensorShape);
    public final INDArray desireNDArr = Nd4j.zeros(desireTensorShape);
    public final INDArray trafficNDArr = Nd4j.zeros(trafficTensorShape);
    public final INDArray stateNDArr = Nd4j.zeros(stateTensorShape);
    public final float[] netOutputs = new float[(int)numElements(outputTensorShape)];
    public final INDArray transformedYUVNDArr = Nd4j.zeros(DataType.UINT8, YUVimgShape);
    public final INDArray transformedWideYUVNDArr = Nd4j.zeros(DataType.UINT8, YUVimgShape);
    public final INDArray augmentRot = Nd4j.zeros(3);
    public final INDArray augmentTrans = Nd4j.zeros(3);
    public final float[][] prevDesire = new float[1][8];
    public final Map<String, INDArray> inputMap =  new HashMap<>();
    public final Map<String, float[]> outputMap =  new HashMap<>();

    public final ParamsInterface params = ParamsInterface.getInstance();

    public final INDArrayIndex[] imgTensor0Slices = new INDArrayIndex[]{NDArrayIndex.point(0), NDArrayIndex.interval(0,6),
            NDArrayIndex.all(), NDArrayIndex.all()};
    public final INDArrayIndex[] imgTensor1Slices = new INDArrayIndex[]{NDArrayIndex.point(0), NDArrayIndex.interval(6,12),
            NDArrayIndex.all(), NDArrayIndex.all()};

    public final INDArrayIndex[] featureSlice0 = new INDArrayIndex[]{NDArrayIndex.point(0), NDArrayIndex.interval(0,CommonModel.HISTORY_BUFFER_LEN-1)};
    public final INDArrayIndex[] featureSlice1 = new INDArrayIndex[]{NDArrayIndex.point(0), NDArrayIndex.interval(1,CommonModel.HISTORY_BUFFER_LEN)};

    public static final float[] FULL_FRAME_SIZE = {1080, 1920};
    public static INDArray fcam_intrinsics = Camera.fcam_intrinsics.dup(); // telephoto
    public static INDArray ecam_intrinsics = Camera.ecam_intrinsics.dup(); // wide
    public final ZMQPubHandler ph = new ZMQPubHandler();
    public final ZMQSubHandler sh = new ZMQSubHandler(true);
    public MsgModelRaw msgModelRaw = new MsgModelRaw();
    public MsgLiveCalibrationData msgLiveCalibrationData = new MsgLiveCalibrationData();
    public ByteBuffer msgLiveCalibrationDataBuffer = msgLiveCalibrationData.getSerializedBuffer();
    public Definitions.LiveCalibrationData.Reader liveCalib;

    public long start, end, timestamp;
    public int lastFrameID = -1;
    public int lastWideFrameID = -1;
    public int firstWideFrameID = -1;
    public int firstFrameID = -1;
    public int wideFrameDrops = 0;
    public int frameDrops = 0;
    public ModelRunner modelRunner;

    public ModelExecutor(ModelRunner modelRunner){
        this.modelRunner = modelRunner;
    }

    public void floatArrToBuffer(float[] arr, ByteBuffer buffer){
        for (int i=0; i<arr.length; i++)
            buffer.putFloat(i*4, arr[i]);
    }

    public boolean isIntrinsicsValid(PrimitiveList.Float.Reader intrinsics){
        // PS: find better ways to check validity.
        return intrinsics.get(0)!=0 & intrinsics.get(2)!=0 & intrinsics.get(4)!=0 & intrinsics.get(5)!=0 & intrinsics.get(8)!=0;
    }

    public void updateCameraMatrix(PrimitiveList.Float.Reader intrinsics, boolean wide){
        if (!isIntrinsicsValid(intrinsics))
            return;
        for (int i=0; i<3; i++){
            for (int j=0; j<3; j++){
                if (wide)
                    ecam_intrinsics.put(i, j, intrinsics.get(i*3 + j));
                else
                    fcam_intrinsics.put(i, j, intrinsics.get(i*3 + j));
            }
        }
    }

    public static double numElements(int[] shape){
        double ret = 1;
        for (int i:shape)
            ret *= i;
        return ret;
    }

    public void run(){
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        ph.createPublishers(Arrays.asList("modelRaw"));
        sh.createSubscribers(Arrays.asList("roadCameraState", "wideRoadCameraState", "pulseDesire", "liveCalibration"));

        boolean wideCameraOnly = params.getBool("WideCameraOnly");

        MsgFrameData msgFrameData;
        Definitions.FrameData.Reader frameData;

        MsgFrameData msgWideFrameData = new MsgFrameData((int)FULL_FRAME_SIZE[0]*(int)FULL_FRAME_SIZE[1]*3);
        Definitions.FrameData.Reader frameWideData;

        ByteBuffer msgWideFrameDataBuffer = msgWideFrameData.getSerializedBuffer();
        ByteBuffer msgFrameDataBuffer;
        if (wideCameraOnly) {
            msgFrameData = msgWideFrameData;
            msgFrameDataBuffer = msgWideFrameDataBuffer;
        }
        else {
            msgFrameData = new MsgFrameData((int) FULL_FRAME_SIZE[0] * (int) FULL_FRAME_SIZE[1] * 3);
            msgFrameDataBuffer = msgFrameData.getSerializedBuffer();
        }

        Mat transformed = new Mat(256, 512, CvType.CV_8UC3);
        Mat transformedWide = new Mat(256, 512, CvType.CV_8UC3);
        Mat transformedYUV = new Mat(384, 512, CvType.CV_8UC1, transformedYUVNDArr.data().asNio());
        Mat transformedWideYUV = new Mat(384, 512, CvType.CV_8UC1, transformedWideYUVNDArr.data().asNio());

        final Size outputSize = new Size(512, 256);

        shapeMap.put("input_imgs", imgTensorShape);
        shapeMap.put("big_input_imgs", imgTensorShape);
        shapeMap.put("features_buffer", stateTensorShape);
        shapeMap.put("desire", desireTensorShape);
        shapeMap.put("traffic_convention", trafficTensorShape);
        shapeMap.put("outputs", outputTensorShape);

        inputMap.put("input_imgs", imgTensorSequence);
        inputMap.put("big_input_imgs", imgWideTensorSequence);
        inputMap.put("features_buffer", stateNDArr);
        inputMap.put("desire", desireNDArr);
        inputMap.put("traffic_convention", trafficNDArr);
        outputMap.put("outputs", netOutputs);

        modelRunner.init(shapeMap);
        modelRunner.warmup();

        INDArray wrapMatrix = Preprocess.getWrapMatrix(augmentRot, fcam_intrinsics, ecam_intrinsics, wideCameraOnly, false);
        INDArray wrapMatrixWide = Preprocess.getWrapMatrix(augmentRot, fcam_intrinsics, ecam_intrinsics, wideCameraOnly, true);

        ByteBuffer imgBuffer;
        ByteBuffer wideImgBuffer;

        sh.recvBuffer("wideRoadCameraState", msgWideFrameDataBuffer);
        if (!wideCameraOnly)
            sh.recvBuffer("roadCameraState", msgFrameDataBuffer);

        frameWideData = msgWideFrameData.deserialize().getFrameData();
        frameData = wideCameraOnly ? frameWideData : msgFrameData.deserialize().getFrameData();

        if (frameData.getNativeImageAddr() != 0) {
            wideImgBuffer = MsgFrameData.getImgBufferFromAddr(frameWideData.getNativeImageAddr());
            imgBuffer = wideCameraOnly ?  wideImgBuffer : MsgFrameData.getImgBufferFromAddr(frameData.getNativeImageAddr());
        }
        else {
            wideImgBuffer = ByteBuffer.allocateDirect((int)FULL_FRAME_SIZE[0]*(int)FULL_FRAME_SIZE[1]*3);
            imgBuffer = wideCameraOnly ?  wideImgBuffer : ByteBuffer.allocateDirect((int)FULL_FRAME_SIZE[0]*(int)FULL_FRAME_SIZE[1]*3);
        }

        Mat wideimageCurr = new Mat((int)FULL_FRAME_SIZE[0], (int)FULL_FRAME_SIZE[1], CvType.CV_8UC3,  wideImgBuffer);
        Mat imageCurr = wideCameraOnly ? wideimageCurr : new Mat((int)FULL_FRAME_SIZE[0], (int)FULL_FRAME_SIZE[1], CvType.CV_8UC3,  imgBuffer);
        updateCameraMatrix(frameWideData.getIntrinsics(), true);
        updateCameraMatrix(frameData.getIntrinsics(), false);
        lastWideFrameID = frameWideData.getFrameId();
        lastFrameID = frameData.getFrameId();

        Preprocess.TransformImg(imageCurr, transformed, wrapMatrix, outputSize);
        Preprocess.TransformImg(wideimageCurr, transformedWide, wrapMatrixWide, outputSize);
        Preprocess.RGB888toYUV420(transformed, transformedYUV);
        Preprocess.RGB888toYUV420(wideimageCurr, transformedWideYUV);
        Preprocess.YUV420toTensor(transformedYUVNDArr, imgTensorSequence, 0);
        Preprocess.YUV420toTensor(transformedWideYUVNDArr, imgWideTensorSequence, 0);

        initialized = true;
        params.putBool("ModelDReady", true);
        while (!stopped) {

            sh.recvBuffer("wideRoadCameraState", msgWideFrameDataBuffer);
            if (!wideCameraOnly)
                sh.recvBuffer("roadCameraState", msgFrameDataBuffer);
            frameWideData = msgWideFrameData.deserialize().getFrameData();
            frameData = wideCameraOnly ? frameWideData : msgFrameData.deserialize().getFrameData();
            if (frameWideData.getNativeImageAddr() == 0) {
                wideImgBuffer.put(frameWideData.getImage().asByteBuffer());
                wideImgBuffer.rewind();
                if (!wideCameraOnly && frameData.getNativeImageAddr() == 0){
                    imgBuffer.put(frameData.getImage().asByteBuffer());
                    imgBuffer.rewind();
                }
            }

            start = System.currentTimeMillis();

            // TODO: Fix this
//            if (sh.updated("pulseDesire")){
//                pulseDesireInput = Integer.parseInt(new String(sh.getData("pulseDesire")));
//                desireNDArr.put(0, pulseDesireInput, 1);
//            }

            if (sh.updated("liveCalibration")) {
                sh.recvBuffer("liveCalibration", msgLiveCalibrationDataBuffer);
                liveCalib = msgLiveCalibrationData.deserialize().getLiveCalibration();
                PrimitiveList.Float.Reader rpy = liveCalib.getRpyCalib();
                for (int i=0; i<3; i++) {
                    augmentRot.putScalar(i, rpy.get(i));
                }
                wrapMatrix = Preprocess.getWrapMatrix(augmentRot, fcam_intrinsics, ecam_intrinsics, wideCameraOnly, false);
                wrapMatrixWide = Preprocess.getWrapMatrix(augmentRot, fcam_intrinsics, ecam_intrinsics, wideCameraOnly, true);
            }

            Preprocess.TransformImg(imageCurr, transformed, wrapMatrix, outputSize);
            Preprocess.TransformImg(wideimageCurr, transformedWide, wrapMatrixWide, outputSize);
            Preprocess.RGB888toYUV420(transformed, transformedYUV);
            Preprocess.RGB888toYUV420(transformedWide, transformedWideYUV);
            Preprocess.YUV420toTensor(transformedYUVNDArr, imgTensorSequence, 0);
            Preprocess.YUV420toTensor(transformedWideYUVNDArr, imgWideTensorSequence, 0);

            modelRunner.run(inputMap, outputMap);

            // TODO: Add desire.
            stateNDArr.put(featureSlice0, stateNDArr.get(featureSlice1));
            for (int i=0; i<CommonModel.FEATURE_LEN; i++)
                stateNDArr.putScalar(0, CommonModel.HISTORY_BUFFER_LEN-1, i, netOutputs[CommonModel.OUTPUT_SIZE+i]);

            imgTensorSequence.put(imgTensor0Slices, imgTensorSequence.get(imgTensor1Slices));
            imgWideTensorSequence.put(imgTensor0Slices, imgWideTensorSequence.get(imgTensor1Slices));

            // publish outputs
            timestamp = System.currentTimeMillis();
            serializeAndPublish();

            end = System.currentTimeMillis();
            // compute runtime stats.
            // skip 1st 10 reading to let it warm up.
            if (iterationNum > 10) {
                timePerIt += end - start;
                frameDrops += (frameData.getFrameId() - lastFrameID) - 1;
                wideFrameDrops += (frameWideData.getFrameId() - lastWideFrameID) - 1;
            }
            else {
                firstFrameID = lastFrameID;
                firstWideFrameID = lastWideFrameID;
            }

            lastFrameID = frameData.getFrameId();
            lastWideFrameID = frameWideData.getFrameId();
            iterationNum++;
        }

        // dispose
        wrapMatrix.close();
        wrapMatrixWide.close();

        imgTensorSequence.close();
        imgWideTensorSequence.close();
        transformedYUVNDArr.close();
        transformedWideYUVNDArr.close();
        for (String inputName : inputMap.keySet()) {
            inputMap.get(inputName).close();
        }
        modelRunner.dispose();
        ph.releaseAll();
        transformed.release();
        transformedYUV.release();
        imageCurr.release();
        wideimageCurr.release();
    }

    public void start() {
        if (thread == null) {
            thread = new Thread(this, threadName);
            thread.setDaemon(false);
            thread.start();
        }
    }

    public void serializeAndPublish(){
        msgModelRaw.fill(netOutputs, timestamp, lastFrameID, -1, getFrameDropPercent(), getIterationRate());
        ph.publishBuffer("modelRaw", msgModelRaw.serialize(true));
    };

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
