package ai.flow.app;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import ai.flow.app.CalibrationScreens.CalibrationInfo;

public class SetUpScreen extends ScreenAdapter {

    FlowUI appContext;

    public SetUpScreen(FlowUI appContext) {
        this.appContext = appContext;
    }

    @Override
    public void show() {

        if (!appContext.params.exists("HasAcceptedTerms")) {
            appContext.setScreen(new TermsScreen(appContext));
            return;
        }

        if (!appContext.params.exists("UserID")) {
            appContext.setScreen(new RegisterScreen(appContext));
            return;
        }

        if (!appContext.params.existsAndCompare("CompletedTrainingVersion", true)){
            appContext.setScreen(new TrainingScreen(appContext));
            return;
        }

        if (!appContext.params.exists("CameraMatrix")) {
           appContext.launcher.startSensorD();
           appContext.setScreen(new CalibrationInfo(appContext, false));
           return;
        }

        appContext.launcher.startAllD();
        appContext.setScreen(new IntroScreen(appContext));
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }
}
