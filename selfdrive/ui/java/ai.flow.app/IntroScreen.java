package ai.flow.app;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.viewport.FillViewport;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.nio.ByteBuffer;

public class IntroScreen extends ScreenAdapter {
    public Mat imgBGR888;
    public String videoName = "intro.avi";
    public VideoCapture capture;
    FlowUI appContext;
    ByteBuffer imgBuffer;
    Pixmap pixelMap;
    Texture texture;
    int vidWidth;
    int vidHeight;
    boolean ret;
    int count = 0;
    int frameCount;
    float alpha = 1.0f;
    Stage stage;
    Image texImage;

    public IntroScreen(FlowUI appContext) {
        if (!Gdx.files.external(videoName).exists()) {
            FileHandle videoIntroSource = Gdx.files.internal("videos/" + videoName);
            videoIntroSource.copyTo(Gdx.files.external(videoName));
        }
        FileHandle videoIntroSourceExt = Gdx.files.external(videoName);
        this.appContext = appContext;
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        capture = new VideoCapture(videoIntroSourceExt.file().getAbsolutePath());
        frameCount = (int) capture.get(Videoio.CAP_PROP_FRAME_COUNT);
        vidWidth = (int) capture.get(Videoio.CAP_PROP_FRAME_WIDTH);
        vidHeight = (int) capture.get(Videoio.CAP_PROP_FRAME_HEIGHT);
        if (imgBuffer == null)
            imgBuffer = ByteBuffer.allocateDirect(vidWidth * vidHeight * 3);
        imgBGR888 = new Mat(vidHeight, vidWidth, CvType.CV_8UC3, imgBuffer);
        pixelMap = new Pixmap(vidWidth, vidHeight, Pixmap.Format.RGB888);
        pixelMap.setBlending(Pixmap.Blending.None);
        texture = new Texture(pixelMap);
        texImage = new Image(texture);

        stage = new Stage(new FillViewport(vidWidth, vidHeight));
        stage.addActor(texImage);
    }

    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (count == frameCount) {
            capture.set(Videoio.CAP_PROP_POS_FRAMES, 0);
            count = 0;
        }

        ret = capture.read(imgBGR888);
        Imgproc.cvtColor(imgBGR888, imgBGR888, Imgproc.COLOR_BGR2RGB);

        pixelMap.setPixels(imgBuffer);
        texture.draw(pixelMap, 0, 0);

        texImage.setColor(1.0f, 1.0f, 1.0f, alpha);
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
        count++;

        if (appContext.modelExecutor.isRunning())
            alpha -= 0.05;

        if (alpha < 0.0f)
            appContext.setScreen(appContext.onRoadScreen);
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
        capture.release();
        texture.dispose();
        pixelMap.dispose();
        imgBGR888.release();
        stage.dispose();
    }
}
