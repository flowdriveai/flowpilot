package ai.flow.modeld.messages;

import ai.flow.definitions.MessageBase;
import ai.flow.definitions.Definitions;
import ai.flow.modeld.ParsedOutputs;

import org.capnproto.PrimitiveList;

import java.nio.ByteBuffer;

public class MsgCameraOdometery extends MessageBase {

    public Definitions.CameraOdometry.Builder odometry;

    public PrimitiveList.Float.Builder rot;
    public PrimitiveList.Float.Builder rotStd;
    public PrimitiveList.Float.Builder trans;
    public PrimitiveList.Float.Builder transStd;

    public MsgCameraOdometery(ByteBuffer rawMessageBuffer) {
        super(rawMessageBuffer);
        initFields();
        bytesSerializedForm = computeSerializedMsgBytes();
        initSerializedBuffer();
    }

    public MsgCameraOdometery() {
        super();
        initFields();
        bytesSerializedForm = computeSerializedMsgBytes();
        initSerializedBuffer();
    }

    private void initFields(){
        event = messageBuilder.initRoot(Definitions.Event.factory);
        odometry = event.initCameraOdometry();

        rot = odometry.initRot(3);
        rotStd = odometry.initRotStd(3);
        trans = odometry.initTrans(3);
        transStd = odometry.initTransStd(3);
    }

    public void fillParsed(ParsedOutputs parsed) {
        for (int i = 0; i < 3; i++) {
            parsed.rot[i] = rot.get(i);
            parsed.rotStd[i] = rotStd.get(i);
            parsed.trans[i] = trans.get(i);
            parsed.trans[i] = transStd.get(i);
        }
    }

    public void fill(ParsedOutputs parsed, long timestamp, long frameId) {
        odometry.setTimestampEof(timestamp);
        odometry.setTimestampEof(frameId);

        for (int i = 0; i < 3; i++) {

            rot.set(i, parsed.rot[i]);
            rotStd.set(i, parsed.rotStd[i]);
            trans.set(i, parsed.trans[i]);
            transStd.set(i, parsed.trans[i]);
        }
    }
}
