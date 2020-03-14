package com.alphawallet.app.ui;

import android.Manifest;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.AmountUpdateCallback;
import com.alphawallet.app.entity.EIP681Request;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.VisibilityFilter;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.ui.QRScanning.DisplayUtils;
import com.alphawallet.app.ui.widget.entity.AmountEntryItem;
import com.alphawallet.app.util.KeyboardUtils;
import com.alphawallet.app.util.QRUtils;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.MyAddressViewModel;
import com.alphawallet.app.viewmodel.MyAddressViewModelFactory;
import com.alphawallet.app.widget.FunctionButtonBar;

import org.web3j.crypto.Keys;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.alphawallet.app.C.Key.WALLET;

public class MyAddressActivity extends BaseActivity implements View.OnClickListener, AmountUpdateCallback, StandardFunctionInterface
{
    public static final String KEY_ADDRESS = "key_address";
    public static final String KEY_MODE = "mode";
    public static final String OVERRIDE_DEFAULT = "override";
    public static final int MODE_ADDRESS = 100;
    public static final int MODE_POS = 101;
    public static final int MODE_CONTRACT = 102;

    @Inject
    MyAddressViewModelFactory myAddressViewModelFactory;
    private MyAddressViewModel viewModel;

    private Wallet wallet;
    private String displayAddress;
    private Token token;
    private ImageView qrImageView;
    private TextView titleView;
    private TextView address;
    private LinearLayout inputAmount;
    private LinearLayout selectAddress;
    private TextView currentNetwork;
    private RelativeLayout selectNetworkLayout;
    private View networkIcon;
    private RelativeLayout layoutHolder;
    private AmountEntryItem amountInput = null;
    private NetworkInfo networkInfo;
    private int currentMode = MODE_ADDRESS;
    private int overrideNetwork;
    private FunctionButtonBar functionBar;
    private int screenWidth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        screenWidth = (int) ((float)DisplayUtils.getScreenResolution(this).x * 0.8f);
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        initViewModel();
        overrideNetwork = 0;
        getInfo();
        viewModel.prepare();
        getPreviousMode();
    }

    private void getPreviousMode() {
        Intent intent = getIntent();
        if (token != null && token.isNonFungible())
        {
            showContract();
        }
        else if (intent != null) {
            int mode = intent.getIntExtra(KEY_MODE, MODE_ADDRESS);
            if (mode == MODE_POS) {
                overrideNetwork = intent.getIntExtra(OVERRIDE_DEFAULT, 1);
                networkInfo = viewModel.getEthereumNetworkRepository().getNetworkByChain(overrideNetwork);
                showPointOfSaleMode();
            }
            else
            {
                showAddress();
            }
        }
    }

    private void initViews() {
        toolbar();
        titleView = findViewById(R.id.title_my_address);
        currentNetwork = findViewById(R.id.current_network);
        selectNetworkLayout = findViewById(R.id.select_network_layout);
        selectNetworkLayout.setOnClickListener(v -> selectNetwork());
        inputAmount = findViewById(R.id.layout_define_request);
        selectAddress = findViewById(R.id.layout_select_address);
        address =  findViewById(R.id.address);
        qrImageView = findViewById(R.id.qr_image);
        selectAddress = findViewById(R.id.layout_select_address);
        layoutHolder = findViewById(R.id.layout_holder);
        networkIcon = findViewById(R.id.network_icon);
        functionBar = findViewById(R.id.layoutButtons);
        qrImageView.setBackgroundResource(R.color.white);

        if (viewModel == null) initViewModel();
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
        if (VisibilityFilter.hideEIP681()) return super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_receive, menu);
        //only pay when token null, eth or erc20
        if (token != null && token.isNonFungible() || currentMode == MODE_POS)// || EthereumNetworkRepository.isPriorityToken(token)) //Currently only allow request for native chain currency, can get here via routes where token is not set.
        {
            menu.findItem(R.id.action_receive_payment)
                    .setVisible(false);
        }
        //if dev mode, and token is not ethereum show contract
        boolean devMode = (checkWritePermission() && EthereumNetworkRepository.extraChains() == null);
        if (!devMode || token == null || token.isEthereum() || currentMode == MODE_CONTRACT)
        {        //remove contract address
            menu.findItem(R.id.action_show_contract)
                    .setVisible(false);
        }

        if (currentMode == MODE_ADDRESS)
        {
            menu.findItem(R.id.action_my_address)
                    .setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_receive_payment:
                showPointOfSaleMode();
                break;
            case R.id.action_show_contract:
                showContract();
                break;
            case R.id.action_my_address:
                showAddress();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showPointOfSaleMode() {
        setContentView(R.layout.activity_eip681);
        initViews();
        getInfo();
        //Generate QR link for receive payment.
        //Initially just generate simple payment.
        //need ticker so user can see how much $USD the value is
        //get token ticker
        findViewById(R.id.toolbar_title).setVisibility(View.GONE);
        setTitle("");
        titleView.setVisibility(View.VISIBLE);
        if (token == null) token = viewModel.getEthereumNetworkRepository().getBlankOverrideToken(networkInfo);
        currentMode = MODE_POS;
        address.setVisibility(View.GONE);
        selectAddress.setVisibility(View.GONE);
        inputAmount.setVisibility(View.VISIBLE);
        amountInput = new AmountEntryItem(
                this,
                viewModel.getTokenRepository(),
                token);
        amountInput.getValue();
        functionBar.setVisibility(View.GONE);
        selectNetworkLayout.setVisibility(View.VISIBLE);
    }

    private void showAddress() {
        getInfo();
        setContentView(R.layout.activity_my_address);
        initViews();
        findViewById(R.id.toolbar_title).setVisibility(View.VISIBLE);

        if (amountInput != null)
        {
            amountInput.onClear();
            amountInput = null;
        }

        displayAddress = Keys.toChecksumAddress(wallet.address);
        setTitle(getString(R.string.my_wallet_address));
        address.setText(displayAddress);
        currentMode = MODE_ADDRESS;
        selectNetworkLayout.setVisibility(View.GONE);
        if (getCurrentFocus() != null) {
            KeyboardUtils.hideKeyboard(getCurrentFocus());
        }
        address.setVisibility(View.VISIBLE);
        onWindowFocusChanged(true);
        functionBar.setVisibility(View.VISIBLE);
        List<Integer> functions = new ArrayList<>(Collections.singletonList(R.string.copy_wallet_address));
        functionBar.setupFunctions(this, functions);
    }

    private void showContract()
    {
        getInfo();
        setContentView(R.layout.activity_contract_address);
        initViews();
        findViewById(R.id.toolbar_title).setVisibility(View.VISIBLE);

        currentMode = MODE_CONTRACT;
        displayAddress = token.getAddress();
        setTitle(getString(R.string.contract_address));
        address.setText(token.getAddress());
        onWindowFocusChanged(true);
        functionBar.setVisibility(View.VISIBLE);
        List<Integer> functions = new ArrayList<>(Collections.singletonList(R.string.copy_contract_address));
        functionBar.setupFunctions(this, functions);
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

                // restart activity if required
                if (info != null && (networkInfo == null || networkInfo.chainId != info.chainId))
                {
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
                qrImageView.setImageBitmap(QRUtils.createQRImage(this, displayAddress, screenWidth));
                qrImageView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in)); //<-- check if this is causing the load delay (it was)
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

    private boolean checkWritePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void amountChanged(String newAmount)
    {
        if (token != null && newAmount != null && newAmount.length() > 0 && Character.isDigit(newAmount.charAt(0)))
        {
            //generate payment request link
            //EIP681 format
            BigDecimal weiAmount = Convert.toWei(newAmount.replace(",", "."), Convert.Unit.ETHER);
            System.out.println("AMT: " + weiAmount.toString());
            EIP681Request request;
            String eip681String;
            if (token.isEthereum())
            {
                request = new EIP681Request(displayAddress, networkInfo.chainId, weiAmount);
                eip681String = request.generateRequest();
            }
            else if (token.isERC20())
            {
                weiAmount = token.getCorrectedAmount(newAmount);
                request = new EIP681Request(displayAddress, token.getAddress(), networkInfo.chainId, weiAmount);
                eip681String = request.generateERC20Request();
            }
            else
            {
                return;
            }
            qrImageView.setImageBitmap(QRUtils.createQRImage(this, eip681String, screenWidth));
        }
    }

    @Override
    public void handleClick(int view)
    {
        switch (view)
        {
            case R.string.copy_wallet_address:
            case R.string.copy_contract_address:
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(KEY_ADDRESS, displayAddress);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                }
                Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
    }
}
