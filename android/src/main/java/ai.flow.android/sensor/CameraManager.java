package ai.flow.android.sensor;

import ai.flow.common.ParamsInterface;
import ai.flow.common.Path;
import ai.flow.sensor.SensorInterface;
import ai.flow.sensor.camera.MsgFrameData;
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
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import com.google.common.util.concurrent.ListenableFuture;
import messaging.ZMQPubHandler;
import org.capnproto.PrimitiveList;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutionException;


public class CameraManager extends SensorInterface {

    public ProcessCameraProvider cameraProvider;
    public Mat frame, frameCrop, frameCropContinuous;
    public String topic;
    public ZMQPubHandler ph;
    public boolean running = false;
    public int defaultFrameWidth = 1164;
    public int defaultFrameHeight = 874;
    public org.opencv.core.Size sz = new org.opencv.core.Size(defaultFrameWidth, defaultFrameHeight);
    public MsgFrameData msgFrameData = new MsgFrameData(0);
    public PrimitiveList.Float.Builder K = msgFrameData.intrinsics;
    public int frequency;
    public int frameID = 0;
    public boolean recording = false;
    public Context context;
    public ParamsInterface params = ParamsInterface.getInstance();
    public YUV2RGBProcessor imProcessor = new YUV2RGBProcessor();

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
            .setTargetResolution(new Size(1164, 874))
            .setVideoFrameRate(20)
            .setBitRate(2000_000)
            .setTargetRotation(Surface.ROTATION_90)
            .setAudioBitRate(0)
            .build();

    public CameraManager(Context context, int frequency, String topic){
        this.context = context;
        this.frequency = frequency;
        this.topic = topic;
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        frame = new Mat();
        frameCropContinuous = new Mat(874, 1164, CvType.CV_8UC3);
        msgFrameData.frameData.setNativeImageAddr(frameCropContinuous.dataAddr());
        ph = new ZMQPubHandler();
        ph.createPublisher(topic);

        loadIntrinsics();
    }

    public void loadIntrinsics(){
        if (params.exists("CameraMatrix")) {
            float[] cameraMatrix = byteToFloat(params.getBytes("CameraMatrix"));
            updateProperty("intrinsics", cameraMatrix);
        }
    }

    public static float[] byteToFloat(byte[] input) {
        float[] ret = new float[input.length / 4];
        for (int x = 0; x < input.length; x += 4) {
            ret[x / 4] = ByteBuffer.wrap(input, x, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        }
        return ret;
    }

    public void setIntrinsics(float[] intrinsics){
        K.set(0, intrinsics[0]);
        K.set(2, intrinsics[2]);
        K.set(4,intrinsics[4]);
        K.set(5, intrinsics[5]);
        K.set(8, 1f);
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
        builder.setTargetResolution(new Size(1280, 960));
        Camera2Interop.Extender<ImageAnalysis> ext = new Camera2Interop.Extender<>(builder);
        ext.setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range<>(frequency, frequency));
        ImageAnalysis imageAnalysis = builder.build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), new ImageAnalysis.Analyzer() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void analyze(@NonNull ImageProxy image) {
                imProcessor.Image2RGB(image, frame);
                if (frameCrop==null) {
                    frameCrop = frame.submat(new Rect((frame.width() - defaultFrameWidth) / 2, (frame.height() - defaultFrameHeight) / 2,
                            defaultFrameWidth, defaultFrameHeight));
                }
                frameCrop.copyTo(frameCropContinuous); // make sub-mat continuous.
                msgFrameData.frameData.setFrameId(frameID);
                ph.publishBuffer(topic, msgFrameData.serialize(true));
                image.close();
                frameID += 1;
            }
        });



        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

        CustomLifecycle lifecycle=new CustomLifecycle();
        lifecycle.doOnResume();
        lifecycle.doOnStart();
        cameraProvider.bindToLifecycle(lifecycle, cameraSelector,
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
        videoCapture.stopRecording();
        cameraProvider.unbindAll();
        ph.releaseAll();
        frame.release();
        frameCrop.release();
        running = false;
    }
}
