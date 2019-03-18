package io.stormbird.wallet.ui;

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
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.web3j.utils.Convert;

import java.math.BigDecimal;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.stormbird.wallet.C;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.AmountUpdateCallback;
import io.stormbird.wallet.entity.EIP681Request;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.repository.EthereumNetworkRepository;
import io.stormbird.wallet.ui.widget.entity.AmountEntryItem;
import io.stormbird.wallet.util.KeyboardUtils;
import io.stormbird.wallet.util.QRUtils;
import io.stormbird.wallet.util.Utils;
import io.stormbird.wallet.viewmodel.MyAddressViewModel;
import io.stormbird.wallet.viewmodel.MyAddressViewModelFactory;
import io.stormbird.wallet.widget.SelectNetworkDialog;

import static io.stormbird.wallet.C.Key.WALLET;
import static io.stormbird.wallet.C.RESET_WALLET;

public class MyAddressActivity extends BaseActivity implements View.OnClickListener, AmountUpdateCallback
{
    public static final String KEY_ADDRESS = "key_address";
    public static final String KEY_MODE = "mode";
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_address);
        initViews();
        initViewModel();
        getInfo();
        getPreviousMode();
    }

    private void getPreviousMode() {
        Intent intent = getIntent();
        if (intent != null) {
            int mode = intent.getIntExtra(KEY_MODE, MODE_ADDRESS);
            if (mode == MODE_POS) {
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
        viewModel.prepare();
    }

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        this.networkInfo = networkInfo;
        currentNetwork.setText(networkInfo.name);
        Utils.setChainColour(networkIcon, networkInfo.chainId);
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
                if (selectAddress.getVisibility() == View.VISIBLE)
                {
                    showPointOfSaleMode();
                    currentMode = MODE_POS;
                }
                else
                {
                    showAddress();
                    currentMode = MODE_ADDRESS;
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
        selectAddress.setVisibility(View.GONE);
        inputAmount.setVisibility(View.VISIBLE);
        amountInput = new AmountEntryItem(
                this,
                viewModel.getTokenRepository(),
                viewModel.getEthereumNetworkRepository().getDefaultNetwork().symbol,
                true,
                viewModel.getEthereumNetworkRepository().getDefaultNetwork().chainId);
        amountInput.getValue();
        selectNetworkLayout.setVisibility(View.VISIBLE);
    }

    private void showAddress() {
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
        SelectNetworkDialog dialog = new SelectNetworkDialog(this, viewModel.getNetworkList(), networkInfo.name);
        dialog.setOnClickListener(v1 -> {
            viewModel.setNetwork(dialog.getSelectedItem());

            // restart activity
            if (!networkInfo.name.equals(dialog.getSelectedItem())) {
                Intent intent = getIntent();
                intent.putExtra(KEY_MODE, currentMode);
                finish();
                startActivity(intent);
            }
            sendBroadcast(new Intent(RESET_WALLET));
            dialog.dismiss();
        });
        dialog.show();
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
        address.setText(displayAddress);
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
        EIP681Request request = new EIP681Request(displayAddress, viewModel.getEthereumNetworkRepository().getDefaultNetwork().chainId, weiAmount);
        String eip681String = request.generateRequest();
        qrImageView.setImageBitmap(QRUtils.createQRImage(this, eip681String, qrImageView.getWidth()));
    }
}
