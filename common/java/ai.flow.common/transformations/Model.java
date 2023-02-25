package ai.flow.common.transformations;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

public class Model {
    public static final float MEDMODEL_CY = 47.6f;
    public static final float MEDMODEL_FL = 910.0f;
    public static final float SBIGMODEL_FL = 455.0f;
    public static final int[] modelInputSize = new int[]{512, 256};
    public static final INDArray medmodel_intrinsics = Nd4j.createFromArray(new float[][]{
            {MEDMODEL_FL,  0.0f,  0.5f * modelInputSize[0]},
            {0.0f,  MEDMODEL_FL,      MEDMODEL_CY},
            {0.0f,  0.0f,  1.0f}
    });

    public static final INDArray sbigmodel_intrinsics = Nd4j.createFromArray(new float[][]{
            {SBIGMODEL_FL,  0.0f,  0.5f * modelInputSize[0]},
            {0.0f,  SBIGMODEL_FL,      0.5f * (256 + MEDMODEL_CY)},
            {0.0f,  0.0f,  1.0f}
    });
}
