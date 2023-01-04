package ai.flow.app;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.net.HttpRequestHeader;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class LoginScreen extends ScreenAdapter {
    FlowUI appContext;
    Stage stage;
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

    public LoginScreen(FlowUI appContext, String email) {
        this.appContext = appContext;
        this.email = email;

        this.LOGIN_URI = appContext.AUTH_ENDPOINT + "/login";
    }

    @Override
    public void show() {
        this.stage = new Stage(new FitViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        Gdx.input.setInputProcessor(stage);

        txfEmail = new TextField(email, appContext.skin);
        txfEmail.setMessageText(" Email");
        txfPassword = new TextField("", appContext.skin);
        txfPassword.setMessageText(" Password");

        btnBack = new TextButton("Back", appContext.skin);
        btnBack.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        appContext.setScreen(new RegisterScreen(appContext));
                    }
                });

        btnLogin = new TextButton("Login", appContext.skin);
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

        title = new Label("Flowdrive Login", appContext.skin);

        table.add(title).align(Align.center).height(75f).colspan(2);
        table.row();
        table.add(txfEmail).width(440f).height(100f).colspan(2).pad(20);
        table.row();
        table.add(txfPassword).width(440f).height(100f).colspan(2).pad(20);
        table.row();
        table.add(btnBack).width(200f).height(100f).uniform().pad(20);
        table.add(btnLogin).width(200f).height(100f).uniform().pad(20);

        stage.addActor(table);
        stage.addActor(tableProgressBar);
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
                            String auth_token = response.get("message").getString("auth_token");
                            LoginSucceeded(email, auth_token);

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

    private void LoginSucceeded(String email, String auth_token) {
        appContext.params.put("UserEmail", email);
        appContext.params.put("UserToken", auth_token);
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

        dialog.show(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // the session cookie is generated, we can check that to log in now.
        if (appContext.params.exists("UserToken")) {
            appContext.setScreen(new SetUpScreen(appContext));
        }

        progressBar.setValue(progressVal);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height);
    }


    @Override
    public void dispose() {
        stage.dispose();
    }
}
