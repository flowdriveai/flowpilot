package ai.flow.app;

import ai.flow.app.helpers.GifDecoder;
import ai.flow.app.helpers.Utils;
import ai.flow.common.Path;
import ai.flow.common.transformations.Camera;
import ai.flow.definitions.Definitions;
import ai.flow.modeld.CommonModel;
import ai.flow.modeld.DesireEnum;
import ai.flow.modeld.ParsedOutputs;
import ai.flow.modeld.Preprocess;
import ai.flow.modeld.messages.MsgModelDataV2;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import messaging.ZMQPubHandler;
import messaging.ZMQSubHandler;
import org.capnproto.PrimitiveList;
import org.nd4j.linalg.api.memory.MemoryWorkspace;
import org.nd4j.linalg.api.memory.conf.WorkspaceConfiguration;
import org.nd4j.linalg.api.memory.enums.AllocationPolicy;
import org.nd4j.linalg.api.memory.enums.LearningPolicy;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.Locale;

import static ai.flow.app.helpers.Utils.getImageButton;
import static ai.flow.app.helpers.Utils.loadTextureMipMap;
import static ai.flow.sensor.messages.MsgFrameBuffer.updateImageBuffer;


public class OnRoadScreen extends ScreenAdapter {
    // avoid GC triggers.
    final WorkspaceConfiguration wsConfig = WorkspaceConfiguration.builder()
            .policyAllocation(AllocationPolicy.STRICT)
            .policyLearning(LearningPolicy.FIRST_LOOP)
            .build();
    FlowUI appContext;
    // For camera frame receiving
    Pixmap pixelMap;
    Texture texture;
    //batch
    SpriteBatch batch;
    // ui
    Stage stageFill, stageUI, stageAlert, stageSettings;
    OrthographicCamera cameraModel, cameraAlertBox;
    Image texImage;
    int defaultDrawLength = 30;
    int drawResolution = 1;
    float fadeStrength = 0.03f;
    float minZ = 0.2f;
    float minLeadProb = 0.5f;
    float leadDrawScale = 6f;
    float borderWidth = 30;
    float expandedBorderWidth = 600;
    int defaultImageWidth = Camera.frameSize[0];
    int defaultImageHeight = Camera.frameSize[1];
    // draw array buffers
    INDArray[] path, lane0, lane1, lane2, lane3;
    INDArray[] edge0, edge1;
    INDArray lead1s, lead2s, lead3s;
    INDArray augmentRot = Nd4j.zeros(1, 3);
    INDArray augmentTrans = Nd4j.zeros(1, 3);
    int[] colorPath = {0, 162, 255};
    int[] colorLanes = {220, 220, 220};
    int[] colorEdges = {255, 0, 0};
    int[] colorLead = {196, 196, 196};
    int[] colorBorder;
    int[] colorBorderUserPrompt = {255, 111, 0};
    int[] colorBorderUserCritical = {222, 15, 15};
    int[] colorBorderNormal = {43, 143, 40};
    int[] colorBorderInactive = {17, 33, 49};
    float[] colorSettingsBar = {46f/255, 46f/255, 46f/255};
    // comm params
    boolean modelAlive, controlsAlive;
    ZMQSubHandler sh;
    ZMQPubHandler ph;
    Definitions.ControlsState.Reader controlState;
    String modelTopic = "modelV2";
    String cameraTopic = "wideRoadCameraState";
    String cameraBufferTopic = "wideRoadCameraBuffer";
    String calibrationTopic = "liveCalibration";
    String desireTopic = "pulseDesire";
    String carStateTopic = "carState";
    String controlsStateTopic = "controlsState";
    String canTopic = "can";

    Label velocityLabel, velocityUnitLabel, alertText1, alertText2, maxCruiseSpeedLabel, dateLabel, vesrionLabel;
    Table velocityTable, maxCruiseTable, alertTable, infoTable, offRoadTable, rootTable, offRoadRootTable;
    Stack statusLabelTemp, statusLabelCan, statusLabelOnline, maxCruise;
    ImageButton settingsButton;
    ParsedOutputs parsed = new ParsedOutputs();
    int canErrCount = 0;
    int canErrCountPrev = 0;
    float uiWidth = 1280;
    float uiHeight = 640;
    float settingsBarWidth;
    INDArray K = Camera.ecam_intrinsics.dup();
    boolean cameraMatrixUpdated = false;
    boolean isMetric;
    boolean laneLess;
    ByteBuffer imgBuffer;
    NV12Renderer nv12Renderer;
    Definitions.FrameBuffer.Reader msgframeBuffer;
    Definitions.FrameData.Reader msgframeData;
    Animation<TextureRegion> animationNight, animationNoon, animationSunset;
    float elapsed;
    boolean isF3;

    public static class StatusColors{
        public static final float[] colorStatusGood = {255/255f, 255/255f, 255/255f};
        public static final float[] colorStatusCritical = {201/255f, 34/255f, 49/255f};
        public static final float[] colorStatusWarn = {218/255f, 202/255f, 37/255f};
    }

    public void updateAugmentVectors(Definitions.LiveCalibrationData.Reader liveCalib){
        PrimitiveList.Float.Reader rpy = liveCalib.getRpyCalib();
        for (int i=0; i<3; i++)
            augmentRot.put(0, i, rpy.get(i));
    }

    public Stack getStatusLabel(String text){
        Image borderTexture = new Image(new Texture(Gdx.files.absolute(Path.internal("selfdrive/assets/icons/rounded-border.png"))));
        Image statusTexture = new Image(loadTextureMipMap("selfdrive/assets/icons/status_label.png"));
        Label textLabel = new Label(text, appContext.skin, "default-font-bold", "white");
        textLabel.setAlignment(Align.center);
        return new Stack(borderTexture, statusTexture, textLabel);
    }

    public void updateStatusLabel(Stack statusLabel, float[] color){
        statusLabel.getChild(1).setColor(color[0], color[1], color[2], 0.8f);
    }

    public void updateStatusLabel(Stack statusLabel, String text, float[] color){
        statusLabel.getChild(1).setColor(color[0], color[1], color[2], 0.8f);
        Label label = (Label)statusLabel.getChild(2);
        label.setText(text);
    }

    public Stack getMaxVelocityLabel(){
        Image bg = new Image(new Texture(Gdx.files.absolute(Path.internal("selfdrive/assets/icons/max_cruise.png"))));
        bg.setColor(0.5f, 0.5f, 0.5f, 0.8f);
        Table table = new Table();
        Label maxLabel = new Label("MAX", appContext.skin, "default-font", "white");
        maxLabel.setColor(0.8f, 0.8f, 0.8f, 1f);
        maxCruiseSpeedLabel = new Label("N/A", appContext.skin, "default-font-bold-med", "white");
        maxLabel.setAlignment(Align.top);
        maxCruiseSpeedLabel.setAlignment(Align.bottom);
        table.add(maxLabel).padTop(2);
        table.row();
        table.add(maxCruiseSpeedLabel).padBottom(2);
        return new Stack(bg, table);
    }

    @SuppressWarnings("NewApi")
    public OnRoadScreen(FlowUI appContext) {
        this.appContext = appContext;

        isF3 = appContext.params.existsAndCompare("F3", true);
        if (!isF3){
            cameraTopic = "roadCameraState";
            cameraBufferTopic = "roadCameraBuffer";
            K = Camera.fcam_intrinsics.dup();
        }

        batch = new SpriteBatch();
        pixelMap = new Pixmap(defaultImageWidth, defaultImageHeight, Pixmap.Format.RGB888);
        pixelMap.setBlending(Pixmap.Blending.None);
        texture = new Texture(pixelMap);
        settingsBarWidth = uiWidth / 3f * Gdx.graphics.getHeight() / Gdx.graphics.getWidth();

        rootTable = new Table();
        rootTable.setFillParent(true);
        rootTable.align(Align.left);

        infoTable = new Table();
        Utils.setTableColor(infoTable, colorSettingsBar[0], colorSettingsBar[1], colorSettingsBar[2], 1);

        rootTable.add(infoTable).expandY();

        velocityTable = new Table();
        velocityTable.setFillParent(true);
        velocityTable.align(Align.top);
        velocityTable.padTop(20);

        maxCruiseTable = new Table();
        maxCruiseTable.setFillParent(true);
        maxCruiseTable.align(Align.topLeft);
        maxCruiseTable.padTop(20);

        alertTable = new Table();
        alertTable.setFillParent(true);
        alertTable.align(Align.bottom);
        alertTable.padBottom(100);

        offRoadRootTable = new Table();
        offRoadRootTable.setFillParent(true);
        offRoadRootTable.align(Align.left);

        offRoadTable = new Table();
        offRoadTable.setBackground(Utils.createRoundedRectangle(900, 550, 20, new Color(0.18f, 0.18f, 0.18f, 0.7f)));

        DateTimeFormatter f = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.US);
        dateLabel = new Label(LocalDateTime.now().format(f),  appContext.skin, "default-font", "white");

        String version = appContext.params.exists("Version") ? "flowpilot v" + appContext.params.getString("Version") : "";
        vesrionLabel = new Label(version,  appContext.skin, "default-font", "white");
        offRoadRootTable.add(dateLabel).align(Align.topLeft).padTop(15);
        offRoadRootTable.add(vesrionLabel).padTop(15).align(Align.topRight);
        offRoadRootTable.row();
        offRoadRootTable.add(offRoadTable).colspan(2).align(Align.left).padTop(10);

        rootTable.add(offRoadRootTable).padLeft(20);

        cameraModel = new OrthographicCamera(defaultImageWidth, defaultImageHeight);
        cameraModel.setToOrtho(true, defaultImageWidth, defaultImageHeight);
        cameraModel.update();

        stageSettings = new Stage(new StretchViewport(uiWidth, uiHeight));

        cameraAlertBox = new OrthographicCamera(uiWidth, uiHeight);
        cameraAlertBox.setToOrtho(false, uiWidth, uiHeight);
        cameraAlertBox.translate(-settingsBarWidth, 0, 0);
        cameraAlertBox.update();

        // used to maintain aspect ratio of stream
        stageFill = new Stage(new FillViewport(defaultImageWidth, defaultImageHeight));
        // used to draw UI components with respect to screen dimensions.
        stageUI = new Stage(new FitViewport(uiWidth, uiHeight));
        // used to draw alert messages.
        stageAlert = new Stage(new ScreenViewport());

        velocityLabel = new Label("", appContext.skin, "default-font-bold-large", "white");
        velocityUnitLabel = new Label("", appContext.skin, "default-font", "white");
        isMetric = appContext.params.existsAndCompare("IsMetric", true);

        if (appContext.params.exists("EndToEndToggle"))
            laneLess = appContext.params.getBool("EndToEndToggle");
        else
            laneLess = false;

        alertText1 = new Label("Flowpilot Unavailable", appContext.skin, "default-font-bold-med", "white");
        alertText2 = new Label("Waiting for controlsd to start", appContext.skin, "default-font", "white");

        texImage = new Image(texture);

        maxCruise = getMaxVelocityLabel();
        maxCruiseTable.add(maxCruise).align(Align.left).padLeft(30);

        velocityTable.add(velocityLabel).align(Align.top);
        velocityTable.row();
        velocityTable.add(velocityUnitLabel).fillY().align(Align.top);

        alertTable.add(alertText1);
        alertTable.row();
        alertTable.add(alertText2).pad(15);

        settingsButton = getImageButton("selfdrive/assets/icons/button_settings.png");
        settingsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                appContext.setScreen(appContext.settingsScreen);
            }
        });
        settingsButton.setColor(1, 1, 1, 0.6f);
        infoTable.add(settingsButton).align(Align.top).height(uiHeight/9f).width(settingsBarWidth).padTop(50);
        infoTable.row();
        statusLabelTemp = getStatusLabel("TEMP\nGOOD");
        updateStatusLabel(statusLabelTemp, StatusColors.colorStatusGood);
        infoTable.add(statusLabelTemp).align(Align.top).height(uiHeight/8f).width(settingsBarWidth*0.8f).padTop(60);
        infoTable.row();
        statusLabelCan = getStatusLabel("CAN\nOFFLINE");
        updateStatusLabel(statusLabelCan, StatusColors.colorStatusCritical);
        infoTable.add(statusLabelCan).align(Align.top).height(uiHeight/8f).width(settingsBarWidth*0.8f).padTop(20);
        infoTable.row();
        statusLabelOnline = getStatusLabel("CONNECT\nOFFLINE");
        updateStatusLabel(statusLabelOnline, StatusColors.colorStatusWarn);
        infoTable.add(statusLabelOnline).align(Align.top).height(uiHeight/8f).width(settingsBarWidth*0.8f).padTop(20);
        infoTable.row();
        Image logoTexture = new Image(loadTextureMipMap("selfdrive/assets/icons/circle-white.png"));
        logoTexture.setColor(1, 1, 1, 0.85f);
        infoTable.add(logoTexture).align(Align.top).size(110).padTop(35).padBottom(40);

        stageFill.addActor(texImage);
        stageUI.addActor(velocityTable);
        stageUI.addActor(maxCruiseTable);
        stageUI.addActor(alertTable);
        stageSettings.addActor(rootTable);

        velocityTable.moveBy(settingsBarWidth/2f, 0); // TODO is this really correct ?
        alertTable.moveBy(settingsBarWidth/2f, 0);
        maxCruiseTable.moveBy(settingsBarWidth, 0);

        texImage.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!appContext.isOnRoad)
                    return;
                infoTable.setVisible(!infoTable.isVisible());
                if (infoTable.isVisible()) {
                    cameraAlertBox.translate(-settingsBarWidth, 0, 0);
                    velocityTable.moveBy(settingsBarWidth/2f, 0);
                    alertTable.moveBy(settingsBarWidth/2f, 0);
                    maxCruiseTable.moveBy(settingsBarWidth, 0);
                }
                else {
                    cameraAlertBox.translate(settingsBarWidth, 0, 0);
                    velocityTable.moveBy(-settingsBarWidth/2f, 0);
                    alertTable.moveBy(-settingsBarWidth/2f, 0);
                    maxCruiseTable.moveBy(-settingsBarWidth, 0);
                }
                cameraAlertBox.update();
            }
        });

        animationNoon = GifDecoder.loadGIFAnimation(Animation.PlayMode.LOOP, Gdx.files.absolute(Path.internal("selfdrive/assets/gifs/noon.gif")).read());
        animationSunset = GifDecoder.loadGIFAnimation(Animation.PlayMode.LOOP, Gdx.files.absolute(Path.internal("selfdrive/assets/gifs/sunset.gif")).read());
        animationNight = GifDecoder.loadGIFAnimation(Animation.PlayMode.LOOP, Gdx.files.absolute(Path.internal("selfdrive/assets/gifs/night.gif")).read());

        sh = new ZMQSubHandler(true);
        ph = new ZMQPubHandler();
        sh.createSubscribers(Arrays.asList(cameraTopic, cameraBufferTopic, modelTopic, calibrationTopic, carStateTopic, controlsStateTopic, canTopic));
        ph.createPublisher(desireTopic);
    }

    public Animation<TextureRegion> getCurrentAnimation(){
        @SuppressWarnings("NewApi") int hour = LocalDateTime.now().getHour();
        if (hour >= 20 || hour < 7)
            return  animationNight;
        else if (hour < 19)
            return animationNoon;
        else
            return animationSunset;
    }

    @Override
    public void show() {
        InputMultiplexer inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(stageSettings);
        inputMultiplexer.addProcessor(stageFill);
        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    // TODO: move to common
    public boolean isIntrinsicsValid(Definitions.FrameData.Reader frameData){
        // PS: find better ways to check validity.
        if (!frameData.hasIntrinsics())
            return false;
        PrimitiveList.Float.Reader intrinsics = frameData.getIntrinsics();
        return intrinsics.get(0)!=0 & intrinsics.get(2)!=0 & intrinsics.get(4)!=0 & intrinsics.get(5)!=0 & intrinsics.get(8)!=0;
    }

    public void updateCameraMatrix(Definitions.FrameData.Reader frameData){
        if (!isIntrinsicsValid(frameData)) {
            System.out.println("got invalid intrinsics from camera manager");
            return;
        }
        PrimitiveList.Float.Reader intrinsics = frameData.getIntrinsics();
        for (int i=0; i<3; i++){
            for (int j=0; j<3; j++){
                K.put(i, j, intrinsics.get(i*3 + j));
            }
        }
        cameraMatrixUpdated = true;
    }

    public void updateCamera() {
        msgframeBuffer = sh.recv(cameraBufferTopic).getWideRoadCameraBuffer();
        msgframeData = sh.recv(cameraTopic).getFrameData();
        imgBuffer = updateImageBuffer(msgframeBuffer, imgBuffer);

        updateCameraMatrix(msgframeData);
    }

    public void renderImage(boolean rgb) {
        if (!rgb) {
            if (nv12Renderer==null)
                nv12Renderer = new NV12Renderer(Camera.frameSize[0], Camera.frameSize[1]);
            nv12Renderer.render(imgBuffer);
        }
        else{
            pixelMap.setPixels(imgBuffer);
            texture.draw(pixelMap, 0, 0);
        }
    }

    public void updateCarState() {
        Definitions.Event.Reader event = sh.recv(carStateTopic);
        float vel = event.getCarState().getVEgo();
        vel = isMetric ? vel * 3.6f : vel * 2.237f;
        velocityLabel.setText(Integer.toString((int)vel));
    }

    public void updateControls() {
        controlState = sh.recv(controlsStateTopic).getControlsState();
        canErrCount = controlState.getCanErrorCounter();
        if (canErrCount != canErrCountPrev)
            updateStatusLabel(statusLabelCan, "CAN\nOFFLINE", StatusColors.colorStatusCritical);
        else
            updateStatusLabel(statusLabelCan, "CAN\nONLINE", StatusColors.colorStatusGood);
        canErrCountPrev = canErrCount;
    }

    public void updateModelOutputs(){
        Definitions.Event.Reader event = sh.recv(modelTopic);
        MsgModelDataV2.fillParsed(parsed, event.getModelV2(), !laneLess);

        try (MemoryWorkspace ws = Nd4j.getWorkspaceManager().getAndActivateWorkspace(wsConfig, "DrawUI")) {
            INDArray RtPath;
            INDArray Rt;
            Rt = Preprocess.eulerAnglesToRotationMatrix(-augmentRot.getFloat(0, 1), -augmentRot.getFloat(0, 2), -augmentRot.getFloat(0, 0), 0.0, false);
            RtPath = Preprocess.eulerAnglesToRotationMatrix(-augmentRot.getFloat(0, 1), -augmentRot.getFloat(0, 2), -augmentRot.getFloat(0, 0), 1.22, false);
            for (int i = 0; i< CommonModel.TRAJECTORY_SIZE; i++)
                parsed.position.get(0)[i] = Math.max(parsed.position.get(0)[i], minZ);
            path = Draw.getLaneCameraFrame(parsed.position, K, RtPath, 0.9f);
            lane0 = Draw.getLaneCameraFrame(parsed.laneLines.get(0), K, Rt, 0.07f);
            lane1 = Draw.getLaneCameraFrame(parsed.laneLines.get(1), K, Rt, 0.05f);
            lane2 = Draw.getLaneCameraFrame(parsed.laneLines.get(2), K, Rt, 0.05f);
            lane3 = Draw.getLaneCameraFrame(parsed.laneLines.get(3), K, Rt, 0.07f);
            edge0 = Draw.getLaneCameraFrame(parsed.roadEdges.get(0), K, Rt, 0.1f);
            edge1 = Draw.getLaneCameraFrame(parsed.roadEdges.get(1), K, Rt, 0.1f);

            lead1s = Draw.getTriangleCameraFrame(parsed.leads.get(0), K, Rt, leadDrawScale);
            //lead2s = Draw.getTriangleCameraFrame(parsed.leads.get(1), K, Rt, leadDrawScale);
            //lead3s = Draw.getTriangleCameraFrame(parsed.leads.get(2), K, Rt, leadDrawScale);
        }
    }

    public void drawAlert(Definitions.ControlsState.Reader controlState) {
        Definitions.ControlsState.AlertStatus alertStatus = null;
        if (controlState != null) {
            alertText1.setText(controlState.getAlertText1().toString());
            alertText2.setText(controlState.getAlertText2().toString());
            alertStatus = controlState.getAlertStatus();
            maxCruiseSpeedLabel.setText(Integer.toString((int)controlState.getVCruise()));
        }

        if (alertStatus==null) {
            colorBorder = colorBorderInactive;
        }
        else if (alertStatus == Definitions.ControlsState.AlertStatus.USER_PROMPT){
            colorBorder = colorBorderUserPrompt;
        }
        else if (alertStatus == Definitions.ControlsState.AlertStatus.CRITICAL){
            colorBorder = colorBorderUserCritical;
        }
        else if (alertStatus == Definitions.ControlsState.AlertStatus.NORMAL){
            colorBorder = colorBorderNormal;
        }

        stageAlert.getViewport().apply();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        float borderShift = 0;
        if (infoTable.isVisible())
            borderShift = settingsBarWidth;
        appContext.shapeRenderer.setProjectionMatrix(cameraAlertBox.combined);
        appContext.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        appContext.shapeRenderer.setColor(colorBorder[0] / 255f, colorBorder[1] / 255f, colorBorder[2] / 255f, 0.9f);

        if (alertText1.getText().toString().equals("") & alertText1.getText().toString().equals(""))
            appContext.shapeRenderer.rectLine(0, 0, uiWidth-borderShift ,0, borderWidth);
        else
            appContext.shapeRenderer.rectLine(0, 0, uiWidth-borderShift ,0, expandedBorderWidth);
        appContext.shapeRenderer.rectLine(uiWidth-borderShift ,0, uiWidth-borderShift, uiHeight, borderWidth);
        appContext.shapeRenderer.rectLine(uiWidth-borderShift, uiHeight, 0, uiHeight, borderWidth);
        appContext.shapeRenderer.rectLine(0, uiHeight,0, 0, borderWidth);
        appContext.shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }


    public void drawStrip(INDArray[] strip, int[] color, float alpha, int drawLength, int res, int startOffset) {
        for (int i = startOffset; i < drawLength; i += res) {
            appContext.shapeRenderer.setColor(color[0] / 255f, color[1] / 255f, color[2] / 255f, alpha);
            appContext.shapeRenderer.triangle(strip[0].getFloat(i, 0), strip[0].getFloat(i, 1),
                    strip[1].getFloat(i, 0), strip[1].getFloat(i, 1),
                    strip[0].getFloat(i + res, 0), strip[0].getFloat(i + res, 1));
            appContext.shapeRenderer.triangle(strip[0].getFloat(i + res, 0), strip[0].getFloat(i + res, 1),
                    strip[1].getFloat(i, 0), strip[1].getFloat(i, 1),
                    strip[1].getFloat(i + res, 0), strip[1].getFloat(i + res, 1));
            alpha -= fadeStrength;
        }
    }

    public void drawLeadTriangle(INDArray leadTriangle, int[] color, float alpha){
        appContext.shapeRenderer.setColor(color[0] / 255f, color[1] / 255f, color[2] / 255f, alpha);
        appContext.shapeRenderer.triangle(leadTriangle.getFloat(0, 0), leadTriangle.getFloat(0, 1),
                leadTriangle.getFloat(1, 0), leadTriangle.getFloat(1, 1),
                leadTriangle.getFloat(2, 0), leadTriangle.getFloat(2, 1));
    }

    public void drawModelOutputs() {
        appContext.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        appContext.shapeRenderer.setProjectionMatrix(cameraModel.combined);
        Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
        Gdx.gl.glBlendFunc(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);
        drawStrip(path, colorPath, 0.7f, defaultDrawLength, drawResolution, 0);
        if (!laneLess) {
            drawStrip(lane0, colorLanes, parsed.laneLineProbs[0], defaultDrawLength, drawResolution, 2);
            drawStrip(lane1, colorLanes, parsed.laneLineProbs[1], defaultDrawLength, drawResolution, 2);
            drawStrip(lane2, colorLanes, parsed.laneLineProbs[2], defaultDrawLength, drawResolution, 2);
            drawStrip(lane3, colorLanes, parsed.laneLineProbs[3], defaultDrawLength, drawResolution, 2);
            drawStrip(edge0, colorEdges, 0.9f, defaultDrawLength, drawResolution, 2);
            drawStrip(edge1, colorEdges, 0.9f, defaultDrawLength, drawResolution, 2);
        }

        if (parsed.leads.get(0).prob > minLeadProb)
            drawLeadTriangle(lead1s, colorLead, parsed.leads.get(0).prob);
        //if (parsed.leads.get(1).prob > minLeadProb)
        //    drawLeadTriangle(lead2s, colorLead, parsed.leads.get(1).prob);
        //if (parsed.leads.get(2).prob > minLeadProb)
        //    drawLeadTriangle(lead3s, colorLead, parsed.leads.get(2).prob);
        appContext.shapeRenderer.end();

        Gdx.gl.glDisable(Gdx.gl.GL_BLEND);
    }

    public void handleDesire(){
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            ph.publish(desireTopic, DesireEnum.LANE_CHANGE_LEFT.getBytes());
        }
        else if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            ph.publish(desireTopic, DesireEnum.LANE_CHANGE_RIGHT.getBytes());
        }
    }

    public void setUnits(){
        if (!controlsAlive)
            velocityUnitLabel.setText("");
        else if (isMetric)
            velocityUnitLabel.setText("kmph");
        else
            velocityUnitLabel.setText("mph");
    }

    @Override
    public void resize(int width, int height) {
        stageUI.getViewport().update(width, height);
        stageFill.getViewport().update(width, height);
        stageAlert.getViewport().update(width, height);
        stageSettings.getViewport().update(width, height);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT | (Gdx.graphics.getBufferFormat().coverageSampling?GL20.GL_COVERAGE_BUFFER_BIT_NV:0));
        elapsed += Gdx.graphics.getDeltaTime();

        if (appContext.isOnRoad) {
            offRoadRootTable.setVisible(false);

            stageFill.getViewport().apply();
            stageFill.act(delta);
            stageFill.draw();

            if (sh.updated(cameraTopic)) {
                updateCamera();
            }
            renderImage(msgframeBuffer.getEncoding() == Definitions.FrameBuffer.Encoding.RGB);

            if (modelAlive)
                drawModelOutputs();

            setUnits();

            if (sh.updated(carStateTopic))
                updateCarState();

            drawAlert(controlState);

            stageUI.getViewport().apply();
            stageUI.draw();

            batch.begin();
            appContext.font.draw(batch, String.valueOf(appContext.launcher.modeld.getIterationRate()),
                    Gdx.graphics.getWidth() - 200,
                    Gdx.graphics.getHeight() - 200);
            batch.end();
        }
        else{
            batch.begin();
            batch.setColor(1, 1, 1, 0.6f);
            batch.draw(getCurrentAnimation().getKeyFrame(elapsed), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            batch.end();

            offRoadRootTable.setVisible(true);

            if (!infoTable.isVisible())
                infoTable.setVisible(true);
        }

        stageSettings.getViewport().apply();
        stageSettings.act(delta);
        stageSettings.draw();

        if (sh.updated(calibrationTopic)) {
            Definitions.LiveCalibrationData.Reader liveCalib = sh.recv(calibrationTopic).getLiveCalibration();
            updateAugmentVectors(liveCalib);
        }

        if (sh.updated(modelTopic)) {
            updateModelOutputs();
            modelAlive = true;
        }

        if (sh.updated(controlsStateTopic)) {
            controlsAlive = true;
            updateControls();
        }

        handleDesire();
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        Gdx.input.setInputProcessor(null);
        stageFill.dispose();
        stageUI.dispose();
        sh.releaseAll();
    }
}
