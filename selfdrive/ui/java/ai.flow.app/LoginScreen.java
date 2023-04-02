package ai.flow.app;

import ai.flow.common.ParamsInterface;
import ai.flow.common.Path;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.net.HttpRequestHeader;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class LoginScreen extends ScreenAdapter {
    FlowUI appContext;
    Stage stageUI, stageBackground;
    Table table;
    TextField txfEmail;
    TextField txfPassword;
    TextButton btnLogin;
    TextButton btnBack;
    String email;
    Dialog dialog;
    ProgressBar progressBar;
    Integer progressVal = 0;
    Label title;
    Table tableProgressBar;

    String LOGIN_URI;
    ParamsInterface params;
    Image background;

    public LoginScreen(FlowUI appContext, String email) {
        this.appContext = appContext;
        this.email = email;

        this.LOGIN_URI = appContext.AUTH_ENDPOINT + "/login";
        params = ParamsInterface.getInstance();

        this.stageUI = new Stage(new FitViewport(1280, 640));

        Texture tex = new Texture(Gdx.files.absolute(Path.internal("selfdrive/assets/images/phones.jpg")));
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        background = new Image(tex);
        background.setColor(1, 1, 1, 0.3f);

        stageBackground = new Stage(new FillViewport(background.getWidth(), background.getHeight()));
        stageBackground.addActor(background);

        txfEmail = new TextField(email, appContext.skin);
        txfEmail.setMessageText(" email");
        txfPassword = new TextField("", appContext.skin);
        txfPassword.setPasswordMode(true);
        txfPassword.setPasswordCharacter('*');
        txfPassword.setMessageText(" password");

        btnBack = new TextButton("Back", appContext.skin);
        btnBack.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        appContext.setScreen(new RegisterScreen(appContext));
                    }
                });

        btnLogin = new TextButton("Login", appContext.skin, "blue");
        btnLogin.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        btnLoginClicked();
                    }
                });

        progressBar = new ProgressBar(0, 4, 1, false, appContext.skin);
        tableProgressBar = new Table();
        tableProgressBar.setFillParent(true);
        tableProgressBar.top();
        tableProgressBar.row().top().expandX().fillX();
        tableProgressBar.add(progressBar);

        table = new Table();
        table.setFillParent(true);

        title = new Label("Login", appContext.skin);

        table.add(title).align(Align.center).height(75f).colspan(2);
        table.row();
        table.add(txfEmail).width(450f).height(80f).colspan(2).pad(20);
        table.row();
        table.add(txfPassword).width(450f).height(80f).colspan(2).pad(20);
        table.row();
        table.add(btnBack).width(200f).height(70f).uniform().pad(20);
        table.add(btnLogin).width(200f).height(70f).uniform().pad(20);

        stageUI.addActor(table);
        stageUI.addActor(tableProgressBar);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stageUI);
    }

    private void btnLoginClicked() {
        progressVal++;
        String email = txfEmail.getText();
        String password = txfPassword.getText();
        manageLogin(email, password);
    }

    private void manageLogin(String email, String password) {
        String content = "{\n\t\"email\": \"" + email +"\",\n\t\"password\": \"" + password + "\",\n\t\"long_living\": true\n}";

        progressVal++;

        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest request =
                requestBuilder
                        .newRequest()
                        .url(LOGIN_URI)
                        .method(Net.HttpMethods.POST)
                        .header(HttpRequestHeader.ContentType, "application/json")
                        .content(content)
                        .timeout(0) // block until the request is completed
                        .build();

        Gdx.net.sendHttpRequest(
                request,
                new Net.HttpResponseListener() {
                    @Override
                    public void handleHttpResponse(Net.HttpResponse httpResponse) {

                        int statusCode = httpResponse.getStatus().getStatusCode();
                        if (statusCode != 200) {
                            loginError(httpResponse);
                            progressVal = 0;
                            return;
                        }
                        progressVal++;

                        // Login succeeded, save access token
                        try {
                            String responseString = httpResponse.getResultAsString();
                            JsonValue response = HttpUtils.parseGenericResponse(responseString);
                            String user_id = response.get("message").getString("user_id");
                            String auth_token = response.get("message").getString("auth_token");
                            LoginSucceeded(email, user_id, auth_token);

                        } catch (Exception exception) {
                            progressVal = 0;
                            exception.printStackTrace();
                        }
                    }

                    @Override
                    public void failed(Throwable t) {
                        progressVal = 0;
                    }

                    @Override
                    public void cancelled() {
                        progressVal = 0;
                    }
                });
    }

    private void LoginSucceeded(String email, String user_id, String auth_token) {
        params.put("UserEmail", email);
        params.put("UserID", user_id);
        params.put("UserToken", auth_token);
        progressVal++;
    }

    private void loginError(Net.HttpResponse httpResponse) {
        String responseString = httpResponse.getResultAsString();
        HttpUtils.DefaultResponse defaultResponse = HttpUtils.parseDefaultResponse(responseString);

        dialog =
                new Dialog("Error", appContext.skin) {
                    public void result(Object obj) {
                        if (obj.equals(true)) {
                            dialog.hide();
                        }
                    }
                };
        dialog.text(defaultResponse.message);
        dialog.button("Retry", true);

        dialog.show(stageUI);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // the session cookie is generated, we can check that to log in now.
        if (Gdx.graphics.getFrameId() % 10 == 0) {
            if (params.exists("UserToken")) {
                appContext.setScreen(new SetUpScreen(appContext));
                return;
            }
        }

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
