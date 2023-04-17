package ai.flow.android;

import ai.flow.android.sensor.CameraManager;
import ai.flow.android.sensor.SensorManager;
import ai.flow.android.vision.SNPEModelRunner;
import ai.flow.app.FlowUI;
import ai.flow.common.ParamsInterface;
import ai.flow.common.Path;
import ai.flow.common.transformations.Camera;
import ai.flow.launcher.Launcher;
import ai.flow.modeld.*;
import ai.flow.sensor.SensorInterface;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Process;
import android.os.*;
import android.provider.Settings;
import android.system.ErrnoException;
import android.system.Os;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.AndroidFragmentApplication;
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

import static android.os.Build.VERSION.SDK_INT;

/** Launches the Android application. */
public class AndroidLauncher extends FragmentActivity implements AndroidFragmentApplication.Callbacks {
	public static Map<String, SensorInterface> sensors;
	public static Context appContext;
	public static ParamsInterface params;
	List<String> requiredPermissions = Arrays.asList(Manifest.permission.CAMERA,
			Manifest.permission.WRITE_EXTERNAL_STORAGE,
			Manifest.permission.READ_EXTERNAL_STORAGE,
			Manifest.permission.RECORD_AUDIO,
			Manifest.permission.READ_PHONE_STATE,
			Manifest.permission.WAKE_LOCK,
			Manifest.permission.VIBRATE);

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

		// keep app from dimming due to inactivity.
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// get wakelock so we can switch windows without getting killed.
		PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
		PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ai.flow.app::wakelock");
		wakeLock.acquire();

		// tune system for max throughput. Does this really help ?
		//if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
		//	getWindow().setSustainedPerformanceMode(true);
		//}

		// request permissions and wait till granted.
		requestPermissions();
		while (!checkPermissions()){
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		String dongleID = "";
		if (telephonyManager != null) {
			dongleID = Settings.Secure.getString(appContext.getContentResolver(), Settings.Secure.ANDROID_ID);
		}

		// TODO, this is very hacky, find simpler way
		params = ParamsInterface.getInstance();
		boolean done = false;
		int keyvaldFailCount = 1;
		while (!done){
			try {
				if (!params.initialized()){
					if (keyvaldFailCount <= 20 & keyvaldFailCount % 20 == 0)
						Toast.makeText(appContext, "Waiting for flowpilot services to start", Toast.LENGTH_LONG).show();
					else if (keyvaldFailCount > 20 & (keyvaldFailCount-20) % 50 == 0)
						Toast.makeText(appContext, "Waiting for flowpilot services to start. Did you start 'launch_flowpilot.sh' ?", Toast.LENGTH_LONG).show();
					try {
						Thread.sleep(200);
					} catch (InterruptedException ex) {
						throw new RuntimeException(ex);
					}
					params.dispose();
					params = ParamsInterface.getInstance();
					keyvaldFailCount++;
				}
				else{
					done = true;
					params.dispose();
					params = ParamsInterface.getInstance();
				}
			} catch (Exception e){
			}
		}

		// populate device specific info.
		params.put("DongleId", dongleID);
		params.put("DeviceManufacturer", Build.MANUFACTURER);
		params.put("DeviceModel", Build.MODEL);

		AndroidApplicationConfiguration configuration = new AndroidApplicationConfiguration();
		CameraManager cameraManager = new CameraManager(getApplication().getApplicationContext(), 20);
		SensorManager sensorManager = new SensorManager(appContext, "sensorEvents", 50);
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

		MainFragment fragment = new MainFragment(new FlowUI(launcher, pid));
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

	private boolean checkPermissions() {
		for (String permission: requiredPermissions){
			if (ContextCompat.checkSelfPermission(appContext, permission) != PackageManager.PERMISSION_GRANTED) {
				return false;
			}
		}

		// External storage access permissions for android 12 and above.
		if (SDK_INT >= Build.VERSION_CODES.R)
			return Environment.isExternalStorageManager();
		return true;
	}

	private void requestPermissions() {
		List<String> requestPermissions = new ArrayList<>();
		for (String permission: requiredPermissions){
			if (ContextCompat.checkSelfPermission(appContext, permission) != PackageManager.PERMISSION_GRANTED)
				requestPermissions.add(permission);
		}
		if (!requestPermissions.isEmpty())
			ActivityCompat.requestPermissions(this, requestPermissions.toArray(new String[0]), 1);

		// External storage access permissions for android 12 and above.
		if (SDK_INT >= Build.VERSION_CODES.R) {
			if (Environment.isExternalStorageManager())
				return;
			try {
				Toast.makeText(appContext, "grant external storage access to flowpilot.", Toast.LENGTH_LONG).show();
				Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
				intent.addCategory("android.intent.category.DEFAULT");
				intent.setData(Uri.parse(String.format("package:%s",getApplicationContext().getPackageName())));
				startActivityForResult(intent, 6969);
			} catch (Exception e) {
				Intent intent = new Intent();
				intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
				startActivityForResult(intent, 6969);
			}
		}
	}

	@Override
	public void exit() {

	}
}

