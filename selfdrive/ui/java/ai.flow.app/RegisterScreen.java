package ai.flow.app;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegisterScreen extends ScreenAdapter {
    // RFC822 Compliant email pattern
    public static final Pattern emailPattern = Pattern.compile("^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$");
    FlowUI appContext;
    Stage stage;
    Table table;
    Table tableProgressBar;
    TextField txfEmail;
    TextButton btnContinue;
    CheckBox noEmailFreqCheckBox;
    CheckBox infrequentEmailFreqCheckBox;
    CheckBox allEmailFreqCheckBox;
    ButtonGroup freqGroup;
    Dialog invalidEmailFmtDialog;
    Dialog sentMailDialog;
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

        noEmailFreqCheckBox = new CheckBox(" No Emails", appContext.skin);
        noEmailFreqCheckBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
            }
        });

        infrequentEmailFreqCheckBox = new CheckBox(" Infrequent Emails", appContext.skin);
        infrequentEmailFreqCheckBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
            }
        });

        allEmailFreqCheckBox = new CheckBox(" All Emails", appContext.skin);
        allEmailFreqCheckBox.toggle();
        allEmailFreqCheckBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
            }
        });

        freqGroup = new ButtonGroup(noEmailFreqCheckBox, infrequentEmailFreqCheckBox, allEmailFreqCheckBox);
        freqGroup.setChecked("allEmailFreqCheckBox");
        freqGroup.setUncheckLast(true);

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
        sentMailDialog.text("A secret token has been sent to your mailbox.\nPlease enter it in the next page");
        sentMailDialog.button("  Go  ", true);

        invalidEmailFmtDialog =
                new Dialog("Info", appContext.skin) {
                    public void result(Object obj) {
                        if (obj.equals(true)) {
                            invalidEmailFmtDialog.hide();
                        }
                    }
                };
        invalidEmailFmtDialog.text("Please enter a valid Email Address");
        invalidEmailFmtDialog.button("  OK  ", true);

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
        table.add(txfEmail).width(450f).height(100f);
        table.row();
        table.add(noEmailFreqCheckBox).height(25f).uniform().left().padTop(10).padBottom(10);
        table.row();
        table.add(infrequentEmailFreqCheckBox).uniform().left().padTop(10).padBottom(10);
        table.row();
        table.add(allEmailFreqCheckBox).uniform().left().padTop(10).padBottom(10);
        table.row();
        table.add(btnContinue).width(200f).height(75f);

        stage.addActor(table);
        stage.addActor(tableProgressBar);
    }

    private boolean validEmailFmt(String email) {
        Matcher matcher = emailPattern.matcher(email);
        return matcher.find();
    }

    private void btnContinueClicked() {
        progressVal++;

        String email = txfEmail.getText();
        String email_frequency = String.valueOf(freqGroup.getCheckedIndex());

        if (!validEmailFmt(email)) {
            invalidEmailFmtDialog.show(stage);
            progressVal--;
        } else {
            manageRegistration(email, email_frequency);
            progressVal++;
        }
    }

    private void manageRegistration(String email, String email_frequency) {
        Map<String, String> form =
                new HashMap<String, String>() {
                    {
                        put("email", email);
                        put("email_frequency", email_frequency);
                    }
                };

        progressVal++;

        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest request =
                requestBuilder
                        .newRequest()
                        .url(REGISTER_URI)
                        .method(Net.HttpMethods.POST)
                        .formEncodedContent(form)
                        .build();

        progressVal++;
        Gdx.net.sendHttpRequest(
                request,
                new Net.HttpResponseListener() {
                    @Override
                    public void handleHttpResponse(Net.HttpResponse httpResponse) {
                        progressVal++;
                        sentMailDialog.show(stage);
                        progressVal++;
                    }

                    @Override
                    public void failed(Throwable t) {
                        progressVal = 0;
                        noInternetDialog.show(stage);
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
