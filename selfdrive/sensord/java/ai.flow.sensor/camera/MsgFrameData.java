package ai.flow.sensor.camera;

import ai.flow.definitions.Definitions;
import ai.flow.definitions.MessageBase;
import org.capnproto.PrimitiveList;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MsgFrameData extends MessageBase {

    public Definitions.FrameData.Builder frameData;
    public ByteBuffer imageBuffer;
    public PrimitiveList.Float.Builder intrinsics;

    public MsgFrameData(int imgSize) {
        super();
        initFields(imgSize);
        bytesSerializedForm = computeSerializedMsgBytes();
        initSerializedBuffer();
    }

    private void initFields(int imgSize){
        event = messageBuilder.initRoot(Definitions.Event.factory);
        frameData = event.initFrameData();
        intrinsics = frameData.initIntrinsics(9);
        if (imgSize > 0) {
            frameData.initImage(imgSize);
            imageBuffer = frameData.getImage().asByteBuffer();

            Field address, capacity;
            try {
                address = Buffer.class.getDeclaredField("address");
                address.setAccessible(true);
                capacity = Buffer.class.getDeclaredField("capacity");
                capacity.setAccessible(true);
                frameData.setNativeImageAddr(address.getLong(imageBuffer));

            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
    }

    public ByteBuffer getImageBuffer(){
        if (frameData.getNativeImageAddr() == 0)
            return imageBuffer;
        else
            return getImgBufferFromAddr();
    }

    public ByteBuffer getImageBuffer(long address){
        if (address == 0)
            return imageBuffer;
        else
            return getImgBufferFromAddr(address);
    }

    public void setImageAddress(ByteBuffer buffer){
        if (imageBuffer != null) {
            System.err.println("[WARNING]: Trying to override native image address.");
        }
        Field address, capacity;
        try {
            address = Buffer.class.getDeclaredField("address");
            address.setAccessible(true);
            capacity = Buffer.class.getDeclaredField("capacity");
            capacity.setAccessible(true);
            frameData.setNativeImageAddr(address.getLong(buffer));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }


    public ByteBuffer getImgBufferFromAddr(){
        if (frameData.getNativeImageAddr() == 0){
            return null;
        }
        Field address, capacity;
        try {
            address = Buffer.class.getDeclaredField("address");
            address.setAccessible(true);
            capacity = Buffer.class.getDeclaredField("capacity");
            capacity.setAccessible(true);
            ByteBuffer bb = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder());

            address.setLong(bb, frameData.getNativeImageAddr());
            capacity.setInt(bb, 1164*874*3);
            bb.clear();
            return bb;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public static ByteBuffer getImgBufferFromAddr(long imgAddress){
        Field address, capacity;
        try {
            address = Buffer.class.getDeclaredField("address");
            address.setAccessible(true);
            capacity = Buffer.class.getDeclaredField("capacity");
            capacity.setAccessible(true);
            ByteBuffer bb = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder());

            address.setLong(bb, imgAddress);
            capacity.setInt(bb, 1164*874*3);
            bb.clear();
            return bb;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }
}
