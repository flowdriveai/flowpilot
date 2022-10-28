package ai.flow.common;

public class utils {
    public static boolean getBoolEnvVar(String key){
        String val = System.getenv(key);
        boolean ret = false;
        if (val != null){
            if (val.equals("1"))
                ret =  true;
        }
        return ret;
    }
}
