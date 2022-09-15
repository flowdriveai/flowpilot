package ai.flow.app.CalibrationScreens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import ai.flow.app.FlowUI;


public class CalibrationInfo extends ScreenAdapter {

    FlowUI appContext;
    Stage stage;
    SpriteBatch batch;
    Table table;
    TextButton btnProceed;
    Label chessboardLink;

    public CalibrationInfo(FlowUI appContext, boolean enableCancel) {
        this.appContext = appContext;

        stage = new Stage(new FitViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);
        batch = new SpriteBatch();
        table = new Table();
        table.setFillParent(true);

        chessboardLink = new Label(
                "    https://bit.ly/chessboard-pattern",
                appContext.skin
        );
        chessboardLink.setColor(Color.CYAN);

        btnProceed = new TextButton("Proceed", appContext.skin);
        btnProceed.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                appContext.setScreen(new CalibrateScreen(appContext, enableCancel));
            }
        });

        table.add(new Label("Instructions", appContext.skin, "default-font-bold-med", "white")).fillX().pad(5).colspan(2);
        table.row();
        table.add( new Label(
                "1. Open the chessboard pattern on some other display from this link.",
                appContext.skin
        )).fillX().pad(5).colspan(2);
        table.row();
        table.add(chessboardLink).fillX().pad(5).colspan(2);
        table.row();
        table.add(new Label(
                "2. Point camera towards the chessboard pattern.",
                appContext.skin
        )).fillX().pad(5).colspan(2);
        table.row();
        table.add(new Label(
                "3. Move the camera & capture board from different distances & angles \n    until the progress bar is full.",
                appContext.skin
        )).fillX().pad(5).colspan(2);
        table.row().padTop(20);
        table.add(btnProceed).padTop(20);

        stage.addActor(table);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(.0f, .0f, .0f, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height);
    }

    @Override
    public void dispose() {
        Gdx.input.setInputProcessor(null);
        stage.dispose();
    }
}
