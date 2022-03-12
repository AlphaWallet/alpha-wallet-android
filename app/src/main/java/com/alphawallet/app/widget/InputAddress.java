package com.alphawallet.app.widget;

import static android.content.Context.CLIPBOARD_SERVICE;

import static com.alphawallet.app.entity.tokenscript.TokenscriptFunction.ZERO_ADDRESS;

import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.ENSCallback;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.ui.QRScanning.QRScanner;
import com.alphawallet.app.ui.widget.adapter.AutoCompleteAddressAdapter;
import com.alphawallet.app.ui.widget.entity.AddressReadyCallback;
import com.alphawallet.app.ui.widget.entity.BoxStatus;
import com.alphawallet.app.ui.widget.entity.ENSHandler;
import com.alphawallet.app.ui.widget.entity.ItemClickListener;
import com.alphawallet.app.util.KeyboardUtils;
import com.alphawallet.app.util.Utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * Created by JB on 28/10/2020.
 */
public class InputAddress extends RelativeLayout implements ItemClickListener, ENSCallback, TextWatcher
{
    private final AutoCompleteTextView editText;
    private final TextView labelText;
    private final TextView pasteItem;
    private final TextView statusText;
    private final UserAvatar avatar;
    private final ImageButton scanQrIcon;
    private final RelativeLayout boxLayout;
    private final TextView errorText;
    private final Context context;
    private int labelResId;
    private int hintRedId;
    private boolean noCam;
    private String imeOptions;
    private String fullAddress;
    private String ensName;
    private boolean handleENS = false;
    private final ENSHandler ensHandler;
    private AWalletAlertDialog dialog;
    private AddressReadyCallback addressReadyCallback = null;
    private long chainOverride;
    private final Pattern findAddress = Pattern.compile("^(\\s?)+(0x)([0-9a-fA-F]{40})(\\s?)+\\z");
    private final float standardTextSize;

    public InputAddress(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        inflate(context, R.layout.item_input_address, this);
        getAttrs(context, attrs);
        this.context = context;

        fullAddress = null;
        ensName = null;

        labelText = findViewById(R.id.label);
        editText = findViewById(R.id.edit_text);
        pasteItem = findViewById(R.id.text_paste);
        statusText = findViewById(R.id.status_text);
        scanQrIcon = findViewById(R.id.img_scan_qr);
        boxLayout = findViewById(R.id.box_layout);
        errorText = findViewById(R.id.error_text);
        avatar = findViewById(R.id.avatar);
        editText.addTextChangedListener(this);
        editText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);

        if (handleENS)
        {
            AutoCompleteAddressAdapter adapterUrl = new AutoCompleteAddressAdapter(context, C.ENS_HISTORY);
            adapterUrl.setListener(this);
            ensHandler = new ENSHandler(this, adapterUrl);
        }
        else
        {
            ensHandler = null;
        }

        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus)
            {
                setBoxColour(BoxStatus.SELECTED);
            }
            else
            {
                setBoxColour(BoxStatus.UNSELECTED);
            }
        });

        setViews();
        setImeOptions();
        chainOverride = 0;
        standardTextSize = editText.getTextSize();
        avatar.resetBinding();
    }

    private void getAttrs(Context context, AttributeSet attrs)
    {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.InputView,
                0, 0
        );

        try
        {
            labelResId = a.getResourceId(R.styleable.InputView_label, R.string.empty);
            hintRedId = a.getResourceId(R.styleable.InputView_hint, R.string.empty);
            handleENS = a.getBoolean(R.styleable.InputView_ens, false);
            imeOptions = a.getString(R.styleable.InputView_imeOptions);
            noCam = a.getBoolean(R.styleable.InputView_nocam, false);
            boolean showHeader = a.getBoolean(R.styleable.InputView_show_header, true);
            int headerTextId = a.getResourceId(R.styleable.InputView_label, R.string.recipient);
            findViewById(R.id.layout_header).setVisibility(showHeader ? View.VISIBLE : View.GONE);
            TextView headerText = findViewById(R.id.text_header);
            headerText.setText(headerTextId);
        }
        finally
        {
            a.recycle();
        }
    }

    private void setViews()
    {
        if (labelResId != R.string.empty)
        {
            labelText.setText(labelResId);
            labelText.setVisibility(View.VISIBLE);
        }

        editText.setHint(hintRedId);

        //Paste
        pasteItem.setOnClickListener(v -> {
            //from clipboard
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
            try
            {
                CharSequence textToPaste = clipboard.getPrimaryClip().getItemAt(0).getText();
                editText.append(textToPaste);
            }
            catch (Exception e)
            {
                Timber.e(e, e.getMessage());
            }
        });

        if (noCam)
        {
            scanQrIcon.setVisibility(View.GONE);
        }
        else
        {
            //QR Scanner
            scanQrIcon.setOnClickListener(v -> {
                Intent intent = new Intent(context, QRScanner.class);
                intent.putExtra(C.EXTRA_CHAIN_ID, chainOverride);
                ((Activity) context).startActivityForResult(intent, C.BARCODE_READER_REQUEST_CODE);
            });
        }
    }

    private void setImeOptions()
    {
        if (imeOptions != null)
        {
            switch (imeOptions)
            {
                case "actionNext":
                {
                    editText.setImeOptions(EditorInfo.IME_ACTION_NEXT);
                    break;
                }
                case "actionDone":
                {
                    editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
                    editText.setRawInputType(InputType.TYPE_CLASS_TEXT);
                    break;
                }
            }
        }
        else
        {
            editText.setImeOptions(EditorInfo.IME_ACTION_DONE); //add default action
        }

        editText.setOnEditorActionListener((v, actionId, event) -> {
            hideKeyboard();
            return false;
        });
    }

    public void setENSAddress(String address)
    {
        fullAddress = address;
        setStatus(address);
    }

    public void setENSName(String _ensName)
    {
        ensName = _ensName;
        setStatus(ensName);
    }

    public void setStatus(CharSequence statusTxt)
    {
        if (TextUtils.isEmpty(statusTxt))
        {
            statusText.setVisibility(View.GONE);
            statusText.setText(R.string.empty);
            avatar.setVisibility(View.GONE);
            avatar.resetBinding();
            if (errorText.getVisibility() == View.VISIBLE) //cancel error
            {
                setBoxColour(BoxStatus.SELECTED);
            }
        }
        else
        {
            String status;
            if (statusTxt.toString().startsWith("0x"))
            {
                status = Utils.formatAddress(statusTxt.toString());
            }
            else
            {
                status = statusTxt.toString();
            }
            statusText.setText(status);
            statusText.setVisibility(View.VISIBLE);
        }
    }

    public void setError(CharSequence errorTxt)
    {
        statusText.setVisibility(View.GONE);
        avatar.setVisibility(View.GONE);
        avatar.resetBinding();
        setBoxColour(BoxStatus.ERROR);
        errorText.setVisibility(View.VISIBLE);
        errorText.setText(errorTxt);
    }

    public void setWaitingSpinner(boolean waiting)
    {
        if (waiting)
        {
            avatar.setWaiting();
        }
        else
        {
            avatar.finishWaiting();
        }
    }

    private void setBoxColour(BoxStatus status)
    {
        switch (status)
        {
            case ERROR:
                boxLayout.setBackgroundResource(R.drawable.background_input_error);
                labelText.setTextColor(context.getColor(R.color.danger));
                break;
            case UNSELECTED:
                boxLayout.setBackgroundResource(R.drawable.background_password_entry);
                labelText.setTextColor(context.getColor(R.color.dove));
                errorText.setVisibility(View.GONE);
                break;
            case SELECTED:
                boxLayout.setBackgroundResource(R.drawable.background_input_selected);
                labelText.setTextColor(context.getColor(R.color.azure));
                errorText.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public void onItemClick(String url)
    {
        if (ensHandler != null)
        {
            ensHandler.handleHistoryItemClick(url);
        }
    }

    @Override
    public void ENSResolved(String address, String ens)
    {
        errorText.setVisibility(View.GONE);
        setWaitingSpinner(false);
        bindAvatar(address, ens);
        if (addressReadyCallback != null)
        {
            addressReadyCallback.resolvedAddress(address, ens);
            addressReadyCallback.addressValid(true);
        }
    }

    @Override
    public void ENSName(String name)
    {
        //start searching the avatar
        bindAvatar(ZERO_ADDRESS, name);
    }

    private void bindAvatar(String address, String ensName)
    {
        avatar.setVisibility(View.VISIBLE);
        Wallet temp = new Wallet(address);
        temp.ENSname = ensName;
        avatar.bindAndFind(temp);
    }

    @Override
    public void ENSComplete()
    {
        displayCheckingDialog(false);
        if (addressReadyCallback != null)
        {
            addressReadyCallback.addressReady(getInputAddress(), getENS());
            addressReadyCallback.addressValid(true);
        }
        else
        {
            throw new RuntimeException("Need to implement AddressReady in your class which implements " +
                    "InputAddress, and set the AddressReady callback: 'inputAddress.setAddressCallback(this);'");
        }
    }

    private String getInputAddress()
    {
        String mainText = editText.getText().toString().trim();
        String status = statusText.getText().toString().trim();

        if (Utils.isAddressValid(fullAddress))
        {
            return fullAddress;
        }
        else if (Utils.isAddressValid(mainText))
        {
            return mainText;
        }
        else if (Utils.isAddressValid(status))
        {
            return status;
        }

        return null;
    }

    private String getENS()
    {
        String mainText = editText.getText().toString().trim();
        String status = statusText.getText().toString().trim();

        if (Utils.isAddressValid(mainText) && !TextUtils.isEmpty(status) && status.contains("."))
        {
            return status;
        }
        else if (Utils.isAddressValid(fullAddress) && !TextUtils.isEmpty(mainText) && mainText.contains("."))
        {
            return mainText;
        }

        return null;
    }

    /**
     * Wait until we have fully resolved the ENS name if required
     * @return
     */
    public void getAddress()
    {
        if (ensHandler != null)
        {
            ensHandler.getAddress();
        }
        else
        {
            ENSComplete();
        }
    }

    @Override
    public void displayCheckingDialog(boolean shouldShow)
    {
        if (shouldShow)
        {
            dialog = new AWalletAlertDialog(getContext());
            dialog.setIcon(AWalletAlertDialog.NONE);
            dialog.setTitle(R.string.title_dialog_check_ens);
            dialog.setProgressMode();
            dialog.setCancelable(false);
            dialog.show();
        }
        else if (dialog != null && dialog.isShowing())
        {
            dialog.dismiss();
        }
    }

    public AutoCompleteTextView getEditText()
    {
        return editText;
    }

    public String getStatusText()
    {
        return statusText.getText().toString();
    }

    public void setChainOverrideForWalletConnect(long chainId)
    {
        chainOverride = chainId;
    }

    public void hideKeyboard()
    {
        KeyboardUtils.hideKeyboard(this);
    }

    public void setAddress(String text)
    {
        if (!TextUtils.isEmpty(text))
        {
            editText.setText(text);
        }
    }

    public String getInputText()
    {
        return editText.getText().toString().trim();
    }

    public void setAddressCallback(AddressReadyCallback addressReadyCallback)
    {
        this.addressReadyCallback = addressReadyCallback;
    }

    public void stopNameCheck()
    {
        displayCheckingDialog(false);
    }

    public AutoCompleteTextView getInputView()
    {
        return editText;
    }

    public int getInputLength()
    {
        return editText.getText().length();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after)
    {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count)
    {
        setStatus(null);
        float ts = editText.getTextSize();
        int amount = getInputLength();
        if (amount > 30 && ts == standardTextSize && !noCam)
        {
            editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, standardTextSize*0.85f); //shrink text size to fit
        }
        else if (amount <= 30 && ts < standardTextSize)
        {
            editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, standardTextSize);
        }
    }

    @Override
    public void afterTextChanged(Editable s)
    {
        if (addressReadyCallback == null) return;

        final Matcher addressMatch = findAddress.matcher(s.toString());
        boolean addressValid = addressMatch.find();
        addressReadyCallback.addressValid(addressValid);

        if (addressValid)
        {
            fullAddress = s.toString().trim();
        }

        setStatus(null);
        if (ensHandler != null && !TextUtils.isEmpty(getInputText()))
        {
            ensHandler.checkAddress();
        }
    }
}
