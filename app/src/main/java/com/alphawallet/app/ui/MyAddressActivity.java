package com.alphawallet.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.AddressMode;
import com.alphawallet.app.entity.CustomViewSettings;
import com.alphawallet.app.entity.EIP681Request;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.ui.QRScanning.DisplayUtils;
import com.alphawallet.app.ui.widget.entity.AmountReadyCallback;
import com.alphawallet.app.util.ens.AWEnsResolver;
import com.alphawallet.app.util.KeyboardUtils;
import com.alphawallet.app.util.QRUtils;
import com.alphawallet.app.viewmodel.MyAddressViewModel;
import com.alphawallet.app.widget.CopyTextView;
import com.alphawallet.app.widget.InputAmount;
import com.alphawallet.ethereum.EthereumNetworkBase;

import org.web3j.crypto.Keys;

import java.math.BigDecimal;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

@AndroidEntryPoint
public class MyAddressActivity extends BaseActivity implements AmountReadyCallback
{
    public static final String KEY_ADDRESS = "key_address";
    public static final String KEY_MODE = "mode";
    public static final String OVERRIDE_DEFAULT = "override";

    private MyAddressViewModel viewModel;

    private Wallet wallet;
    private String displayAddress;
    private String displayName;
    private Token token;
    private ImageView qrImageView;
    private LinearLayout layoutInputAmount;
    private NetworkInfo networkInfo;
    private AddressMode currentMode = AddressMode.MODE_ADDRESS;
    private long overrideNetwork;
    private int screenWidth;
    private CopyTextView copyAddress;
    private CopyTextView copyWalletName;
    private ProgressBar ensFetchProgressBar;

    private InputAmount amountInput;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        screenWidth = (int) ((float)DisplayUtils.getScreenResolution(this).x * 0.8f);
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
            AddressMode mode = AddressMode.values()[intent.getIntExtra(KEY_MODE, AddressMode.MODE_ADDRESS.ordinal())];
            if (mode == AddressMode.MODE_POS)
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
        layoutInputAmount = findViewById(R.id.layout_define_request);
        qrImageView = findViewById(R.id.qr_image);
        qrImageView.setBackgroundResource(R.color.surface);
        ensFetchProgressBar = findViewById(R.id.ens_fetch_progress);

        if (viewModel == null) initViewModel();
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this)
                .get(MyAddressViewModel.class);
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

        switch (currentMode)
        {
            case MODE_ADDRESS:
                menu.findItem(R.id.action_my_address)
                        .setVisible(false);
                menu.findItem(R.id.action_networks)
                        .setVisible(false);
                break;
            case MODE_POS:
                menu.findItem(R.id.action_my_address)
                        .setVisible(false);
                menu.findItem(R.id.action_show_contract)
                        .setVisible(false);
                break;
            case MODE_CONTRACT:
                menu.findItem(R.id.action_show_contract)
                        .setVisible(false);
                menu.findItem(R.id.action_networks)
                        .setVisible(false);
                break;
        }

        //Only show contract if we've come from a token and the token is not base chain
        if (token == null || token.isEthereum())
        {
            menu.findItem(R.id.action_show_contract)
                    .setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.action_show_contract)
        {
            showContract();
        }
        else if (item.getItemId() == R.id.action_my_address)
        {
            showAddress();
        }
        else if (item.getItemId() == R.id.action_networks) {
            selectNetwork();
        }

        return super.onOptionsItemSelected(item);
    }

    private void showPointOfSaleMode()
    {
        setContentView(R.layout.activity_eip681);
        initViews();
        findViewById(R.id.toolbar_title).setVisibility(View.GONE);
        setTitle("");
        displayAddress = Keys.toChecksumAddress(wallet.address);
        networkInfo = viewModel.getEthereumNetworkRepository().getNetworkByChain(overrideNetwork);
        currentMode = AddressMode.MODE_POS;
        layoutInputAmount.setVisibility(View.VISIBLE);

        amountInput = findViewById(R.id.input_amount);
        setupPOSMode(networkInfo);
    }

    private void setupPOSMode(NetworkInfo info)
    {
        if (token == null) token = viewModel.getTokenService().getToken(info.chainId, wallet.address);
        amountInput.setupToken(token, viewModel.getAssetDefinitionService(), viewModel.getTokenService(), this);
        amountInput.setAmount("");
        updateCryptoAmount(BigDecimal.ZERO);
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
        currentMode = AddressMode.MODE_ADDRESS;
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
                    .reverseResolveEns(displayAddress)
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

        currentMode = AddressMode.MODE_CONTRACT;
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
                    long networkId = result.getData().getLongExtra(C.EXTRA_CHAIN_ID, -1);
                    NetworkInfo info = viewModel.getNetworkByChain(networkId);
                    if (info != null)
                    {
                        networkInfo = info;
                        getInfo();
                        token = null;
                        overrideNetwork = networkId;
                        setupPOSMode(info);
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
                amountInput.setupToken(token, viewModel.getAssetDefinitionService(), viewModel.getTokenService(), this);
            }
        }
    }

    private void getInfo()
    {
        if (viewModel == null) initViewModel();
        wallet = getIntent().getParcelableExtra(C.Key.WALLET);
        long chainId = getIntent().getLongExtra(C.EXTRA_CHAIN_ID, EthereumNetworkBase.MAINNET_ID);
        token = viewModel.getTokenService().getToken(chainId, getIntent().getStringExtra(C.EXTRA_ADDRESS));
        long fallBackChainId = token != null ? token.tokenInfo.chainId : MAINNET_ID;
        overrideNetwork = getIntent().getLongExtra(OVERRIDE_DEFAULT, fallBackChainId);

        if (wallet == null)
        {
            //have no address. Can only quit the activity
            finish();
        }
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
            Timber.d("AMT: %s", weiAmount.toString());
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
