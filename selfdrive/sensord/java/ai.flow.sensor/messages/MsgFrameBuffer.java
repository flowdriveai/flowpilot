package ai.flow.sensor.messages;

import ai.flow.common.transformations.Camera;
import ai.flow.definitions.Definitions;
import ai.flow.definitions.MessageBase;

import java.nio.ByteBuffer;

import static ai.flow.common.BufferUtils.addressFromBuffer;
import static ai.flow.common.BufferUtils.bufferFromAddress;

public class MsgFrameBuffer extends MessageBase {

    public Definitions.FrameBuffer.Builder frameBuffer;

    public MsgFrameBuffer(int imgSize, int cameraType) {
        super();
        initFields(imgSize, cameraType);
        bytesSerializedForm = computeSerializedMsgBytes();
        initSerializedBuffer();
    }

    private void initFields(int imgSize, int cameraType){
        event = messageBuilder.initRoot(Definitions.Event.factory);
        if (cameraType == Camera.CAMERA_TYPE_ROAD)
            frameBuffer = event.initRoadCameraBuffer();
        else if (cameraType == Camera.CAMERA_TYPE_WIDE)
            frameBuffer = event.initWideRoadCameraBuffer();
        else if (cameraType == Camera.CAMERA_TYPE_DRIVER)
            frameBuffer = event.initDriverCameraBuffer();
        else
            throw new IllegalArgumentException("Invalid camera type specified");
        if (imgSize > 0) {
            frameBuffer.initImage(imgSize);
        }
    }

    public static ByteBuffer updateImageBuffer(Definitions.FrameBuffer.Reader msgFrameBuffer, ByteBuffer imgBuffer){
        if (imgBuffer == null){
            if (msgFrameBuffer.getImageAddress() != 0) {
                imgBuffer = bufferFromAddress(msgFrameBuffer.getImageAddress(), Camera.frameSize[0]*Camera.frameSize[1]*3/2);
            }
            else
                imgBuffer = ByteBuffer.allocateDirect(msgFrameBuffer.getImage().size());
        }
        if (msgFrameBuffer.getImageAddress() == 0 & msgFrameBuffer.hasImage()) {
            // TODO: avoid this copy.
            imgBuffer.put(msgFrameBuffer.getImage().asByteBuffer());
            imgBuffer.rewind();
        }
        return imgBuffer;
    }

    public ByteBuffer getImageBuffer(){
        return frameBuffer.getImage().asByteBuffer();
    }

    public void setImageBufferAddress(ByteBuffer buffer){
        frameBuffer.setImageAddress(addressFromBuffer(buffer));
    }
    public void setImageBufferAddress(long address){
        frameBuffer.setImageAddress(address);
    }
}
