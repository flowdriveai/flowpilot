package ai.flow.vision;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static ai.flow.common.SystemUtils.isAndroid;

public class TNNModelRunner extends ModelRunner{
    String modelPath = "models/supercombo_simple";
    String deviceType = "OPENCL";
    TNN model;
    Map<String, ByteBuffer> container = new HashMap<>();
    boolean useGPU;

    public TNNModelRunner(String modelPath, boolean useGPU){
        this.modelPath = modelPath;
        this.useGPU = useGPU;
    }

    @Override
    public void init(Map<String, int[]> shapes){
        System.loadLibrary("tnnjni");
        model = new TNN();

        if (useGPU)
            deviceType = "OPENCL";
        else if (isAndroid())
            deviceType = "ARM";
        else
            deviceType = "X86";

        model.init(modelPath, deviceType);

        model.createInput("input_imgs", shapes.get("input_imgs"));
        model.createInput("initial_state", shapes.get("initial_state"));
        model.createInput("desire", shapes.get("desire"));
        model.createInput("traffic_convention", shapes.get("traffic_convention"));
        model.createOutput("outputs", shapes.get("outputs"));
    }

    @Override
    public void run(ByteBuffer inputImgs, ByteBuffer desire, ByteBuffer trafficConvention, ByteBuffer state, float[] netOutputs) {
        container.put("input_imgs", inputImgs);
        container.put("desire", desire);
        container.put("traffic_convention", trafficConvention);
        container.put("initial_state", state);

        model.forward(container, "outputs", netOutputs);
    }
}
