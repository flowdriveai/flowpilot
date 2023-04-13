package ai.flow.modeld.messages;

import ai.flow.definitions.Definitions;
import ai.flow.definitions.MessageBase;
import org.capnproto.PrimitiveList;

public class MsgFrameData extends MessageBase {

    public Definitions.FrameData.Builder frameData;
    public PrimitiveList.Float.Builder intrinsics;

    public MsgFrameData() {
        super();
        initFields();
        bytesSerializedForm = computeSerializedMsgBytes();
        initSerializedBuffer();
    }

    private void initFields(){
        event = messageBuilder.initRoot(Definitions.Event.factory);
        frameData = event.initFrameData();
        intrinsics = frameData.initIntrinsics(9);
    }
}
