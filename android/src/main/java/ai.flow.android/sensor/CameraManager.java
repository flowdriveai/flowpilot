package ai.flow.android.sensor;

import ai.flow.common.ParamsInterface;
import ai.flow.common.Path;
import ai.flow.common.transformations.Camera;
import ai.flow.definitions.Definitions;
import ai.flow.modeld.messages.MsgFrameData;
import ai.flow.sensor.SensorInterface;
import ai.flow.sensor.messages.MsgFrameBuffer;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.interop.Camera2Interop;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.VideoCapture;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import com.google.common.util.concurrent.ListenableFuture;
import messaging.ZMQPubHandler;
import org.capnproto.PrimitiveList;
import org.opencv.core.Core;

import java.io.File;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import static ai.flow.android.sensor.Utils.fillYUVBuffer;
import static ai.flow.common.BufferUtils.byteToFloat;
import static ai.flow.common.transformations.Camera.fcamIntrinsicParam;


public class CameraManager extends SensorInterface {

    public ProcessCameraProvider cameraProvider;
    public String frameDataTopic, frameBufferTopic;
    public ZMQPubHandler ph;
    public boolean running = false;
    public int W = Camera.frameSize[0];
    public int H = Camera.frameSize[1];
    public MsgFrameData msgFrameData = new MsgFrameData();
    public MsgFrameBuffer msgFrameBuffer;
    public PrimitiveList.Float.Builder K = msgFrameData.intrinsics;
    public int frequency;
    public int frameID = 0;
    public boolean recording = false;
    public Context context;
    public ParamsInterface params = ParamsInterface.getInstance();
    public Fragment lifeCycleFragment;
    ByteBuffer yuvBuffer;

    static class CustomLifecycle implements LifecycleOwner {

        private final LifecycleRegistry mLifecycleRegistry;
        CustomLifecycle() {
            mLifecycleRegistry = new LifecycleRegistry(this);
            mLifecycleRegistry.setCurrentState(Lifecycle.State.CREATED);
        }

        void doOnResume() {
            mLifecycleRegistry.setCurrentState(Lifecycle.State.RESUMED);
        }

        void doOnStart() {
            mLifecycleRegistry.setCurrentState(Lifecycle.State.STARTED);
        }

        @NonNull
        public Lifecycle getLifecycle() {
            return mLifecycleRegistry;
        }
    }

    @SuppressLint("RestrictedApi")
    public VideoCapture videoCapture = new VideoCapture.Builder()
            .setTargetResolution(new Size(Camera.frameSize[0], Camera.frameSize[1]))
            .setVideoFrameRate(20)
            .setBitRate(2000_000)
            .setTargetRotation(Surface.ROTATION_90)
            .setAudioBitRate(0)
            .build();

    public CameraManager(Context context, int frequency, int cameraType){
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        this.context = context;
        this.frequency = frequency;

        if (cameraType == Camera.CAMERA_TYPE_WIDE){
            this.frameDataTopic = "wideRoadCameraState";
            this.frameBufferTopic = "wideRoadCameraBuffer";
        } else if (cameraType == Camera.CAMERA_TYPE_ROAD) {
            this.frameDataTopic = "roadCameraState";
            this.frameBufferTopic = "roadCameraBuffer";
        }

        msgFrameBuffer = new MsgFrameBuffer(W * H * 3/2, cameraType);
        yuvBuffer = msgFrameBuffer.frameBuffer.getImage().asByteBuffer();
        msgFrameBuffer.frameBuffer.setEncoding(Definitions.FrameBuffer.Encoding.YUV);
        msgFrameBuffer.frameBuffer.setFrameHeight(H);
        msgFrameBuffer.frameBuffer.setFrameWidth(W);

        ph = new ZMQPubHandler();
        ph.createPublishers(Arrays.asList(frameDataTopic, frameBufferTopic));

        loadIntrinsics();
    }

    public void loadIntrinsics(){
        if (params.exists(fcamIntrinsicParam)) {
            float[] cameraMatrix = byteToFloat(params.getBytes(fcamIntrinsicParam));
            updateProperty("intrinsics", cameraMatrix);
        }
    }

    public void setIntrinsics(float[] intrinsics){
        K.set(0, intrinsics[0]);
        K.set(2, intrinsics[2]);
        K.set(4,intrinsics[4]);
        K.set(5, intrinsics[5]);
        K.set(8, 1f);
    }

    public void setLifeCycleFragment(Fragment lifeCycleFragment){
        this.lifeCycleFragment = lifeCycleFragment;
    }

    public void start() {
        if (running)
            return;
        running = true;
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    cameraProvider = cameraProviderFuture.get();
                    bindImageAnalysis(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(context));
    }

    @SuppressLint({"RestrictedApi", "UnsafeOptInUsageError"})
    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        builder.setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST);
        builder.setTargetResolution(new Size(W, H));
        Camera2Interop.Extender<ImageAnalysis> ext = new Camera2Interop.Extender<>(builder);
        ext.setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range<>(frequency, frequency));
        ImageAnalysis imageAnalysis = builder.build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), new ImageAnalysis.Analyzer() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void analyze(@NonNull ImageProxy image) {

                fillYUVBuffer(image, yuvBuffer);

                ImageProxy.PlaneProxy yPlane = image.getPlanes()[0];

                msgFrameBuffer.frameBuffer.setYWidth(W);
                msgFrameBuffer.frameBuffer.setYHeight(H);
                msgFrameBuffer.frameBuffer.setYPixelStride(yPlane.getPixelStride());
                msgFrameBuffer.frameBuffer.setUvWidth(W /2);
                msgFrameBuffer.frameBuffer.setUvHeight(H /2);
                msgFrameBuffer.frameBuffer.setUvPixelStride(image.getPlanes()[1].getPixelStride());
                msgFrameBuffer.frameBuffer.setUOffset(W * H);
                if (image.getPlanes()[1].getPixelStride() == 2)
                    msgFrameBuffer.frameBuffer.setVOffset(W * H +1);
                else
                    msgFrameBuffer.frameBuffer.setVOffset(W * H + W * H /4);
                msgFrameBuffer.frameBuffer.setStride(yPlane.getRowStride());

                msgFrameData.frameData.setFrameId(frameID);

                ph.publishBuffer(frameDataTopic, msgFrameData.serialize(true));
                ph.publishBuffer(frameBufferTopic, msgFrameBuffer.serialize(true));

                image.close();
                frameID += 1;
            }
        });

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

        CustomLifecycle lifecycle=new CustomLifecycle();
        lifecycle.doOnResume();
        lifecycle.doOnStart();
        cameraProvider.bindToLifecycle(lifeCycleFragment.getViewLifecycleOwner(), cameraSelector,
                imageAnalysis, videoCapture);
    }

    @SuppressLint("RestrictedApi")
    public void startRecordCamera() {
        if (recording)
            return ;
        recording = true;
        @SuppressLint("SdCardPath") File movieDir = new File(Path.getVideoStorageDir());

        if (!movieDir.exists()) {
            movieDir.mkdirs();
        }

        String videoFileName = new SimpleDateFormat("yyyy-MM-dd--HH-mm-ss--SSS").format(new Date());
        String vidFilePath = movieDir.getAbsolutePath() + "/" + videoFileName + ".mp4";

        File vidFile = new File(vidFilePath);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Do something
        }

        videoCapture.startRecording(
                new VideoCapture.OutputFileOptions.Builder(vidFile).build(),
                ContextCompat.getMainExecutor(context),
                new VideoCapture.OnVideoSavedCallback() {
                    @Override
                    public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
                        System.out.println("[INFO] Video Saved: " + vidFile.getName());
                    }
                    @Override
                    public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
                        System.err.println("[WARNING] Video Save Error: " + vidFile.getName());
                        System.out.println(message);
                    }
                }
        );
    }

    @Override
    public void record(boolean shouldRecord) {
        if (shouldRecord)
            startRecordCamera();
        else
            stopRecordCamera();
    }

    @SuppressLint("RestrictedApi")
    public void stopRecordCamera() {
        if (!recording)
            return;
        videoCapture.stopRecording();
        recording = false;
    }

    @Override
    public void updateProperty(String property, float[] value) {
        if (property.equals("intrinsics")){
            assert value.length == 9 : "invalid intrinsic matrix buffer length";
            setIntrinsics(value);
        }
    }

    public boolean isRunning() {
        return running;
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void stop() {
        // TODO: add pause/resume functionality
        if (!running)
            return;
        videoCapture.stopRecording();
        cameraProvider.unbindAll();
        running = false;
    }

    @Override
    public void dispose(){
        stop();
        ph.releaseAll();
    }
}
