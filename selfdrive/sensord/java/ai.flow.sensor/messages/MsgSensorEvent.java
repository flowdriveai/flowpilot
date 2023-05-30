package ai.flow.sensor.messages;

import ai.flow.definitions.Definitions;
import ai.flow.definitions.MessageBase;

public class MsgSensorEvent extends MessageBase {
    public Definitions.SensorEventData.Builder sensorEvent;
    public static int TypeAccelerometer = 0;
    public static int TypeGyroscope = 1;

    public MsgSensorEvent(int sensorType) {
        super();
        initFields(sensorType);
        bytesSerializedForm = computeSerializedMsgBytes();
        initSerializedBuffer();
    }

    private void initFields(int type){
        event = messageBuilder.initRoot(Definitions.Event.factory);
        if (type == TypeAccelerometer) {
            sensorEvent = event.initAccelerometer();
            sensorEvent.initAcceleration().initV(3);
        }
        else if (type == TypeGyroscope) {
            sensorEvent = event.initGyroscope();
            sensorEvent.initGyro().initV(3);
        }
    }
}
