package ai.flow.app.headless;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import ai.flow.common.SystemUtils;
import ai.flow.launcher.Launcher;
import ai.flow.sensor.SensorInterface;
import ai.flow.sensor.SensorManager;
import ai.flow.sensor.camera.CameraManager;
import ai.flow.app.FlowUI;
import ai.flow.sensor.camera.DummyCameraManager;
import ai.flow.vision.ModelExecutor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/** Launches the headless application */
public class HeadlessLauncher {
	public static void main(String[] args) throws IOException {
		createApplication();
	}

	private static Application createApplication() throws IOException {
		// Note: you can use a custom ApplicationListener implementation for the headless project instead of FlowUI.
		SensorInterface cameraManager;
		cameraManager = new CameraManager("roadCameraState", 20, System.getenv("ROAD_CAMERA_SOURCE"), 1164, 874);

		SensorManager sensorManager = new SensorManager();
		Map<String, SensorInterface> sensors = new HashMap<String, SensorInterface>() {{
			put("roadCamera", cameraManager);
			put("motionSensors", sensorManager);
		}};
		Launcher launcher = new Launcher(sensors, new ModelExecutor());
		return new HeadlessApplication(new FlowUI(launcher, SystemUtils.getPID()), getDefaultConfiguration());
	}

	private static HeadlessApplicationConfiguration getDefaultConfiguration() {
		HeadlessApplicationConfiguration configuration = new HeadlessApplicationConfiguration();
		configuration.updatesPerSecond = -1; // When this value is negative, FlowUI#render() is never called.
		return configuration;
	}
}
