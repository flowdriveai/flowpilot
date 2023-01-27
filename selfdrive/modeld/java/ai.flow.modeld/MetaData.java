package ai.flow.modeld;

public class MetaData {
    public float engagedProb = 0.0f;
    public float[] desirePrediction = new float[4*CommonModel.DESIRE_LEN];
    public float[] desireState = new float[CommonModel.DESIRE_LEN];
    public DisengagePredictions disengagePredictions = new DisengagePredictions();
    public boolean hardBrakePredicted = false;
}
