package ai.flow.modeld;

import ai.flow.common.ParamsInterface;
import ai.flow.common.transformations.Camera;
import ai.flow.definitions.Definitions;
import ai.flow.modeld.messages.MsgModelRaw;
import messaging.ZMQPubHandler;
import messaging.ZMQSubHandler;
import org.capnproto.PrimitiveList;
import org.nd4j.linalg.api.memory.MemoryWorkspace;
import org.nd4j.linalg.api.memory.conf.WorkspaceConfiguration;
import org.nd4j.linalg.api.memory.enums.AllocationPolicy;
import org.nd4j.linalg.api.memory.enums.LearningPolicy;
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
import static ai.flow.common.utils.numElements;
import static ai.flow.sensor.messages.MsgFrameBuffer.updateImageBuffer;

public class ModelExecutorF3 extends ModelExecutor implements Runnable{

    public boolean stopped = false;
    boolean exit = false;
    public Thread thread;
    public final String threadName = "modeld";
    public boolean initialized = false;
    public long timePerIt = 0;
    public long iterationNum = 1;

    public static int[] imgTensorShape = {1, 12, 128, 256};
    public static final int[] desireTensorShape = {1, CommonModelF3.HISTORY_BUFFER_LEN+1, CommonModelF3.DESIRE_LEN};
    public static final int[] trafficTensorShape = {1, CommonModelF3.TRAFFIC_CONVENTION_LEN};
    public static final int[] stateTensorShape = {1, CommonModelF3.HISTORY_BUFFER_LEN, CommonModelF3.FEATURE_LEN};
    public static final int[] featureTensorShape = {1, 8, CommonModelF3.FEATURE_LEN};
    public static final int[] outputTensorShape = {1, CommonModelF3.NET_OUTPUT_SIZE};

    public static final Map<String, int[]> inputShapeMap = new HashMap<>();
    public static final Map<String, int[]> outputShapeMap = new HashMap<>();
    public final INDArray desireNDArr = Nd4j.zeros(desireTensorShape);
    public final INDArray trafficNDArr = Nd4j.zeros(trafficTensorShape);
    public final INDArray featuresNDArr = Nd4j.zeros(featureTensorShape);
    public final INDArray stateNDArr = Nd4j.zeros(stateTensorShape);
    public final float[] netOutputs = new float[(int)numElements(outputTensorShape)];
    public final INDArray augmentRot = Nd4j.zeros(3);
    public final INDArray augmentTrans = Nd4j.zeros(3);
    public final float[]prevDesire = new float[CommonModelF3.DESIRE_LEN];
    public final float[]desireIn = new float[CommonModelF3.DESIRE_LEN];
    public final Map<String, INDArray> inputMap =  new HashMap<>();
    public final Map<String, float[]> outputMap =  new HashMap<>();

    public final ParamsInterface params = ParamsInterface.getInstance();

    public final INDArrayIndex[] stateFeatureSlice0 = new INDArrayIndex[]{NDArrayIndex.point(0), NDArrayIndex.interval(0, CommonModelF3.HISTORY_BUFFER_LEN-1)};
    public final INDArrayIndex[] stateFeatureSlice1 = new INDArrayIndex[]{NDArrayIndex.point(0), NDArrayIndex.interval(1, CommonModelF3.HISTORY_BUFFER_LEN)};
    public final INDArrayIndex[] desireFeatureSlice0 = new INDArrayIndex[]{NDArrayIndex.point(0), NDArrayIndex.interval(0, CommonModelF3.HISTORY_BUFFER_LEN)};
    public final INDArrayIndex[] desireFeatureSlice1 = new INDArrayIndex[]{NDArrayIndex.point(0), NDArrayIndex.interval(1, CommonModelF3.HISTORY_BUFFER_LEN+1)};

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
    int desire;
    public ModelRunner modelRunner;
    Definitions.FrameData.Reader frameData;
    Definitions.FrameData.Reader frameWideData;
    Definitions.FrameBuffer.Reader msgFrameBuffer;
    Definitions.FrameBuffer.Reader msgFrameWideBuffer;
    ByteBuffer imgBuffer;
    ByteBuffer wideImgBuffer;
    boolean wideCameraOnly;
    boolean snpe;
    final WorkspaceConfiguration wsConfig = WorkspaceConfiguration.builder()
            .policyAllocation(AllocationPolicy.STRICT)
            .policyLearning(LearningPolicy.FIRST_LOOP)
            .build();

    public ModelExecutorF3(ModelRunner modelRunner){
        this.modelRunner = modelRunner;
    }

    public boolean isIntrinsicsValid(PrimitiveList.Float.Reader intrinsics){
        // TODO: find better ways to check validity.
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

    public void updateCameraState(){
        frameWideData = sh.recv("wideRoadCameraState").getFrameData();
        msgFrameWideBuffer = sh.recv("wideRoadCameraBuffer").getWideRoadCameraBuffer();
        frameData = wideCameraOnly ? frameWideData : sh.recv("roadCameraState").getFrameData();
        msgFrameBuffer = wideCameraOnly ? msgFrameWideBuffer : sh.recv("roadCameraBuffer").getRoadCameraBuffer();
        imgBuffer = updateImageBuffer(msgFrameBuffer, imgBuffer);
        wideImgBuffer = updateImageBuffer(msgFrameWideBuffer, wideImgBuffer);
    }

    public void run(){
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        wideCameraOnly = params.getBool("WideCameraOnly");
        snpe = params.getBool("UseSNPE");
        if (snpe)
            imgTensorShape = new int[]{1, 128, 256, 12}; // SNPE only supports NHWC input.

        INDArray netInputBuffer, netInputWideBuffer;

        ph.createPublishers(Arrays.asList("modelRaw"));
        sh.createSubscribers(Arrays.asList("roadCameraState", "wideRoadCameraState", "roadCameraBuffer",
                                           "wideRoadCameraBuffer", "pulseDesire", "liveCalibration", "lateralPlan"));

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

        updateCameraState();
        updateCameraMatrix(frameWideData.getIntrinsics(), true);
        updateCameraMatrix(frameData.getIntrinsics(), wideCameraOnly);

        // TODO:Clean this shit.
        ImagePrepare imagePrepare;
        ImagePrepare imageWidePrepare;
        boolean rgb;
        if (getUseGPU()){
            rgb = msgFrameBuffer.getEncoding() == Definitions.FrameBuffer.Encoding.RGB;
            imagePrepare = new ImagePrepareGPU(FULL_FRAME_SIZE[0], FULL_FRAME_SIZE[1], rgb, msgFrameBuffer.getYWidth(), msgFrameBuffer.getYHeight(),
                    msgFrameBuffer.getYPixelStride(), msgFrameBuffer.getUvWidth(), msgFrameBuffer.getUvHeight(), msgFrameBuffer.getUvPixelStride(),
                    msgFrameBuffer.getUOffset(), msgFrameBuffer.getVOffset(), msgFrameBuffer.getStride());
            rgb = msgFrameWideBuffer.getEncoding() == Definitions.FrameBuffer.Encoding.RGB;
            imageWidePrepare = new ImagePrepareGPU(FULL_FRAME_SIZE[0], FULL_FRAME_SIZE[1], rgb, msgFrameWideBuffer.getYWidth(), msgFrameWideBuffer.getYHeight(),
                    msgFrameWideBuffer.getYPixelStride(), msgFrameWideBuffer.getUvWidth(), msgFrameWideBuffer.getUvHeight(), msgFrameWideBuffer.getUvPixelStride(),
                    msgFrameWideBuffer.getUOffset(), msgFrameWideBuffer.getVOffset(), msgFrameWideBuffer.getStride());
        }
        else{
            rgb = msgFrameBuffer.getEncoding() == Definitions.FrameBuffer.Encoding.RGB;
            imagePrepare = new ImagePrepareCPU(FULL_FRAME_SIZE[0], FULL_FRAME_SIZE[1], rgb, msgFrameBuffer.getYWidth(), msgFrameBuffer.getYHeight(),
                    msgFrameBuffer.getYPixelStride(), msgFrameBuffer.getUvWidth(), msgFrameBuffer.getUvHeight(), msgFrameBuffer.getUvPixelStride(),
                    msgFrameBuffer.getUOffset(), msgFrameBuffer.getVOffset(), msgFrameBuffer.getStride());
            rgb = msgFrameWideBuffer.getEncoding() == Definitions.FrameBuffer.Encoding.RGB;
            imageWidePrepare = new ImagePrepareCPU(FULL_FRAME_SIZE[0], FULL_FRAME_SIZE[1], rgb, msgFrameWideBuffer.getYWidth(), msgFrameWideBuffer.getYHeight(),
                    msgFrameWideBuffer.getYPixelStride(), msgFrameWideBuffer.getUvWidth(), msgFrameWideBuffer.getUvHeight(), msgFrameWideBuffer.getUvPixelStride(),
                    msgFrameWideBuffer.getUOffset(), msgFrameWideBuffer.getVOffset(), msgFrameWideBuffer.getStride());
        }

        lastWideFrameID = frameWideData.getFrameId();
        lastFrameID = frameData.getFrameId();

        initialized = true;
        params.putBool("ModelDReady", true);
        while (!exit) {
            if (stopped){
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                continue;
            }

            updateCameraState();
            start = System.currentTimeMillis();

            if (sh.updated("lateralPlan")){
                desire = sh.recv("lateralPlan").getLateralPlan().getDesire().ordinal();
                if (desire >= 0 && desire < CommonModelF2.DESIRE_LEN)
                    desireIn[desire] = 1.0f;
            }

            desireNDArr.put(desireFeatureSlice0, desireNDArr.get(desireFeatureSlice1));
            for (int i=1; i<CommonModelF3.DESIRE_LEN; i++){
                if (desireIn[i] - prevDesire[i] > 0.99f)
                    desireNDArr.putScalar(0, -1, i, desireIn[i]);
                else
                    desireNDArr.putScalar(0, -1, i, 0.0f);
                prevDesire[i] = desireIn[i];
            }

            if (sh.updated("liveCalibration")) {
                liveCalib = sh.recv("liveCalibration").getLiveCalibration();
                PrimitiveList.Float.Reader rpy = liveCalib.getRpyCalib();
                for (int i = 0; i < 3; i++) {
                    augmentRot.putScalar(i, rpy.get(i));
                }
                wrapMatrix = Preprocess.getWrapMatrix(augmentRot, fcam_intrinsics, ecam_intrinsics, wideCameraOnly, false);
                wrapMatrixWide = Preprocess.getWrapMatrix(augmentRot, fcam_intrinsics, ecam_intrinsics, true, true);
            }

            netInputBuffer = imagePrepare.prepare(imgBuffer, wrapMatrix);
            netInputWideBuffer = imageWidePrepare.prepare(wideImgBuffer, wrapMatrixWide);

            if (snpe){
                try (MemoryWorkspace ws = Nd4j.getWorkspaceManager().getAndActivateWorkspace(wsConfig, "ModelD")) {
                // NCHW to NHWC
                netInputBuffer = netInputBuffer.permute(0, 2, 3, 1).dup();
                netInputWideBuffer = netInputWideBuffer.permute(0, 2, 3, 1).dup();
                }
            }
            
            inputMap.put("input_imgs", netInputBuffer);
            inputMap.put("big_input_imgs", netInputWideBuffer);
            modelRunner.run(inputMap, outputMap);

            stateNDArr.put(stateFeatureSlice0, stateNDArr.get(stateFeatureSlice1));
            for (int i = 0; i < CommonModelF3.FEATURE_LEN; i++)
                stateNDArr.putScalar(0, CommonModelF3.HISTORY_BUFFER_LEN - 1, i, netOutputs[CommonModelF3.OUTPUT_SIZE + i]);
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
            } else {
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
        stopped = false;
        if (thread == null) {
            thread = new Thread(this, threadName);
            thread.setDaemon(false);
            thread.start();
        }
    }

    public void serializeAndPublish(){
        msgModelRaw.fill(netOutputs, timestamp, lastFrameID, -1, getFrameDropPercent(), getIterationRate());
        ph.publishBuffer("modelRaw", msgModelRaw.serialize(true));
    }

    public long getIterationRate() {
        return timePerIt/iterationNum;
    }

    public float getFrameDropPercent() {
        return (float)100*frameDrops/(lastFrameID-firstFrameID);
    }

    public boolean isRunning() {
        return !stopped;
    }

    public boolean isInitialized(){
        return initialized;
    }

    public void dispose(){
        exit = true;
    }

    public void stop() {
        stopped = true;
    }
}
