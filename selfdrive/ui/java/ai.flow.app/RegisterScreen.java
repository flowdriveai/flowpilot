package ai.flow.app;

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
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class RegisterScreen extends ScreenAdapter {
    // RFC822 Compliant email pattern
    FlowUI appContext;
    Stage stageUI, stageBackground;
    Table table;
    Table tableProgressBar;
    TextField txfEmail;
    TextField txfPassword;
    TextButton btnContinue;
    Dialog sentMailDialog;
    Dialog customDialog;
    Dialog emailAlreadyExistsDialog;
    Dialog noInternetDialog;
    String REGISTER_URI;
    Integer progressVal = 0; // ranges from 0 - 5
    ProgressBar progressBar;
    Label label;
    Image background;

    public RegisterScreen(FlowUI appContext) {
        this.appContext = appContext;
        this.REGISTER_URI = appContext.AUTH_ENDPOINT + "/register";

        this.stageUI = new Stage(new FitViewport(1280, 640));

        Texture tex = new Texture(Gdx.files.absolute(Path.internal("selfdrive/assets/images/phones.jpg")));
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        background = new Image(tex);
        background.setColor(1, 1, 1, 0.3f);

        stageBackground = new Stage(new FillViewport(background.getWidth(), background.getHeight()));
        stageBackground.addActor(background);

        txfEmail = new TextField("", appContext.skin);
        txfEmail.setMessageText(" email");

        txfPassword = new TextField("", appContext.skin);
        txfPassword.setPasswordMode(true);
        txfPassword.setPasswordCharacter('*');
        txfPassword.setMessageText(" password");

        btnContinue = new TextButton("Continue", appContext.skin, "blue");
        btnContinue.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        btnContinueClicked();
                    }
                });

        sentMailDialog =
                new Dialog("Info", appContext.skin) {
                    public void result(Object obj) {
                        if (obj.equals(true)) {
                            sentMailDialog.hide();
                            appContext.setScreen(new LoginScreen(appContext, txfEmail.getText()));
                        }
                    }
                };
        sentMailDialog.text(new Label("A verification link has been sent to your Email inbox", appContext.skin, "default-font-30", "white"));
        sentMailDialog.button("  Go  ", true);

        emailAlreadyExistsDialog =
                new Dialog("Info", appContext.skin) {
                    public void result(Object obj) {
                        if (obj.equals(true)) {
                            emailAlreadyExistsDialog.hide();
                            appContext.setScreen(new LoginScreen(appContext, txfEmail.getText()));
                        }
                    }
                };
        emailAlreadyExistsDialog.text(new Label("Email already exists. Please log in", appContext.skin, "default-font-30", "white"));
        emailAlreadyExistsDialog.button("  Go  ", true);
        emailAlreadyExistsDialog.getContentTable().pad(20);

        noInternetDialog =
                new Dialog("Info", appContext.skin) {
                    public void result(Object obj) {
                        if (obj.equals(true)) {
                            noInternetDialog.hide();
                        }
                    }
                };
        noInternetDialog.text(new Label("Facing network issues\nPlease retry", appContext.skin, "default-font-30", "white"));
        noInternetDialog.button("  OK  ", true);

        progressBar = new ProgressBar(0, 5, 1, false, appContext.skin);
        tableProgressBar = new Table();
        tableProgressBar.setFillParent(true);
        tableProgressBar.top();
        tableProgressBar.row().top().expandX().fillX();
        tableProgressBar.add(progressBar);

        table = new Table();
        table.setFillParent(true);

        label = new Label("Register", appContext.skin);

        table.add(label).align(Align.center).height(75f);
        table.row();
        table.add(txfEmail).width(450f).height(80f).pad(20);
        table.row();
        table.add(txfPassword).width(450f).height(80f).pad(20);
        table.row();
        table.add(btnContinue).width(200f).height(70f).pad(20);

        stageUI.addActor(table);
        stageUI.addActor(tableProgressBar);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stageUI);
    }

    private void btnContinueClicked() {
        progressVal++;

        String email = txfEmail.getText();
        String password = txfPassword.getText();

        manageRegistration(email, password);
        progressVal++;
    }

    private void manageRegistration(String email, String password) {
        String content = "{\n\t\"email\": \"" + email + "\",\n\t\"password\": \"" + password +"\"\n}";

        progressVal++;

        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest request =
                requestBuilder
                        .newRequest()
                        .url(REGISTER_URI)
                        .method(Net.HttpMethods.POST)
                        .header(HttpRequestHeader.ContentType, "application/json")
                        .content(content)
                        .timeout(0) // block until the request is completed
                        .build();

        progressVal++;
        Gdx.net.sendHttpRequest(
                request,
                new Net.HttpResponseListener() {
                    @Override
                    public void handleHttpResponse(Net.HttpResponse httpResponse) {
                        progressVal++;
                        int statusCode = httpResponse.getStatus().getStatusCode();
                        if (statusCode == 201) {
                            sentMailDialog.show(stageUI);
                            progressVal++;
                        } else if (statusCode == 202){
                            emailAlreadyExistsDialog.show(stageUI);
                            progressVal++;
                        } else {
                            // Unknown error, respond with a response message
                            String responseString = httpResponse.getResultAsString();
                            HttpUtils.DefaultResponse response = HttpUtils.parseDefaultResponse(responseString);

                            customDialog =
                                    new Dialog("Info", appContext.skin) {
                                        public void result(Object obj) {
                                            if (obj.equals(true)) {
                                                customDialog.hide();
                                            }
                                        }
                                    };
                            customDialog.text(response.message);
                            customDialog.button("  OK  ", true);
                            customDialog.getContentTable().pad(20);
                            customDialog.show(stageUI);
                            progressVal = 0;
                        }
                    }

                    @Override
                    public void failed(Throwable t) {
                        customDialog =
                                new Dialog("Error", appContext.skin) {
                                    public void result(Object obj) {
                                        if (obj.equals(true)) {
                                            customDialog.hide();
                                        }
                                    }
                                };
                        customDialog.text(
                                (t.getMessage() == null) ? t.toString() : t.getMessage()
                        );
                        customDialog.button("  OK  ", true);
                        customDialog.show(stageUI);
                        customDialog.getContentTable().pad(20);
                        progressVal = 0;
                    }

                    @Override
                    public void cancelled() {
                        progressVal = 0;
                        noInternetDialog.show(stageUI);
                    }
                });
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
        stageBackground.dispose();
    }
}
