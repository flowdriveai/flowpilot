package ai.flow.vision;

import ai.onnxruntime.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ONNXModelRunner extends ModelRunner{
    String modelPath = "models/supercombo_simple.onnx";
    Map<String, OnnxTensor> container = new HashMap<>();
    OrtEnvironment env = OrtEnvironment.getEnvironment();
    OrtSession session;
    Map<String, long[]> shapes = new HashMap<>();

    OnnxTensor inputImgTensor = null;
    OnnxTensor inputStateTensor = null;
    OnnxTensor inputDesireTensor = null;
    OnnxTensor inputTrafficTensor = null;

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
    public void run(ByteBuffer inputImgs, ByteBuffer desire, ByteBuffer trafficConvention, ByteBuffer state, float[] netOutputs) {
        float[] netOutputsCpy = null;
        try {
            inputImgTensor = OnnxTensor.createTensor(env, inputImgs.asFloatBuffer(), shapes.get("input_imgs"));
            inputDesireTensor = OnnxTensor.createTensor(env, desire.asFloatBuffer(), shapes.get("desire"));
            inputTrafficTensor = OnnxTensor.createTensor(env, trafficConvention.asFloatBuffer(), shapes.get("traffic_convention"));
            inputStateTensor = OnnxTensor.createTensor(env, state.asFloatBuffer(), shapes.get("initial_state"));

            container.put("input_imgs", inputImgTensor);
            container.put("desire", inputDesireTensor);
            container.put("traffic_convention", inputTrafficTensor);
            container.put("initial_state", inputStateTensor);

            try (OrtSession.Result netOutputsTensor = session.run(container);){
                netOutputsCpy = ((float[][])netOutputsTensor.get(0).getValue())[0];
                } catch(OrtException e){
                    System.out.println(e);
            }

            } catch (OrtException e) {
                throw new RuntimeException(e);
        }
        System.arraycopy(netOutputsCpy, 0, netOutputs, 0, netOutputs.length );
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
