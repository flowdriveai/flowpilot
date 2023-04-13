package ai.flow.android.vision;

import ai.flow.modeld.ModelRunner;
import android.app.Application;
import com.qualcomm.qti.snpe.FloatTensor;
import com.qualcomm.qti.snpe.NeuralNetwork;
import com.qualcomm.qti.snpe.SNPE;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.qualcomm.qti.snpe.NeuralNetwork.Runtime;

public class SNPEModelRunner extends ModelRunner {
    private Application context;
    NeuralNetwork network;
    String modelPath;
    public Map<String, FloatTensor> container = new HashMap<>();
    public Map<String, float[]> containerArrs = new HashMap<>();
    public Map<String, int[]> inputShapes;
    public int warmupIters = 50;
    boolean useGPU;

    public SNPEModelRunner(Application context, String modelPath, boolean useGPU){
        this.context = context;
        this.modelPath = modelPath;
        this.useGPU = useGPU;
    }

    public static double numElements(int[] shape){
        double ret = 1;
        for (int i:shape)
            ret *= i;
        return ret;
    }

    @Override
    public void init(Map<String, int[]> inputShapes, Map<String, int[]> outputShapes) {
        this.inputShapes = inputShapes;

        SNPE.NeuralNetworkBuilder builder = null;
        File modelStream = new File(modelPath + ".dlc");
        try {
            builder = new SNPE.NeuralNetworkBuilder(context)
                    .setDebugEnabled(false)
                    .setPerformanceProfile(NeuralNetwork.PerformanceProfile.SUSTAINED_HIGH_PERFORMANCE)
                    .setExecutionPriorityHint(NeuralNetwork.ExecutionPriorityHint.HIGH)
                    .setModel(modelStream)
                    .setInitCacheEnabled(true, "snpe", context);

            if (useGPU)
                builder.setRuntimeOrder(Runtime.GPU_FLOAT16);

        } catch (IOException e) {
            e.printStackTrace();
        }

        assert builder != null;
        network = builder.build();

        for (String inputName : this.inputShapes.keySet()) {
            container.put(inputName, network.createFloatTensor(this.inputShapes.get(inputName)));
            containerArrs.put(inputName, new float[(int)numElements(this.inputShapes.get(inputName))]);
        }
    }

    public void writeTensor(FloatTensor tensor, float[] arr){
        tensor.write(arr, 0, arr.length);
    }

    @Override
    public void warmup(){
        for (int i=0; i<warmupIters; i++)
            network.execute(container);
    }

    @Override
    public void run(Map<String, INDArray> inputMap, Map<String, float[]> outputMap){
        for (String inputName : inputMap.keySet()) {
            inputMap.get(inputName).data().asNioFloat().get(containerArrs.get(inputName));
            writeTensor(container.get(inputName), containerArrs.get(inputName));
        }

        Map<String, FloatTensor> out = network.execute(container);

        for (String outputName : outputMap.keySet()) {
            out.get(outputName).read(outputMap.get(outputName), 0, outputMap.get(outputName).length);
        }
    }

    @Override
    public void dispose(){
        network.release();
        for (FloatTensor tensor : container.values()) {
            tensor.release();
        }
    }
}
