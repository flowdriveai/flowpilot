package ai.flow.android.sensor;

import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;

public class Utils {

    private static ByteBuffer removePlaneBufferPadding(ImageProxy.PlaneProxy plane, int planeWidth, int planeHeight) {
        ByteBuffer dst =  ByteBuffer.allocate(planeWidth * planeHeight);
        ByteBuffer src = plane.getBuffer();
        int rowStride = plane.getRowStride();
        ByteBuffer row;
        for (int i = 0; i < planeHeight; i++) {
            row = clipBuffer(src, i * rowStride, planeWidth);
            dst.put(row);
        }
        dst.rewind();
        return dst;
    }

    private static ByteBuffer clipBuffer(ByteBuffer buffer, int start, int size) {
        ByteBuffer duplicate = buffer.duplicate();
        duplicate.position(start);
        duplicate.limit(Math.min(start + size, duplicate.capacity()));
        return duplicate.slice();
    }

    public static void fillYUVBuffer(ImageProxy image, ByteBuffer yuvBuffer){
        yuvBuffer.rewind();
        ImageProxy.PlaneProxy yPlane = image.getPlanes()[0];
        ByteBuffer yBuffer = yPlane.getBuffer();
        if (yPlane.getRowStride() != image.getWidth()){
            yBuffer = removePlaneBufferPadding(yPlane, image.getWidth(), image.getHeight());
        }
        yuvBuffer.put(yBuffer);

        // interleaved uv pixels, nv12 or nv21
        if (image.getPlanes()[1].getPixelStride() == 2) {
            ImageProxy.PlaneProxy uvPlane = image.getPlanes()[2];
            ByteBuffer uvBuffer = uvPlane.getBuffer();
            if (uvPlane.getRowStride() != image.getWidth()){
                uvBuffer = removePlaneBufferPadding(uvPlane, image.getWidth(), image.getHeight()/2);
            }
            yuvBuffer.put(uvBuffer);
        }
        else{
            ImageProxy.PlaneProxy uPlane = image.getPlanes()[1];
            ImageProxy.PlaneProxy vPlane = image.getPlanes()[2];
            yuvBuffer.put(uPlane.getBuffer());
            yuvBuffer.put(vPlane.getBuffer());
        }
    }
}
