package ai.flow.app.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
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

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {
	public static void main(String[] args) throws IOException {
		createApplication();
	}

	private static Lwjgl3Application createApplication() throws IOException {
		SensorInterface cameraManager;
		if (System.getenv("USE_VIDEO_STREAM") != null) {
			cameraManager = new CameraManager("roadCameraState", 30, "tmp", 1164, 874);
		}
		else {
			// use external stream. 
			cameraManager = new CameraManager("roadCameraState", 30, Integer.parseInt(System.getenv("EXTERNAL_STREAM_URL")), 1164, 874);
		}
		SensorManager sensorManager = new SensorManager();

		Map<String, SensorInterface> sensors = new HashMap<String, SensorInterface>() {{
			put("roadCamera", cameraManager);
			put("motionSensors", sensorManager);
		}};
		Launcher launcher = new Launcher(sensors, new ModelExecutor());
		return new Lwjgl3Application(new FlowUI(launcher, SystemUtils.getPID()), getDefaultConfiguration());
	}

	private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
		Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
		configuration.setTitle("flow-pilot");
		configuration.setWindowedMode(1000, 562);
		configuration.setResizable(true);
		configuration.setForegroundFPS(25);
		configuration.setWindowIcon("icons/flow-pilot.png");
		configuration.setBackBufferConfig(8, 8, 8, 8, 16, 0, 8);
		return configuration;
	}
}
