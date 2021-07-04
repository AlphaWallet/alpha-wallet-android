package com.alphawallet.app.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
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

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.CustomViewSettings;
import com.alphawallet.app.entity.EIP681Request;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.ui.QRScanning.DisplayUtils;
import com.alphawallet.app.ui.widget.entity.AmountReadyCallback;
import com.alphawallet.app.util.AWEnsResolver;
import com.alphawallet.app.util.KeyboardUtils;
import com.alphawallet.app.util.QRUtils;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.MyAddressViewModel;
import com.alphawallet.app.viewmodel.MyAddressViewModelFactory;
import com.alphawallet.app.widget.CopyTextView;
import com.alphawallet.app.widget.InputAmount;

import org.web3j.crypto.Keys;

import java.math.BigDecimal;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

public class MyAddressActivity extends BaseActivity implements AmountReadyCallback
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
    private LinearLayout layoutInputAmount;
    private LinearLayout selectAddress;
    private TextView currentNetwork;
    private RelativeLayout selectNetworkLayout;
    private View networkIcon;
    private NetworkInfo networkInfo;
    private int currentMode = MODE_ADDRESS;
    private int overrideNetwork;
    private int screenWidth;
    private CopyTextView copyAddress;
    private CopyTextView copyWalletName;
    private ProgressBar ensFetchProgressBar;

    private InputAmount amountInput;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        screenWidth = (int) ((float)DisplayUtils.getScreenResolution(this).x * 0.8f);
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        initViewModel();
        overrideNetwork = 0;
        getInfo();
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
        if (selectNetworkLayout != null) selectNetworkLayout.setOnClickListener(v -> selectNetwork());
        layoutInputAmount = findViewById(R.id.layout_define_request);
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
    }

    private void setNetworkUi(NetworkInfo networkInfo)
    {
        currentNetwork.setText(networkInfo.name);
        Utils.setChainColour(networkIcon, networkInfo.chainId);
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
        if (amountInput != null) amountInput.onDestroy();
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
        if (item.getItemId() == R.id.action_receive_payment)
        {
            showPointOfSaleMode();
        }
        else if (item.getItemId() == R.id.action_show_contract)
        {
            showContract();
        }
        else if (item.getItemId() == R.id.action_my_address)
        {
            showAddress();
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
        if (token == null) token = viewModel.getTokenService().getToken(networkInfo.chainId, wallet.address);
        currentMode = MODE_POS;
        address.setVisibility(View.GONE);
        selectAddress.setVisibility(View.GONE);
        layoutInputAmount.setVisibility(View.VISIBLE);
        amountInput = findViewById(R.id.input_amount);
        amountInput.setupToken(token, null, viewModel.getTokenService(), this);
        updateCryptoAmount(BigDecimal.ZERO);
        setNetworkUi(networkInfo);
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
            amountInput.onDestroy();
            amountInput = null;
        }

        displayAddress = Keys.toChecksumAddress(wallet.address);
        setTitle(getString(R.string.my_wallet_address));
        copyAddress.setText(displayAddress);
        currentMode = MODE_ADDRESS;
        if (getCurrentFocus() != null) {
            KeyboardUtils.hideKeyboard(getCurrentFocus());
        }
        copyAddress.setVisibility(View.VISIBLE);
        onWindowFocusChanged(true);
        updateAddressWithENS(wallet.ENSname); //JB: see if there's any cached value to display while we wait for ENS

        //When view changes, this function loads again. It will again try to fetch ENS
        if(TextUtils.isEmpty(displayName))
        {
            new AWEnsResolver(TokenRepository.getWeb3jService(MAINNET_ID), getApplicationContext())
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

    ActivityResultLauncher<Intent> getNetwork = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK)
                {
                    int networkId = result.getData().getIntExtra(C.EXTRA_CHAIN_ID, -1);
                    NetworkInfo info = viewModel.getNetworkByChain(networkId);
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
    });

    private void selectNetwork() {
        Intent intent = new Intent(MyAddressActivity.this, SelectNetworkActivity.class);
        intent.putExtra(C.EXTRA_LOCAL_NETWORK_SELECT_FLAG, true);
        intent.putExtra(C.EXTRA_CHAIN_ID, networkInfo.chainId);
        getNetwork.launch(intent);
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
                amountInput.setupToken(token, null, viewModel.getTokenService(), this);
            }
        }
    }

    private void getInfo()
    {
        wallet = getIntent().getParcelableExtra(C.Key.WALLET);
        token = getIntent().getParcelableExtra(C.EXTRA_TOKEN_ID);
        int fallBackChainId = token != null ? token.tokenInfo.chainId : MAINNET_ID;
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
    public void amountReady(BigDecimal value, BigDecimal gasFee)
    {
        // unimplemented
    }

    @Override
    public void updateCryptoAmount(BigDecimal weiAmount)
    {
        if (token != null)
        {
            //generate payment request link
            //EIP681 format
            if (BuildConfig.DEBUG) System.out.println("AMT: " + weiAmount.toString());
            EIP681Request request;
            String eip681String;
            if (token.isEthereum())
            {
                request = new EIP681Request(displayAddress, networkInfo.chainId, weiAmount);
                eip681String = request.generateRequest();
            }
            else if (token.isERC20())
            {
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
