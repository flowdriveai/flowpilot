package ai.flow.app.CalibrationScreens;

import ai.flow.app.FlowUI;
import ai.flow.app.SetUpScreen;
import ai.flow.calibration.CameraCalibratorIntrinsic;
import ai.flow.common.transformatons.Camera;
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
import org.opencv.core.Mat;

import java.nio.ByteBuffer;
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
    // messaging
    ZMQSubHandler sh = new ZMQSubHandler(true);
    // calibrator object
    CameraCalibratorIntrinsic calibrator;
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
    Definitions.FrameBuffer.Reader msgFrameBuffer;
    ByteBuffer imgBuffer;

    public CalibrateScreen(FlowUI appContext, boolean enableCancel) {
        this.appContext = appContext;
        pixelMap.setBlending(Blending.None);
        calibrator = new CameraCalibratorIntrinsic(9, 6, appContext.params); // 6 by 9 chessboard :))
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
                appContext.setScreen(new CalibrationInfo(appContext, enableCancel));
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

        sh.createSubscriber("wideRoadCameraBuffer");
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stageUI);
    }

    public void updateCamera(){
        msgFrameBuffer = sh.recv("wideRoadCameraBuffer").getWideRoadCameraBuffer();
        updateImageBuffer(msgFrameBuffer, imgBuffer);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (sh.updated("roadCameraState")) {
            updateCamera();
        }

        // images are processed in background to prevent annoying lag in UI camera stream.
        if (currFrameID % 20 == 0 && currFrameID != prevFrameID && executor.getActiveCount() < executor.getCorePoolSize() && !executor.isShutdown()) {
            prevFrameID = currFrameID;
            executor.submit(() -> {
                // if current imageBuffer is accepted, new image points get added to calibrator object.
                calibrator.addImage(imageMat);
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

                appContext.params.put("CameraMatrix", cameraMatrixBuffer);
                appContext.params.put("DistortionCoefficients", distortionCoefficientsBuffer);

                // new camera matrix would be published in FrameData.
                appContext.sensors.get("roadCamera").updateProperty("intrinsics", byteToFloat(cameraMatrixBuffer));
            }
            else
                System.err.println("[WARN]: Camera not calibrated.");
            appContext.setScreen(new SetUpScreen(appContext)); // TODO display in GUI.
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
        sh.releaseAll();
    }
}
