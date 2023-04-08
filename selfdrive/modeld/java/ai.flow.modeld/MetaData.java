package ai.flow.modeld;

public class MetaData {
    public float engagedProb = 0.0f;
    public float[] desirePrediction = new float[4*CommonModelF2.DESIRE_LEN];
    public float[] desireState = new float[CommonModelF2.DESIRE_LEN];
    public DisengagePredictions disengagePredictions = new DisengagePredictions();
    public boolean hardBrakePredicted = false;
}
