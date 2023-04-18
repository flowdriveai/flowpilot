package ai.flow.android;

import ai.flow.hardware.HardwareManager;
import android.view.Window;
import android.view.WindowManager;

public class AndroidHardwareManager extends HardwareManager {
    public Window window;
    public AndroidHardwareManager(Window window){
        this.window = window;
    }

    public void enableScreenWakeLock(boolean enable){
        if (enable)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}
