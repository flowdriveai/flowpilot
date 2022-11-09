package ai.flow.common;

import java.nio.file.Paths;

public class Path {
    public static String getFlowPilotRoot() {
        if (SystemUtils.isAndroid())
            // on android, actual flowpilot root resides with termux which cannot be accessed.
            // returns external storage path for now. This may change in the future.
            return "/storage/emulated/0/flowpilot";
        else{
            return Paths.get(System.getProperty("user.dir")).getParent().toString();
        }
    }

    public static String internal(String relativePath){
        return Paths.get(getFlowPilotRoot(), relativePath).toString();
    }

    public static String getFlowdriveDir(){
        if (SystemUtils.isAndroid())
            return "/storage/emulated/0/flowpilot/.flowdrive";
        return System.getenv("HOME") + "/.flowdrive";
    }

    public static String getVideoStorageDir(){
        return getFlowdriveDir() + "/media/0/realdata/videos";
    }
}
