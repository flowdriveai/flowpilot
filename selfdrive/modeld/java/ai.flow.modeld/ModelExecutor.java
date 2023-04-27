package ai.flow.modeld;

public abstract class ModelExecutor {
    public void init(){}
    public long getIterationRate(){return 0;}
    public float getFrameDropPercent() {return 0f;}
    public boolean isRunning() {return false;}
    public boolean isInitialized() {return false;}
    public void dispose(){}
    public void stop() {}
    public void start() {}
}
