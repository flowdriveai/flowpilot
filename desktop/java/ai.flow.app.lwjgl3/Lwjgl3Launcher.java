package ai.flow.app.lwjgl3;

import ai.flow.app.FlowUI;
import ai.flow.common.SystemUtils;
import ai.flow.common.transformations.Camera;
import ai.flow.launcher.Launcher;
import ai.flow.modeld.ModelExecutor;
import ai.flow.modeld.ModelRunner;
import ai.flow.modeld.ONNXModelRunner;
import ai.flow.modeld.TNNModelRunner;
import ai.flow.sensor.SensorInterface;
import ai.flow.sensor.SensorManager;
import ai.flow.sensor.camera.DualCameraManager;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

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
		//cameraManager = new CameraManager("wideRoadCameraState", 20, System.getenv("ROAD_CAMERA_SOURCE"), Camera.frameSize[0], Camera.frameSize[1]);
		cameraManager = new DualCameraManager(System.getenv("WIDE_ROAD_CAMERA_SOURCE"), System.getenv("ROAD_CAMERA_SOURCE"), 20, Camera.frameSize[0], Camera.frameSize[1]);
		SensorManager sensorManager = new SensorManager();

		Map<String, SensorInterface> sensors = new HashMap<String, SensorInterface>() {{
			put("wideRoadCamera", cameraManager);
			put("motionSensors", sensorManager);
		}};

		String modelPath = "selfdrive/assets/models/supercombo";

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
		configuration.setWindowedMode(1200, 600);
		configuration.setResizable(true);
		configuration.setForegroundFPS(25);
		configuration.setWindowIcon("icons/flow-pilot.png");
		configuration.setBackBufferConfig(8, 8, 8, 8, 16, 0, 8);
		return configuration;
	}
}
