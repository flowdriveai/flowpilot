package ai.flow.app.CalibrationScreens;

import ai.flow.app.FlowUI;
import ai.flow.app.SetUpScreen;
import ai.flow.calibration.CameraCalibratorIntrinsic;
import ai.flow.common.ParamsInterface;
import ai.flow.common.transformations.Camera;
import ai.flow.common.transformations.YUV2RGB;
import ai.flow.definitions.Definitions;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Blending;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import messaging.ZMQSubHandler;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static ai.flow.common.BufferUtils.MatToByte;
import static ai.flow.common.BufferUtils.byteToFloat;
import static ai.flow.sensor.messages.MsgFrameBuffer.updateImageBuffer;


public class CalibrateScreen extends ScreenAdapter {
    FlowUI appContext;
    // For camera frame receiving
    int frameWidth = Camera.frameSize[0];
    int frameHeight = Camera.frameSize[1];
    Pixmap pixelMap = new Pixmap(frameWidth, frameHeight, Pixmap.Format.RGB888);
    Texture texture = new Texture(pixelMap);
    Image texImage = new Image(texture);
    Mat imageMat;
    SpriteBatch batch = new SpriteBatch();
    Stage stageFill = new Stage(new FillViewport(frameWidth, frameHeight));
    Stage stageUI;
    TextButton btnInstructions;
    TextButton btnBack;
    Table tableMenuBar;
    Table tableProgressBar;
    ProgressBar progressBar;
    int currFrameID = -1, prevFrameID = -1;
    int numImages = 30;
    int cameraType;
    String frameDataTopic, frameBufferTopic, intrinsicParamName, distortionParamName, cameraName;
    // messaging
    ZMQSubHandler sh = new ZMQSubHandler(true);
    // calibrator object
    CameraCalibratorIntrinsic calibrator;
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
    Definitions.FrameBuffer.Reader msgFrameBuffer;
    Definitions.FrameData.Reader msgFrameData;
    ByteBuffer imgBuffer;
    YUV2RGB yuv2RGB = null;
    ParamsInterface params = ParamsInterface.getInstance();

    public CalibrateScreen(FlowUI appContext, int cameraType, boolean enableCancel) {
        this.appContext = appContext;

        this.cameraType = cameraType;
        if (cameraType == Camera.CAMERA_TYPE_WIDE){
            frameDataTopic = "wideRoadCameraState";
            frameBufferTopic = "wideRoadCameraBuffer";
            intrinsicParamName = "WideCameraMatrix";
            distortionParamName = "WideDistortionCoefficients";
            cameraName = "wideRoadCamera";
        } else if (cameraType == Camera.CAMERA_TYPE_ROAD) {
            frameDataTopic = "roadCameraState";
            frameBufferTopic = "roadCameraBuffer";
            intrinsicParamName = "CameraMatrix";
            distortionParamName = "DistortionCoefficients";
            cameraName = "roadCamera";
        }

        pixelMap.setBlending(Blending.None);
        calibrator = new CameraCalibratorIntrinsic(9, 6); // 6 by 9 chessboard :))
        stageUI = new Stage(new StretchViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));

        btnBack = new TextButton("Cancel", appContext.skin);
        btnBack.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (btnBack.isDisabled())
                    return;
                appContext.setScreen(appContext.settingsScreen);
            }
        });
        btnBack.setDisabled(!enableCancel);

        btnInstructions = new TextButton("Instructions", appContext.skin);
        btnInstructions.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                appContext.setScreen(new CalibrationInfo(appContext, cameraType, enableCancel));
            }
        });

        progressBar = new ProgressBar(0, numImages, 1, false, appContext.skin);
        tableProgressBar = new Table();
        tableProgressBar.setFillParent(true);
        tableProgressBar.top();
        tableProgressBar.row().top().expandX().fillX();
        tableProgressBar.add(progressBar);

        tableMenuBar = new Table();
        tableMenuBar.setFillParent(true);
        tableMenuBar.bottom();
        tableMenuBar.row().fillX().expandX().fillY();
        tableMenuBar.add(btnBack);
        tableMenuBar.add(btnInstructions);

        stageFill.addActor(texImage);
        stageUI.addActor(tableProgressBar);
        stageUI.addActor(tableMenuBar);

        sh.createSubscribers(Arrays.asList(frameBufferTopic, frameDataTopic));
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stageUI);
    }

    public void updateCamera(){
        // handles receiving, rendering and converting to rgb of images.
        if (cameraType == Camera.CAMERA_TYPE_ROAD){
            msgFrameBuffer = sh.recv(frameBufferTopic).getRoadCameraBuffer();
            msgFrameData = sh.recv(frameDataTopic).getRoadCameraState();
        }
        else{
            msgFrameBuffer = sh.recv(frameBufferTopic).getWideRoadCameraBuffer();
            msgFrameData = sh.recv(frameDataTopic).getWideRoadCameraState();
        }
        currFrameID = msgFrameData.getFrameId();
        imgBuffer = updateImageBuffer(msgFrameBuffer, imgBuffer);

        boolean rgb = msgFrameBuffer.getEncoding() == Definitions.FrameBuffer.Encoding.RGB;

        if (rgb){
            pixelMap.setPixels(imgBuffer);
            texture.draw(pixelMap, 0, 0);
            imageMat = new Mat(Camera.frameSize[1], Camera.frameSize[0], CvType.CV_8UC3, imgBuffer);
        }
        else {
            if (yuv2RGB == null)
                yuv2RGB = new YUV2RGB(Camera.frameSize[0], Camera.frameSize[1], Camera.frameSize[1], 2);
            yuv2RGB.run(imgBuffer);
            pixelMap.setPixels(yuv2RGB.getRGBBuffer());
            texture.draw(pixelMap, 0, 0);
            imageMat = new Mat(Camera.frameSize[1], Camera.frameSize[0], CvType.CV_8UC3, yuv2RGB.getRGBBuffer());
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (sh.updated(frameBufferTopic)) {
            updateCamera();
        }

        // images are processed in background to prevent annoying lag in UI camera stream.
        if (currFrameID % 20 == 0 && currFrameID != prevFrameID && executor.getActiveCount() < executor.getCorePoolSize() && !executor.isShutdown()) {
            prevFrameID = currFrameID;
            executor.submit(() -> {
                // if current imageBuffer is accepted, new image points get added to calibrator object.
                calibrator.addImage(imageMat);
                imageMat.release();
            });
        }

        progressBar.setValue(calibrator.currentImagePoints()); // update progress bar

        // once all images are collected, these are passed to calibrator
        // which processes images in background and updates calibration status.
        if (calibrator.imagePoints.size() == numImages) {
            // shutdown executor since no new images would be processed.
            executor.shutdown();
            try {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // run actual calibration. TODO: add a wait spinner instead of hanging whole UI.
            calibrator.calibrate();
            if (calibrator.status == CameraCalibratorIntrinsic.STATUS_SUCCESS) {
                byte[] cameraMatrixBuffer = MatToByte(calibrator.cameraMatrix);
                byte[] distortionCoefficientsBuffer = MatToByte(calibrator.distortionCoefficients);

                params.put(intrinsicParamName, cameraMatrixBuffer);
                params.put(distortionParamName, distortionCoefficientsBuffer);

                // new camera matrix would be published in FrameData.
                appContext.sensors.get(cameraName).updateProperty("intrinsics", byteToFloat(cameraMatrixBuffer));
            }
            else
                System.err.println("[WARN]: Camera not calibrated.");  // TODO display in GUI.
            appContext.setScreen(new SetUpScreen(appContext));
            return;
        }

        stageFill.getViewport().apply();
        stageFill.act(Gdx.graphics.getDeltaTime());
        stageFill.draw();

        stageUI.getViewport().apply();
        stageUI.act(Gdx.graphics.getDeltaTime());
        stageUI.draw();
    }

    @Override
    public void resize(int width, int height) {
        stageFill.getViewport().update(width, height);
        stageUI.getViewport().update(width, height);
    }

    @Override
    public void dispose() {
        Gdx.input.setInputProcessor(null);
        texture.dispose();
        pixelMap.dispose();
        stageFill.dispose();
        batch.dispose();
        imageMat.release();
        if (yuv2RGB != null)
            yuv2RGB.dispose();
        sh.releaseAll();
        params.dispose();
    }
}
