package ai.flow.common;

import java.nio.file.Paths;

public class Path {
    @SuppressWarnings("NewApi")
    public static String getFlowPilotRoot() {
        if (SystemUtils.isAndroid())
            // on android, actual flowpilot root resides with termux which cannot be accessed.
            // returns external storage path for now. This may change in the future.
            return "/sdcard/flowpilot/";
        else{
            return Paths.get(System.getProperty("user.dir")).toString();
        }
    }

    @SuppressWarnings("NewApi")
    public static String internal(String relativePath){
        return Paths.get(getFlowPilotRoot(), relativePath).toString();
    }

    public static String getModelDir(){
        String prefix = "f2";
        return internal("selfdrive/assets/models/" + prefix + "/supercombo");
    }

    public static String getFlowdriveDir(){
        if (SystemUtils.isAndroid())
            return "/sdcard/flowpilot/.flowdrive";
        return System.getenv("HOME") + "/.flowdrive";
    }

    public static String getVideoStorageDir(){
        return getFlowdriveDir() + "/media/0/videos";
    }
}
