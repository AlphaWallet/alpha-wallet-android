package com.alphawallet.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.Nullable;

import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.alphawallet.app.widget.InputView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.alphawallet.app.util.DappBrowserUtils;
import com.alphawallet.app.util.Utils;

import java.util.List;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.DApp;
import timber.log.Timber;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AddEditDappActivity extends BaseActivity {
    public static final String KEY_MODE = "mode";
    public static final String KEY_DAPP = "dapp";
    public static final int MODE_ADD = 0;
    public static final int MODE_EDIT = 1;

    private TextView title;
    private InputView name;
    private InputView url;
    private Button button;
    private ImageView icon;

    private int mode;
    private DApp dapp;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_dapp);
        toolbar();
        setTitle("");

        title = findViewById(R.id.title);
        name = findViewById(R.id.dapp_title);
        url = findViewById(R.id.dapp_url);
        button = findViewById(R.id.btn_confirm);
        icon = findViewById(R.id.icon);

        Intent intent = getIntent();
        if (intent != null) {
            mode = intent.getExtras().getInt(KEY_MODE);
            dapp = (DApp) intent.getExtras().get(KEY_DAPP);
        } else {
            finish();
        }

        String visibleUrl = Utils.getDomainName(dapp.getUrl());

        String favicon;
        if (!TextUtils.isEmpty(visibleUrl)) {
            favicon = DappBrowserUtils.getIconUrl(visibleUrl);
            Glide.with(this)
                    .load(favicon)
                    .apply(new RequestOptions().placeholder(R.drawable.ic_logo))
                    .into(icon);
        }

        switch (mode) {
            case MODE_ADD: {
                setTitle(getString(R.string.add_to_my_dapps));
                button.setText(R.string.action_add);
                name.setText(dapp.getName());
                name.getEditText().setSelection(0);
                url.setText(dapp.getUrl());
                url.getEditText().setSelection(0);
                button.setOnClickListener(v -> {
                    dapp.setName(name.getText().toString());
                    dapp.setUrl(url.getText().toString());
                    add(dapp); });
                break;
            }
            case MODE_EDIT: {
                setTitle(getString(R.string.edit_dapp));
                button.setText(R.string.action_save);
                url.setText(dapp.getUrl());
                url.getEditText().setSelection(0);
                name.setText(dapp.getName());
                name.getEditText().setSelection(0);
                button.setOnClickListener(v -> {
                    save(dapp);
                });
                break;
            }
            default: {
                break;
            }
        }
    }

    private void save(DApp dapp) {
        try
        {
            List<DApp> myDapps = DappBrowserUtils.getMyDapps(this);
            for (DApp d : myDapps)
            {
                if (d.getName().equals(dapp.getName()) &&
                        d.getUrl().equals(dapp.getUrl()))
                {
                    d.setName(name.getText().toString());
                    d.setUrl(url.getText().toString());
                }
            }
            DappBrowserUtils.saveToPrefs(this, myDapps);
        }
        catch (Exception e)
        {
            Timber.e(e);
        }
        finally
        {
            finish();
        }
    }

    private void add(DApp dapp) {
        List<DApp> myDapps = DappBrowserUtils.getMyDapps(this);
        myDapps.add(dapp);
        DappBrowserUtils.saveToPrefs(this, myDapps);
        finish();
    }
}
