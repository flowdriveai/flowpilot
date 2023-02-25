package ai.flow.app.headless;

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
import com.badlogic.gdx.Application;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static ai.flow.common.SystemUtils.getUseGPU;


/** Launches the headless application */
public class HeadlessLauncher {
	public static void main(String[] args) throws IOException {
		createApplication();
	}

	private static Application createApplication() throws IOException {
		// Note: you can use a custom ApplicationListener implementation for the headless project instead of FlowUI.
		SensorInterface cameraManager;
		cameraManager = new DualCameraManager(System.getenv("WIDE_ROAD_CAMERA_SOURCE"), System.getenv("ROAD_CAMERA_SOURCE"), 20, Camera.frameSize[0], Camera.frameSize[1]);

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
		return new HeadlessApplication(new FlowUI(launcher, SystemUtils.getPID()), getDefaultConfiguration());
	}

	private static HeadlessApplicationConfiguration getDefaultConfiguration() {
		HeadlessApplicationConfiguration configuration = new HeadlessApplicationConfiguration();
		configuration.updatesPerSecond = -1; // When this value is negative, FlowUI#render() is never called.
		return configuration;
	}
}
