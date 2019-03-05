package io.stormbird.wallet.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.stormbird.wallet.C;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.*;
import io.stormbird.wallet.interact.FetchTokensInteract;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.repository.TokenRepositoryType;
import io.stormbird.wallet.ui.widget.entity.AmountEntryItem;
import io.stormbird.wallet.util.KeyboardUtils;
import io.stormbird.wallet.util.QRUtils;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;

import static io.stormbird.wallet.C.EXTRA_CONTRACT_ADDRESS;
import static io.stormbird.wallet.C.Key.WALLET;

public class MyAddressActivity extends BaseActivity implements View.OnClickListener, AmountUpdateCallback
{
    public static final String KEY_ADDRESS = "key_address";

    @Inject
    protected EthereumNetworkRepositoryType ethereumNetworkRepository;
    @Inject
    protected TokenRepositoryType tokenRepository;

    private Wallet wallet;
    private String displayAddress;
    private Token token;

    private ImageView qrImageView;
    private TextView titleView;
    private Button copyButton;
    private LinearLayout inputAmount;
    private LinearLayout selectAddress;

    private AmountEntryItem amountInput = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_my_address);

        titleView = findViewById(R.id.title_my_address);
        copyButton = findViewById(R.id.copy_action);

        toolbar();

        setTitle(getString(R.string.empty));

        getInfo();

        inputAmount = findViewById(R.id.layout_define_request);
        selectAddress = findViewById(R.id.layout_select_address);

        ((TextView) findViewById(R.id.address)).setText(displayAddress);
        copyButton.setOnClickListener(this);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        inputAmount = findViewById(R.id.layout_define_request);
        selectAddress = findViewById(R.id.layout_select_address);

        findViewById(R.id.layout_holder).setOnClickListener(view -> {
            if (getCurrentFocus() != null)
            {
                KeyboardUtils.hideKeyboard(getCurrentFocus());
            }
        });
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (amountInput != null) amountInput.onClear();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        if (token == null || token.isEthereum()) //Currently only allow request for native chain currency, can get here via routes where token is not set.
        {
            getMenuInflater().inflate(R.menu.menu_receive, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_receive_payment:
                if (selectAddress.getVisibility() == View.VISIBLE)
                {
                    //Generate QR link for receive payment.
                    //Initially just generate simple payment.
                    //need ticker so user can see how much $USD the value is
                    selectAddress.setVisibility(View.GONE);
                    inputAmount.setVisibility(View.VISIBLE);
                    amountInput = new AmountEntryItem(this, tokenRepository,
                                                      ethereumNetworkRepository.getDefaultNetwork().symbol, true, ethereumNetworkRepository.getDefaultNetwork().chainId);
                    amountInput.getValue();
                }
                else
                {
                    KeyboardUtils.hideKeyboard(getCurrentFocus());
                    selectAddress.setVisibility(View.VISIBLE);
                    inputAmount.setVisibility(View.GONE);
                    amountInput.onClear();
                    amountInput = null;
                    onWindowFocusChanged(true);
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus)
        {
            if (amountInput == null)
            {
                getInfo();
                qrImageView = findViewById(R.id.qr_image);
                qrImageView.setImageBitmap(QRUtils.createQRImage(this, displayAddress, qrImageView.getWidth()));
                qrImageView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
            }
            else
            {
                amountInput.getValue();
            }
        }
    }

    private void getInfo()
    {
        wallet = getIntent().getParcelableExtra(WALLET);
        token = getIntent().getParcelableExtra(C.EXTRA_TOKEN_ID);

        displayAddress = wallet.address;
        titleView.setText(R.string.my_wallet_address);
        copyButton.setText(R.string.copy_wallet_address);
    }

    @Override
    public void onClick(View v) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(KEY_ADDRESS, displayAddress);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
        }
        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void amountChanged(String newAmount)
    {
        //generate payment request link
        //EIP681 format
        BigDecimal weiAmount = Convert.toWei(newAmount, Convert.Unit.ETHER);
        EIP681Request request = new EIP681Request(displayAddress, ethereumNetworkRepository.getDefaultNetwork().chainId, weiAmount);
        String eip681String = request.generateRequest();
        qrImageView.setImageBitmap(QRUtils.createQRImage(this, eip681String, qrImageView.getWidth()));
    }
}
