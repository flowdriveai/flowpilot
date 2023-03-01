package ai.flow.common;

import org.opencv.core.Mat;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BufferUtils {
    public static ByteBuffer bufferFromAddress(long imgAddress, int size){
        Field address, capacity;
        try {
            address = Buffer.class.getDeclaredField("address");
            address.setAccessible(true);
            capacity = Buffer.class.getDeclaredField("capacity");
            capacity.setAccessible(true);
            ByteBuffer buffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder());
            address.setLong(buffer, imgAddress);
            capacity.setInt(buffer, size);
            buffer.clear();
            return buffer;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public static long addressFromBuffer(ByteBuffer buffer){
        Field address, capacity;
        try {
            address = Buffer.class.getDeclaredField("address");
            address.setAccessible(true);
            capacity = Buffer.class.getDeclaredField("capacity");
            capacity.setAccessible(true);
            return address.getLong(buffer);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public void floatArrToBuffer(float[] arr, ByteBuffer buffer){
        for (int i=0; i<arr.length; i++)
            buffer.putFloat(i*4, arr[i]);
    }

    public static float[] byteToFloat(byte[] input) {
        float[] ret = new float[input.length / 4];
        for (int x = 0; x < input.length; x += 4) {
            ret[x / 4] = ByteBuffer.wrap(input, x, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        }
        return ret;
    }

    public static byte[] MatToByte(Mat mat){
        byte[] ret = new byte[(int)(mat.total() * mat.channels()) * 4];
        for(int i = 0; i < mat.rows(); i++) {
            for(int j = 0; j < mat.cols(); j++) {
                ByteBuffer.wrap(ret, (i * mat.cols() + j)*4, 4).order(ByteOrder.LITTLE_ENDIAN).putFloat((float) mat.get(i, j)[0]);
            }
        }
        return ret;
    }

    public static void cloneByteBuffer(ByteBuffer source, ByteBuffer target) {
        int sourceP = source.position();
        int sourceL = source.limit();
        target.put(source);
        target.flip();
        source.position(sourceP);
        source.limit(sourceL);
    }
}
