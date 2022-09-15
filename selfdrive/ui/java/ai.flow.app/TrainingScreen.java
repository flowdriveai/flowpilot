package ai.flow.app;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.viewport.FillViewport;

public class TrainingScreen extends ScreenAdapter {
    FlowUI appContext;
    Texture texture;
    float alpha = 1.0f;
    Stage stage;
    Image texImage;
    int trainingStep = 0;
    boolean restart = false;

    int[][][] touchBoxes = {{{115, 275}, {115, 111}, {731, 109}, {721, 270}},
            {{1624, 1080}, {1920, 1080}, {1920, 0}, {1624, 0}},
            {{1624, 1080}, {1920, 1080}, {1920, 0}, {1624, 0}},
            {{1405, 200}, {1597, 508}, {1622, 508}, {1774, 207}},
            {{1528, 468}, {1660, 466}, {1591, 523}},
            {{1624, 1080}, {1920, 1080}, {1920, 0}, {1624, 0}},
            {{1616, 393}, {1649, 305}, {1776, 268}, {1764, 368}},
            {{1221, 1080}, {1530, 1080}, {1549, 0}, {1219, 0}},
            {{1370, 542}, {1737, 544}, {1737, 466}, {1370, 469}},
            {{0, 0}, {1920, 0}, {1920, 1080}, {0, 1080}},
            {{1413, 868}, {1413, 558}, {1722, 558}, {1718, 872}},
            {{1624, 1080}, {1920, 1080}, {1920, 0}, {1624, 0}},
            {{1259, 0}, {1234, 448}, {1461, 899}, {1916, 1022}, {1914, 0}},
            {{1624, 1080}, {1920, 1080}, {1920, 0}, {1624, 0}},
            {{1486, 915}, {1497, 161}, {1751, 128}, {1745, 907}},
            {{1255, 537}, {1246, 339}, {1633, 345}, {1614, 544}},
            {{1624, 1080}, {1920, 1080}, {1920, 0}, {1624, 0}},
            {{1624, 1080}, {1920, 1080}, {1920, 0}, {1624, 0}},
            {{0, 0}, {1920, 0}, {1920, 1080}, {0, 1080}},
            };

    public TrainingScreen(FlowUI appContext) {
        this.appContext = appContext;
        texture = new Texture(Gdx.files.internal("training/step" + trainingStep + ".png"));
        texImage = new Image(texture);
        stage = new Stage(new FillViewport(1920, 1080));
        stage.addActor(texImage);
    }

    public boolean inside(int x, int y, int[][] poly){
        int intersects = 0;
        for (int i = 0; i < poly.length; i++) {
            int x1 = poly[i][0];
            int y1 = poly[i][1];
            int x2 = poly[(i+1)%poly.length][0];
            int y2 = poly[(i+1)%poly.length][1];
            if (((y1 <= y && y < y2) || (y2 <= y && y < y1)) && x < ((x2 - x1) / (y2 - y1) * (y - y1) + x1)) intersects++;
        }
        return (intersects & 1) == 1;
    }

    public boolean handleCustom(int x, int y, int step){
        if (step == 9){ // handle driver monitoring data collection.
            if (inside(x, y, new int[][] {{115, 251}, {113, 118},
                                        {572, 117}, {574, 253}})){
                appContext.params.putBool("RecordFront", false);
                return true;
            } else if (inside(x, y, new int[][] {{643, 253}, {645, 113},
                                                {1104, 109}, {1102, 255}})) {
                appContext.params.putBool("RecordFront", true);
                return true;
            }
            return false;
        } else if (step == 18) { // handle restart training.
            if (inside(x, y, new int[][] {{107, 268}, {111, 113},
                    {529, 113}, {529, 268}})){
                restart = true; // handle restart
                return true;
            } else if (inside(x, y, new int[][] {{627, 276}, {627, 113},
                                            {1263, 115}, {1257, 274}})) {
                appContext.params.putBool("CompletedTrainingVersion", true);
                return true;
            }
            return false;
        }
        return true;
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height);
    }

    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();

        if (Gdx.input.justTouched()) {
            Vector2 position = stage.screenToStageCoordinates(new Vector2(Gdx.input.getX(), Gdx.input.getY()));

            if (inside((int)position.x, (int)position.y, touchBoxes[trainingStep]) &&
                    handleCustom((int)position.x, (int)position.y, trainingStep)){
                    trainingStep++;
                if (restart){
                    restart = false;
                    trainingStep = 0;
                }
                if (trainingStep > 18) {
                    appContext.setScreen(new SetUpScreen(appContext));
                } else {
                    texture.dispose();
                    texture = new Texture(Gdx.files.internal("training/step" + trainingStep + ".png"));
                    texImage = new Image(texture);
                    stage.clear();
                    stage.addActor(texImage);
                }
            }
        }
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
        texture.dispose();
        stage.dispose();
    }
}
