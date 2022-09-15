package ai.flow.sensor;

public abstract class SensorInterface {
    public void start(){}
    public void stop(){}
    public boolean isRunning(){return false;}
    public boolean isRecording(){return false;}
    public void updateProperty(String property, float[] value){}
    public void updateProperty(String property, float value){}
    public void updateProperty(String property, int[] value){}
    public void updateProperty(String property, int value){}
    public void updateProperty(String property, boolean[] value){}
    public void updateProperty(String property, boolean value){}
    public void record(boolean shouldRecord){}
}
