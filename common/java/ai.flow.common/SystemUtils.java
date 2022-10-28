package ai.flow.common;

import java.lang.management.ManagementFactory;

public class SystemUtils {

    public static String linux = "Linux";
    public static String mac = "Darwin";
    public static String windows = "Windows";
    public static String unknown = "Unknown";
    public static String android = "Android";

    public static String getPlatform(){
        String os = System.getProperty("os.name");
        if (os.contains("win"))
            return windows;
        else if (isAndroid())
            return android;
        else if (os.contains("nix") || os.contains("nux") || os.contains("aix"))
            return linux;
        else if (os.contains("mac"))
            return mac;
        else
            return unknown;
    }

    public static boolean isAndroid(){
        return System.getProperty("java.runtime.name").equals("Android Runtime");
    }

    public static int getPID(){
        if (!isAndroid()){
            return Integer.parseInt(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
        }
        else{
            return -1;
        }
    }

    public static boolean getUseGPU(){
        return utils.getBoolEnvVar("USE_GPU");
    }
}
