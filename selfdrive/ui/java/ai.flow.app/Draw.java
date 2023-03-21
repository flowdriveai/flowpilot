package ai.flow.app;

import ai.flow.modeld.CommonModelF3;
import ai.flow.modeld.LeadDataV3;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;

import java.util.ArrayList;

public class Draw {
    /**
     * Scale to convert raw net outputs to meters.
     */
    public static final float LEAD_X_SCALE = 10f;
    public static final float LEAD_Y_SCALE = 10f;

    public static INDArray[] getLaneCameraFrame(ArrayList<float[]> lane, INDArray K, INDArray Rt, float width) {
        // Camera assumes x left, y up, z forward
        INDArray X = Nd4j.create(lane.get(1), 1, CommonModelF3.TRAJECTORY_SIZE);
        INDArray Y = Nd4j.create(lane.get(2), 1, CommonModelF3.TRAJECTORY_SIZE);
        INDArray Z = Nd4j.create(lane.get(0), 1, CommonModelF3.TRAJECTORY_SIZE);
        INDArray W = Nd4j.ones(1, CommonModelF3.TRAJECTORY_SIZE);

        // Make a line strip
        INDArray projected_left_edge = projectToCamera(Nd4j.vstack(X.add(-width / 2.0), Y, Z, W), K, Rt);
        INDArray projected_right_edge = projectToCamera(Nd4j.vstack(X.add(width / 2.0), Y, Z, W), K, Rt);

        return new INDArray[]{projected_left_edge, projected_right_edge};
    }

    public static INDArray projectToCamera(INDArray worldCoords, INDArray K, INDArray Rt){
        INDArray projection_matrix = K.mmul(Rt);
        INDArray projected = projection_matrix.mmul(worldCoords).transpose();
        projected = projected.div(projected.getColumn(2).reshape(projected.shape()[0], 1)); // normalize projected coordinates by W

        return projected.get(NDArrayIndex.all(), NDArrayIndex.interval(0,2)); // drop W column
    }

    public static INDArray getTriangleCameraFrame(LeadDataV3 lead, INDArray K, INDArray Rt, float scale) {
        float[][] lead_pos_buffer = {{lead.y[0] * LEAD_Y_SCALE, 1.32f, lead.x[0] * LEAD_X_SCALE, 1f},
                {lead.y[0] * LEAD_Y_SCALE - scale, 1.32f + scale, lead.x[0] * LEAD_X_SCALE, 1f},
                {lead.y[0] * LEAD_Y_SCALE + scale, 1.32f + scale, lead.x[0] * LEAD_X_SCALE, 1f}};
        INDArray lead_pos = Nd4j.createFromArray(lead_pos_buffer).transpose();
        return projectToCamera(lead_pos, K, Rt);
    }
}
