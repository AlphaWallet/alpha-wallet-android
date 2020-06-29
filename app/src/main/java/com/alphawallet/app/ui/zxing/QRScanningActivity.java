package com.alphawallet.app.ui.zxing;

import android.Manifest;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.ui.BaseActivity;
import com.alphawallet.app.ui.widget.OnQRCodeScannedListener;
import com.alphawallet.app.viewmodel.QRScanningViewModel;
import com.alphawallet.app.viewmodel.QRScanningViewModelFactory;

import java.util.Objects;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class QRScanningActivity extends BaseActivity implements OnQRCodeScannedListener {

    @Inject
    QRScanningViewModelFactory qrScanningViewModelFactory;
    private QRScanningViewModel viewModel;

    private static final int RC_HANDLE_CAMERA_PERM = 2;

    public static final int DENY_PERMISSION = 1;

    private FullScannerFragment fullScannerFragment;

    @Override
    public void onCreate(Bundle state)
    {
        AndroidInjection.inject(this);

        super.onCreate(state);
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED)
        {
            setContentView(R.layout.activity_full_screen_scanner_fragment);
            initView();
        }
        else
        {
            requestCameraPermission();
        }
    }

    private void initView()
    {
        toolbar();
        enableDisplayHomeAsUp();
        setTitle(getString(R.string.action_scan_dapp));

        fullScannerFragment = (FullScannerFragment) getSupportFragmentManager().findFragmentById(R.id.scanner_fragment);

        if(getIntent().getExtras() != null && getIntent().getExtras().containsKey(C.EXTRA_UNIVERSAL_SCAN))
        {
            Objects.requireNonNull(fullScannerFragment).registerListener(this);
        }

        viewModel = ViewModelProviders.of(this, qrScanningViewModelFactory)
                .get(QRScanningViewModel.class);
        viewModel.prepare(this);
    }

    // Handles the requesting of the camera permission.
    private void requestCameraPermission()
    {
        Log.w("QR SCanner", "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};
        ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM); //always ask for permission to scan
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean handled = false;

        if (requestCode == RC_HANDLE_CAMERA_PERM)
        {
            for (int i = 0; i < permissions.length; i++)
            {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (permission.equals(Manifest.permission.CAMERA))
                {
                    if (grantResult == PackageManager.PERMISSION_GRANTED)
                    {
                        setContentView(R.layout.activity_full_screen_scanner_fragment);
                        initView();
                        handled = true;
                    }
                }
            }
        }

        // Handle deny permission
        if (!handled)
        {
            Intent intent = new Intent();
            setResult(DENY_PERMISSION, intent);
            finish();
        }
    }

    @Override
    public void onReceive(String result)
    {
        handleQRCode(result);
    }

    public void handleQRCode(String qrCode)
    {
        viewModel.handleResult(this, qrCode);
    }

    @Override
    public void onBackPressed()
    {
        viewModel.finishWithCancel(this);
    }
}
