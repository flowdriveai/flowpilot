package ai.flow.android;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.system.ErrnoException;
import android.system.Os;
import android.telephony.TelephonyManager;
import android.view.WindowManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import ai.flow.android.sensor.CameraManager;
import ai.flow.android.sensor.SensorManager;
import ai.flow.android.vision.ModelExecutor;
import ai.flow.app.FlowUI;
import ai.flow.common.Params;
import ai.flow.common.ParamsInterface;
import ai.flow.launcher.Launcher;
import ai.flow.sensor.SensorInterface;

import java.util.*;

import org.acra.ACRA;
import org.acra.BuildConfig;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.data.StringFormat;
import org.acra.config.ToastConfigurationBuilder;
import org.acra.config.HttpSenderConfigurationBuilder;
import org.acra.sender.HttpSender;


/** Launches the Android application. */
public class AndroidLauncher extends AndroidApplication {

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		initACRA(base);
	}

	public static Map<String, SensorInterface> sensors;
	public static Context appContext;
	List<String> requiredPermissions = Arrays.asList(android.Manifest.permission.CAMERA,
			android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
			android.Manifest.permission.READ_EXTERNAL_STORAGE,
			android.Manifest.permission.RECORD_AUDIO,
			android.Manifest.permission.READ_PHONE_STATE,
			Manifest.permission.VIBRATE);

	@SuppressLint("HardwareIds")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		appContext = getContext();

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

		// keep app from dimming due to inactivity.
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// request permissions and wait till granted.
		requestPermissions();
		while (!checkPermissions()){
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		ParamsInterface params = ParamsInterface.getInstance();
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
		CameraManager cameraManager = new CameraManager(appContext, 20, "roadCameraState");
		SensorManager sensorManager = new SensorManager(appContext, "sensorEvents", 50);
		sensors = new HashMap<String, SensorInterface>() {{
			put("roadCamera", cameraManager);
			put("motionSensors", sensorManager);
		}};
		int pid = android.os.Process.myPid();
		Launcher launcher = new Launcher(sensors, new ModelExecutor((Application) getContext().getApplicationContext()));
		initialize(new FlowUI(launcher, pid), configuration);
	}

	private void initACRA(Context base) {
		if(ACRA.isACRASenderServiceProcess()) return;

		ACRA.init((Application) base.getApplicationContext(), new CoreConfigurationBuilder()
				.withBuildConfigClass(BuildConfig.class)
				.withReportFormat(StringFormat.JSON)
				.withPluginConfigurations(
						new ToastConfigurationBuilder()
								.withText(getString(ai.flow.app.R.string.acra_toast_text))
								.build(),
						new HttpSenderConfigurationBuilder()
								.withUri("https://acra.flowdrive.ai/report")
								.withBasicAuthLogin("lDbhwiSZ1wqwqfWe")
								.withBasicAuthPassword("PJLqQ4eG5GKYf8ZP")
								.withHttpMethod(HttpSender.Method.POST)
								.build()
				)
		);
	}

	private boolean checkPermissions() {
		for (String permission: requiredPermissions){
			if (ContextCompat.checkSelfPermission(appContext, permission) != PackageManager.PERMISSION_GRANTED) {
				System.out.println(permission);
				return false;
			}
		}
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
	}
}
