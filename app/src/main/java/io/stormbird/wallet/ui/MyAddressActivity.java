package io.stormbird.wallet.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.util.QRUtils;

import static io.stormbird.wallet.C.EXTRA_CONTRACT_ADDRESS;
import static io.stormbird.wallet.C.Key.WALLET;

public class MyAddressActivity extends BaseActivity implements View.OnClickListener {
    public static final String KEY_ADDRESS = "key_address";

    @Inject
    protected EthereumNetworkRepositoryType ethereumNetworkRepository;

    private Wallet wallet;
    private String displayAddress;

    private ImageView qrImageView;
    private TextView titleView;
    private Button copyButton;

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

        NetworkInfo networkInfo = ethereumNetworkRepository.getDefaultNetwork();
        ((TextView) findViewById(R.id.address)).setText(displayAddress);
        copyButton.setOnClickListener(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getInfo();
            qrImageView = findViewById(R.id.qr_image);
            qrImageView.setImageBitmap(QRUtils.createQRImage(this, displayAddress, qrImageView.getWidth()));
            qrImageView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        }
    }

    private void getInfo()
    {
        wallet = getIntent().getParcelableExtra(WALLET);
        displayAddress = getIntent().getStringExtra(EXTRA_CONTRACT_ADDRESS);

        if (displayAddress != null)
        {
            titleView.setText(R.string.contract_address);
            copyButton.setText(R.string.copy_addr_to_clipboard);
        }
        else
        {
            displayAddress = wallet.address;
            titleView.setText(R.string.my_wallet_address);
            copyButton.setText(R.string.copy_wallet_address);
        }
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
}
