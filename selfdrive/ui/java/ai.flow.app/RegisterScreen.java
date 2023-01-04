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
import com.badlogic.gdx.utils.viewport.FitViewport;

public class RegisterScreen extends ScreenAdapter {
    // RFC822 Compliant email pattern
    FlowUI appContext;
    Stage stage;
    Table table;
    Table tableProgressBar;
    TextField txfEmail;
    TextField txfPassword;
    TextButton btnContinue;
    Dialog invalidEmailFmtDialog;
    Dialog sentMailDialog;
    Dialog customDialog;
    Dialog emailAlreadyExistsDialog;
    Dialog noInternetDialog;
    String REGISTER_URI;
    Integer progressVal = 0; // ranges from 0 - 5
    ProgressBar progressBar;
    Label label;

    public RegisterScreen(FlowUI appContext) {
        this.appContext = appContext;
        this.REGISTER_URI = appContext.AUTH_ENDPOINT + "/register";
    }

    @Override
    public void show() {
        this.stage = new Stage(new FitViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        Gdx.input.setInputProcessor(stage);

        txfEmail = new TextField("", appContext.skin);
        txfEmail.setMessageText(" Email");

        txfPassword = new TextField("", appContext.skin);
        txfPassword.setMessageText(" Password");

        btnContinue = new TextButton("Continue", appContext.skin);
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
        sentMailDialog.text("A verification link has been sent to your Email inbox");
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
        emailAlreadyExistsDialog.text("Email already exists. Please log in");
        emailAlreadyExistsDialog.button("  Go  ", true);

        noInternetDialog =
                new Dialog("Info", appContext.skin) {
                    public void result(Object obj) {
                        if (obj.equals(true)) {
                            noInternetDialog.hide();
                        }
                    }
                };
        noInternetDialog.text("Facing network issues\nPlease retry");
        noInternetDialog.button("  OK  ", true);

        progressBar = new ProgressBar(0, 5, 1, false, appContext.skin);
        tableProgressBar = new Table();
        tableProgressBar.setFillParent(true);
        tableProgressBar.top();
        tableProgressBar.row().top().expandX().fillX();
        tableProgressBar.add(progressBar);

        table = new Table();
        table.setFillParent(true);

        label = new Label("FlowDrive Registration", appContext.skin);

        table.add(label).align(Align.center).height(75f);
        table.row();
        table.add(txfEmail).width(450f).height(100f).pad(20);
        table.row();
        table.add(txfPassword).width(450f).height(100f).pad(20);
        table.row();
        table.add(btnContinue).width(200f).height(75f).pad(20);

        stage.addActor(table);
        stage.addActor(tableProgressBar);
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
                            sentMailDialog.show(stage);
                            progressVal++;
                        } else if (statusCode == 202){
                            emailAlreadyExistsDialog.show(stage);
                            progressVal++;
                        } else {
                            // Unknown error, respond with a response message
                            String responseString = httpResponse.getResultAsString();
                            HttpUtils.Response response = HttpUtils.parseResponse(responseString);

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
                            customDialog.show(stage);
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
                        customDialog.show(stage);
                        progressVal = 0;
                    }

                    @Override
                    public void cancelled() {
                        progressVal = 0;
                        noInternetDialog.show(stage);
                    }
                });
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

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
