package com.alphawallet.app.ui.zxing;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

import com.alphawallet.app.R;
import com.alphawallet.app.ui.QRScanning.BarcodeScannerView;
import com.alphawallet.app.ui.QRScanning.DisplayUtils;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static android.os.VibrationEffect.DEFAULT_AMPLITUDE;

public class ZXingScannerView extends BarcodeScannerView
{
    private static final String TAG = "ZXingScannerView";
    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String lastResult = "";
    private long lastResultTime = 0;
    private static long ERROR_DIFF = 1000 * 4; //4 Seconds between error reports

    public interface ResultHandler {
        void handleResult(Result rawResult);
        boolean checkResultIsValid(Result rawResult);
    }

    private MultiFormatReader mMultiFormatReader;
    public static final List<BarcodeFormat> ALL_FORMATS = new ArrayList<>();
    private List<BarcodeFormat> mFormats;
    private ResultHandler mResultHandler;

    static
    {
        ALL_FORMATS.add(BarcodeFormat.QR_CODE);
        ALL_FORMATS.add(BarcodeFormat.AZTEC);
    }

    public ZXingScannerView(Context context) {
        super(context);
        initMultiFormatReader();
        this.context = context;
    }

    public ZXingScannerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        initMultiFormatReader();
        this.context = context;
    }

    public void setFormats(List<BarcodeFormat> formats) {
        mFormats = formats;
        initMultiFormatReader();
    }

    public void setResultHandler(ResultHandler resultHandler) {
        mResultHandler = resultHandler;
    }

    public Collection<BarcodeFormat> getFormats() {
        if(mFormats == null) {
            return ALL_FORMATS;
        }
        return mFormats;
    }

    private void initMultiFormatReader() {
        Map<DecodeHintType,Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, getFormats());
        mMultiFormatReader = new MultiFormatReader();
        mMultiFormatReader.setHints(hints);
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if(mResultHandler == null) {
            return;
        }
        
        try {
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size size = parameters.getPreviewSize();
            int width = size.width;
            int height = size.height;

            if (DisplayUtils.getScreenOrientation(getContext()) == Configuration.ORIENTATION_PORTRAIT) {
                int rotationCount = getRotationCount();
                if (rotationCount == 1 || rotationCount == 3) {
                    int tmp = width;
                    width = height;
                    height = tmp;
                }
                data = getRotatedData(data, camera);
            }

            Result rawResult = null;
            PlanarYUVLuminanceSource source = buildLuminanceSource(data, width, height);

            if (source != null) {
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                try {
                    rawResult = mMultiFormatReader.decodeWithState(bitmap);
                } catch (ReaderException re) {
                    // continue
                } catch (NullPointerException npe) {
                    // This is terrible
                } catch (ArrayIndexOutOfBoundsException aoe) {

                } finally {
                    mMultiFormatReader.reset();
                }

                if (rawResult == null) {
                    LuminanceSource invertedSource = source.invert();
                    bitmap = new BinaryBitmap(new HybridBinarizer(invertedSource));
                    try {
                        rawResult = mMultiFormatReader.decodeWithState(bitmap);
                    } catch (NotFoundException e) {
                        // continue
                    } finally {
                        mMultiFormatReader.reset();
                    }
                }
            }

            if (rawResult != null) {
                CheckQRImage(rawResult, camera, this);
            } else {
                camera.setOneShotPreviewCallback(this);
            }
        } catch(RuntimeException e) {
            // TODO: Terrible hack. It is possible that this method is invoked after camera is released.
            Log.e(TAG, e.toString(), e);
        }
    }

    private void CheckQRImage(final Result finalRawResult, final Camera camera, final Camera.PreviewCallback pbc)
    {
        handler.post(() -> {
            // Stopping the preview can take a little long.
            // So we want to set result handler to null to discard subsequent calls to
            // onPreviewFrame.
            ResultHandler tmpResultHandler = mResultHandler;

            if (tmpResultHandler != null)
            {
                if (tmpResultHandler.checkResultIsValid(finalRawResult))
                {
                    stopCameraPreview();
                    tmpResultHandler.handleResult(finalRawResult);
                }
                else
                {
                    if (!lastResult.equals(finalRawResult.getText()) || lastResultTime < (System.currentTimeMillis() - ERROR_DIFF))
                    {
                        lastResultTime = System.currentTimeMillis();
                        lastResult = finalRawResult.getText();
                        Toast.makeText(context, R.string.toast_invalid_code, Toast.LENGTH_SHORT).show();
                        vibrate();
                    }
                    camera.setOneShotPreviewCallback(pbc);
                }
            }
        });
    }

    public void resumeCameraPreview(ResultHandler resultHandler) {
        mResultHandler = resultHandler;
        super.resumeCameraPreview();
    }

    private void vibrate()
    {
        Vibrator vb = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vb != null && vb.hasVibrator())
        {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                VibrationEffect vibe = VibrationEffect.createOneShot(200, DEFAULT_AMPLITUDE);
                vb.vibrate(vibe);
            }
            else
            {
                //noinspection deprecation
                vb.vibrate(200);
            }
        }
    }

    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
        Rect rect = getFramingRectInPreview(width, height);
        if (rect == null) {
            return null;
        }
        // Go ahead and assume it's YUV rather than die.
        PlanarYUVLuminanceSource source = null;

        try {
            source = new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top,
                    rect.width(), rect.height(), false);
        } catch(Exception e) {
        }

        return source;
    }
}
