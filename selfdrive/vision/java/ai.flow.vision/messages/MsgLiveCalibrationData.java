package ai.flow.vision.messages;

import ai.flow.definitions.Definitions;
import ai.flow.definitions.MessageBase;
import org.capnproto.PrimitiveList;

import java.nio.ByteBuffer;

public class MsgLiveCalibrationData extends MessageBase {

    public Definitions.LiveCalibrationData.Builder liveCalib;

    public PrimitiveList.Float.Builder extrinsicMatrix;
    public PrimitiveList.Float.Builder rpyCalib;
    public PrimitiveList.Float.Builder rpyCalibSpread;

    public MsgLiveCalibrationData(ByteBuffer rawMessageBuffer) {
        super(rawMessageBuffer);
        initFields();
        bytesSerializedForm = computeSerializedMsgBytes();
        initSerializedBuffer();
    }

    public MsgLiveCalibrationData() {
        super();
        initFields();
        bytesSerializedForm = computeSerializedMsgBytes();
        initSerializedBuffer();
    }

    private void initFields(){
        event = messageBuilder.initRoot(Definitions.Event.factory);
        liveCalib = event.initLiveCalibration();

        extrinsicMatrix = liveCalib.initExtrinsicMatrix(12);
        rpyCalib = liveCalib.initRpyCalib(3);
        rpyCalibSpread = liveCalib.initRpyCalibSpread(3);
    }
}
