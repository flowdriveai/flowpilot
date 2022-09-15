package ai.flow.sensor.camera;

import ai.flow.common.ParamsInterface;
import ai.flow.sensor.SensorInterface;
import messaging.ZMQPubHandler;
import org.capnproto.PrimitiveList;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CameraManager extends SensorInterface implements Runnable {
    public Thread thread;
    public boolean stopped = false;
    public boolean initialized = false;
    public VideoCapture capture;
    public static ZMQPubHandler ph = new ZMQPubHandler();
    public String topic;
    public long deltaTime;
    public int defaultFrameWidth = 1164;
    public int defaultFrameHeight = 874;
    public MsgFrameData msgFrameData = new MsgFrameData(0);
    ByteBuffer imageBuffer = ByteBuffer.allocateDirect(defaultFrameWidth*defaultFrameHeight*3);
    public Mat frame;
    public PrimitiveList.Float.Builder K = msgFrameData.intrinsics;
    public int frameID = 0;
    public ParamsInterface params = ParamsInterface.getInstance();

    public void setIntrinsics(float[] intrinsics){
        assert (intrinsics.length == 9) : "invalid intrinsic matrix length";
        for (int i=0; i<intrinsics.length; i++)
            K.set(i, intrinsics[i]);
    }

    public CameraManager(String topic, int frequency) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        this.topic = topic;
        capture = new VideoCapture("tmp");
        //capture = new VideoCapture("/storage/emulated/0/Android/data/ai.flow.android/files/tmp");
        msgFrameData.setImageAddress(imageBuffer);
        frame = new Mat(874, 1164, CvType.CV_8UC3, msgFrameData.getImageBuffer());
        deltaTime = (long) 1000/frequency; //ms
        loadIntrinsics();
    }

    public void run(){
        initialized = true;
        ph.createPublisher(topic);
        while (!stopped){
            capture.read(frame);
            frameID += 1;
            msgFrameData.frameData.setFrameId(frameID);
            Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2RGB);
            ph.publishBuffer(topic, msgFrameData.serialize());
           try{
                Thread.sleep(deltaTime);
            }catch (InterruptedException e){
                ph.releaseAll();}
        }
    }

    public void cloneByteBuffer(ByteBuffer source, ByteBuffer target) {
        int sourceP = source.position();
        int sourceL = source.limit();
        target.put(source);
        target.flip();
        source.position(sourceP);
        source.limit(sourceL);
    }

    public boolean isRunning() {
        return this.initialized;
    }

    public static float[] byteToFloat(byte[] input) {
        float[] ret = new float[input.length / 4];
        for (int x = 0; x < input.length; x += 4) {
            ret[x / 4] = ByteBuffer.wrap(input, x, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        }
        return ret;
    }

    public void loadIntrinsics(){
        if (params.exists("CameraMatrix")) {
            float[] cameraMatrix = byteToFloat(params.getBytes("CameraMatrix"));
            updateProperty("intrinsics", cameraMatrix);
        }
    }

    @Override
    public synchronized void updateProperty(String property, float[] value) {
        if (property.equals("intrinsics")){
            assert value.length == 9 : "invalid intrinsic matrix buffer length";
            setIntrinsics(value);
        }
    }

    public void start() {
        if (thread == null) {
            thread = new Thread(this, "camerad");
            thread.setDaemon(false);
            thread.start();
        }
    }

    public void stop() {
        stopped = true;
        ph.releaseAll();
        capture.release();
        frame.release();
    }
}
