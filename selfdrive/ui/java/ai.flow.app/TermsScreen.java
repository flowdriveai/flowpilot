package ai.flow.app;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import ai.flow.app.CalibrationScreens.CalibrationInfo;
import ai.flow.common.ParamsInterface;
import ai.flow.common.SystemUtils;


public class TermsScreen extends ScreenAdapter {

    FlowUI appContext;
    ParamsInterface params;
    Stage stage;
    TextButton acceptTerms;
    Label termsText;
    SpriteBatch batch;
    Table rootTable;
    ScrollPane scrollPane;
    String termsString =
            "Please read these Terms of Use ('Terms') carefully before using flowpilot which is open-sourced software developed by flowdrive.ai built on top of openpilot\n" +
            "\n" +
            "Before using and by accessing flowpilot, you indicate that you have read, understood, and agree to these Terms. These Terms apply to all users and others who access or use flowpilot. If others use flowpilot through your user account or vehicle, you are responsible to ensure that they only use flowpilot when it is safe to do so, and in compliance with these Terms and with applicable law. If you disagree with any part of the Terms, you should not access or use flowpilot.\n" +
            "\n" +
            "Communications:\n\n" +
            "You agree that flowdrive may contact you by email or telephone in connection with flowpilot or for other business purposes. You may opt out of receiving email messages at any time by contacting us at support@flowdrive.ai.\n" +
            "\n" +
            "We collect, use, and share information from and about you and your vehicle in connection with flowpilot. You consent to comma accessing the systems associated with flowpilot, without additional notice or consent, for the purposes of providing flowpilot, data collection, software updates, safety and cybersecurity, suspension or removal of your account, and as disclosed in the Privacy Policy (available at https://flowdrie.ai/privacy).\n" +
            "\n" +
            "Safety:\n\n" +
            "flowpilot performs the functions of Adaptive Cruise Control (ACC) and Lane Keeping Assist System (LKAS) designed for use in compatible motor vehicles. While using openpilot, it is your responsibility to obey all laws, traffic rules, and traffic regulations governing your vehicle and its operation. Access to and use of flowpilot is at your own risk and responsibility, and flowpilot should be accessed and/or used only when you can do so safely.\n" +
            "\n" +
            "flowpilot does not make your vehicle \"autonomous\" or capable of operation without the active monitoring of a licensed driver. It is designed to assist a licensed driver. A licensed driver must pay attention to the road, remain aware of navigation at all times, and be prepared to take immediate action. Failure to do so can cause damage, injury, or death.\n" +
            "\n" +
            "Supported Locations and Models\n\n" +
            "flowpilot is compatible only with particular makes and models of vehicles. For a complete list of currently supported vehicles, visit https://flowdrive.ai. flowpilot will not function properly when installed in an incompatible vehicle.\n" +
            "\n" +
            "Indemnification:\n\n" +
            "To the maximum extent allowable by law, you agree to defend, indemnify and hold harmless flowdrive, and its employees, partners, suppliers, contractors, investors, agents, officers, directors, and affiliates, from and against any and all claims, damages, causes of action, penalties, interest, demands, obligations, losses, liabilities, costs or debt, additional taxes, and expenses (including but not limited to attorneys' fees), resulting from or arising out of (i) your use and access of, or inability to use or access, openpilot, (ii) your breach of these Terms, (iii) the inaccuracy of any information, representation or warranty made by you, (iv) activities of anyone other than you in connection with flowpilot conducted through your flowdrive device or account, (v) any other of your activities under or in connection with these Terms or flowpilot.\n" +
            "\n" +
            "Limitation of Liability:\n\n" +
            "In no event shall flowdrive, nor its directors, employees, partners, agents, suppliers, or affiliates, be liable for any indirect, incidental, special, consequential or punitive damages, including without limitation, loss of profits, data, use, goodwill, or other intangible losses, resulting from (i) your access to or use of or inability to access or use of the Software; or (ii) any conduct or content of any third party on the Software whether based on warranty, contract, tort (including negligence) or any other legal theory, whether or not we have been informed of the possibility of such damage, and even if a remedy set forth herein is found to have failed of its essential purpose.\n" +
            "\n" +
            "No Warranty or Obligations to Maintain or Service:\n\n" +
            "flowdrive provides flowpilot without representations, conditions, or warranties of any kind. flowpilot is provided on an \"AS IS\" and \"AS AVAILABLE\" basis, including with all faults and errors as may occur. To the extent permitted by law and unless prohibited by law, flowdrive on behalf of itself and all persons and parties acting by, through, or for flowdrive, explicitly disclaims all warranties or conditions, express, implied, or collateral, including any implied warranties of merchantability, satisfactory quality, and fitness for a particular purpose in respect of flowpilot.\n" +
            "\n" +
            "To the extent permitted by law, flowdrive does not warrant the flowpilot, performance, or availability of flowpilot under all conditions. flowdrive is not responsible for any failures caused by server errors, misdirected or redirected transmissions, failed internet connections, interruptions or failures in the transmission of data, any computer virus, or any acts or omissions of third parties that damage the network or impair wireless service.\n" +
            "\n" +
            "We undertake reasonable measures to preserve and secure information collected through our flowpilot. However, no data collection, transmission or storage system is 100% secure, and there is always a risk that your information may be intercepted without our consent. In using flowpilot, you acknowledge that flowdrive is not responsible for intercepted information, and you hereby release us from any and all claims arising out of or related to the use of intercepted information in any unauthorized manner.\n" +
            "\n" +
            "By providing flowpilot, flowdrive does not transfer or license its intellectual property or grant rights in its brand names, nor does comma make representations with respect to third-party intellectual property rights.\n" +
            "\n" +
            "We are not obligated to provide any maintenance or support for flowpilot, technical or otherwise. If we voluntarily provide any maintenance or support for flowpilot, we may stop any such maintenance, support, or services at any time in our sole discretion.\n" +
            "\n" +
            "Modification of Software:\n\n" +
            "In no event shall flowdrive, nor its directors, employees, partners, agents, suppliers, or affiliates, be liable if you choose to modify the software.\n" +
            "\n" +
            "Changes:\n\n" +
            "We reserve the right, at our sole discretion, to modify or replace these Terms at any time. If a revision is material we will provide at least 15 days' notice prior to any new terms taking effect. What constitutes a material change will be determined at our sole discretion.\n" +
            "\n" +
            "By continuing to access or use our Software after any revisions become effective, you agree to be bound by the revised terms. If you do not agree to the new terms, you are no longer authorized to use the Software.\n" +
            "\n" +
            "Contact Us:\n\n" +
            "If you have any questions about these Terms, please contact us at support@flowdrive.ai.";

    public TermsScreen(FlowUI appContext) {
        this.appContext = appContext;

        params = appContext.params;
        stage = new Stage(new FitViewport(1920, 1080));
        batch = new SpriteBatch();

        rootTable = new Table();
        rootTable.setFillParent(true);

        termsText = new Label(termsString, appContext.skin);
        termsText.setAlignment(Align.center);
        termsText.setWrap(true);

        scrollPane = new ScrollPane(termsText, appContext.skin);
        scrollPane.setSmoothScrolling(true);

        acceptTerms = new TextButton("ACCEPT", appContext.skin, "blue");
        acceptTerms.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                params.putBool("HasAcceptedTerms", true);
                appContext.setScreen(new SetUpScreen(appContext));
            }
        });

        rootTable.add(new Label("The Terms and Conditions below are effective for all users", appContext.skin, "default-font-bold-med", "white")).center().padTop(30);
        rootTable.row();
        rootTable.add(new Label("Last Updated on July 13, 2022", appContext.skin, "default-font-bold-med", "white")).center().padTop(20);
        rootTable.row();
        rootTable.add(scrollPane).width(1720).pad(30);
        rootTable.row();
        rootTable.add(acceptTerms).center().pad(20);
        stage.addActor(rootTable);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(.0f, .0f, .0f, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height);
    }

    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        Gdx.input.setInputProcessor(null);
        stage.dispose();
    }
}
