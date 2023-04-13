package ai.flow.app;

import ai.flow.common.ParamsInterface;
import ai.flow.common.Path;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class PlanInvalidScreen extends ScreenAdapter {
    FlowUI appContext;
    Stage stageUI, stageBackground;
    Table table;
    TextButton btnTryAgain;
    TextButton btnLogout;
    ProgressBar progressBar;
    Integer progressVal = 0;
    Label title;
    Table tableProgressBar;
    ScrollPane scrollPane;

    Label txtReasons;
    String strReasons =
            "You are viewing this screen because of one of these reasons: " +
            "\n\n" +
            "* You are currently not enrolled in F3 Beta" +
            "\n" +
            "* You are enrolled in F3 Beta, but your plan is expired";

    private static final ParamsInterface params = ParamsInterface.getInstance();
    Image background;

    public PlanInvalidScreen(FlowUI appContext) {
        this.appContext = appContext;

        this.stageUI = new Stage(new FitViewport(1280, 640));

        Texture tex = new Texture(Gdx.files.absolute(Path.internal("selfdrive/assets/images/phones.jpg")));
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        background = new Image(tex);
        background.setColor(1, 1, 1, 0.3f);

        stageBackground = new Stage(new FillViewport(background.getWidth(), background.getHeight()));
        stageBackground.addActor(background);

        txtReasons = new Label(strReasons, appContext.skin);
        txtReasons.setAlignment(Align.left);
        txtReasons.setWrap(true);
        Container container = new Container(txtReasons);
        container.pad(20).width(800f);

        scrollPane = new ScrollPane(container, appContext.skin);
        scrollPane.setSmoothScrolling(true);

        btnLogout = new TextButton("Logout", appContext.skin);
        btnLogout.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        progressVal++;
                        params.deleteKey("UserToken");
                        appContext.setScreen(new SetUpScreen(appContext));
                    }
                });

        btnTryAgain = new TextButton("Try Again", appContext.skin, "blue");
        btnTryAgain.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        progressVal++;
                        RequestSink.fetchUserInfo();
                        appContext.setScreen(new SetUpScreen(appContext));
                    }
                });

        progressBar = new ProgressBar(0, 1, 1, false, appContext.skin);
        tableProgressBar = new Table();
        tableProgressBar.setFillParent(true);
        tableProgressBar.top();
        tableProgressBar.row().top().expandX().fillX();
        tableProgressBar.add(progressBar);

        table = new Table();
        table.setFillParent(true);

        title = new Label("Plan Invalid", appContext.skin);

        table.add(title).align(Align.center).height(75f).colspan(2);
        table.row();
        table.add(scrollPane).align(Align.center).pad(20).width(900f).height(300f).colspan(2);
        table.row();
        table.add(btnLogout).width(200f).height(70f).uniform().pad(20);
        table.add(btnTryAgain).width(200f).height(70f).uniform().pad(20);

        stageUI.addActor(table);
        stageUI.addActor(tableProgressBar);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stageUI);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        progressBar.setValue(progressVal);

        stageBackground.getViewport().apply();
        stageBackground.act(delta);
        stageBackground.draw();

        stageUI.getViewport().apply();
        stageUI.act(delta);
        stageUI.draw();
    }

    @Override
    public void resize(int width, int height) {
        stageUI.getViewport().update(width, height);
    }


    @Override
    public void dispose() {
        stageUI.dispose();
        params.dispose();
    }
}
