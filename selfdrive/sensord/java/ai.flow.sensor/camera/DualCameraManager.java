package ai.flow.sensor.camera;

import ai.flow.sensor.SensorInterface;

public class DualCameraManager extends SensorInterface {
    public CameraManager eCameraManager;
    public CameraManager fCameraManager;
    public DualCameraManager(String fcamSrc, String ecamSrc, String fcamTopic, String ecamTopic, int frequency, int frameWidth, int frameHeight){
        eCameraManager = new CameraManager(ecamTopic, frequency, ecamSrc, frameWidth, frameHeight);
        fCameraManager = new CameraManager(fcamTopic, frequency, fcamSrc, frameWidth, frameHeight);
    }
    public void start() {
        eCameraManager.start();
        fCameraManager.start();
    }

    public void stop() {
        eCameraManager.stop();
        fCameraManager.stop();
    }
}
