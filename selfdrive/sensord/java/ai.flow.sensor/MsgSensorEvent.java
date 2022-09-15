package ai.flow.sensor;

import ai.flow.definitions.Definitions;
import ai.flow.definitions.MessageBase;
import org.capnproto.StructList;

import java.nio.ByteBuffer;

public class MsgSensorEvent extends MessageBase {
    public StructList.Builder<Definitions.SensorEventData.Builder> sensorEvent;

    public MsgSensorEvent(ByteBuffer rawMessageBuffer) {
        super(rawMessageBuffer);
        initFields();
        bytesSerializedForm = computeSerializedMsgBytes();
        initSerializedBuffer();
    }

    public MsgSensorEvent() {
        super();
        initFields();
        bytesSerializedForm = computeSerializedMsgBytes();
        initSerializedBuffer();
    }

    private void initFields(){
        event = messageBuilder.initRoot(Definitions.Event.factory);
        sensorEvent = event.initSensorEvents(2);

        sensorEvent.get(0).initAcceleration().initV(3);
        sensorEvent.get(1).initGyro().initV(3);
    }
}
