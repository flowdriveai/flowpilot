package ai.flow.vision;

import org.nd4j.linalg.api.ndarray.INDArray;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Map;

public abstract class ModelRunner{
    public void init(Map<String, int[]> shapes ){}
    public void warmup(){}
    public void run(ByteBuffer inputImgs, ByteBuffer desire, ByteBuffer trafficConvention, ByteBuffer state, float[] netOutputs){}
    public void run(FloatBuffer inputImgs, FloatBuffer desire, FloatBuffer trafficConvention, FloatBuffer state, float[] netOutputs){}
    public void run(INDArray inputImgs, INDArray desire, INDArray trafficConvention, INDArray state, float[] netOutputs){}
    public void dispose(){}
}
