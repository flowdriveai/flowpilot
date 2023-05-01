package ai.flow.android;

import ai.flow.app.R;
import ai.flow.common.ParamsInterface;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.termux.shared.termux.TermuxConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.os.Build.VERSION.SDK_INT;

public class LoadingActivity extends AppCompatActivity {

    List<String> requiredPermissions = Arrays.asList(Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.VIBRATE,
            "com.termux.permission.RUN_COMMAND");

    public boolean bootComplete = false;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_loading);

        ImageView imageView = findViewById(R.id.spinner);
        Glide.with(this).load(R.drawable.spinner).into(imageView);

        ensureBoot();
    }

    private boolean checkPermissions() {
        for (String permission: requiredPermissions){
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        // External storage access permissions for android 12 and above.
        if (SDK_INT >= Build.VERSION_CODES.R)
            return Environment.isExternalStorageManager();
        return true;
    }

    private void ensureBoot(){
        new Thread(new Runnable() {
            public void run() {
                // request permissions and wait till granted.
                requestPermissions();
                int i = 0;
                while (!checkPermissions()){
                    // show toast every 4 seconds
                    if (i%40 == 0)
                        Toast.makeText(getApplicationContext(), "Flowpilot needs all required permissions to be granted to work.", Toast.LENGTH_LONG).show();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    i++;
                }

                // boot all the flowpilot daemons in non-java land.
                bootTermux();

                ParamsInterface params = ParamsInterface.getInstance();
                params.getBool("F3");

                bootComplete = true;

                Intent intent = new Intent(getApplicationContext(), AndroidLauncher.class);
                startActivity(intent);
            }
        }).start();
    }

    public void bootTermux(){
        Intent intent = new Intent();
        intent.setClassName(TermuxConstants.TERMUX_PACKAGE_NAME, TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE_NAME);
        intent.setAction(TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND);
        intent.putExtra(TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE.EXTRA_COMMAND_PATH, "/data/data/com.termux/files/usr/bin/bash");
        intent.putExtra(TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE.EXTRA_ARGUMENTS, new String[]{"boot_flowpilot"});
        intent.putExtra(TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE.EXTRA_WORKDIR, "/data/data/com.termux/files/home");
        intent.putExtra(TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE.EXTRA_BACKGROUND, true);
        intent.putExtra(TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE.EXTRA_SESSION_ACTION, "0");
        intent.putExtra(TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE.EXTRA_COMMAND_LABEL, "boot flowpilot");
        startService(intent);
    }

    private void requestPermissions() {
        List<String> requestPermissions = new ArrayList<>();
        for (String permission: requiredPermissions){
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
                requestPermissions.add(permission);
        }
        if (!requestPermissions.isEmpty())
            ActivityCompat.requestPermissions(this, requestPermissions.toArray(new String[0]), 1);

        // External storage access permissions for android 12 and above.
        if (SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager())
                return;
            try {
                Toast.makeText(this, "grant external storage access to flowpilot.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s",getApplicationContext().getPackageName())));
                startActivityForResult(intent, 6969);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, 6969);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}