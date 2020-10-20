package com.alphawallet.app.ui;

import android.Manifest;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.AmountUpdateCallback;
import com.alphawallet.app.entity.EIP681Request;
import com.alphawallet.app.entity.NetworkInfo;

import com.alphawallet.app.entity.CustomViewSettings;

import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.ui.QRScanning.DisplayUtils;
import com.alphawallet.app.ui.widget.entity.AmountEntryItem;
import com.alphawallet.app.util.AWEnsResolver;
import com.alphawallet.app.util.KeyboardUtils;
import com.alphawallet.app.util.QRUtils;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.ImportWalletViewModel;
import com.alphawallet.app.viewmodel.MyAddressViewModel;
import com.alphawallet.app.viewmodel.MyAddressViewModelFactory;
import com.alphawallet.app.widget.CopyTextView;

import org.web3j.crypto.Keys;
import org.web3j.utils.Convert;

import java.math.BigDecimal;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class MyAddressActivity extends BaseActivity implements AmountUpdateCallback
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
    private String displayName;
    private Token token;
    private ImageView qrImageView;
    private TextView titleView;
    private TextView address;
    private LinearLayout inputAmount;
    private LinearLayout selectAddress;
    private TextView currentNetwork;
    private RelativeLayout selectNetworkLayout;
    private View networkIcon;
    private AmountEntryItem amountInput = null;
    private NetworkInfo networkInfo;
    private int currentMode = MODE_ADDRESS;
    private int overrideNetwork;
    private int screenWidth;
    private CopyTextView copyAddress;
    private CopyTextView copyWalletName;
    private ProgressBar ensFetchProgressBar;

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

    private void getPreviousMode()
    {
        Intent intent = getIntent();
        if (token != null && token.isNonFungible())
        {
            showContract();
        }
        else if (intent != null)
        {
            int mode = intent.getIntExtra(KEY_MODE, MODE_ADDRESS);
            if (mode == MODE_POS)
            {
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
        networkIcon = findViewById(R.id.network_icon);
        qrImageView.setBackgroundResource(R.color.white);
        ensFetchProgressBar = findViewById(R.id.ens_fetch_progress);

        if (viewModel == null) initViewModel();
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this, myAddressViewModelFactory)
                .get(MyAddressViewModel.class);
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
        if (CustomViewSettings.hideEIP681()) return super.onCreateOptionsMenu(menu);
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

    private void showPointOfSaleMode()
    {
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
        displayAddress = Keys.toChecksumAddress(wallet.address);
        networkInfo = viewModel.getEthereumNetworkRepository().getNetworkByChain(overrideNetwork);
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
        selectNetworkLayout.setVisibility(View.VISIBLE);

        if (networkInfo != null)
        {
            currentNetwork.setText(networkInfo.name);
            Utils.setChainColour(networkIcon, networkInfo.chainId);
        }
    }

    private void showAddress()
    {
        getInfo();
        setContentView(R.layout.activity_my_address);
        initViews();
        findViewById(R.id.toolbar_title).setVisibility(View.VISIBLE);

        copyWalletName = findViewById(R.id.copy_wallet_name);
        copyAddress = findViewById(R.id.copy_address);

        if (amountInput != null)
        {
            amountInput.onClear();
            amountInput = null;
        }

        displayAddress = Keys.toChecksumAddress(wallet.address);
        setTitle(getString(R.string.my_wallet_address));
        copyAddress.setText(displayAddress);
        currentMode = MODE_ADDRESS;
        selectNetworkLayout.setVisibility(View.GONE);
        if (getCurrentFocus() != null) {
            KeyboardUtils.hideKeyboard(getCurrentFocus());
        }
        copyAddress.setVisibility(View.VISIBLE);
        onWindowFocusChanged(true);
        updateAddressWithENS(wallet.ENSname); //JB: see if there's any cached value to display while we wait for ENS

        //When view changes, this function loads again. It will again try to fetch ENS
        if(TextUtils.isEmpty(displayName))
        {
            new AWEnsResolver(TokenRepository.getWeb3jService(EthereumNetworkRepository.MAINNET_ID), getApplicationContext())
                    .resolveEnsName(displayAddress)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(this::updateAddressWithENS, this::printTrace).isDisposed();
            if (ensFetchProgressBar != null) {
                ensFetchProgressBar.setVisibility(View.VISIBLE);
            }
        }
        else
        {
            updateAddressWithENS(displayName);
        }
    }

    private void printTrace(Throwable throwable) {
        if (ensFetchProgressBar != null) {
            ensFetchProgressBar.setVisibility(View.GONE);
        }
        updateAddressWithENS(wallet.ENSname); // JB: if there's any issue then fall back to cached name
    }

    private void showContract()
    {
        getInfo();
        setContentView(R.layout.activity_contract_address);
        initViews();
        findViewById(R.id.toolbar_title).setVisibility(View.VISIBLE);
        copyAddress = findViewById(R.id.copy_address);
        copyAddress.setVisibility(View.VISIBLE);

        currentMode = MODE_CONTRACT;
        displayAddress = Keys.toChecksumAddress(token.getAddress());
        setTitle(getString(R.string.contract_address));
        copyAddress.setText(displayAddress);
        onWindowFocusChanged(true);
    }

    private void updateAddressWithENS(String ensName)
    {
        if (ensFetchProgressBar != null) {
            ensFetchProgressBar.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(ensName))
        {
            displayName = ensName;
            copyWalletName.setVisibility(View.VISIBLE);
            copyWalletName.setText(ensName);
        }
        else
        {
            copyWalletName.setVisibility(View.GONE);
        }
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

                if (info != null)
                {
                    getInfo();
                    Intent intent = getIntent();
                    intent.putExtra(KEY_MODE, MODE_POS);
                    intent.putExtra(C.Key.WALLET, wallet);
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
        wallet = getIntent().getParcelableExtra(C.Key.WALLET);
        token = getIntent().getParcelableExtra(C.EXTRA_TOKEN_ID);
        int fallBackChainId = token != null ? token.tokenInfo.chainId : EthereumNetworkBase.MAINNET_ID;
        overrideNetwork = getIntent().getIntExtra(OVERRIDE_DEFAULT, fallBackChainId);

        if (wallet == null)
        {
            //have no address. Can only quit the activity
            finish();
        }
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
}
