package ai.flow.modeld;

import org.nd4j.linalg.api.ndarray.INDArray;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static ai.flow.common.SystemUtils.isAndroid;

public class TNNModelRunner extends ModelRunner{
    String modelPath;
    String deviceType;
    String IODeviceType = "NAIVE";
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

        //if (isAndroid())
        //    IODeviceType = "ARM";

        model.init(modelPath, deviceType, IODeviceType);

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

    @Override
    public void run(INDArray inputImgs, INDArray desire, INDArray trafficConvention, INDArray state, float[] netOutputs) {
        container.put("input_imgs", inputImgs.data().asNio());
        container.put("desire", desire.data().asNio());
        container.put("traffic_convention", trafficConvention.data().asNio());
        container.put("initial_state", state.data().asNio());

        model.forward(container, "outputs", netOutputs);
    }
}
