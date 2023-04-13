package ai.flow.modeld;

import static ai.flow.modeld.CommonModelF2.LEAD_TRAJ_LEN;

public class LeadDataV3 {
    public float prob = 0.0f;
    public float probTime = 0.0f;
    public float[] t = {0.0f, 2.0f, 4.0f, 6.0f, 8.0f, 10.0f};
    public float[] x = new float[LEAD_TRAJ_LEN];
    public float[] y = new float[LEAD_TRAJ_LEN];
    public float[] v = new float[LEAD_TRAJ_LEN];
    public float[] a = new float[LEAD_TRAJ_LEN];
    public float[] XStd = new float[LEAD_TRAJ_LEN];
    public float[] YStd = new float[LEAD_TRAJ_LEN];
    public float[] VStd = new float[LEAD_TRAJ_LEN];
    public float[] AStd = new float[LEAD_TRAJ_LEN];
}
