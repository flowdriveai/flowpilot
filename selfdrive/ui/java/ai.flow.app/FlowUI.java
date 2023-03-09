package ai.flow.app;

import ai.flow.common.ParamsInterface;
import ai.flow.common.Path;
import ai.flow.launcher.Launcher;
import ai.flow.modeld.ModelExecutor;
import ai.flow.sensor.SensorInterface;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import org.nd4j.linalg.factory.Nd4j;
import org.opencv.core.Core;

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
    public boolean isOnRoad = false;
    public Thread updateOnroadThread = null;

    public FlowUI(Launcher launcher, int pid) {
        this.pid = pid;
        this.launcher = launcher;
        this.modelExecutor = launcher.modeld;
        this.sensors = launcher.sensors;

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        Nd4j.zeros(1); // init nd4j (any better ways?)

        updateOnroadThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.interrupted()){
                    if (params.exists("IsOnroad"))
                        isOnRoad = params.getBool("IsOnroad");
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    if (!isOnRoad){
                        modelExecutor.stop();
                    }
                    else{
                        modelExecutor.start();
                    }
                }
            }
        });
    }

    public static void loadFont(String fontPath, String fontName, int size, Skin skin){
        FreeTypeFontGenerator fontGen = new FreeTypeFontGenerator(Gdx.files.absolute(Path.internal(fontPath)));
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
        loadFont("selfdrive/assets/fonts/Inter-Regular.ttf", "default-font-16", 16, skin);
        loadFont("selfdrive/assets/fonts/Inter-Regular.ttf", "default-font", 36, skin);
        loadFont("selfdrive/assets/fonts/Inter-Regular.ttf", "default-font-64", 64, skin);
        loadFont("selfdrive/assets/fonts/opensans_bold.ttf", "default-font-bold", 20, skin);
        loadFont("selfdrive/assets/fonts/opensans_bold.ttf", "default-font-bold-med", 45, skin);
        loadFont("selfdrive/assets/fonts/opensans_bold.ttf", "default-font-bold-large", 100, skin);
    }

    @Override
    public void create() {
        params.putInt("FlowpilotPID", pid);

        updateOnroadThread.start();

        if (Gdx.gl != null) { // else headless mode
            sound = Gdx.audio.newSound(Gdx.files.absolute(Path.internal("selfdrive/assets/sounds/click.mp3")));
            shapeRenderer = new ShapeRenderer();
            font = new BitmapFont();
            font.setColor(0f, 1f, 0f, 1f);
            font.getData().setScale(2);
            batch = new SpriteBatch();
            skin = new Skin(new TextureAtlas(Gdx.files.absolute(Path.internal("selfdrive/assets/skins/uiskin.atlas"))));
            loadInternalFonts(skin);
            skin.load(Gdx.files.absolute(Path.internal("selfdrive/assets/skins/uiskin.json")));

            settingsScreen = new SettingsScreen(this);
            onRoadScreen = new OnRoadScreen(this);

            setScreen(new SetUpScreen(this));
        }
        else{
            launcher.startSensorD();
            launcher.startAllD();
        }
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
