package ai.flow.app;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.net.HttpCookie;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoginScreen extends ScreenAdapter {
    FlowUI appContext;
    Stage stage;
    Table table;
    TextField txfToken;
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

        txfToken = new TextField("", appContext.skin);
        txfToken.setMessageText(" Token");

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

        dialog =
                new Dialog("Error", appContext.skin) {
                    public void result(Object obj) {
                        if (obj.equals(true)) {
                            dialog.hide();
                        }
                    }
                };
        dialog.text("Login Credentials Incorrect");
        dialog.button("Retry", true);

        progressBar = new ProgressBar(0, 4, 1, false, appContext.skin);
        tableProgressBar = new Table();
        tableProgressBar.setFillParent(true);
        tableProgressBar.top();
        tableProgressBar.row().top().expandX().fillX();
        tableProgressBar.add(progressBar);

        table = new Table();
        table.setFillParent(true);

        title = new Label("Login with your secret token", appContext.skin);

        table.add(title).align(Align.center).height(75f).colspan(2);
        table.row();
        table.add(txfToken).width(440f).height(100f).colspan(2).pad(20);
        table.row();
        table.add(btnBack).width(200f).height(100f).uniform().pad(20);
        table.add(btnLogin).width(200f).height(100f).uniform().pad(20);

        stage.addActor(table);
        stage.addActor(tableProgressBar);
    }

    private void btnLoginClicked() {
        progressVal++;
        String token = txfToken.getText();
        manageLogin(email, token);
    }

    private void manageLogin(String email, String token) {
        Map<String, String> form =
                new HashMap<String, String>() {
                    {
                        put("email", email);
                        put("token", token);
                    }
                };

        progressVal++;

        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest request =
                requestBuilder
                        .newRequest()
                        .url(LOGIN_URI)
                        .method(Net.HttpMethods.POST)
                        .formEncodedContent(form)
                        .build();

        Gdx.net.sendHttpRequest(
                request,
                new Net.HttpResponseListener() {
                    @Override
                    public void handleHttpResponse(Net.HttpResponse httpResponse) {
                        int statusCode = httpResponse.getStatus().getStatusCode();
                        if (statusCode != 200) {
                            loginError();
                            progressVal = 0;
                            return;
                        }
                        progressVal++;

                        try {
                            String cookieHeader = httpResponse.getHeader("set-cookie");
                            List<HttpCookie> cookies = HttpCookie.parse(cookieHeader);
                            LoginSucceeded(cookies);

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

    private void LoginSucceeded(List<HttpCookie> cookies) {
        appContext.params.put("UserID", cookies.get(0).getValue());
        progressVal++;
    }

    private void loginError() {
        dialog.show(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // the session cookie is generated, we can check that to log in now.
        if (appContext.params.exists("UserID")) {
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
