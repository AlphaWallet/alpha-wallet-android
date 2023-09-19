package com.alphawallet.app.ui.QRScanning;

import static androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE;
import static com.alphawallet.app.repository.SharedPreferenceRepository.FULL_SCREEN_STATE;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.app.ActivityCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.analytics.Analytics;
import com.alphawallet.app.entity.AnalyticsProperties;
import com.alphawallet.app.entity.analytics.QrScanResultType;
import com.alphawallet.app.entity.analytics.QrScanSource;
import com.alphawallet.app.ui.BaseActivity;
import com.alphawallet.app.ui.WalletConnectActivity;
import com.alphawallet.app.ui.WalletConnectV2Activity;
import com.alphawallet.app.viewmodel.QrScannerViewModel;
import com.alphawallet.app.walletconnect.util.WalletConnectHelper;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.google.zxing.client.android.Intents;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

/**
 * Created by JB on 12/09/2021.
 */
@AndroidEntryPoint
public class QRScannerActivity extends BaseActivity
{
    public static final int RC_HANDLE_IMAGE_PICKUP = 3;
    public static final int DENY_PERMISSION = 1;
    public static final int WALLET_CONNECT = 2;
    private static final int RC_HANDLE_CAMERA_PERM = 2;
    private QrScannerViewModel viewModel;
    private long chainIdOverride;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        hideSystemUI();

        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED)
        {
            initView();
        }
        else
        {
            requestCameraPermission();
        }

        initViewModel();
    }

    private void initViewModel()
    {
        viewModel = new ViewModelProvider(this)
                .get(QrScannerViewModel.class);
    }

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if(result.getContents() == null)
                {
                    onError(null);
                }
                else
                {
                    handleQRCode(result.getContents());
                }
            });

    private void initView()
    {
        chainIdOverride = getIntent().getLongExtra(C.EXTRA_CHAIN_ID, 0);
        setContentView(R.layout.activity_qr_scanner);
        ScanOptions options = new ScanOptions();
        options.addExtra(Intents.Scan.SCAN_TYPE, Intents.Scan.MIXED_SCAN);
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        options.setPrompt(getString(R.string.message_scan_camera));
        barcodeLauncher.launch(options);

        setupToolbar();
    }

    private void setupToolbar()
    {
        toolbar();
        setTitle(getString(R.string.action_scan_dapp));
        enableDisplayHomeAsUp(R.drawable.ic_close);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        String source = getIntent().getStringExtra(QrScanSource.KEY);
        AnalyticsProperties props = new AnalyticsProperties();
        props.put(Analytics.PROPS_QR_SCAN_SOURCE, source);
        viewModel.track(Analytics.Navigation.SCAN_QR_CODE, props);
    }

    private void onError(Throwable throwable)
    {
        displayErrorDialog(getString(R.string.title_dialog_error), getString(R.string.error_browse_selection));
    }

    // Handles the requesting of the camera permission.
    private void requestCameraPermission()
    {
        Timber.tag("QR SCanner").w("Camera permission is not granted. Requesting permission");

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
                        initView();
                        handled = true;
                    }
                }
            }
        }
        else if (requestCode == RC_HANDLE_IMAGE_PICKUP)
        {
            handled = true;
        }

        // Handle deny permission
        if (!handled)
        {
            Intent intent = new Intent();
            setResult(DENY_PERMISSION, intent);
            finish();
        }
    }

    private void displayErrorDialog(String title, String errorMessage)
    {
        viewModel.track(Analytics.Action.SCAN_QR_CODE_ERROR);

        AWalletAlertDialog aDialog = new AWalletAlertDialog(this);
        aDialog.setTitle(title);
        aDialog.setMessage(errorMessage);
        aDialog.setIcon(AWalletAlertDialog.ERROR);
        aDialog.setButtonText(R.string.button_ok);
        aDialog.setButtonListener(v -> {
            aDialog.dismiss();
        });
        aDialog.show();
    }

    private void startWalletConnect(String qrCode)
    {
        Intent intent;
        if (WalletConnectHelper.isWalletConnectV1(qrCode))
        {
            intent = new Intent(this, WalletConnectActivity.class);
            intent.putExtra("qrCode", qrCode);
            intent.putExtra(C.EXTRA_CHAIN_ID, chainIdOverride);
        }
        else
        {
            intent = new Intent(this, WalletConnectV2Activity.class);
            intent.putExtra("url", qrCode);
        }
        startActivity(intent);
        setResult(WALLET_CONNECT);
        finish();
    }

    @Override
    public void onBackPressed()
    {
        viewModel.track(Analytics.Action.SCAN_QR_CODE_CANCELLED);
        Intent intent = new Intent();
        setResult(Activity.RESULT_CANCELED, intent);
        finish();
    }

    public void handleQRCode(String qrCode)
    {
        String resultType = getIntent().getStringExtra(QrScanResultType.KEY);
        if (!TextUtils.isEmpty(resultType))
        {
            AnalyticsProperties props = new AnalyticsProperties();
            props.put(QrScanResultType.KEY, resultType);
            viewModel.track(Analytics.Action.SCAN_QR_CODE_SUCCESS, props);
        }

        if (qrCode.startsWith("wc:"))
        {
            startWalletConnect(qrCode);
        }
        else
        {
            Intent intent = new Intent();
            intent.putExtra(C.EXTRA_QR_CODE, qrCode);
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    }

    private void hideSystemUI()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean(FULL_SCREEN_STATE, false))
        {
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
            WindowInsetsControllerCompat inset = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
            inset.setSystemBarsBehavior(BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            inset.hide(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.navigationBars());
        }
    }
}
