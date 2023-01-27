package ai.flow.modeld;

import org.nd4j.linalg.api.ndarray.INDArray;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Map;

public abstract class ModelRunner{
    public void init(Map<String, int[]> shapes ){}
    public void warmup(){}
    public void run(Map<String, INDArray> inputMap, Map<String, float[]> outputMap){}
    public void dispose(){}
}
