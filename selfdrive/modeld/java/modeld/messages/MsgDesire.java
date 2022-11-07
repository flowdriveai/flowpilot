package ai.flow.modeld.messages;

import ai.flow.definitions.Definitions;
import ai.flow.definitions.MessageBase;
import ai.flow.modeld.ParsedOutputs;
import org.capnproto.PrimitiveList;

import java.nio.ByteBuffer;

public class MsgDesire extends MessageBase {

    public Definitions.Desire.Builder desire_cap;
    public PrimitiveList.Float.Builder meta;

    public MsgDesire(ByteBuffer rawMessageBuffer) {
        super(rawMessageBuffer);
        initFields();
        bytesSerializedForm = computeSerializedMsgBytes();
        initSerializedBuffer();
    }

    public MsgDesire() {
        super();
        initFields();
        bytesSerializedForm = computeSerializedMsgBytes();
        initSerializedBuffer();
    }

    private void initFields(){
        event = messageBuilder.initRoot(Definitions.Event.factory);
        desire_cap = event.initDesire();
        meta = desire_cap.initMeta(44);
    }

    public void fillParsed(ParsedOutputs parsed, Definitions.Desire.Reader reader) {
        for (int i = 0; i < 44; i++)
            parsed.meta[0][i] = reader.getMeta().get(i);
    }

    public void fill(ParsedOutputs parsed, long timestamp) {
        for (int i = 0; i < 44; i++)
            meta.set(i, parsed.meta[0][i]);
    }
}
