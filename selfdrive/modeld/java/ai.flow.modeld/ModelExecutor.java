package ai.flow.modeld;

public abstract class ModelExecutor {
    public void start(){}
    public long getIterationRate(){return 0;}
    public float getFrameDropPercent() {return 0f;}
    public boolean isRunning() {return false;}
    public void dispose(){}
    public void stop() {}
}
