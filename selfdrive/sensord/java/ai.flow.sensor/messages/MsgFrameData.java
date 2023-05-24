package ai.flow.modeld.messages;

import ai.flow.common.transformations.Camera;
import ai.flow.definitions.Definitions;
import ai.flow.definitions.MessageBase;
import org.capnproto.PrimitiveList;

public class MsgFrameData extends MessageBase {

    public Definitions.FrameData.Builder frameData;
    public PrimitiveList.Float.Builder intrinsics;

    public MsgFrameData(int cameraType) {
        super();
        initFields(cameraType);
        bytesSerializedForm = computeSerializedMsgBytes();
        initSerializedBuffer();
    }

    private void initFields(int cameraType){
        event = messageBuilder.initRoot(Definitions.Event.factory);
        if (cameraType == Camera.CAMERA_TYPE_ROAD)
            frameData = event.initRoadCameraState();
        intrinsics = frameData.initIntrinsics(9);
    }
}
