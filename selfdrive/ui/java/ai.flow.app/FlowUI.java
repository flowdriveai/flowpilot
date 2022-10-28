package ai.flow.app;

import ai.flow.vision.ModelExecutor;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import ai.flow.common.ParamsInterface;
import ai.flow.launcher.Launcher;
import ai.flow.sensor.SensorInterface;

import org.nd4j.linalg.factory.Nd4j;
import org.opencv.core.Core;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;


public class FlowUI extends Game {
    final String API_ENDPOINT = "https://api.flowdrive.ai";
    final String AUTH_ENDPOINT = API_ENDPOINT + "/auth";
    public ShapeRenderer shapeRenderer;
    public SpriteBatch batch;
    public BitmapFont font;
    public Sound sound;
    public Skin skin;
    public int pid;
    public Launcher launcher;
    public Map<String, SensorInterface> sensors;
    public ModelExecutor modelExecutor;
    // reuse common screens
    public SettingsScreen settingsScreen;
    public OnRoadScreen onRoadScreen;
    public ParamsInterface params = ParamsInterface.getInstance();

    public FlowUI(Launcher launcher, int pid) {
        this.pid = pid;
        this.launcher = launcher;
        this.modelExecutor = launcher.modeld;
        this.sensors = launcher.sensors;
    }

    public static float[] byteToFloat(byte[] input) {
        float[] ret = new float[input.length / 4];
        for (int x = 0; x < input.length; x += 4) {
            ret[x / 4] = ByteBuffer.wrap(input, x, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        }
        return ret;
    }

    public static double[] byteToDouble(byte[] input) {
        double[] ret = new double[input.length / 8];
        for (int x = 0; x < input.length; x += 8) {
            ret[x / 8] = ByteBuffer.wrap(input, x, 8).order(ByteOrder.LITTLE_ENDIAN).getDouble();
        }
        return ret;
    }

    public static void loadFont(String fontPath, String fontName, int size, Skin skin){
        FreeTypeFontGenerator fontGen = new FreeTypeFontGenerator(Gdx.files.internal(fontPath));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.genMipMaps = true;
        parameter.magFilter = Texture.TextureFilter.MipMapLinearLinear;
        parameter.minFilter = Texture.TextureFilter.Linear;
        parameter.size = size;
        BitmapFont font = fontGen.generateFont(parameter);
        fontGen.dispose();
        skin.add(fontName, font);
    }

    public void loadInternalFonts(Skin skin){
        loadFont("fonts/Inter-Regular.ttf", "default-font-16", 16, skin);
        loadFont("fonts/Inter-Regular.ttf", "default-font", 36, skin);
        loadFont("fonts/Inter-Regular.ttf", "default-font-64", 64, skin);
        loadFont("fonts/opensans_bold.ttf", "default-font-bold", 20, skin);
        loadFont("fonts/opensans_bold.ttf", "default-font-bold-med", 45, skin);
        loadFont("fonts/opensans_bold.ttf", "default-font-bold-large", 100, skin);
    }

    @Override
    public void create() {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        Nd4j.zeros(1); // init nd4j (any better ways?)
        copyResources();

        params.putInt("FlowpilotPID", pid);

        if (Gdx.gl != null) { // else headless mode
            sound = Gdx.audio.newSound(Gdx.files.internal("sounds/click.mp3"));
            shapeRenderer = new ShapeRenderer();
            font = new BitmapFont();
            font.setColor(0f, 1f, 0f, 1f);
            font.getData().setScale(2);
            batch = new SpriteBatch();
            skin = new Skin(new TextureAtlas(Gdx.files.internal("skins/uiskin.atlas")));
            loadInternalFonts(skin);
            skin.load(Gdx.files.internal("skins/uiskin.json"));

            settingsScreen = new SettingsScreen(this);
            onRoadScreen = new OnRoadScreen(this);

            setScreen(new SetUpScreen(this));
        }
        else{
            launcher.startSensorD();
            launcher.startAllD();
        }
    }

    public void copyResources() {
        FileHandle videoSource = Gdx.files.internal("tmp");
        videoSource.copyTo(Gdx.files.external("tmp"));

        FileHandle model = Gdx.files.internal("models/supercombo.dlc");
        model.copyTo(Gdx.files.external("supercombo.dlc"));

        FileHandle tnnProto = Gdx.files.internal("models/supercombo.tnnproto");
        tnnProto.copyTo(Gdx.files.external("supercombo.tnnproto"));

        FileHandle tnnModel = Gdx.files.internal("models/supercombo.tnnmodel");
        tnnModel.copyTo(Gdx.files.external("supercombo.tnnmodel"));
    }

    @Override
    public void dispose() {
        if (Gdx.gl != null) { // else headless mode
            batch.dispose();
            shapeRenderer.dispose();
            font.dispose();
            launcher.dispose();
        }
    }
}
