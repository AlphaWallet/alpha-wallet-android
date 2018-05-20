package io.stormbird.wallet.ui.zxing;


import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import io.stormbird.wallet.R;
import io.stormbird.wallet.ui.BaseActivity;

public class QRScanningActivity extends BaseActivity
{
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    @Override
    public void onCreate(Bundle state)
    {
        super.onCreate(state);
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED)
        {
            setContentView(R.layout.activity_full_screen_scanner_fragment);
        }
        else
        {
            requestCameraPermission();
        }
    }

    // Handles the requesting of the camera permission.
    private void requestCameraPermission() {
        Log.w("QR SCanner", "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                                                                 Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
        }
    }
}
