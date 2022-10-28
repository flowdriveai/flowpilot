package ai.flow.android.vision;

import ai.flow.vision.ModelRunner;
import android.app.Application;
import com.qualcomm.qti.snpe.FloatTensor;
import com.qualcomm.qti.snpe.NeuralNetwork;
import com.qualcomm.qti.snpe.SNPE;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import static com.qualcomm.qti.snpe.NeuralNetwork.Runtime;

public class SNPEModelRunner extends ModelRunner {
    private Application context;
    NeuralNetwork network;
    String modelPath;
    public Map<String, FloatTensor> container = new HashMap<>();
    public Map<String, int[]> shapes;
    public int warmupIters = 50;
    boolean useGPU;

    float[] imgTensorSequenceArr = new float[128*256*12];
    float[] desireArr = new float[8];
    float[] trafficArr = new float[2];
    float[] stateArr = new float[512];

    public SNPEModelRunner(Application context, String modelPath, boolean useGPU){
        this.context = context;
        this.modelPath = modelPath;
        this.useGPU = useGPU;
    }

    @Override
    public void init(Map<String, int[]> shapes) {
        this.shapes = shapes;

        SNPE.NeuralNetworkBuilder builder = null;
        File modelStream = new File(modelPath + ".dlc");
        try {
            builder = new SNPE.NeuralNetworkBuilder(context)
                    .setDebugEnabled(false)
                    .setPerformanceProfile(NeuralNetwork.PerformanceProfile.SUSTAINED_HIGH_PERFORMANCE)
                    .setExecutionPriorityHint(NeuralNetwork.ExecutionPriorityHint.HIGH)
                    .setModel(modelStream);

            if (useGPU)
                builder.setRuntimeOrder(Runtime.GPU_FLOAT16);

        } catch (IOException e) {
            e.printStackTrace();
        }

        assert builder != null;
        network = builder.build();

        container.put("input_imgs", network.createFloatTensor(shapes.get("input_imgs")));
        container.put("desire", network.createFloatTensor(shapes.get("desire")));
        container.put("traffic_convention", network.createFloatTensor(shapes.get("traffic_convention")));
        container.put("initial_state", network.createFloatTensor(shapes.get("initial_state")));
    }

    public void BufferToFloatArr(float[] arr, ByteBuffer buffer){
        for (int i=0; i<arr.length; i++)
            arr[i] = buffer.getFloat(i*4);
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
    public void run(ByteBuffer inputImgs, ByteBuffer desire, ByteBuffer trafficConvention, ByteBuffer state, float[] netOutputs){
        // not implemented
    }

    @Override
    public void run(INDArray inputImgs, INDArray desire, INDArray trafficConvention, INDArray state, float[] netOutputs){

        inputImgs.data().asNioFloat().get(imgTensorSequenceArr);
        desire.data().asNioFloat().get(desireArr);
        trafficConvention.data().asNioFloat().get(trafficArr);
        state.data().asNioFloat().get(stateArr);

        writeTensor(container.get("input_imgs"), imgTensorSequenceArr);
        writeTensor(container.get("desire"), desireArr);
        writeTensor(container.get("traffic_convention"), trafficArr);
        writeTensor(container.get("initial_state"), stateArr);

        network.execute(container).get("outputs").read(netOutputs, 0, netOutputs.length);
    }

    @Override
    public void dispose(){
        network.release();
        for (FloatTensor tensor : container.values()) {
            tensor.release();
        }
    }
}
