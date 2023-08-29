package ai.flow.android.sensor;

import ai.flow.sensor.messages.MsgSensorEvent;
import ai.flow.sensor.SensorInterface;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import messaging.ZMQPubHandler;
import org.capnproto.PrimitiveList;

import java.util.Arrays;

public class SensorManager extends SensorInterface implements Runnable{

    public ZMQPubHandler ph;
    public String topic;
    public boolean initialized = false;
    public boolean running = false;
    public int frequency;
    public Thread thread;
    public int delay; // in milliseconds;
    public MsgSensorEvent msgAcceleration = new MsgSensorEvent(MsgSensorEvent.TypeAccelerometer);
    public MsgSensorEvent msgGyroscope = new MsgSensorEvent(MsgSensorEvent.TypeGyroscope);
    public android.hardware.SensorManager sensorManager;

    public Sensor sensorAccelerometer;
    public SensorEventListener listenerAccelerometer;
    PrimitiveList.Float.Builder accVec3 = msgAcceleration.sensorEvent.getAcceleration().getV();

    public Sensor sensorGyroscope;
    public SensorEventListener listenerGyroscope;
    PrimitiveList.Float.Builder gyroVec3 = msgGyroscope.sensorEvent.getGyro().getV();

    public SensorManager(Context context, int frequency) {
        ph = new ZMQPubHandler();
        ph.createPublishers(Arrays.asList("accelerometer", "gyroscope"));
        this.frequency = frequency;
        this.delay = (int) (1.0f / frequency * 1000);

        sensorManager = (android.hardware.SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        listenerAccelerometer = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                accVec3.set(0, sensorEvent.values[0]);
                accVec3.set(1, sensorEvent.values[1]);
                accVec3.set(2, sensorEvent.values[2]);
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };

        listenerGyroscope = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                gyroVec3.set(0, sensorEvent.values[0]);
                gyroVec3.set(1, sensorEvent.values[1]);
                gyroVec3.set(2, sensorEvent.values[2]);
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };
    }

    public void start() {
        if (thread == null) {
            thread = new Thread(this, "sensorManager");
            thread.setDaemon(false);
            thread.start();
        }
    }

    public void run(){
        if (running)
            return;
        sensorManager.registerListener(listenerAccelerometer, sensorAccelerometer, delay*1000);
        sensorManager.registerListener(listenerGyroscope, sensorGyroscope, delay*1000);
        initialized = true;
        running = true;

        while (running){
            ph.publishBuffer("accelerometer", msgAcceleration.serialize(true));
            ph.publishBuffer("gyroscope", msgGyroscope.serialize(true));
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        if (!running)
            return;
        sensorManager.unregisterListener(listenerAccelerometer);
        sensorManager.unregisterListener(listenerGyroscope);
        initialized = false;
        running = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        thread = null;
    }

    public void dispose(){
        stop();
    }

    public boolean isRunning(){
        return running;
    }
    public boolean isInitialized(){
        return initialized;
    }
}
