package ai.flow.vision;

public interface ModelExecutorInterface {
    public void start();
    public void stop();
    public boolean isRunning();
    public long getIterationRate();
    public float getFrameDropPercent();
}
