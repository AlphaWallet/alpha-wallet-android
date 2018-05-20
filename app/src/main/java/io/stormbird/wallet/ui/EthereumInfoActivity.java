package io.stormbird.wallet.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;

import io.stormbird.wallet.R;

public class EthereumInfoActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ethereum_info);
        toolbar();
        setTitle("");
    }
}
