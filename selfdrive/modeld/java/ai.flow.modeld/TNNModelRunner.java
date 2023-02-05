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
    public void init(Map<String, int[]> inputShapes, Map<String, int[]> outputShapes){
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

        for (String inputName : inputShapes.keySet()) {
            model.createInput(inputName, inputShapes.get(inputName));
        }
        for (String outputName : outputShapes.keySet()) {
            model.createOutput(outputName, outputShapes.get(outputName));
        }
    }

    @Override
    public void run(Map<String, INDArray> inputMap, Map<String, float[]> outputMap) {
        for (String inputName : inputMap.keySet()) {
            container.put(inputName, inputMap.get(inputName).data().asNio());
        }
        model.forward(container, "outputs", outputMap.get("outputs"));
    }
}
