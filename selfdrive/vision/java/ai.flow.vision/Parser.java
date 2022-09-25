package ai.flow.vision;

// Core java classes
import java.lang.Math;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class Parser {

    public static final float MAX_DISTANCE = 140f;
    public static final float MAX_REL_V = 10f;
    public static final int LEAD_X_SCALE = 10;
    public static final int LEAD_Y_SCALE = 10;
    public static final int DESIRE_PRED_SIZE = 32;
    public static final int LEAD_V_SCALE = 1;
    public static final int OTHER_META_SIZE = 4;
    public static final int DESIRE_LEN = 8;
    public static final int TRAJECTORY_SIZE = 33;
    public static final int MODEL_WIDTH = 512;
    public static final int MODEL_HEIGHT = 256;
    public static final float MODEL_FRAME_SIZE = (MODEL_WIDTH * MODEL_HEIGHT * 3.0f / 2.0f);
    public static final int PLAN_MHP_N = 5;
    public static final int PLAN_MHP_COLUMNS = 30;
    public static final int PLAN_MHP_VALS = 30*33;
    public static final int PLAN_MHP_SELECTION = 1;
    public static final int PLAN_MHP_GROUP_SIZE =  (2*PLAN_MHP_VALS + PLAN_MHP_SELECTION);
    public static final int LEAD_MHP_N = 5;
    public static final int LEAD_MHP_VALS = 4;
    public static final int LEAD_MHP_SELECTION = 3;
    public static final int LEAD_MHP_GROUP_SIZE = (2*LEAD_MHP_VALS + LEAD_MHP_SELECTION);
    public static final int POSE_SIZE = 12;
    public static final int PLAN_IDX = 0;
    public static final int LL_IDX = PLAN_IDX + PLAN_MHP_N*PLAN_MHP_GROUP_SIZE;
    public static final int LL_PROB_IDX = LL_IDX + 4*2*2*33;
    public static final int RE_IDX = LL_PROB_IDX + 4;
    public static final int LEAD_IDX = RE_IDX + 2*2*2*33;
    public static final int LEAD_PROB_IDX = LEAD_IDX + LEAD_MHP_N*(LEAD_MHP_GROUP_SIZE);
    public static final int DESIRE_STATE_IDX = LEAD_PROB_IDX + 3;
    public static final int META_IDX = DESIRE_STATE_IDX + DESIRE_LEN;
    public static final int POSE_IDX = META_IDX + OTHER_META_SIZE + DESIRE_PRED_SIZE;
    public static final int OUTPUT_SIZE =  POSE_IDX + POSE_SIZE;
    public static final int TEMPORAL_SIZE = 512;
    public static final  float[] T_IDXS = {0.f, 0.00976562f, 0.0390625f, 0.08789062f, 0.15625f, 0.24414062f,  0.3515625f,  0.47851562f,
        0.625f, 0.79101562f, 0.9765625f, 1.18164062f,  1.40625f,  1.65039062f,  1.9140625f,
        2.19726562f, 2.5f, 2.82226562f, 3.1640625f, 3.52539062f, 3.90625f, 4.30664062f, 4.7265625f, 5.16601562f,
        5.625f, 6.10351562f, 6.6015625f, 7.11914062f, 7.65625f, 8.21289062f, 8.7890625f, 9.38476562f, 10.f};

    public static final float[] X_IDXS = {0.f, 0.1875f, 0.75f, 1.6875f, 3.f, 4.6875f, 6.75f, 9.1875f, 12.f,  15.1875f, 18.75f, 22.6875f,
        27.f,  31.6875f,  36.75f, 42.1875f, 48.f, 54.1875f, 60.75f,  67.6875f,  75.f, 82.6875f, 90.75f, 99.1875f, 108.f, 117.1875f,
        126.75f, 136.6875f, 147.f, 157.6875f, 168.75f, 180.1875f, 192.0f};
    public static final float[] t_offsets = {0.0f, 2.0f, 4.0f};

    public ParsedOutputs parsed = new ParsedOutputs();
    public ArrayList<LeadDataV2> leads = parsed.leads;
    public MetaData metaDataOutput = parsed.metaData;
    public float[] desireState = metaDataOutput.desireState;
    public float[] desirePrediction = metaDataOutput.desirePrediction;

    public float[][] meta = parsed.meta;
    public float[][] pose = parsed.pose;
    public float[][] state = parsed.state;

    public float[] trans = parsed.trans;
    public float[] transStd = parsed.transStd;
    public float[] rot = parsed.rot;
    public float[] rotStd = parsed.rotStd;
    public float[] laneLineProbs = parsed.laneLineProbs;
    public ArrayList<float[]> laneLineStds = parsed.laneLineStds;
    public ArrayList<ArrayList<float[]>> laneLines = parsed.laneLines;
    public ArrayList<ArrayList<float[]>> roadEdges = parsed.roadEdges;

    public Map<String, float[]> net_outputs = new HashMap<String, float[]>();
    public float[] plan_t_arr = new float[TRAJECTORY_SIZE];
    public float[] best_plan = new float[PLAN_MHP_GROUP_SIZE];

    public SizeMapArrayPool pool = new SizeMapArrayPool();

    public Parser(){
        net_outputs.put("plan", new float[LL_IDX - PLAN_IDX]);
        net_outputs.put("laneLines", new float[LL_PROB_IDX - LL_IDX]);
        net_outputs.put("laneLinesProb", new float[RE_IDX - LL_PROB_IDX]);
        net_outputs.put("roadEdges", new float[LEAD_IDX - RE_IDX]);
        net_outputs.put("lead", new float[LEAD_PROB_IDX - LEAD_IDX]);
        net_outputs.put("leadProb", new float[DESIRE_STATE_IDX - LEAD_PROB_IDX]);
        net_outputs.put("meta", new float[POSE_IDX - DESIRE_STATE_IDX]);
        net_outputs.put("pose", new float[OUTPUT_SIZE - POSE_IDX]);
        net_outputs.put("state", new float[TEMPORAL_SIZE]);
    }

    public void copyOfRange(float[] src, float[] dst, int start, int end){
        for (int i=start; i<end; i++){
            dst[i-start] = src[i];
        }
    }

    public float getMax(float[] x){
         float max = x[0];
         for (int i=1; i<x.length; i++){
            max = Math.max(max, x[i]);
         }
         return max;
    }

    public void softmax(float[] x, float[] output){
        float max = getMax(x);
        float sum = 0;
        for (int i=0; i<x.length; i++)
            sum += Math.exp(x[i] - max);

        for (int i=0; i<x.length; i++)
            output[i] = (float)Math.exp(x[i]) / sum;
    }

    public float[] softmax(float[] x){
        float max = getMax(x);
        float[] output = new float[x.length];
        float sum = 0;
        for (int i=0; i<x.length; i++)
            sum += Math.exp(x[i] - max);

        for (int i=0; i<x.length; i++)
            output[i] = x[i] / sum;
        return output;
    }

    public float sigmoid(float x)
    {
        return  1.0f/ (float)(1 + Math.exp(-x));
    }

    public float[] softPlus(float[] x)

    {
        float[] output = new float[x.length];
        float temp;
        for(int i=0; i < x.length; i++)
        {
            temp = ( x[i]>=0 )?x[i]:0;
            output[i] = Double.valueOf(Math.log1p(Math.exp(-Math.abs(x[i])))).floatValue() + temp;
        }
        return output;
    }

    public void softPlus(float[] output, float[] x)
    {
        float temp;
        for(int i=0; i < x.length; i++)
        {
            temp = ( x[i]>=0 )?x[i]:0;
            output[i] = Double.valueOf(Math.log1p(Math.exp(-Math.abs(x[i])))).floatValue() + temp;
        }
    }

    public void softPlus(float[] output, float[] x, int start, int end)
    {
        float temp;
        for(int i=0; i < end-start; i++)
        {
            temp = ( x[start+i]>=0 )?x[start+i]:0;
            output[i] = Double.valueOf(Math.log1p(Math.exp(-Math.abs(x[start+i])))).floatValue() + temp;
        }
    }

    public float[] getBestPlan(float[] x) {
        int plan_mhp_max_idx = 0;
        for(int i=1; i < PLAN_MHP_N; i++)
            if (x[(i + 1)*(PLAN_MHP_GROUP_SIZE) - 1] > x[(plan_mhp_max_idx + 1)*(PLAN_MHP_GROUP_SIZE) - 1])
                plan_mhp_max_idx = i;

        return Arrays.copyOfRange(x, plan_mhp_max_idx*(PLAN_MHP_GROUP_SIZE), (plan_mhp_max_idx+1)*(PLAN_MHP_GROUP_SIZE));
    }

    public void getBestPlan(float[] x, float[] output) {
        int plan_mhp_max_idx = 0;
        for(int i=1; i < PLAN_MHP_N; i++)
            if (x[(i + 1)*(PLAN_MHP_GROUP_SIZE) - 1] > x[(plan_mhp_max_idx + 1)*(PLAN_MHP_GROUP_SIZE) - 1])
                plan_mhp_max_idx = i;

        copyOfRange(x, output, plan_mhp_max_idx*(PLAN_MHP_GROUP_SIZE), (plan_mhp_max_idx+1)*(PLAN_MHP_GROUP_SIZE));
    }

    public void getBestPlan(float[] x, float[] output, int start) {
        int plan_mhp_max_idx = 0;
        for(int i=1; i < PLAN_MHP_N; i++)
            if (x[start + (i + 1)*(PLAN_MHP_GROUP_SIZE) - 1] > x[start + (plan_mhp_max_idx + 1)*(PLAN_MHP_GROUP_SIZE) - 1])
                plan_mhp_max_idx = i;

        copyOfRange(x, output, start+plan_mhp_max_idx*(PLAN_MHP_GROUP_SIZE), start+(plan_mhp_max_idx+1)*(PLAN_MHP_GROUP_SIZE));
    }

    public void fillXYZT(ArrayList<float[]>xyzt, float[] data, int columns, int column_offset, float[] plan_t)
    {
        float[] x_arr = xyzt.get(0);
        float[] y_arr = xyzt.get(1);
        float[] z_arr = xyzt.get(2);
        float[] t_arr = xyzt.get(3);

        for(int i=0; i < TRAJECTORY_SIZE; i++)
        {
            if (column_offset >= 0)
            {
                t_arr[i] = T_IDXS[i];
                x_arr[i] = data[i * columns + column_offset];
            }
            else
            {
                t_arr[i] = plan_t[i];
                x_arr[i] = X_IDXS[i];
            }
            y_arr[i] = data[i*columns + 1 + column_offset];
            z_arr[i] = data[i*columns + 2 + column_offset];
        }
    }

    public void fillXYZT(ArrayList<float[]>xyzt, float[] data, int start, int columns, int column_offset, float[] plan_t)
    {
        float[] x_arr = xyzt.get(0);
        float[] y_arr = xyzt.get(1);
        float[] z_arr = xyzt.get(2);
        float[] t_arr = xyzt.get(3);

        for(int i=0; i < TRAJECTORY_SIZE; i++)
        {
            if (column_offset >= 0)
            {
                t_arr[i] = T_IDXS[i];
                x_arr[i] = data[start + i * columns + column_offset];
            }
            else
            {
                t_arr[i] = plan_t[i];
                x_arr[i] = X_IDXS[i];
            }
            y_arr[i] = data[start + i*columns + 1 + column_offset];
            z_arr[i] = data[start + i*columns + 2 + column_offset];
        }
    }

    public float[] get_best_data(float[] data, int size, int group_size, int offset)
    {
        int max_idx = 0;
        for(int i=1; i < size; i++)
            if (data[(i + 1) * group_size + offset] > data[(max_idx + 1) * group_size + offset])
                max_idx = i;

        return Arrays.copyOfRange(data, max_idx * group_size, (max_idx+1) * group_size);
    }

    public void get_best_data(float[] data, int size, int group_size, int offset, float[] output)
    {
        int max_idx = 0;
        for(int i=1; i < size; i++) {
            if (data[(i + 1) * group_size + offset] > data[(max_idx + 1) * group_size + offset])
                max_idx = i;
        }
        copyOfRange(data, output, max_idx * group_size, (max_idx+1) * group_size);
    }

    public float[] get_lead_data(float[] lead, int t_offset)
    {
        return get_best_data(lead, LEAD_MHP_N, LEAD_MHP_GROUP_SIZE, t_offset - LEAD_MHP_SELECTION);
    }

    public void get_lead_data(float[] lead, int t_offset, float[] output)
    {
        get_best_data(lead, LEAD_MHP_N, LEAD_MHP_GROUP_SIZE, t_offset - LEAD_MHP_SELECTION, output);
    }

    public void fill_lead_v2(LeadDataV2 lead, float[] lead_data, float[] prob, int t_offset, float t)
    {
        float[] xyva = lead.xyva;
        float[] xyvaStd = lead.xyvaStd;
        float[] temp_data = pool.getArray(LEAD_MHP_GROUP_SIZE);
        get_lead_data(lead_data, t_offset, temp_data);
        lead.prob = sigmoid(prob[t_offset]);

        for(int i=0; i < LEAD_MHP_VALS; i++)
        {
            xyva[i] = temp_data[i];
            xyvaStd[i] = (float)Math.exp(temp_data[LEAD_MHP_VALS + i]);
        }
        pool.returnArray(temp_data);
    }

    public void fillMeta(float[] metaData){
        float[] temp = pool.getArray(DESIRE_LEN);
        copyOfRange(metaData, temp, 0, DESIRE_LEN);
        softmax(temp, desireState);
        int offset = DESIRE_LEN + OTHER_META_SIZE;
        for (int i=0; i<4; i++){
            copyOfRange(metaData, temp, offset+i*DESIRE_LEN, offset+(i+1)*DESIRE_LEN);
            softmax(temp, temp);
            for (int j=i*DESIRE_LEN; j<(i+1)*DESIRE_LEN; j++){
                desirePrediction[j] = temp[j-i*DESIRE_LEN];
            }
        }
        metaDataOutput.engagedProb = sigmoid(metaData[DESIRE_LEN]);
        metaDataOutput.disengagePredictions.gasDesengageProbs[0] = sigmoid(metaData[DESIRE_LEN+1]);
        metaDataOutput.disengagePredictions.brakeDisengageProbs[0] = sigmoid(metaData[DESIRE_LEN+2]);
        metaDataOutput.disengagePredictions.steerOverrideProbs[0] = sigmoid(metaData[DESIRE_LEN+3]);
        pool.returnArray(temp);
    }

    public ParsedOutputs parser(float[] outs){

        copyOfRange(outs, net_outputs.get("lead"), LEAD_IDX, LEAD_PROB_IDX);
        copyOfRange(outs, net_outputs.get("leadProb"), LEAD_PROB_IDX, DESIRE_STATE_IDX);
        copyOfRange(outs, net_outputs.get("meta"), DESIRE_STATE_IDX, POSE_IDX);
        copyOfRange(outs, net_outputs.get("pose"), POSE_IDX, OUTPUT_SIZE);

        getBestPlan(outs, best_plan, PLAN_IDX);

        for(int i=0; i < TRAJECTORY_SIZE; i++)
            plan_t_arr[i] = best_plan[i*PLAN_MHP_COLUMNS + 15];

        fillXYZT(parsed.position, best_plan, PLAN_MHP_COLUMNS, 0, plan_t_arr);
        fillXYZT(parsed.velocity, best_plan, PLAN_MHP_COLUMNS, 3, plan_t_arr);
        fillXYZT(parsed.orientation, best_plan, PLAN_MHP_COLUMNS, 9, plan_t_arr);
        fillXYZT(parsed.orientationRate, best_plan, PLAN_MHP_COLUMNS, 12, plan_t_arr);

        for(int i=0; i < 4; i++) {
            fillXYZT(laneLines.get(i), outs, LL_IDX + i*TRAJECTORY_SIZE*2, 2, -1, plan_t_arr);
            laneLineProbs[i] = sigmoid(outs[LL_PROB_IDX+i]);
            softPlus(laneLineStds.get(i), outs, 2*TRAJECTORY_SIZE*(4 + i), 2*TRAJECTORY_SIZE*(4 + i+1));
            fillXYZT(roadEdges.get(i), outs, RE_IDX + i*TRAJECTORY_SIZE*2, 2, -1, plan_t_arr);
        }

        for(int t_offset=0; t_offset < LEAD_MHP_SELECTION; t_offset++)
            fill_lead_v2(leads.get(t_offset), net_outputs.get("lead"), net_outputs.get("leadProb"), t_offset, t_offsets[t_offset]);

        copyOfRange(net_outputs.get("meta"), meta[0], 0, meta[0].length);
        copyOfRange(net_outputs.get("pose"), pose[0], 0, pose[0].length);
        copyOfRange(outs, state[0], OUTPUT_SIZE, OUTPUT_SIZE+TEMPORAL_SIZE);

        fillMeta(meta[0]);

        copyOfRange(pose[0], trans, 0, 3);
        copyOfRange(pose[0], transStd, 6, 9);

        for(int j=0; j < transStd.length; j++)
            transStd[j] += 1e-6;

        copyOfRange(pose[0], rot, 6, 9);
        copyOfRange(pose[0], rotStd, 6, 9);
        softPlus(rotStd, Arrays.copyOfRange(pose[0], 9, 12));

        for(int j=0; j < rotStd.length; j++){
            rotStd[j] *= Math.PI / 180.0;
            rot[j] *= Math.PI / 180.0;
        }

        return parsed;
    }
}
