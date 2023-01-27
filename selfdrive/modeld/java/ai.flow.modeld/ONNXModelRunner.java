package ai.flow.modeld;

import ai.onnxruntime.*;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ONNXModelRunner extends ModelRunner{
    String modelPath;
    Map<String, OnnxTensor> container = new HashMap<>();
    OrtEnvironment env = OrtEnvironment.getEnvironment();
    OrtSession session;
    Map<String, long[]> shapes = new HashMap<>();

    boolean useGPU;

    public ONNXModelRunner(String modelPath, boolean useGPU){
        this.modelPath = modelPath;
        this.useGPU = useGPU;
    }

    @Override
    public void init(Map<String, int[]> shapes) {
        try {
            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
            opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
            opts.setInterOpNumThreads(8);
            opts.setIntraOpNumThreads(4);
            opts.setExecutionMode(OrtSession.SessionOptions.ExecutionMode.SEQUENTIAL);

            if (useGPU) {
                opts.addCUDA();
            }
            session = env.createSession(modelPath + ".onnx", opts);
            } catch (OrtException e) {
                throw new RuntimeException(e);
        }

        for (String name : shapes.keySet()) {
            this.shapes.put(name, Arrays.stream(shapes.get(name)).mapToLong((i) -> (long) i).toArray());
        }
    }

    @Override
    public void run(Map<String, INDArray> inputMap, Map<String, float[]> outputMap) {
        float[] netOutputsCpy = null;
        try {
            for (String inputName : inputMap.keySet()) {
                container.put(inputName, OnnxTensor.createTensor(env, inputMap.get(inputName).data().asNioFloat(), shapes.get(inputName)));
            }
            try (OrtSession.Result netOutputsTensor = session.run(container);){
                netOutputsCpy = ((float[][])netOutputsTensor.get(0).getValue())[0];
            } catch(OrtException e){
                System.out.println(e);
            }

        } catch (OrtException e) {
            throw new RuntimeException(e);
        }
        System.arraycopy(netOutputsCpy, 0, outputMap.get("outputs"), 0, outputMap.get("outputs").length);
    }

    @Override
    public void dispose(){
        try {
            env.close();
        } catch (OrtException e) {
            throw new RuntimeException(e);
        }
    }
}
