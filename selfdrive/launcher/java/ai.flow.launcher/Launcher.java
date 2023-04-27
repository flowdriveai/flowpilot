package ai.flow.launcher;

import ai.flow.common.ParamsInterface;
import ai.flow.common.Path;
import ai.flow.common.transformations.Camera;
import ai.flow.modeld.*;
import ai.flow.sensor.SensorInterface;
import ai.flow.sensor.SensorManager;
import ai.flow.sensor.camera.CameraManager;

import java.util.HashMap;
import java.util.Map;


public class Launcher {
    public ModelExecutor modeld;
    public Map<String, SensorInterface> sensors;
    public FlowInitd flowInitd = new FlowInitd();
    public ParamsInterface params = ParamsInterface.getInstance();
    SensorInterface cameraManager;

    public Launcher(Map<String, SensorInterface> sensors, ModelExecutor modelExecutor){
        this.sensors = sensors;
        this.modeld = modelExecutor;
    }

    public void initModelD() {
        modeld.init();
    }

    public void startSensorD() {
        for (String sensorName : sensors.keySet()) {
            if (!sensors.get(sensorName).isRunning())
                sensors.get(sensorName).start();
        }
    }

    public void dispose() {
        for (String sensorName : sensors.keySet()) {
            sensors.get(sensorName).stop();
        }
        modeld.dispose();
        flowInitd.send(FlowInitd.SIGSTOP);
    }

    public void startAllD() {
        startSensorD();
        initModelD();
    }

    public void main(String[] args) {
        CameraManager fCameraManager = new CameraManager(20, System.getenv("ROAD_CAMERA_SOURCE"), Camera.frameSize[0], Camera.frameSize[1]);
        SensorManager sensorManager = new SensorManager();
        this.sensors = new HashMap<String, SensorInterface>() {{
            put("roadCamera", fCameraManager);
            put("motionSensors", sensorManager);
        }};

        String modelPath = Path.getModelDir();

        ModelRunner model = new TNNModelRunner(modelPath, true);

        ModelExecutor modelExecutor;
        modelExecutor = new ModelExecutorF2(model);

        this.modeld = modelExecutor;
        this.startAllD();
    }
}
