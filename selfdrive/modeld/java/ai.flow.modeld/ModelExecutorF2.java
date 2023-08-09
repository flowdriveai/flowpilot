package ai.flow.modeld;

import ai.flow.common.ParamsInterface;
import ai.flow.common.transformations.Camera;
import ai.flow.definitions.Definitions;
import ai.flow.modeld.messages.MsgCameraOdometery;
import ai.flow.modeld.messages.MsgModelDataV2;
import messaging.ZMQPubHandler;
import messaging.ZMQSubHandler;
import org.capnproto.PrimitiveList;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.opencv.core.Core;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static ai.flow.common.SystemUtils.getUseGPU;
import static ai.flow.common.utils.numElements;
import static ai.flow.sensor.messages.MsgFrameBuffer.updateImageBuffer;

public class ModelExecutorF2 extends ModelExecutor implements Runnable{

    public boolean stopped = true;
    boolean exit = false;
    public Thread thread;
    public final String threadName = "modeld";
    public boolean initialized = false;
    public ParsedOutputs outs;
    public long timePerIt = 0;
    public long iterationNum = 1;

    public static final int[] imgTensorShape = {1, 12, 128, 256};
    public static final int[] desireTensorShape = {1, 8};
    public static final int[] trafficTensorShape = {1, 2};
    public static final int[] stateTensorShape = {1, 512};
    public static final int[] outputTensorShape = {1, 6609};

    public static final Map<String, int[]> inputShapeMap = new HashMap<>();
    public static final Map<String, int[]> outputShapeMap = new HashMap<>();
    public final INDArray desireNDArr = Nd4j.zeros(desireTensorShape);
    public final INDArray trafficNDArr = Nd4j.zeros(trafficTensorShape);
    public final INDArray stateNDArr = Nd4j.zeros(stateTensorShape);
    public final float[] netOutputs = new float[(int)numElements(outputTensorShape)];
    public final INDArray augmentRot = Nd4j.zeros(3);
    public final INDArray augmentTrans = Nd4j.zeros(3);
    public final float[]prevDesire = new float[CommonModelF2.DESIRE_LEN];
    public final float[]desireIn = new float[CommonModelF2.DESIRE_LEN];
    public final Map<String, INDArray> inputMap =  new HashMap<>();
    public final Map<String, float[]> outputMap =  new HashMap<>();
    public final Parser parser = new Parser();

    public final ParamsInterface params = ParamsInterface.getInstance();

    public static final int[] FULL_FRAME_SIZE = Camera.frameSize;
    public static INDArray fcam_intrinsics = Camera.fcam_intrinsics.dup();
    public static INDArray ecam_intrinsics = Camera.ecam_intrinsics.dup();
    public final ZMQPubHandler ph = new ZMQPubHandler();
    public final ZMQSubHandler sh = new ZMQSubHandler(true);
    public Definitions.LiveCalibrationData.Reader liveCalib;

    public long start, end, timestamp;
    public int lastFrameID = -1;
    public int firstFrameID = -1;
    public int totalFrameDrops = 0;
    public int frameDrops = 0; // per iteration
    public ModelRunner modelRunner;
    Definitions.FrameData.Reader frameData;
    Definitions.FrameBuffer.Reader msgFrameBuffer;
    public MsgCameraOdometery msgCameraOdometery = new MsgCameraOdometery();
    public MsgModelDataV2 msgModelDataV2 = new MsgModelDataV2();
    ByteBuffer imgBuffer;
    int desire;


    public ModelExecutorF2(ModelRunner modelRunner){
        this.modelRunner = modelRunner;
    }

    public boolean isIntrinsicsValid(PrimitiveList.Float.Reader intrinsics){
        // TODO: find better ways to check validity.
        return intrinsics.get(0)!=0 & intrinsics.get(2)!=0 & intrinsics.get(4)!=0 & intrinsics.get(5)!=0 & intrinsics.get(8)!=0;
    }

    public void updateCameraMatrix(PrimitiveList.Float.Reader intrinsics){
        if (!isIntrinsicsValid(intrinsics))
            return;
        for (int i=0; i<3; i++){
            for (int j=0; j<3; j++){
                fcam_intrinsics.put(i, j, intrinsics.get(i*3 + j));
            }
        }
    }

    public void updateCameraState(){
        frameData = sh.recv("roadCameraState").getRoadCameraState();
        msgFrameBuffer = sh.recv("roadCameraBuffer").getRoadCameraBuffer();
        imgBuffer = updateImageBuffer(msgFrameBuffer, imgBuffer);
    }

    public void run(){
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        INDArray netInputBuffer;

        ph.createPublishers(Arrays.asList("modelV2", "cameraOdometry"));
        sh.createSubscribers(Arrays.asList("roadCameraState", "roadCameraBuffer", "lateralPlan", "liveCalibration"));

        inputShapeMap.put("input_imgs", imgTensorShape);
        inputShapeMap.put("initial_state", stateTensorShape);
        inputShapeMap.put("desire", desireTensorShape);
        inputShapeMap.put("traffic_convention", trafficTensorShape);
        outputShapeMap.put("outputs", outputTensorShape);

        inputMap.put("initial_state", stateNDArr);
        inputMap.put("desire", desireNDArr);
        inputMap.put("traffic_convention", trafficNDArr);
        outputMap.put("outputs", netOutputs);

        modelRunner.init(inputShapeMap, outputShapeMap);
        modelRunner.warmup();

        INDArray wrapMatrix = Preprocess.getWrapMatrix(augmentRot, fcam_intrinsics, ecam_intrinsics, false, false);

        updateCameraState();
        updateCameraMatrix(frameData.getIntrinsics());

        // TODO:Clean this shit.
        ImagePrepare imagePrepare;
        boolean rgb;
        if (!getUseGPU()){
            rgb = msgFrameBuffer.getEncoding() == Definitions.FrameBuffer.Encoding.RGB;
            imagePrepare = new ImagePrepareGPU(FULL_FRAME_SIZE[0], FULL_FRAME_SIZE[1], rgb, msgFrameBuffer.getYWidth(), msgFrameBuffer.getYHeight(),
                    msgFrameBuffer.getYPixelStride(), msgFrameBuffer.getUvWidth(), msgFrameBuffer.getUvHeight(), msgFrameBuffer.getUvPixelStride(),
                    msgFrameBuffer.getUOffset(), msgFrameBuffer.getVOffset(), msgFrameBuffer.getStride());
        }
        else{
            rgb = msgFrameBuffer.getEncoding() == Definitions.FrameBuffer.Encoding.RGB;
            imagePrepare = new ImagePrepareCPU(FULL_FRAME_SIZE[0], FULL_FRAME_SIZE[1], rgb, msgFrameBuffer.getYWidth(), msgFrameBuffer.getYHeight(),
                    msgFrameBuffer.getYPixelStride(), msgFrameBuffer.getUvWidth(), msgFrameBuffer.getUvHeight(), msgFrameBuffer.getUvPixelStride(),
                    msgFrameBuffer.getUOffset(), msgFrameBuffer.getVOffset(), msgFrameBuffer.getStride());
        }

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

            for (int i=1; i<CommonModelF2.DESIRE_LEN; i++){
                if (desireIn[i] - prevDesire[i] > 0.99f)
                    desireNDArr.put(0, i, desireIn[i]);
                else
                    desireNDArr.put(0, i, 0.0f);
                prevDesire[i] = desireIn[i];
            }

            if (sh.updated("liveCalibration")) {
                liveCalib = sh.recv("liveCalibration").getLiveCalibration();
                PrimitiveList.Float.Reader rpy = liveCalib.getRpyCalib();
                for (int i = 0; i < 3; i++) {
                    augmentRot.putScalar(i, rpy.get(i));
                }
                wrapMatrix = Preprocess.getWrapMatrix(augmentRot, fcam_intrinsics, ecam_intrinsics, false, false);
            }

            netInputBuffer = imagePrepare.prepare(imgBuffer, wrapMatrix);

            inputMap.put("input_imgs", netInputBuffer);
            modelRunner.run(inputMap, outputMap);

            outs = parser.parser(netOutputs);

            for (int i=0; i<outs.state[0].length; i++)
                stateNDArr.put(0, i, outs.state[0][i]);

            // publish outputs
            timestamp = System.currentTimeMillis();
            serializeAndPublish();

            end = System.currentTimeMillis();
            // compute runtime stats.
            // skip 1st 10 reading to let it warm up.
            if (iterationNum > 10) {
                timePerIt += end - start;
                totalFrameDrops += (frameData.getFrameId() - lastFrameID) - 1;
            } else {
                firstFrameID = lastFrameID;
            }

            frameDrops = frameData.getFrameId() - lastFrameID - 1;
            lastFrameID = frameData.getFrameId();
            iterationNum++;
        }

        // dispose
        wrapMatrix.close();

        for (String inputName : inputMap.keySet()) {
            inputMap.get(inputName).close();
        }
        modelRunner.dispose();
        imagePrepare.dispose();
        ph.releaseAll();
    }

    public void init() {
        if (thread == null) {
            thread = new Thread(this, threadName);
            thread.setDaemon(false);
            thread.start();
        }
    }

    public void serializeAndPublish(){
        msgModelDataV2.fill(outs, timestamp, frameData.getFrameId(), -1, getFrameDropPercent(), getIterationRate(), -1);
        msgCameraOdometery.fill(outs, timestamp, frameData.getFrameId());

        ph.publishBuffer("modelV2", msgModelDataV2.serialize(frameDrops < 1));
        ph.publishBuffer("cameraOdometry", msgCameraOdometery.serialize(frameDrops < 1));
    };

    public long getIterationRate() {
        return timePerIt/iterationNum;
    }

    public float getFrameDropPercent() {
        return (float)100* totalFrameDrops /(lastFrameID-firstFrameID);
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
    public void start(){
        if (thread == null)
            init();
        stopped = false;
    }
}
