package ai.flow.vision;

import static ai.flow.vision.Parser.NUM_META_INTERVALS;

public class DisengagePredictions {
    public float[] t = {2f, 4f, 6f, 8f, 10f};
    public float[] brakeDisengageProbs = new float[NUM_META_INTERVALS];
    public float[] gasDesengageProbs = new float[NUM_META_INTERVALS];
    public float[] steerOverrideProbs = new float[NUM_META_INTERVALS];
    public float[] brake3MetersPerSecondSquaredProbs = new float[NUM_META_INTERVALS];
    public float[] brake4MetersPerSecondSquaredProbs = new float[NUM_META_INTERVALS];
    public float[] brake5MetersPerSecondSquaredProbs = new float[NUM_META_INTERVALS];
}
