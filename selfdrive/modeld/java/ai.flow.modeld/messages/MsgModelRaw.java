package ai.flow.modeld.messages;

import ai.flow.definitions.Definitions;
import ai.flow.definitions.MessageBase;
import ai.flow.modeld.CommonModelF3;
import org.capnproto.PrimitiveList;

import java.nio.ByteBuffer;

public class MsgModelRaw extends MessageBase {

    public Definitions.ModelRaw.Builder modelRaw;
    public PrimitiveList.Float.Builder rawPreds;

    public MsgModelRaw(ByteBuffer rawMessageBuffer) {
        super(rawMessageBuffer);
        initFields();
        bytesSerializedForm = computeSerializedMsgBytes();
        initSerializedBuffer();
    }

    public MsgModelRaw() {
        super();
        initFields();
        bytesSerializedForm = computeSerializedMsgBytes();
        initSerializedBuffer();
    }

    private void initFields(){
        event = messageBuilder.initRoot(Definitions.Event.factory);
        modelRaw = event.initModelRaw();
        rawPreds = modelRaw.initRawPredictions(CommonModelF3.NET_OUTPUT_SIZE);
    }

    public void fill(float[] outs, long timestamp, int frameId,
                     int frameAge, float frameDropPerc, float modelExecutionTime) {

        modelRaw.setValid(true);
        modelRaw.setModelExecutionTime(modelExecutionTime);
        modelRaw.setFrameId(frameId);
        modelRaw.setTimestampEof(timestamp);
        modelRaw.setFrameDropPerc(frameDropPerc);
        modelRaw.setFrameAge(frameAge);
        for (int i = 0; i < CommonModelF3.NET_OUTPUT_SIZE; i++)
            rawPreds.set(i, outs[i]);
    }
}
