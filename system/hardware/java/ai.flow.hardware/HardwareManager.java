package ai.flow.hardware;

public abstract class HardwareManager {
    public void setBrightness(float brightness){}
    public void turnOffScreen(boolean doTurnOff){}
    public void enableCPUWakeLock(boolean enable){}
    public void enableScreenWakeLock(boolean enable){}
;}
