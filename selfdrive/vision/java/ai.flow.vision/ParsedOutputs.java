package ai.flow.vision;

// Core java classes
import java.util.ArrayList;
import java.util.Arrays;

public class ParsedOutputs {
    public ArrayList<float[]> position; // path
    public ArrayList<float[]> orientation; // path
    public ArrayList<float[]> velocity; // path
    public ArrayList<float[]> orientationRate; // path

    public ArrayList<ArrayList<float[]>> laneLines;
    public float[] laneLineStds;
    public float[] laneLineProbs;
    public ArrayList<ArrayList<float[]>> roadEdges;
    public float[] roadEdgeStds;

    public ArrayList<LeadDataV3> leads;
    public MetaData metaData;

    public float[][] meta;
    public float[][] pose;
    public float[][] state;

    // Camera Odometry
    public float[] trans;
    public float[] transStd;
    public float[] rot;
    public float[] rotStd;

    public ParsedOutputs(){
        this.initLanes();
        this.initLeads();
        this.initMeta();
        this.initCameraOdometery();
        this.initLanes();
        this.initRoadEdges();
    }

    public ArrayList<float[]> initXYZT(){
        float[] x_arr = new float[Parser.TRAJECTORY_SIZE];
        float[] y_arr = new float[Parser.TRAJECTORY_SIZE];
        float[] z_arr = new float[Parser.TRAJECTORY_SIZE];
        float[] t_arr = new float[Parser.TRAJECTORY_SIZE];
        float[] x_std = new float[Parser.TRAJECTORY_SIZE];
        float[] y_std = new float[Parser.TRAJECTORY_SIZE];
        float[] z_std = new float[Parser.TRAJECTORY_SIZE];
        return new ArrayList<float[]>(Arrays.asList(
            x_arr, y_arr, z_arr, t_arr, x_std, y_std, z_std
        ));
    }

    public void initLanes() {
        position = initXYZT();
        orientation = initXYZT();
        velocity = initXYZT();
        orientationRate = initXYZT();

        laneLines = new ArrayList<ArrayList<float[]>>();
        laneLineStds = new float[4];
        laneLineProbs = new float[4];

        for (int i = 0; i < 4; i++)
            laneLines.add(initXYZT());
    }

    public void initRoadEdges() {
        roadEdges = new ArrayList<ArrayList<float[]>>();
        roadEdgeStds = new float[2];
        for (int i = 0; i < 4; i++)
            roadEdges.add(initXYZT());
    }

    public void initMeta() {
        metaData = new MetaData();

        meta = new float[1][44];
        state = new float[1][512];
    }

    public void initCameraOdometery(){
        pose = new float[1][12];
        trans = new float[3];
        transStd = new float[3];
        rot = new float[3];
        rotStd = new float[3];
    }

    public void initLeads(){
        leads = new ArrayList<LeadDataV3>();
        for (int i=0; i<Parser.LEAD_MHP_SELECTION; i++){
            leads.add(new LeadDataV3());
        }
    }
}
