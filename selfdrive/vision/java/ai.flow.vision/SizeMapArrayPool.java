package ai.flow.vision;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SizeMapArrayPool {

    private final HashMap<Integer, List<float[]>> pool = new HashMap<>();
    private final HashMap<Integer, List<Boolean>> inUse = new HashMap<>();

    public synchronized float[] getArray(int size) {
        if (pool.containsKey(size)) {
            List<float[]> arrays = pool.get(size);
            List<Boolean> currInUse = inUse.get(size);
            for (int i = 0; i < arrays.size(); i++) {
                if (!currInUse.get(i)) {
                    currInUse.set(i, true);
                    return arrays.get(i);
                }
            }
        }
        else{
            float[] newArr = new float[size];
            List<float[]> newArrGroup = new ArrayList<float[]>();
            List<Boolean> newInUseGroup = new ArrayList<Boolean>();
            newArrGroup.add(newArr);
            newInUseGroup.add(true);
            pool.put(size, newArrGroup);
            inUse.put(size, newInUseGroup);
            return newArr;
        }
        float[] newArr = new float[size];
        pool.get(size).add(newArr);
        inUse.get(size).add(true);
        return newArr;
    }

    public synchronized void returnArray(float[] array) {
        if (pool.containsKey(array.length)) {
            List<float[]> arrays = pool.get(array.length);
            List<Boolean> currInUse = inUse.get(array.length);
            for (int i = 0; i < arrays.size(); i++) {
                if (array == arrays.get(i)) {
                    currInUse.set(i, false);
                    return;
                }
            }
        }
        throw new RuntimeException("Array " + array + " was not obtained from the pool " + this);
    }
}
