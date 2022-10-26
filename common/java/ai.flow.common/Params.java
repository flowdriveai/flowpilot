package ai.flow.common;

import org.fusesource.lmdbjni.*;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.fusesource.lmdbjni.Constants.*;

public class Params extends ParamsInterface {

    /**
     Implementation based on lmdbjni.
     **/

    private static Env env;
    private static Database db;

    public Params(){
        if (env != null) // Each process should have a single environment.
            return;
        env = new Env();
        String home = System.getenv("HOME");
        String dbPath = home + "/.flowdrive/params";
        new File(dbPath).mkdirs();
        env.setMapSize(1024*1024*1024);
        env.open(dbPath);
        db = env.openDatabase();
    }

    public void putInt(String key, int value){
        byte[] byteKey = bytes(key);
        db.put(byteKey, ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array());
    }

    public void putFloat(String key, float value){
        byte[] byteKey = bytes(key);
        db.put(byteKey, ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value).array());
    }

    public void putShort(String key, short value){
        byte[] byteKey = bytes(key);
        db.put(byteKey, ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array());
    }

    public void putLong(String key, long value){
        byte[] byteKey = bytes(key);
        db.put(byteKey, ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array());
    }

    public void putBool(String key, boolean value){
        System.out.println(key + " " + value);
        byte[] byteKey = bytes(key);
        db.put(byteKey, value ? "1".getBytes() : "0".getBytes());
    }

    public void putDouble(String key, double value){
        byte[] byteKey = bytes(key);
        db.put(byteKey, ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(value).array());
    }

    public void put(String key, byte[] value){
        byte[] byteKey = bytes(key);
        db.put(byteKey, value);
    }

    public void put(String key, String value){
        byte[] byteKey = bytes(key);
        db.put(byteKey, bytes(value));
    }

    public byte[] getBytes(String key){
        byte[] byteKey = bytes(key);
        return db.get(byteKey);
    }

    public int getInt(String key){
        byte[] byteKey = bytes(key);
        return ByteBuffer.wrap(db.get(byteKey)).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public float getFloat(String key){
        byte[] byteKey = bytes(key);
        try (Transaction tx = env.createReadTransaction()) {
            return ByteBuffer.wrap(db.get(tx, byteKey)).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        }
    }

    public short getShort(String key){
        byte[] byteKey = bytes(key);
        return ByteBuffer.wrap(db.get(byteKey)).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }

    public long getLong(String key){
        byte[] byteKey = bytes(key);
        return ByteBuffer.wrap(db.get(byteKey)).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    public double getDouble(String key){
        byte[] byteKey = bytes(key);
        return ByteBuffer.wrap(db.get(byteKey)).order(ByteOrder.LITTLE_ENDIAN).getDouble();
    }

    public String getString(String key){
        byte[] byteKey = bytes(key);
        return new String(db.get(byteKey));
    }

    public boolean getBool(String key){
        byte[] byteKey = bytes(key);
        return new String(db.get(byteKey)).equals("1");
    }

    public void deleteKey(String key){
        byte[] byteKey = bytes(key);
        db.delete(byteKey);
    }

    public boolean exists(String key){
        byte[] byteKey = bytes(key);
        return db.get(byteKey) != null;
    }

    public void blockTillExists(String key) throws InterruptedException {
        while (!exists(key))
            Thread.sleep(50);
    }

    public boolean existsAndCompare(String key, boolean value){
        if (exists(key)) {
            return getBool(key) == value;
        }
        return false;
    }
}