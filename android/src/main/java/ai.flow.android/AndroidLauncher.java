package ai.flow.android;

import ai.flow.android.sensor.CameraManager;
import ai.flow.android.sensor.SensorManager;
import ai.flow.android.vision.SNPEModelRunner;
import ai.flow.app.FlowUI;
import ai.flow.common.ParamsInterface;
import ai.flow.common.Path;
import ai.flow.common.transformations.Camera;
import ai.flow.hardware.HardwareManager;
import ai.flow.launcher.Launcher;
import ai.flow.modeld.*;
import ai.flow.sensor.SensorInterface;
import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.os.Process;
import android.os.*;
import android.provider.Settings;
import android.system.ErrnoException;
import android.system.Os;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.AndroidFragmentApplication;
import com.termux.shared.termux.TermuxConstants;
import org.acra.ACRA;
import org.acra.BuildConfig;
import org.acra.ErrorReporter;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.HttpSenderConfigurationBuilder;
import org.acra.config.ToastConfigurationBuilder;
import org.acra.data.StringFormat;
import org.acra.sender.HttpSender;
import org.jetbrains.annotations.NotNull;

import java.util.*;


/** Launches the main android flowpilot application. */
public class AndroidLauncher extends FragmentActivity implements AndroidFragmentApplication.Callbacks {
	public static Map<String, SensorInterface> sensors;
	public static Context appContext;
	public static ParamsInterface params;

	@SuppressLint("HardwareIds")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		appContext = getApplicationContext();

		// set environment variables from intent extras.
		Bundle bundle = getIntent().getExtras();
		if (bundle != null) {
			for (String key : bundle.keySet()) {
				if (bundle.get(key) == null)
					continue;
				try {
					Os.setenv(key, (String)bundle.get(key), true);
				} catch (Exception ignored) {
				}
			}
		}

		try {
			Os.setenv("USE_GPU", "1", true);
		} catch (ErrnoException e) {
			throw new RuntimeException(e);
		}


		HardwareManager androidHardwareManager = new AndroidHardwareManager(getWindow());
		// keep app from dimming due to inactivity.
		androidHardwareManager.enableScreenWakeLock(true);

		// get wakelock so we can switch windows without getting killed.
		PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
		PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ai.flow.app::wakelock");

		// acquiring wakelock causes crash on some devices.
		try {
			wakeLock.acquire();
		} catch (Exception e){
			System.err.println(e);
		}

		// tune system for max throughput. Does this really help ?
		//if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
		//	getWindow().setSustainedPerformanceMode(true);
		//}

		params = ParamsInterface.getInstance();

		TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		String dongleID = "";
		if (telephonyManager != null) {
			dongleID = Settings.Secure.getString(appContext.getContentResolver(), Settings.Secure.ANDROID_ID);
		}

		// populate device specific info.
		params.put("DongleId", dongleID);
		params.put("DeviceManufacturer", Build.MANUFACTURER);
		params.put("DeviceModel", Build.MODEL);

		AndroidApplicationConfiguration configuration = new AndroidApplicationConfiguration();
		CameraManager cameraManager = new CameraManager(getApplication().getApplicationContext(), 20);
		SensorManager sensorManager = new SensorManager(appContext, 100);
		sensors = new HashMap<String, SensorInterface>() {{
			put("roadCamera", cameraManager);
			put("motionSensors", sensorManager);
		}};

		int pid = Process.myPid();

		String modelPath = Path.getModelDir();

		ModelRunner model;
		boolean useGPU = true; // always use gpus on android phones.
		if (params.getBool("UseSNPE"))
			model = new SNPEModelRunner(getApplication(), modelPath, useGPU);
		else
			model = new TNNModelRunner(modelPath, useGPU);

		ModelExecutor modelExecutor;
		modelExecutor = new ModelExecutorF2(model);
		Launcher launcher = new Launcher(sensors, modelExecutor);

		ErrorReporter ACRAreporter = ACRA.getErrorReporter();
		ACRAreporter.putCustomData("DongleId", dongleID);
		ACRAreporter.putCustomData("AndroidAppVersion", ai.flow.app.BuildConfig.VERSION_NAME);
		ACRAreporter.putCustomData("FlowpilotVersion", params.getString("Version"));
		ACRAreporter.putCustomData("VersionMisMatch", checkVersionMisMatch().toString());

		ACRAreporter.putCustomData("GitCommit", params.getString("GitCommit"));
		ACRAreporter.putCustomData("GitBranch", params.getString("GitBranch"));
		ACRAreporter.putCustomData("GitRemote", params.getString("GitRemote"));

		MainFragment fragment = new MainFragment(new FlowUI(launcher, androidHardwareManager, pid));
		cameraManager.setLifeCycleFragment(fragment);
		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		trans.replace(android.R.id.content, fragment);
		trans.commit();
	}

	public static class MainFragment extends AndroidFragmentApplication {
		FlowUI flowUI;

		MainFragment(FlowUI flowUI) {
			this.flowUI = flowUI;
		}

		@Override
		public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			return initializeForView(flowUI);
		}
	}

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		initACRA(base);
	}

	private void initACRA(Context base) {
		String ACRA_URI = null, ACRA_AUTH_LOGIN = null, ACRA_AUTH_PASSWORD = null;
		try {
			ACRA_URI = (String)ai.flow.app.BuildConfig.class.getField("ACRA_URI").get(null);
			ACRA_AUTH_LOGIN = (String)ai.flow.app.BuildConfig.class.getField("ACRA_AUTH_LOGIN").get(null);
			ACRA_AUTH_PASSWORD = (String)ai.flow.app.BuildConfig.class.getField("ACRA_AUTH_PASSWORD").get(null);
		} catch (Exception e) {}

		if (ACRA_URI == null || ACRA_AUTH_LOGIN == null || ACRA_AUTH_PASSWORD == null)
			return;

		CoreConfigurationBuilder builder = new CoreConfigurationBuilder(this)
				.withBuildConfigClass(BuildConfig.class)
				.withReportFormat(StringFormat.JSON);

		builder.getPluginConfigurationBuilder(HttpSenderConfigurationBuilder.class)
				.withUri(ACRA_URI)
				.withBasicAuthLogin(ACRA_AUTH_LOGIN)
				.withBasicAuthPassword(ACRA_AUTH_PASSWORD)
				.withHttpMethod(HttpSender.Method.POST)
				.setEnabled(true);
		builder.getPluginConfigurationBuilder(ToastConfigurationBuilder.class)
				.withText("crash report sent to flowpilot maintainers")
				.setEnabled(false);

		ACRA.init((Application) base.getApplicationContext(), builder);
	}

	private Boolean checkVersionMisMatch() {
		// check version mismatch between android app and github repo project.
		if (!params.getString("Version").equals(ai.flow.app.BuildConfig.VERSION_NAME)) {
			Toast.makeText(appContext, "WARNING: App version mismatch detected. Make sure you are using compatible versions of apk and github repo.", Toast.LENGTH_LONG).show();
			return true;
		}
		return false;
	}

	@Override
	public void exit() {
	}

	@Override
	public void onBackPressed() {
		return;
	}
}

