package ai.flow.app;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Pixmap.Blending;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.*;
import ai.flow.definitions.MessageBase;
import ai.flow.definitions.Definitions;
import ai.flow.vision.ParsedOutputs;
import ai.flow.vision.Parser;
import ai.flow.vision.Preprocess;

import ai.flow.vision.messages.MsgLiveCalibrationData;
import messaging.ZMQPubHandler;
import org.capnproto.PrimitiveList;
import org.nd4j.linalg.api.memory.MemoryWorkspace;
import org.nd4j.linalg.api.memory.conf.WorkspaceConfiguration;
import org.nd4j.linalg.api.memory.enums.AllocationPolicy;
import org.nd4j.linalg.api.memory.enums.LearningPolicy;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.nio.ByteBuffer;
import java.util.Arrays;

import ai.flow.vision.messages.MsgFrameData;
import ai.flow.vision.messages.MsgModelDataV2;
import ai.flow.vision.DesireEnum;

import messaging.ZMQSubHandler;


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
    float leadDrawScale = 6f; /** in meters (think of it as a 6m sign board) **/
    float borderWidth = 30;
    float expandedBorderWidth = 600;
    int defaultImageWidth = 1164;
    int defaultImageHeight = 874;
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
    // initialize messages and respective buffers
    MsgModelDataV2 msgModelDataV2 = new MsgModelDataV2();
    MsgFrameData msgFrameData = new MsgFrameData(1164*874*3);
    MsgLiveCalibrationData msgLiveCalib = new MsgLiveCalibrationData();
    Definitions.LiveCalibrationData.Reader liveCalib;
    Definitions.ModelDataV2.Reader modelDataV2;
    Definitions.FrameData.Reader frameData;
    Definitions.ControlsState.Reader controlState;
    ByteBuffer msgFrameDataBuffer = msgFrameData.getSerializedBuffer();
    ByteBuffer modelMsgByteBuffer = msgModelDataV2.getSerializedBuffer();
    ByteBuffer msgLiveCalibBuffer = msgLiveCalib.getSerializedBuffer();
    ByteBuffer msgCarStateBuffer = ByteBuffer.allocateDirect(1000);
    ByteBuffer msgControlsStateBuffer = ByteBuffer.allocateDirect(1000);
    ByteBuffer imgBuffer;

    String modelTopic = "modelV2";
    String cameraTopic = "roadCameraState";
    String calibrationTopic = "liveCalibration";
    String desireTopic = "pulseDesire";
    String carStateTopic = "carState";
    String controlsStateTopic = "controlsState";
    String canTopic = "can";

    Label velocityLabel, velocityUnitLabel, alertText1, alertText2, maxCruiseSpeedLabel;
    Table velocityTable, maxCruiseTable, alertTable, infoTable;
    Stack statusLabelTemp, statusLabelCan, statusLabelOnline, maxCruise;
    ImageButton settingsButton;
    ParsedOutputs parsed = new ParsedOutputs();
    int canErrCount = 0;
    int canErrCountPrev = 0;
    float uiWidth = 1280;
    float uiHeight = 720;
    float settingsBarWidth;

    final float[][] K_buffer =  {{910.0f,  0.0f, 582.0f},
            {0.0f,  910.0f, 437.0f},
            {0.0f , 0.0f,  1.0f }};
    final INDArray K = Nd4j.createFromArray(K_buffer);
    boolean cameraMatrixUpdated = false;
    boolean isMetric;
    boolean laneLess;

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

    public void setTableColor(Table table, float r, float g, float b, float a){
        Pixmap bgPixmap = new Pixmap(1,1, Pixmap.Format.RGB565);
        bgPixmap.setColor(r, g, b, a);
        bgPixmap.fill();
        TextureRegionDrawable textureRegionDrawableBg = new TextureRegionDrawable(new TextureRegion(new Texture(bgPixmap)));
        table.setBackground(textureRegionDrawableBg);
        bgPixmap.dispose();
    }

    public ImageButton getImageButton(String texturePath){ //TODO Move to common
        Texture buttonTexture = loadTextureMipMap(texturePath);
        return new ImageButton(new TextureRegionDrawable(buttonTexture));
    }

    public Texture loadTextureMipMap(String path){
        Texture texture = new Texture(Gdx.files.internal(path), true);
        texture.setFilter(Texture.TextureFilter.MipMapLinearNearest, Texture.TextureFilter.Linear);
        return texture;
    }

    public Stack getStatusLabel(String text){
        Image borderTexture = new Image(new Texture("icons/rounded-border.png"));
        Image statusTexture = new Image(loadTextureMipMap("icons/status_label.png"));
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
        Image bg = new Image(new Texture("icons/max_cruise.png"));
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

    public void updateMaxCruise(float speed){
    }

    public OnRoadScreen(FlowUI appContext) {
        this.appContext = appContext;
        pixelMap = new Pixmap(defaultImageWidth, defaultImageHeight, Pixmap.Format.RGB888);
        pixelMap.setBlending(Blending.None);
        texture = new Texture(pixelMap);
        batch = new SpriteBatch();
        settingsBarWidth = uiWidth / 3f * Gdx.graphics.getHeight() / Gdx.graphics.getWidth();

        infoTable = new Table();
        infoTable.setWidth(settingsBarWidth);
        infoTable.setHeight(uiHeight);
        infoTable.align(Align.topLeft);
        setTableColor(infoTable, colorSettingsBar[0], colorSettingsBar[1], colorSettingsBar[2], 1);

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

        settingsButton = getImageButton("icons/button_settings.png");
        settingsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                appContext.setScreen(appContext.settingsScreen);
            }
        });
        settingsButton.setColor(1, 1, 1, 0.6f);
        infoTable.add(settingsButton).align(Align.top).height(uiHeight/9f).width(settingsBarWidth).padTop(30);

        infoTable.row();
        statusLabelTemp = getStatusLabel("TEMP\nGOOD");
        updateStatusLabel(statusLabelTemp, StatusColors.colorStatusGood);
        infoTable.add(statusLabelTemp).align(Align.top).height(uiHeight/8f).width(settingsBarWidth*0.8f).padTop(100);
        infoTable.row();
        statusLabelCan = getStatusLabel("CAN\nOFFLINE");
        updateStatusLabel(statusLabelCan, StatusColors.colorStatusCritical);
        infoTable.add(statusLabelCan).align(Align.top).height(uiHeight/8f).width(settingsBarWidth*0.8f).padTop(20);
        infoTable.row();
        statusLabelOnline = getStatusLabel("CONNECT\nOFFLINE");
        updateStatusLabel(statusLabelOnline, StatusColors.colorStatusWarn);
        infoTable.add(statusLabelOnline).align(Align.top).height(uiHeight/8f).width(settingsBarWidth*0.8f).padTop(20);
        infoTable.row();
        Image logoTexture = new Image(loadTextureMipMap("icons/circle-white.png"));
        logoTexture.setColor(1, 1, 1, 0.85f);
        infoTable.add(logoTexture).align(Align.top).size(120).padTop(35);

        stageFill.addActor(texImage);
        stageUI.addActor(velocityTable);
        stageUI.addActor(maxCruiseTable);
        stageUI.addActor(alertTable);
        stageSettings.addActor(infoTable);

        velocityTable.moveBy(settingsBarWidth/2f, 0); // TODO is this really correct ?
        alertTable.moveBy(settingsBarWidth/2f, 0);
        maxCruiseTable.moveBy(settingsBarWidth, 0);

        texImage.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
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

        sh = new ZMQSubHandler(true);
        ph = new ZMQPubHandler();
        sh.createSubscribers(Arrays.asList(cameraTopic, modelTopic, calibrationTopic, carStateTopic, controlsStateTopic, canTopic));
        ph.createPublisher(desireTopic);
    }

    @Override
    public void show() {
        InputMultiplexer inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(stageSettings);
        inputMultiplexer.addProcessor(stageFill);
        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    // TODO: move to common
    public boolean isIntrinsicsValid(MsgFrameData frameMsg){
        // PS: find better ways to check validity.
        if (!frameMsg.frameData.hasIntrinsics())
            return false;
        PrimitiveList.Float.Reader intrinsics = frameMsg.frameData.getIntrinsics().asReader();
        return intrinsics.get(0)!=0 & intrinsics.get(2)!=0 & intrinsics.get(4)!=0 & intrinsics.get(5)!=0 & intrinsics.get(8)!=0;
    }

    public void updateCameraMatrix(MsgFrameData frameMsg){
        if (!isIntrinsicsValid(frameMsg))
            return;
        PrimitiveList.Float.Reader intrinsics = frameMsg.frameData.getIntrinsics().asReader();
        for (int i=0; i<3; i++){
            for (int j=0; j<3; j++){
                K_buffer[i][j] = intrinsics.get(i*3 + j);
                K.put(i, j, intrinsics.get(i*3 + j));
            }
        }
        cameraMatrixUpdated = true;
    }

    public void updateCamera() {
        sh.recvBuffer(cameraTopic, msgFrameDataBuffer);
        frameData = MessageBase.deserialize(msgFrameDataBuffer).getFrameData();
        if (imgBuffer == null){
            if (frameData.getNativeImageAddr() != 0)
                imgBuffer = msgFrameData.getImageBuffer(frameData.getNativeImageAddr());
            else
                imgBuffer = ByteBuffer.allocateDirect(1164*874*3);
        }
        else {
            if (frameData.getNativeImageAddr() == 0) {
                imgBuffer.put(frameData.getImage().asByteBuffer());
                imgBuffer.rewind();
            }
        }
        // update K only once.
        if (!cameraMatrixUpdated){
            updateCameraMatrix(msgFrameData);
        }
        pixelMap.setPixels(imgBuffer);
        texture.draw(pixelMap, 0, 0);
    }

    public void updateCarState() {
        sh.recvBuffer(carStateTopic, msgCarStateBuffer);
        Definitions.Event.Reader event = MessageBase.deserialize(msgCarStateBuffer);
        float vel = event.getCarState().getVEgo();
        vel = isMetric ? vel * 3.6f : vel * 2.237f;
        velocityLabel.setText(Integer.toString((int)vel));
    }

    public void updateControls() {
        sh.recvBuffer(controlsStateTopic, msgControlsStateBuffer);
        controlState = MessageBase.deserialize(msgControlsStateBuffer).getControlsState();
        canErrCount = controlState.getCanErrorCounter();
        if (canErrCount != canErrCountPrev)
            updateStatusLabel(statusLabelCan, "CAN\nOFFLINE", StatusColors.colorStatusCritical);
        else
            updateStatusLabel(statusLabelCan, "CAN\nONLINE", StatusColors.colorStatusGood);
        canErrCountPrev = canErrCount;
    }

    public void updateModelOutputs(){
        sh.recvBuffer(modelTopic, modelMsgByteBuffer);

        Definitions.Event.Reader eventReader = MessageBase.deserialize(modelMsgByteBuffer);
        msgModelDataV2.fillParsed(parsed, eventReader.getModelV2(), !laneLess);

        try (MemoryWorkspace ws = Nd4j.getWorkspaceManager().getAndActivateWorkspace(wsConfig, "DrawUI")) {
            INDArray RtPath;
            INDArray Rt;
            Rt = Preprocess.eulerAnglesToRotationMatrix(-augmentRot.getFloat(0, 0), -augmentRot.getFloat(0, 1), -augmentRot.getFloat(0, 2), 0.0, false);
            RtPath = Preprocess.eulerAnglesToRotationMatrix(-augmentRot.getFloat(0, 0), -augmentRot.getFloat(0, 1), -augmentRot.getFloat(0, 2), 1.22, false);
            for (int i=0; i<Parser.TRAJECTORY_SIZE; i++)
                parsed.position.get(0)[i] = Math.max(parsed.position.get(0)[i], minZ);
            path = Draw.getLaneCameraFrame(parsed.position, K, RtPath, 0.9f);
            lane0 = Draw.getLaneCameraFrame(parsed.laneLines.get(0), K, Rt, 0.07f);
            lane1 = Draw.getLaneCameraFrame(parsed.laneLines.get(1), K, Rt, 0.05f);
            lane2 = Draw.getLaneCameraFrame(parsed.laneLines.get(2), K, Rt, 0.05f);
            lane3 = Draw.getLaneCameraFrame(parsed.laneLines.get(3), K, Rt, 0.07f);
            edge0 = Draw.getLaneCameraFrame(parsed.roadEdges.get(0), K, Rt, 0.3f);
            edge1 = Draw.getLaneCameraFrame(parsed.roadEdges.get(1), K, Rt, 0.3f);

            lead1s = Draw.getTriangleCameraFrame(parsed.leads.get(0), K, Rt, leadDrawScale);
            lead2s = Draw.getTriangleCameraFrame(parsed.leads.get(1), K, Rt, leadDrawScale);
            lead3s = Draw.getTriangleCameraFrame(parsed.leads.get(2), K, Rt, leadDrawScale);
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


    public void drawStrip(INDArray[] strip, int[] color, float alpha, int drawLength, int res) {
        for (int i = 2; i < drawLength; i += res) {
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
        drawStrip(path, colorPath, 0.7f, defaultDrawLength, drawResolution);
        if (!laneLess) {
            drawStrip(lane0, colorLanes, parsed.laneLineProbs[0], defaultDrawLength, drawResolution);
            drawStrip(lane1, colorLanes, parsed.laneLineProbs[1], defaultDrawLength, drawResolution);
            drawStrip(lane2, colorLanes, parsed.laneLineProbs[2], defaultDrawLength, drawResolution);
            drawStrip(lane3, colorLanes, parsed.laneLineProbs[3], defaultDrawLength, drawResolution);
            drawStrip(edge0, colorEdges, 0.9f, defaultDrawLength, drawResolution);
            drawStrip(edge1, colorEdges, 0.9f, defaultDrawLength, drawResolution);
        }

        drawLeadTriangle(lead1s, colorLead, parsed.leads.get(0).prob);
        //drawLeadTriangle(lead2s, colorLead, parsed.leads.get(1).prob);
        //drawLeadTriangle(lead3s, colorLead, parsed.leads.get(2).prob);
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

        if (sh.updated(cameraTopic)) {
            updateCamera();
        }

        stageFill.getViewport().apply();
        stageFill.act(delta);
        stageFill.draw();

        if (sh.updated(calibrationTopic)) {
            sh.recvBuffer(calibrationTopic, msgLiveCalibBuffer);
            liveCalib = msgLiveCalib.deserialize().getLiveCalibration();
            updateAugmentVectors(liveCalib);
        }

        if (sh.updated(modelTopic)){
            updateModelOutputs();
            modelAlive = true;
        }

        if (modelAlive)
            drawModelOutputs();

        if (sh.updated(controlsStateTopic)) {
            controlsAlive = true;
            updateControls();
        }

        setUnits();

        if (sh.updated(carStateTopic))
            updateCarState();

        drawAlert(controlState);

        stageUI.getViewport().apply();
        stageUI.draw();

        stageSettings.getViewport().apply();
        stageSettings.act(delta);
        stageSettings.draw();

        batch.begin();
        appContext.font.draw(batch, String.valueOf(appContext.launcher.modeld.getIterationRate()),
                Gdx.graphics.getWidth() - 200,
                Gdx.graphics.getHeight() - 200);

        appContext.font.draw(batch, "% " + appContext.launcher.modeld.getFrameDropPercent(),
                Gdx.graphics.getWidth() - 200,
                Gdx.graphics.getHeight() - 230);
        batch.end();

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
