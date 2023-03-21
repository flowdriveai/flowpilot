package ai.flow.modeld;

// Core java classes

public class CommonModelF2 {
    public static final int DESIRE_LEN = 8;
    public static final int TRAJECTORY_SIZE = 33;

    public static final int DESIRE_PRED_SIZE = 32;
    public static final int OTHER_META_SIZE = 32;
    public static final int NUM_META_INTERVALS = 5;
    public static final int META_STRIDE = 6;

    public static final int PLAN_MHP_N = 5;
    public static final int PLAN_MHP_COLUMNS = 15;
    public static final int PLAN_MHP_VALS = 15*33;
    public static final int PLAN_MHP_SELECTION = 1;
    public static final int PLAN_MHP_GROUP_SIZE =  (2*PLAN_MHP_VALS + PLAN_MHP_SELECTION);

    public static final int LEAD_MHP_N = 5;
    public static final int LEAD_TRAJ_LEN = 6;
    public static final int LEAD_PRED_DIM = 4;
    public static final int LEAD_MHP_VALS = LEAD_PRED_DIM * LEAD_TRAJ_LEN;
    public static final int LEAD_MHP_SELECTION = 3;
    public static final int LEAD_MHP_GROUP_SIZE = (2*LEAD_MHP_VALS + LEAD_MHP_SELECTION);

    public static final int POSE_SIZE = 12;

    public static final int PLAN_IDX = 0;
    public static final int LL_IDX = PLAN_IDX + PLAN_MHP_N * PLAN_MHP_GROUP_SIZE;
    public static final int LL_PROB_IDX = LL_IDX + 4*2*2*33;
    public static final int RE_IDX = LL_PROB_IDX + 8;
    public static final int LEAD_IDX = RE_IDX + 2*2*2*33;
    public static final int LEAD_PROB_IDX = LEAD_IDX + LEAD_MHP_N * LEAD_MHP_GROUP_SIZE;
    public static final int DESIRE_STATE_IDX = LEAD_PROB_IDX + 3;
    public static final int META_IDX = DESIRE_STATE_IDX + DESIRE_LEN;
    public static final int POSE_IDX = META_IDX + OTHER_META_SIZE + DESIRE_PRED_SIZE;
    public static final int OUTPUT_SIZE =  POSE_IDX + POSE_SIZE;
    public static final int TEMPORAL_SIZE = 512;

    public static final float FCW_THRESHOLD_5MS2_HIGH = 0.15f;
    public static final float FCW_THRESHOLD_5MS2_LOW = 0.05f;
    public static final float FCW_THRESHOLD_3MS2 = 0.7f;

    public static final float[] prev_brake_5ms2_probs = {0f, 0f, 0f, 0f, 0f};
    public static final float[] prev_brake_3ms2_probs = {0f, 0f, 0f};

    public static final  float[] T_IDXS = {0.f, 0.00976562f, 0.0390625f, 0.08789062f, 0.15625f, 0.24414062f,  0.3515625f,  0.47851562f,
            0.625f, 0.79101562f, 0.9765625f, 1.18164062f,  1.40625f,  1.65039062f,  1.9140625f,
            2.19726562f, 2.5f, 2.82226562f, 3.1640625f, 3.52539062f, 3.90625f, 4.30664062f, 4.7265625f, 5.16601562f,
            5.625f, 6.10351562f, 6.6015625f, 7.11914062f, 7.65625f, 8.21289062f, 8.7890625f, 9.38476562f, 10.f};

    public static final float[] X_IDXS = {0.f, 0.1875f, 0.75f, 1.6875f, 3.f, 4.6875f, 6.75f, 9.1875f, 12.f,  15.1875f, 18.75f, 22.6875f,
            27.f,  31.6875f,  36.75f, 42.1875f, 48.f, 54.1875f, 60.75f,  67.6875f,  75.f, 82.6875f, 90.75f, 99.1875f, 108.f, 117.1875f,
            126.75f, 136.6875f, 147.f, 157.6875f, 168.75f, 180.1875f, 192.0f};
    public static final float[] t_offsets = {0.0f, 2.0f, 4.0f};
}

