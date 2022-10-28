package ai.flow.app.lwjgl3;

import ai.flow.vision.ModelExecutor;
import ai.flow.vision.ModelRunner;
import ai.flow.vision.ONNXModelRunner;
import ai.flow.vision.TNNModelRunner;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import ai.flow.common.SystemUtils;
import ai.flow.launcher.Launcher;
import ai.flow.sensor.SensorInterface;
import ai.flow.sensor.SensorManager;
import ai.flow.sensor.camera.CameraManager;
import ai.flow.app.FlowUI;
import ai.flow.sensor.camera.DummyCameraManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static ai.flow.common.SystemUtils.getUseGPU;

/** Launches the desktop application. */
public class Lwjgl3Launcher {
	public static void main(String[] args) throws IOException {
		createApplication();
	}

	private static Lwjgl3Application createApplication() throws IOException {
		SensorInterface cameraManager;
		cameraManager = new CameraManager("roadCameraState", 20, System.getenv("ROAD_CAMERA_SOURCE"), 1164, 874);
		SensorManager sensorManager = new SensorManager();

		Map<String, SensorInterface> sensors = new HashMap<String, SensorInterface>() {{
			put("roadCamera", cameraManager);
			put("motionSensors", sensorManager);
		}};

		String modelPath = "models/supercombo";

		// onnx CPU performs better than TNN.
		ModelRunner model;
		if (getUseGPU())
			model = new TNNModelRunner(modelPath, getUseGPU());
		else
			model = new ONNXModelRunner(modelPath, getUseGPU());

		Launcher launcher = new Launcher(sensors, new ModelExecutor(model));
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
