package ai.flow.sensor.camera;

import ai.flow.common.transformations.Camera;
import ai.flow.sensor.SensorInterface;

public class DualCameraManager extends SensorInterface {
    public CameraManager eCameraManager;
    public CameraManager fCameraManager;
    public DualCameraManager(String ecamSrc, String fcamSrc, int frequency, int frameWidth, int frameHeight){
        eCameraManager = new CameraManager(Camera.CAMERA_TYPE_WIDE, frequency, ecamSrc, frameWidth, frameHeight);
        fCameraManager = new CameraManager(Camera.CAMERA_TYPE_ROAD, frequency, fcamSrc, frameWidth, frameHeight);
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
