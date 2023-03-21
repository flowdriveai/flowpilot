package ai.flow.vision;

public class LeadDataV3 {
    public float prob = 0.0f;
    public float probTime = 0.0f;
    public float[] t = {0.0f, 2.0f, 4.0f, 6.0f, 8.0f, 10.0f};
    public float[] x = new float[Parser.LEAD_TRAJ_LEN];
    public float[] y = new float[Parser.LEAD_TRAJ_LEN];
    public float[] v = new float[Parser.LEAD_TRAJ_LEN];
    public float[] a = new float[Parser.LEAD_TRAJ_LEN];
    public float[] XStd = new float[Parser.LEAD_TRAJ_LEN];
    public float[] YStd = new float[Parser.LEAD_TRAJ_LEN];
    public float[] VStd = new float[Parser.LEAD_TRAJ_LEN];
    public float[] AStd = new float[Parser.LEAD_TRAJ_LEN];
}
