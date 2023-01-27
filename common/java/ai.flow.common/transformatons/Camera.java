package ai.flow.common.transformatons;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

public class Camera {
    public static final float f_focal_length = 2648.0f;
    public static final float e_focal_length = 567.0f;
    public static final int[] frameSize = new int[]{1920, 1080};
    public static INDArray fcam_intrinsics = Nd4j.createFromArray(new float[][]{
            {f_focal_length,  0.0f,  frameSize[0]/2f},
            {0.0f,  f_focal_length,  frameSize[1]/2f},
            {0.0f,  0.0f,  1.0f}
    });

    public static INDArray ecam_intrinsics = Nd4j.createFromArray(new float[][]{
            {e_focal_length,  0.0f,  frameSize[0]/2f},
            {0.0f,  e_focal_length,  frameSize[1]/2f},
            {0.0f,  0.0f,  1.0f}
    });

    public static final INDArray view_from_device = Nd4j.createFromArray(new float[][]{
            {0.0f,  1.0f,  0.0f},
            {0.0f,  0.0f,  1.0f},
            {1.0f,  0.0f,  0.0f}
    });

}
