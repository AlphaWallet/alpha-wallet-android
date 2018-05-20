package io.stormbird.wallet.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.animation.AnimationUtils;
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

import static io.stormbird.wallet.C.Key.WALLET;

public class MyAddressActivity extends BaseActivity implements View.OnClickListener {
    public static final String KEY_ADDRESS = "key_address";

    @Inject
    protected EthereumNetworkRepositoryType ethereumNetworkRepository;

    private Wallet wallet;

    private ImageView qrImageView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_my_address);

        toolbar();

        setTitle(getString(R.string.empty));

        wallet = getIntent().getParcelableExtra(WALLET);
        NetworkInfo networkInfo = ethereumNetworkRepository.getDefaultNetwork();
        ((TextView) findViewById(R.id.address)).setText(wallet.address);
        findViewById(R.id.copy_action).setOnClickListener(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            wallet = getIntent().getParcelableExtra(WALLET);
            qrImageView = findViewById(R.id.qr_image);
            qrImageView.setImageBitmap(QRUtils.createQRImage(this, wallet.address, qrImageView.getWidth()));
            qrImageView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        }
    }

    @Override
    public void onClick(View v) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(KEY_ADDRESS, wallet.address);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
        }
        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
    }
}
