package ai.flow.common;

public class utils {
    public static boolean getBoolEnvVar(String key) {
        String val = System.getenv(key);
        boolean ret = false;
        if (val != null) {
            if (val.equals("1"))
                ret = true;
        }
        return ret;
    }

    public static double secSinceBoot() {
        return System.nanoTime() / 1e9;
    }

    public static double milliSinceBoot() {
        return System.nanoTime() / 1e6;
    }

    public static double nanoSinceBoot() {
        return System.nanoTime();
    }
}
