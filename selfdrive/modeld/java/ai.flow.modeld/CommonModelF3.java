package ai.flow.modeld;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

public class CommonModelF3 {

    public static final int LEAD_MHP_SELECTION = 3;
    public static final int TRAJECTORY_SIZE = 33;
    public static final int FEATURE_LEN = 128;
    public static final int HISTORY_BUFFER_LEN = 99;
    public static final int DESIRE_LEN = 8;
    public static final int DESIRE_PRED_LEN = 4;
    public static final int TRAFFIC_CONVENTION_LEN = 2;
    public static final int DRIVING_STYLE_LEN = 12;
    public static final int MODEL_FREQ = 20;
    public static final int OUTPUT_SIZE = 5978;
    // Padding to final get output shape as multiple of 4
    public static final int PAD_SIZE = 2;
    public static final int NET_OUTPUT_SIZE = OUTPUT_SIZE + FEATURE_LEN + PAD_SIZE;
    public static final float[] T_IDXS = {0.f, 0.00976562f, 0.0390625f, 0.08789062f, 0.15625f, 0.24414062f,  0.3515625f,  0.47851562f,
        0.625f, 0.79101562f, 0.9765625f, 1.18164062f,  1.40625f,  1.65039062f,  1.9140625f,
        2.19726562f, 2.5f, 2.82226562f, 3.1640625f, 3.52539062f, 3.90625f, 4.30664062f, 4.7265625f, 5.16601562f,
        5.625f, 6.10351562f, 6.6015625f, 7.11914062f, 7.65625f, 8.21289062f, 8.7890625f, 9.38476562f, 10.f};

    public static final float[] X_IDXS = {0.f, 0.1875f, 0.75f, 1.6875f, 3.f, 4.6875f, 6.75f, 9.1875f, 12.f,  15.1875f, 18.75f, 22.6875f,
        27.f,  31.6875f,  36.75f, 42.1875f, 48.f, 54.1875f, 60.75f,  67.6875f,  75.f, 82.6875f, 90.75f, 99.1875f, 108.f, 117.1875f,
        126.75f, 136.6875f, 147.f, 157.6875f, 168.75f, 180.1875f, 192.0f};

    public static final int MODEL_WIDTH = 512;
    public static final int MODEL_HEIGHT = 256;
    public static final int MODEL_FRAME_SIZE = MODEL_WIDTH * MODEL_HEIGHT * 3 / 2;
    public static final int BUF_SIZE = MODEL_FRAME_SIZE * 2;

    public static INDArray transform_scale_buffer(INDArray M, float s) {
        INDArray transform_out = Nd4j.create(new float[]{
                                            1.0f / s, 0.0f, 0.5f,
                                            0.0f, 1.0f / s, 0.5f,
                                            0.0f, 0.0f, 1.0f}, 3, 3);

        INDArray transform_in = Nd4j.create(new float[]{
                s, 0.0f, -0.5f * s,
                0.0f, s, -0.5f * s,
                0.0f, 0.0f, 1.0f}, 3, 3);

        return transform_in.mmul(M.mmul(transform_out));
    }
}

