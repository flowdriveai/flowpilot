package ai.flow.sensor.camera;

import ai.flow.common.ParamsInterface;
import ai.flow.sensor.SensorInterface;
import messaging.ZMQPubHandler;
import org.capnproto.PrimitiveList;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;

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
    public int defaultFrameWidth;
    public int defaultFrameHeight;
    public MsgFrameData msgFrameData = new MsgFrameData(0);
    public Mat frame, frameProcessed, frameCrop, framePadded;
    public PrimitiveList.Float.Builder K = msgFrameData.intrinsics;
    public int frameID = 0;
    public ParamsInterface params = ParamsInterface.getInstance();

    public void setIntrinsics(float[] intrinsics){
        assert (intrinsics.length == 9) : "invalid intrinsic matrix length";
        for (int i=0; i<intrinsics.length; i++)
            K.set(i, intrinsics[i]);
    }

    public CameraManager(String topic, int frequency, String videoSrc, int frameWidth, int frameHeight) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        this.topic = topic;
        this.defaultFrameWidth = frameWidth;
        this.defaultFrameHeight = frameHeight;

        // start capturing from video / webcam or ip cam.
        if (videoSrc == null)
            videoSrc = "tmp"; // use a sample video file.
        try {
            capture = new VideoCapture(Integer.parseInt(videoSrc));
        } catch (Exception e){
            capture = new VideoCapture(videoSrc);
        }

        // try to get the nearest resolution to default.
        capture.set(Videoio.CAP_PROP_FRAME_WIDTH, defaultFrameWidth);
        capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, defaultFrameHeight);
        capture.set(Videoio.CAP_PROP_BUFFERSIZE, 1); // grab latest frame only.
        capture.set(Videoio.CAP_PROP_FOURCC, VideoWriter.fourcc('M', 'J', 'P', 'G'));
        capture.set(Videoio.CAP_PROP_FPS, 30);

        // init mat buffers once and reuse.
        frame = new Mat();
        framePadded = new Mat();
        frameProcessed = new Mat(defaultFrameHeight, defaultFrameWidth, CvType.CV_8UC3);
        msgFrameData.frameData.setNativeImageAddr(frameProcessed.dataAddr());

        deltaTime = (long) 1000/frequency; //ms
        loadIntrinsics();
    }

    public void processFrame(Mat frame){
        // correct color-space.
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2RGB);
        Mat currFrame = frame;

        // apply padding if image dimensions are too small.
        int padWidth = 0, padHeight = 0;
        if (frame.width() < defaultFrameWidth)
            padWidth = defaultFrameWidth - frame.width();
        if (frame.height() < defaultFrameHeight)
            padHeight = defaultFrameHeight - frame.height();

        if (padWidth != 0 || padHeight != 0){
            int top = padHeight / 2;
            int bottom = padHeight-(padHeight/2);
            int left = padWidth / 2;
            int right = padWidth-(padWidth/2);
            Core.copyMakeBorder(frame, framePadded, top, bottom, left, right, Core.BORDER_CONSTANT);
            currFrame = framePadded;
        }

        // apply cropping if image dimensions are larger.
        if (frameCrop==null) {
            frameCrop = currFrame.submat(
                        new Rect(
                                Math.abs(currFrame.width() - defaultFrameWidth) / 2,
                                Math.abs(currFrame.height() - defaultFrameHeight) / 2,
                                defaultFrameWidth, defaultFrameHeight
                                )
                        );
        }

        // cropping may make the buffer non-continuous. cannot avoid a copy here.
        frameCrop.copyTo(frameProcessed);
    }

    public void run(){
        initialized = true;
        ph.createPublisher(topic);
        long start, end, diff;
        while (!stopped){
            start = System.currentTimeMillis();
            capture.read(frame);
            end = System.currentTimeMillis();
            frameID += 1;
            processFrame(frame);
            msgFrameData.frameData.setFrameId(frameID);
            ph.publishBuffer(topic, msgFrameData.serialize());
            diff = end - start;
            if (diff < deltaTime){
                try{
                    Thread.sleep(deltaTime-diff);
                }catch (InterruptedException e){
                    ph.releaseAll();}
            }
            else if ((diff - deltaTime) > 5){
                System.out.println("[WARNING]: camera lagging by " + (diff-deltaTime) + " ms");
            }
        }
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
