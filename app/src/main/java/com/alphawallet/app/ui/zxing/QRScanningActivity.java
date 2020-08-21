package com.alphawallet.app.ui.zxing;

import android.Manifest;
import android.app.Activity;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.TextView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.ui.BaseActivity;
import com.alphawallet.app.ui.WalletConnectActivity;
import com.alphawallet.app.ui.widget.OnQRCodeScannedListener;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.lang.ref.SoftReference;
import java.util.Objects;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;

public class QRScanningActivity extends BaseActivity implements OnQRCodeScannedListener {

    private static final int RC_HANDLE_CAMERA_PERM = 2;
    public static final int RC_HANDLE_IMAGE_PICKUP = 3;

    public static final int DENY_PERMISSION = 1;

    private FullScannerFragment fullScannerFragment;

    private TextView flashButton;
    private TextView myAddressButton;
    private TextView browseButton;
    private Disposable disposable;
    private AWalletAlertDialog dialog;

    @Override
    public void onCreate(Bundle state)
    {
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

        flashButton = findViewById(R.id.flash_button);
        myAddressButton = findViewById(R.id.my_address_button);
        browseButton = findViewById(R.id.browse_button);

        fullScannerFragment = (FullScannerFragment) getSupportFragmentManager().findFragmentById(R.id.scanner_fragment);

        if (getIntent().getExtras() != null && getIntent().getExtras().containsKey(C.EXTRA_UNIVERSAL_SCAN))
        {
            Objects.requireNonNull(fullScannerFragment).registerListener(this);
        }

        flashButton.setOnClickListener(view -> {
            try
            {
                boolean isFlashOn = fullScannerFragment.toggleFlash();

                if (isFlashOn)
                {
                    flashButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_flash_off, 0,0);
                }
                else
                {
                    flashButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_flash, 0,0);
                }
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

    private void pickImage()
    {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), RC_HANDLE_IMAGE_PICKUP);
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

    @Override
    public void onReceive(String result)
    {
        handleQRCode(result);
    }

    public void handleQRCode(String qrCode)
    {
        if (qrCode.startsWith("wc:")) {
            startWalletConnect(qrCode);
        } else {
            Intent intent = new Intent();
            intent.putExtra(C.EXTRA_QR_CODE, qrCode);
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    }

    private void startWalletConnect(String qrCode) {
        Intent intent = new Intent(this, WalletConnectActivity.class);
        intent.putExtra("qrCode", qrCode);
        startActivity(intent);
        setResult(RESULT_OK);
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_HANDLE_IMAGE_PICKUP && resultCode == Activity.RESULT_OK)
        {
            if (data != null) {
                Uri selectedImage = data.getData();

                disposable = concertAndHandle(selectedImage)
                        .observeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onSuccess, this::onError);
            }
        }
    }

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
        else
        {
            handleQRCode(result.getText());
        }
    }

    private Single<Result> concertAndHandle(Uri selectedImage)
    {
        return Single.fromCallable(() -> {

            SoftReference<Bitmap> softReferenceBitmap;
            softReferenceBitmap = new SoftReference<>(MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage));

            if (softReferenceBitmap.get() != null)
            {
                int width = softReferenceBitmap.get().getWidth(), height = softReferenceBitmap.get().getHeight();
                int[] pixels = new int[width * height];
                softReferenceBitmap.get().getPixels(pixels, 0, width, 0, 0, width, height);
                softReferenceBitmap.get().recycle();
                softReferenceBitmap.clear();
                RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
                BinaryBitmap bBitmap = new BinaryBitmap(new HybridBinarizer(source));
                MultiFormatReader reader = new MultiFormatReader();
                return reader.decodeWithState(bBitmap);
            }

            return null;
        });
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void stop()
    {
        if (disposable != null && !disposable.isDisposed())
        {
            disposable.dispose();
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
        dialog = aDialog;
        dialog.show();
    }
}
