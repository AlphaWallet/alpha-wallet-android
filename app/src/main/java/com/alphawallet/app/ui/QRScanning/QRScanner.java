package com.alphawallet.app.ui.QRScanning;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.PreferenceManager;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.ui.BaseActivity;
import com.alphawallet.app.ui.WalletConnectActivity;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.BeepManager;
import com.google.zxing.common.HybridBinarizer;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE;
import static com.alphawallet.app.repository.SharedPreferenceRepository.FULL_SCREEN_STATE;

/**
 * Created by JB on 12/09/2021.
 */
public class QRScanner extends BaseActivity
{
    private DecoratedBarcodeView barcodeView;
    private BeepManager beepManager;
    private String lastText;
    private Disposable disposable;
    private long chainIdOverride;
    private TextView flashButton;
    private TextView myAddressButton;
    private TextView browseButton;
    private boolean torchOn = false;

    private static final int RC_HANDLE_CAMERA_PERM = 2;
    public static final int RC_HANDLE_IMAGE_PICKUP = 3;
    public static final int DENY_PERMISSION = 1;
    public static final int WALLET_CONNECT = 2;

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
    }

    private void initView()
    {
        setContentView(R.layout.activity_qr_scanner);

        chainIdOverride = getIntent().getLongExtra(C.EXTRA_CHAIN_ID, 0);
        barcodeView = findViewById(R.id.scanner_view);
        Collection<BarcodeFormat> formats = Arrays.asList(BarcodeFormat.QR_CODE, BarcodeFormat.CODE_39, BarcodeFormat.AZTEC);
        barcodeView.getBarcodeView().setDecoderFactory(new DefaultDecoderFactory(formats));
        barcodeView.initializeFromIntent(getIntent());
        barcodeView.decodeContinuous(callback);
        barcodeView.setStatusText("");

        beepManager = new BeepManager(this);

        flashButton = findViewById(R.id.flash_button);
        myAddressButton = findViewById(R.id.my_address_button);
        browseButton = findViewById(R.id.browse_button);

        setupToolbar();

        setupButtons();
    }

    private void setupToolbar() {
        toolbar();
        setTitle(getString(R.string.action_scan_dapp));
        enableDisplayHomeAsUp(R.drawable.ic_close);
    }

    private void setupButtons()
    {
        flashButton.setOnClickListener(view -> {
            try
            {
                if (!torchOn)
                {
                    flashButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_flash_off, 0,0);
                    barcodeView.setTorchOn();
                }
                else
                {
                    flashButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_flash, 0,0);
                    barcodeView.setTorchOff();
                }
                torchOn = !torchOn;
            }
            catch (Exception e)
            {
                onError(e);
            }
        });

        myAddressButton.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.putExtra(C.EXTRA_ACTION_NAME, C.ACTION_MY_ADDRESS_SCREEN);
            setResult(Activity.RESULT_OK, intent);
            finish();
        });

        browseButton.setOnClickListener(view -> {
            if (ActivityCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            {
                String[] permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
                requestPermissions(permissions, RC_HANDLE_IMAGE_PICKUP);
            }
            else
            {
                pickImage();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (barcodeView != null) barcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (barcodeView != null) barcodeView.pause();
    }

    private final BarcodeCallback callback = new BarcodeCallback()
    {
        @Override
        public void barcodeResult(BarcodeResult result)
        {
            if (result.getText() == null || result.getText().equals(lastText))
            {
                // Prevent duplicate scans
                return;
            }

            lastText = result.getText();
            barcodeView.setStatusText(result.getText());

            beepManager.playBeepSoundAndVibrate();

            //send result
            handleQRCode(result.getText());
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints)
        {
        }
    };

    ActivityResultLauncher<String> getQRImage = registerForActivityResult(new ActivityResultContracts.GetContent(),
            uri -> {
                disposable = concertAndHandle(uri)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onSuccess, this::onError);
            });

    private void onError(Throwable throwable)
    {
        displayErrorDialog(getString(R.string.title_dialog_error), getString(R.string.error_browse_selection));
    }

    private void onSuccess(Result result)
    {
        if (result == null)
        {
            displayErrorDialog(getString(R.string.title_dialog_error), getString(R.string.error_browse_selection));
        }
        else if (!TextUtils.isEmpty(result.getText()))
        {
            handleQRCode(result.getText());
        }
    }

    private void pickImage()
    {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        getQRImage.launch("image/*");
    }

    private Single<Result> concertAndHandle(Uri selectedImage)
    {
        return Single.fromCallable(() -> {

            if (selectedImage == null) return new Result("", null, null, BarcodeFormat.QR_CODE);

            Bitmap bitmap = getMutableCapturedImage(selectedImage);

            if (bitmap != null)
            {
                int width = bitmap.getWidth(), height = bitmap.getHeight();
                int[] pixels = new int[width * height];
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
                bitmap.recycle();

                RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
                BinaryBitmap bBitmap = new BinaryBitmap(new HybridBinarizer(source));
                MultiFormatReader reader = new MultiFormatReader();
                return reader.decodeWithState(bBitmap);
            }

            return null;
        });
    }

    private Bitmap getMutableCapturedImage(Uri selectedPhotoUri)
    {
        try
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            {
                ImageDecoder.Source bitMapSrc = ImageDecoder.createSource(getContentResolver(), selectedPhotoUri);
                return ImageDecoder.decodeBitmap(bitMapSrc).copy(Bitmap.Config.ARGB_8888, true);
            }
            else
            {
                //noinspection deprecation
                return MediaStore.Images.Media.getBitmap(getContentResolver(), selectedPhotoUri);
            }
        }
        catch (IOException e)
        {
            return null;
        }
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
                        barcodeView.resume();
                        handled = true;
                    }
                }
            }
        }
        else if (requestCode == RC_HANDLE_IMAGE_PICKUP)
        {
            pickImage();
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
        Intent intent = new Intent(this, WalletConnectActivity.class);
        intent.putExtra("qrCode", qrCode);
        intent.putExtra(C.EXTRA_CHAIN_ID, chainIdOverride);
        startActivity(intent);
        setResult(WALLET_CONNECT);
        finish();
    }

    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent();
        setResult(Activity.RESULT_CANCELED, intent);
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return barcodeView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void stop()
    {
        if (disposable != null && !disposable.isDisposed())
        {
            disposable.dispose();
        }
    }

    public void handleQRCode(String qrCode)
    {
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
