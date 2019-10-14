package com.alphawallet.app.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alphawallet.app.entity.VisibilityFilter;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.ui.widget.entity.AmountEntryItem;
import com.alphawallet.app.util.KeyboardUtils;
import com.alphawallet.app.util.QRUtils;
import com.alphawallet.app.util.Utils;

import org.web3j.utils.Convert;

import java.math.BigDecimal;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.AmountUpdateCallback;
import com.alphawallet.app.entity.EIP681Request;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.viewmodel.MyAddressViewModel;
import com.alphawallet.app.viewmodel.MyAddressViewModelFactory;

import static com.alphawallet.app.C.Key.WALLET;

public class MyAddressActivity extends BaseActivity implements View.OnClickListener, AmountUpdateCallback
{
    public static final String KEY_ADDRESS = "key_address";
    public static final String KEY_MODE = "mode";
    public static final String OVERRIDE_DEFAULT = "override";
    public static final int MODE_ADDRESS = 100;
    public static final int MODE_POS = 101;

    @Inject
    MyAddressViewModelFactory myAddressViewModelFactory;
    private MyAddressViewModel viewModel;

    private Wallet wallet;
    private String displayAddress;
    private Token token;
    private ImageView qrImageView;
    private TextView titleView;
    private TextView address;
    private Button copyButton;
    private LinearLayout inputAmount;
    private LinearLayout selectAddress;
    private TextView currentNetwork;
    private RelativeLayout selectNetworkLayout;
    private View networkIcon;
    private LinearLayout layoutHolder;
    private AmountEntryItem amountInput = null;
    private NetworkInfo networkInfo;
    private int currentMode = MODE_ADDRESS;
    private int overrideNetwork;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        overrideNetwork = 0;
        setContentView(R.layout.activity_my_address);
        initViews();
        initViewModel();
        getInfo();
        getPreviousMode();
        setupContractData();

        viewModel.prepare();
    }

    private void getPreviousMode() {
        Intent intent = getIntent();
        if (intent != null) {
            int mode = intent.getIntExtra(KEY_MODE, MODE_ADDRESS);
            if (mode == MODE_POS) {
                overrideNetwork = intent.getIntExtra(OVERRIDE_DEFAULT, 1);
                networkInfo = viewModel.getEthereumNetworkRepository().getNetworkByChain(overrideNetwork);
                showPointOfSaleMode();
            }
        }
    }

    private void initViews() {
        toolbar();
        setTitle(getString(R.string.empty));
        titleView = findViewById(R.id.title_my_address);
        copyButton = findViewById(R.id.copy_action);
        currentNetwork = findViewById(R.id.current_network);
        selectNetworkLayout = findViewById(R.id.select_network_layout);
        selectNetworkLayout.setOnClickListener(v -> selectNetwork());
        inputAmount = findViewById(R.id.layout_define_request);
        selectAddress = findViewById(R.id.layout_select_address);
        address =  findViewById(R.id.address);
        copyButton.setOnClickListener(this);
        qrImageView = findViewById(R.id.qr_image);
        inputAmount = findViewById(R.id.layout_define_request);
        selectAddress = findViewById(R.id.layout_select_address);
        layoutHolder = findViewById(R.id.layout_holder);
        networkIcon = findViewById(R.id.network_icon);
    }

    private void initViewModel() {
        viewModel = ViewModelProviders.of(this, myAddressViewModelFactory).get(MyAddressViewModel.class);
        viewModel.defaultNetwork().observe(this, this::onDefaultNetwork);
    }

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        if (token != null && overrideNetwork == 0)
        {
            this.networkInfo = viewModel.getEthereumNetworkRepository().getNetworkByChain(token.tokenInfo.chainId);
        }
        else
        {
            this.networkInfo = networkInfo;
        }

        currentNetwork.setText(this.networkInfo.name);
        Utils.setChainColour(networkIcon, this.networkInfo.chainId);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        layoutHolder.setOnClickListener(view -> {
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
            case R.id.action_receive_payment: {
                switch (currentMode)
                {
                    case MODE_ADDRESS:
                        showPointOfSaleMode();
                        break;
                    case MODE_POS:
                        showAddress();
                        break;
                }
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void showPointOfSaleMode() {
        //Generate QR link for receive payment.
        //Initially just generate simple payment.
        //need ticker so user can see how much $USD the value is
        currentMode = MODE_POS;
        selectAddress.setVisibility(View.GONE);
        inputAmount.setVisibility(View.VISIBLE);
        amountInput = new AmountEntryItem(
                this,
                viewModel.getTokenRepository(),
                networkInfo.symbol,
                true,
                networkInfo.chainId,
                EthereumNetworkRepository.hasRealValue(networkInfo.chainId));
        amountInput.getValue();
        selectNetworkLayout.setVisibility(View.VISIBLE);
    }

    private void showAddress() {
        currentMode = MODE_ADDRESS;
        selectNetworkLayout.setVisibility(View.GONE);
        if (getCurrentFocus() != null) {
            KeyboardUtils.hideKeyboard(getCurrentFocus());
        }
        selectAddress.setVisibility(View.VISIBLE);
        inputAmount.setVisibility(View.GONE);
        if (amountInput != null) {
            amountInput.onClear();
            amountInput = null;
        }
        onWindowFocusChanged(true);
    }

    private void selectNetwork() {
        Intent intent = new Intent(MyAddressActivity.this, SelectNetworkActivity.class);
        intent.putExtra(C.EXTRA_SINGLE_ITEM, true);
        intent.putExtra(C.EXTRA_CHAIN_ID, String.valueOf(networkInfo.chainId));
        startActivityForResult(intent, C.REQUEST_SELECT_NETWORK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == C.REQUEST_SELECT_NETWORK) {
            if (resultCode == RESULT_OK) {
                int networkId = data.getIntExtra(C.EXTRA_CHAIN_ID, -1);
                NetworkInfo info = viewModel.setNetwork(networkId);

                // restart activity
                if (info != null && networkInfo.chainId != info.chainId) {
                    Intent intent = getIntent();
                    intent.putExtra(KEY_MODE, MODE_POS);
                    intent.putExtra(OVERRIDE_DEFAULT, info.chainId);
                    finish();
                    startActivity(intent);
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
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
                qrImageView.setImageBitmap(QRUtils.createQRImage(this, displayAddress, qrImageView.getWidth()));
                //qrImageView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in)); //<-- check if this is causing the load delay (it was)
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
        address.setText(displayAddress);
    }

    private void setupContractData()
    {
        if (token != null && !token.isEthereum() && VisibilityFilter.showContractAddress(token))
        {
            findViewById(R.id.text_contract_address).setVisibility(View.VISIBLE);
            findViewById(R.id.layout_contract).setVisibility(View.VISIBLE);
            TextView contractAddress = findViewById(R.id.contract_address);
            Button contractCopy = findViewById(R.id.copy_contract_action);
            contractAddress.setText(token.getAddress());
            contractCopy.setOnClickListener(v -> copyContract());
        }
    }

    private void copyContract()
    {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(KEY_ADDRESS, token.getAddress());
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
        }
        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
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
        if (newAmount != null && newAmount.length() > 0 && Character.isDigit(newAmount.charAt(0)))
        {
            //generate payment request link
            //EIP681 format
            BigDecimal weiAmount = Convert.toWei(newAmount.replace(",", "."), Convert.Unit.ETHER);
            System.out.println("AMT: " + weiAmount.toString());
            EIP681Request request = new EIP681Request(displayAddress, networkInfo.chainId, weiAmount);
            String eip681String = request.generateRequest();
            qrImageView.setImageBitmap(QRUtils.createQRImage(this, eip681String, qrImageView.getWidth()));
        }
    }
}
