package ai.flow.launcher;

import ai.flow.common.ParamsInterface;
import ai.flow.common.transformatons.Camera;
import ai.flow.modeld.ModelExecutor;
import ai.flow.modeld.ModelRunner;
import ai.flow.modeld.TNNModelRunner;
import ai.flow.sensor.SensorInterface;
import ai.flow.sensor.SensorManager;
import ai.flow.sensor.camera.DualCameraManager;

import java.io.IOException;
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

    public void startModelD() {
        if (!modeld.isRunning())
            modeld.start();
    }

    public void startSensorD() {
        for (String sensorName : sensors.keySet()) {
            if (!sensors.get(sensorName).isRunning())
                sensors.get(sensorName).start();
        }
    }

    public void startPythonDaemons(){
        flowInitd.send(FlowInitd.SIGSTART);
    }

    public void dispose() {
        for (String sensorName : sensors.keySet()) {
            sensors.get(sensorName).stop();
        }
        modeld.stop();
        flowInitd.send(FlowInitd.SIGSTOP);
    }

    public void startAllD() {
        startSensorD();
        startModelD();
        startPythonDaemons();
    }

    public void main(String[] args) throws IOException {

        cameraManager = new DualCameraManager(System.getenv("WIDE_ROAD_CAMERA_SOURCE"), System.getenv("ROAD_CAMERA_SOURCE"), 20, Camera.frameSize[0], Camera.frameSize[1]);
        SensorManager sensorManager = new SensorManager();
        this.sensors = new HashMap<String, SensorInterface>() {{
            put("roadCamera", cameraManager);
            put("motionSensors", sensorManager);
        }};

        String modelPath = "models/supercombo";
        ModelRunner model = new TNNModelRunner(modelPath, true);

        this.modeld = new ModelExecutor(model);
        this.startAllD();
    }
}
