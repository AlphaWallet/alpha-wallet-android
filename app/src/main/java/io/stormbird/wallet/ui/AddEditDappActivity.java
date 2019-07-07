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
import com.userexperior.UserExperior;

import java.net.URISyntaxException;
import java.util.List;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.DApp;
import io.stormbird.wallet.util.DappBrowserUtils;
import io.stormbird.wallet.util.Utils;

public class AddEditDappActivity extends BaseActivity {
    public static final String KEY_MODE = "mode";
    public static final String KEY_DAPP = "dapp";
    public static final int MODE_ADD = 0;
    public static final int MODE_EDIT = 1;

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
            case MODE_ADD: {
                title.setText(R.string.add_to_my_dapps);
                button.setText(R.string.action_add);
                name.setText(dapp.getName());
                url.setText(dapp.getUrl());
                button.setOnClickListener(v -> add(dapp));
                break;
            }
            case MODE_EDIT: {
                title.setText(R.string.edit_dapp);
                button.setText(R.string.action_save);
                url.setText(dapp.getUrl());
                name.setText(dapp.getName());
                button.setOnClickListener(v -> save(dapp));
                break;
            }
            default: {
                break;
            }
        }
        UserExperior.startRecording(getApplicationContext(), "b96f2b04-99a7-45e8-9354-006b9f9fe770");
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
            e.printStackTrace();
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
