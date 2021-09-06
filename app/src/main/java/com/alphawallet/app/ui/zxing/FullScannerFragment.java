package com.alphawallet.app.ui.zxing;

import android.app.Activity;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.QRResult;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.ui.WalletConnectActivity;
import com.alphawallet.app.ui.widget.OnQRCodeScannedListener;
import com.alphawallet.app.util.QRParser;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;
import static com.alphawallet.app.entity.EIP681Type.OTHER;

public class FullScannerFragment extends Fragment implements ZXingScannerView.ResultHandler
{
    public static final String BarcodeObject = "Barcode";
    public static final int SUCCESS = RESULT_OK; /* currenly, this is the only possible result, so does it really make sense to use it? - Weiwu
                                            yes it does because there's also 'DENY_PERMISSION' I assume that wasn't coded at the time
                                            of the comment - JB*/

    private static final String AUTO_FOCUS_STATE = "AUTO_FOCUS_STATE";
    private static final String SELECTED_FORMATS = "SELECTED_FORMATS";
    private static final String CAMERA_ID = "CAMERA_ID";
    private ZXingScannerView mScannerView;
    private boolean mAutoFocus;
    private ArrayList<Integer> mSelectedIndices;
    private int mCameraId = -1;
    private int mScanCode = 0;
    private int chainIdOverride;

    private OnQRCodeScannedListener listener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state)
    {
        mScannerView = new ZXingScannerView(getActivity());

        if (state != null)
        {
            mAutoFocus = state.getBoolean(AUTO_FOCUS_STATE, true);
            mSelectedIndices = state.getIntegerArrayList(SELECTED_FORMATS);
            chainIdOverride = state.getInt(C.EXTRA_CHAIN_ID, 0);
            mCameraId = state.getInt(CAMERA_ID, -1);
        }
        else
        {
            mAutoFocus = true;
            mSelectedIndices = null;
            mCameraId = -1;
        }
        setupFormats();
        return mScannerView;
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        mScannerView.setResultHandler(this);
        mScannerView.startCamera(mCameraId);
        mScannerView.setAutoFocus(mAutoFocus);
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putBoolean(AUTO_FOCUS_STATE, mAutoFocus);
        outState.putIntegerArrayList(SELECTED_FORMATS, mSelectedIndices);
        outState.putInt(CAMERA_ID, mCameraId);
        outState.putInt(C.EXTRA_CHAIN_ID, chainIdOverride);
    }

    @Override
    public void handleResult(Result rawResult)
    {
        try
        {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getActivity(), notification);
            r.play();
        } catch (Exception e) {}

        //Check for listener
        if(listener != null)
        {
            listener.onReceive(rawResult.getText());
        }
        else if (rawResult.getText().startsWith("wc:"))
        {
            startWalletConnect(rawResult.getText());
        }
        else
        {
            Intent intent = new Intent();
            intent.putExtra(BarcodeObject, rawResult.getText());
            getActivity().setResult(SUCCESS, intent);
            getActivity().finish();
        }
    }

    private void startWalletConnect(String qrCode)
    {
        Intent intent = new Intent(getActivity(), WalletConnectActivity.class);
        intent.putExtra("qrCode", qrCode);
        intent.putExtra(C.EXTRA_CHAIN_ID, chainIdOverride);
        startActivity(intent);
        getActivity().setResult(QRScanningActivity.WALLET_CONNECT);
        getActivity().finish();
    }

    @Override
    public boolean checkResultIsValid(Result rawResult)
    {
        if (rawResult == null || rawResult.getText() == null) return false;
        boolean pass = true;
        String qrCode = rawResult.getText();
        try
        {
            QRParser parser = QRParser.getInstance(EthereumNetworkBase.extraChains());
            QRResult qrResult = parser.parse(qrCode);

            if (qrResult.type == OTHER)
            {
                pass = false;
            }
        }
        catch (Exception e)
        {
            pass = false;
        }

        return pass;
    }

    public void closeMessageDialog() {
        closeDialog("scan_results");
    }

    public void closeFormatsDialog() {
        closeDialog("format_selector");
    }

    public void closeDialog(String dialogName) {
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        DialogFragment fragment = (DialogFragment) fragmentManager.findFragmentByTag(dialogName);
        if(fragment != null) {
            fragment.dismiss();
        }
    }

    public void setupFormats() {
        List<BarcodeFormat> formats = new ArrayList<BarcodeFormat>();
        if(mSelectedIndices == null || mSelectedIndices.isEmpty()) {
            mSelectedIndices = new ArrayList<Integer>();
            for(int i = 0; i < ZXingScannerView.ALL_FORMATS.size(); i++) {
                mSelectedIndices.add(i);
            }
        }

        for(int index : mSelectedIndices) {
            formats.add(ZXingScannerView.ALL_FORMATS.get(index));
        }
        if(mScannerView != null) {
            mScannerView.setFormats(formats);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();
        closeMessageDialog();
        closeFormatsDialog();
    }

    public void registerListener(OnQRCodeScannedListener listener)
    {
        this.listener = listener;
    }

    public boolean toggleFlash() throws Exception
    {
        return mScannerView.toggleFlash();
    }

    public void setChainOverride(int chainOverride)
    {
        chainIdOverride = chainOverride;
    }
}
