package ai.flow.sensor.camera;

import ai.flow.common.ParamsInterface;
import ai.flow.common.transformations.Camera;
import ai.flow.common.transformations.RGB2YUV;
import ai.flow.definitions.Definitions;
import ai.flow.modeld.messages.MsgFrameData;
import ai.flow.sensor.SensorInterface;
import ai.flow.sensor.messages.MsgFrameBuffer;
import messaging.ZMQPubHandler;
import org.capnproto.PrimitiveList;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static ai.flow.common.BufferUtils.bufferFromAddress;
import static ai.flow.common.BufferUtils.byteToFloat;
import static ai.flow.common.transformations.Camera.CAMERA_TYPE_ROAD;
import static ai.flow.common.transformations.Camera.fcamIntrinsicParam;

public class CameraManager extends SensorInterface implements Runnable {
    public Thread thread;
    public boolean stopped = false;
    public boolean exit = false;
    public boolean initialized = false;
    private final boolean useYUV = true;
    public VideoCapture capture;
    public static ZMQPubHandler ph = new ZMQPubHandler();
    public long deltaTime;
    public int defaultFrameWidth;
    public int defaultFrameHeight;
    public MsgFrameData msgFrameData = new MsgFrameData(CAMERA_TYPE_ROAD);
    public MsgFrameBuffer msgFrameBuffer;
    public Mat frame, frameProcessed, frameCrop, framePadded;
    public PrimitiveList.Float.Builder K = msgFrameData.intrinsics;
    public int frameID = 0;
    public ParamsInterface params = ParamsInterface.getInstance();
    public String frameDataTopic = null;
    public String frameBufferTopic = null;
    public String cameraParamName = null;
    public RGB2YUV rgb2yuv;
    ByteBuffer yuvBuffer, rgbBuffer;

    public void setIntrinsics(float[] intrinsics){
        assert (intrinsics.length == 9) : "invalid intrinsic matrix length";
        for (int i=0; i<intrinsics.length; i++) {
            K.set(i, intrinsics[i]);
        }
    }

    public CameraManager(int cameraType, int frequency, String videoSrc, int frameWidth, int frameHeight) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        if (cameraType == Camera.CAMERA_TYPE_WIDE){
            frameDataTopic = "wideRoadCameraState";
            frameBufferTopic = "wideRoadCameraBuffer";
            cameraParamName = "WideCameraMatrix";
        } else if (cameraType == CAMERA_TYPE_ROAD) {
            frameDataTopic = "roadCameraState";
            frameBufferTopic = "roadCameraBuffer";
            cameraParamName = fcamIntrinsicParam;
        }

        msgFrameBuffer = new MsgFrameBuffer(frameWidth*frameHeight*3/2, cameraType);

        this.defaultFrameWidth = frameWidth;
        this.defaultFrameHeight = frameHeight;

        if (useYUV) {
            rgb2yuv = new RGB2YUV(null, null, frameHeight, frameWidth);
        }

        // start capturing from video / webcam or ip cam.
        if (videoSrc == null)
            return; // use stream from external custom source.
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
        if (useYUV) {
            yuvBuffer = msgFrameBuffer.frameBuffer.getImage().asByteBuffer();
            msgFrameBuffer.frameBuffer.setEncoding(Definitions.FrameBuffer.Encoding.YUV);
        }
        else {
            msgFrameBuffer.frameBuffer.setEncoding(Definitions.FrameBuffer.Encoding.RGB);
            msgFrameBuffer.setImageBufferAddress(frameProcessed.dataAddr());
        }
        msgFrameBuffer.frameBuffer.setFrameHeight(frameHeight);
        msgFrameBuffer.frameBuffer.setFrameWidth(frameWidth);
        msgFrameBuffer.frameBuffer.setYHeight(frameHeight);
        msgFrameBuffer.frameBuffer.setYWidth(frameWidth);
        msgFrameBuffer.frameBuffer.setYPixelStride(1);
        msgFrameBuffer.frameBuffer.setUvWidth(frameWidth/2);
        msgFrameBuffer.frameBuffer.setUvHeight(frameHeight/2);
        msgFrameBuffer.frameBuffer.setUvPixelStride(2);
        msgFrameBuffer.frameBuffer.setUOffset(frameHeight*frameWidth);
        msgFrameBuffer.frameBuffer.setVOffset(frameHeight*frameWidth+1);
        msgFrameBuffer.frameBuffer.setStride(frameWidth);

        rgbBuffer = bufferFromAddress(frameProcessed.dataAddr(), frameHeight*frameWidth*3);

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
        if (frameProcessed==null)
            return;
        ph.createPublishers(Arrays.asList(frameDataTopic, frameBufferTopic));
        long start, end, diff;
        while (!exit){
            if (stopped){
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                continue;
            }
            start = System.currentTimeMillis();
            capture.read(frame);
            end = System.currentTimeMillis();
            frameID += 1;
            processFrame(frame);
            if (useYUV) {
                rgb2yuv.run(rgbBuffer);
                rgb2yuv.read_buffer(yuvBuffer);
            }
            msgFrameData.frameData.setFrameId(frameID);
            ph.publishBuffer(frameDataTopic, msgFrameData.serialize(true));
            ph.publishBuffer(frameBufferTopic, msgFrameBuffer.serialize(true));
            diff = end - start;
            if (diff < deltaTime){
                try{
                    Thread.sleep(deltaTime-diff);
                }catch (InterruptedException e){
                    ph.releaseAll();}
            }
            else if ((diff - deltaTime) > 5){
                System.out.println("[WARNING]: " + frameDataTopic + " camera lagging by " + (diff-deltaTime) + " ms");
            }
        }
    }

    public boolean isRunning() {
        return this.initialized;
    }

    public void loadIntrinsics(){
        if (params.exists(cameraParamName)) {
            float[] cameraMatrix = byteToFloat(params.getBytes(cameraParamName));
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
        stopped = false;
        if (thread == null) {
            thread = new Thread(this, "camerad:" + frameDataTopic);
            thread.setDaemon(false);
            thread.start();
        }
    }

    public void stop() {
        stopped = true;
    }

    public void dispose(){
        exit = true;
        stopped = true;
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        thread = null;
        ph.releaseAll();
        if (capture!=null)
            capture.release();
        if (frame!=null)
            frame.release();
    }
}