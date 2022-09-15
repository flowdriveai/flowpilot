package ai.flow.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Path {
    public static String getFlowPilotRoot() {
        if (SystemUtils.isAndroid())
            return "/storage/emulated/0/flow-pilot";
        else{
            return Paths.get(System.getProperty("user.dir")).getParent().toString();
        }
    }

    public static String internal(String relativePath){
        return Paths.get(getFlowPilotRoot(), relativePath).toString();
    }

    public static void initDataDir(){
        try {
            Files.createDirectories(Paths.get(internal("data")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
