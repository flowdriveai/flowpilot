package ai.flow.modeld;

import org.nd4j.linalg.api.ndarray.INDArray;

import java.nio.ByteBuffer;

public interface ImagePrepare {
    public INDArray prepare(ByteBuffer imgBuffer, INDArray transform);
    public void dispose();
}
