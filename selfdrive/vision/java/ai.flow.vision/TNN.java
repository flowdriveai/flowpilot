package ai.flow.vision;

import java.nio.ByteBuffer;
import java.util.Map;

public class TNN {
    public native boolean init(String modelPath, String deviceType);

    public native void createInput(String name, int[] shape);
    public native void createOutput(String name, int[] shape);

    public native void forward(Map<String, ByteBuffer> container);
    public native void forward(Map<String, ByteBuffer> container, String outputName, float[] out);
    public native float[] forward(Map<String, ByteBuffer> container, String outputName);

    public native ByteBuffer getOutputBuffer(String name);
    public native float[] getOutput(String name);
    public native void getOutput(String name, float[] out);
}

