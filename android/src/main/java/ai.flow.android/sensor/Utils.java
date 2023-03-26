package ai.flow.android.sensor;

import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;

public class Utils {
    public static void fillYUVBuffer(ImageProxy image, ByteBuffer yuvBuffer){
        yuvBuffer.rewind();
        ImageProxy.PlaneProxy yPlane = image.getPlanes()[0];
        yuvBuffer.put(yPlane.getBuffer());

        // interleaved uv pixels, nv12 or nv21
        if (image.getPlanes()[1].getPixelStride() == 2) {
            ImageProxy.PlaneProxy uvPlane = image.getPlanes()[2];
            yuvBuffer.put(uvPlane.getBuffer());
        }
        else{
            ImageProxy.PlaneProxy uPlane = image.getPlanes()[1];
            ImageProxy.PlaneProxy vPlane = image.getPlanes()[2];
            yuvBuffer.put(uPlane.getBuffer());
            yuvBuffer.put(vPlane.getBuffer());
        }
    }
}
