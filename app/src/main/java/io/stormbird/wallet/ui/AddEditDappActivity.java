package io.stormbird.wallet.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.net.URISyntaxException;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.DApp;
import io.stormbird.wallet.util.DappBrowserUtils;
import io.stormbird.wallet.util.Utils;

public class AddEditDappActivity extends BaseActivity {
    private TextView title;
    private EditText name;
    private EditText url;
    private Button button;
    private ImageView icon;

    private int mode;
    private DApp dapp;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_dapp);

        title = findViewById(R.id.title);
        name = findViewById(R.id.dapp_title);
        url = findViewById(R.id.dapp_url);
        button = findViewById(R.id.btn_confirm);
        icon = findViewById(R.id.icon);

        Intent intent = getIntent();
        if (intent != null) {
            mode = intent.getExtras().getInt("mode");
            dapp = (DApp) intent.getExtras().get("dapp");
        } else {
            finish();
        }

        String visibleUrl = null;
        try {
            visibleUrl = Utils.getDomainName(dapp.getUrl());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        String favicon;
        if (visibleUrl != null) {
            favicon = DappBrowserUtils.getIconUrl(visibleUrl);
            Glide.with(this)
                    .load(favicon)
                    .apply(new RequestOptions().circleCrop())
                    .apply(new RequestOptions().placeholder(R.drawable.ic_logo))
                    .into(icon);
        }

        switch (mode) {
            case 0: { // Add
                title.setText(R.string.add_to_my_dapps);
                button.setText(R.string.action_add);
                url.setText(dapp.getUrl());
                button.setOnClickListener(v -> add());
                break;
            }
            case 1: { // Edit
                title.setText(R.string.edit_dapp);
                button.setText(R.string.action_save);
                url.setText(dapp.getUrl());
                name.setText(dapp.getName());
                button.setOnClickListener(v -> save());
                break;
            }
            default: {
                break;
            }
        }
    }

    private void save() {

    }

    private void add() {

    }
}
