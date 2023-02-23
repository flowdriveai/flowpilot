package ai.flow.modeld;

import ai.flow.common.ParamsInterface;
import ai.flow.common.transformatons.Camera;
import ai.flow.definitions.Definitions;
import ai.flow.modeld.messages.MsgFrameData;
import ai.flow.modeld.messages.MsgModelRaw;
import messaging.ZMQPubHandler;
import messaging.ZMQSubHandler;
import org.capnproto.PrimitiveList;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.opencv.core.Core;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static ai.flow.common.SystemUtils.getUseGPU;

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
    public static final int[] featureTensorShape = {1, 8, CommonModel.FEATURE_LEN};
    public static final int[] outputTensorShape = {1, CommonModel.NET_OUTPUT_SIZE};

    public static final Map<String, int[]> inputShapeMap = new HashMap<>();
    public static final Map<String, int[]> outputShapeMap = new HashMap<>();
    public final INDArray desireNDArr = Nd4j.zeros(desireTensorShape);
    public final INDArray trafficNDArr = Nd4j.zeros(trafficTensorShape);
    public final INDArray featuresNDArr = Nd4j.zeros(featureTensorShape);
    public final INDArray stateNDArr = Nd4j.zeros(stateTensorShape);
    public final float[] netOutputs = new float[(int)numElements(outputTensorShape)];
    public final INDArray augmentRot = Nd4j.zeros(3);
    public final INDArray augmentTrans = Nd4j.zeros(3);
    public final float[][] prevDesire = new float[1][8];
    public final Map<String, INDArray> inputMap =  new HashMap<>();
    public final Map<String, float[]> outputMap =  new HashMap<>();

    public final ParamsInterface params = ParamsInterface.getInstance();

    public final INDArrayIndex[] featureSlice0 = new INDArrayIndex[]{NDArrayIndex.point(0), NDArrayIndex.interval(0,CommonModel.HISTORY_BUFFER_LEN-1)};
    public final INDArrayIndex[] featureSlice1 = new INDArrayIndex[]{NDArrayIndex.point(0), NDArrayIndex.interval(1,CommonModel.HISTORY_BUFFER_LEN)};

    public final INDArrayIndex[] gatherFeatureSlices = new INDArrayIndex[]{NDArrayIndex.point(0), NDArrayIndex.indices(94, 89, 84, 79, 74, 69, 64, 59)};
    public final INDArrayIndex[] gatherFeatureSlices0 = new INDArrayIndex[]{NDArrayIndex.point(0), NDArrayIndex.all()};
    public static final int[] FULL_FRAME_SIZE = Camera.frameSize;
    public static INDArray fcam_intrinsics = Camera.fcam_intrinsics.dup(); // telephoto
    public static INDArray ecam_intrinsics = Camera.ecam_intrinsics.dup(); // wide
    public final ZMQPubHandler ph = new ZMQPubHandler();
    public final ZMQSubHandler sh = new ZMQSubHandler(true);
    public MsgModelRaw msgModelRaw = new MsgModelRaw();
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

        INDArray netInputBuffer, netInputWideBuffer;

        ph.createPublishers(Arrays.asList("modelRaw"));
        sh.createSubscribers(Arrays.asList("roadCameraState", "wideRoadCameraState", "pulseDesire", "liveCalibration"));

        boolean wideCameraOnly = params.getBool("WideCameraOnly");

        ImagePrepare imagePrepare;
        ImagePrepare imageWidePrepare;
        if (getUseGPU()){
            imagePrepare = new ImagePrepareGPU(FULL_FRAME_SIZE[0], FULL_FRAME_SIZE[1], true);
            imageWidePrepare = new ImagePrepareGPU(FULL_FRAME_SIZE[0], FULL_FRAME_SIZE[1], true);
        }
        else{
            imagePrepare = new ImagePrepareCPU(FULL_FRAME_SIZE[0], FULL_FRAME_SIZE[1], true);
            imageWidePrepare = new ImagePrepareCPU(FULL_FRAME_SIZE[0], FULL_FRAME_SIZE[1], true);
        }

        Definitions.FrameData.Reader frameData;
        Definitions.FrameData.Reader frameWideData;

        inputShapeMap.put("input_imgs", imgTensorShape);
        inputShapeMap.put("big_input_imgs", imgTensorShape);
        inputShapeMap.put("features_buffer", featureTensorShape);
        inputShapeMap.put("desire", desireTensorShape);
        inputShapeMap.put("traffic_convention", trafficTensorShape);
        outputShapeMap.put("outputs", outputTensorShape);

        inputMap.put("features_buffer", featuresNDArr);
        inputMap.put("desire", desireNDArr);
        inputMap.put("traffic_convention", trafficNDArr);
        outputMap.put("outputs", netOutputs);

        modelRunner.init(inputShapeMap, outputShapeMap);
        modelRunner.warmup();

        INDArray wrapMatrix = Preprocess.getWrapMatrix(augmentRot, fcam_intrinsics, ecam_intrinsics, wideCameraOnly, false);
        INDArray wrapMatrixWide = Preprocess.getWrapMatrix(augmentRot, fcam_intrinsics, ecam_intrinsics, true, true);

        ByteBuffer imgBuffer = null;
        ByteBuffer wideImgBuffer = null;

        frameWideData = sh.recv("wideRoadCameraState").getFrameData();
        if (!wideCameraOnly)
            frameData = sh.recv("roadCameraState").getFrameData();
        else
            frameData = frameWideData;

        if (frameData.getNativeImageAddr() != 0) {
            wideImgBuffer = MsgFrameData.getImgBufferFromAddr(frameWideData.getNativeImageAddr());
            imgBuffer = wideCameraOnly ?  wideImgBuffer : MsgFrameData.getImgBufferFromAddr(frameData.getNativeImageAddr());
        }

        updateCameraMatrix(frameWideData.getIntrinsics(), true);
        updateCameraMatrix(frameData.getIntrinsics(), wideCameraOnly);
        lastWideFrameID = frameWideData.getFrameId();
        lastFrameID = frameData.getFrameId();

        initialized = true;
        params.putBool("ModelDReady", true);
        while (!stopped) {

            frameWideData = sh.recv("wideRoadCameraState").getFrameData();
            if (!wideCameraOnly)
                frameData = sh.recv("roadCameraState").getFrameData();
            else
                frameData = frameWideData;

            start = System.currentTimeMillis();

            // TODO: Fix this
//            if (sh.updated("pulseDesire")){
//                pulseDesireInput = Integer.parseInt(new String(sh.getData("pulseDesire")));
//                desireNDArr.put(0, pulseDesireInput, 1);
//            }

            if (sh.updated("liveCalibration")) {
                liveCalib = sh.recv("liveCalibration").getLiveCalibration();
                PrimitiveList.Float.Reader rpy = liveCalib.getRpyCalib();
                for (int i=0; i<3; i++) {
                    augmentRot.putScalar(i, rpy.get(i));
                }
                wrapMatrix = Preprocess.getWrapMatrix(augmentRot, fcam_intrinsics, ecam_intrinsics, wideCameraOnly, false);
                wrapMatrixWide = Preprocess.getWrapMatrix(augmentRot, fcam_intrinsics, ecam_intrinsics, true, true);
            }

            netInputBuffer = imagePrepare.prepare(imgBuffer, wrapMatrix);
            netInputWideBuffer = imageWidePrepare.prepare(wideImgBuffer, wrapMatrixWide);

            inputMap.put("input_imgs", netInputBuffer);
            inputMap.put("big_input_imgs", netInputWideBuffer);
            modelRunner.run(inputMap, outputMap);

            // TODO: Add desire.
            stateNDArr.put(featureSlice0, stateNDArr.get(featureSlice1));
            for (int i=0; i<CommonModel.FEATURE_LEN; i++)
                stateNDArr.putScalar(0, CommonModel.HISTORY_BUFFER_LEN-1, i, netOutputs[CommonModel.OUTPUT_SIZE+i]);
            featuresNDArr.put(gatherFeatureSlices0, stateNDArr.get(gatherFeatureSlices));

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

        for (String inputName : inputMap.keySet()) {
            inputMap.get(inputName).close();
        }
        modelRunner.dispose();
        imagePrepare.dispose();
        imageWidePrepare.dispose();
        ph.releaseAll();
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
