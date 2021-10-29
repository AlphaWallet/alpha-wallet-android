package com.alphawallet.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.ui.widget.divider.ListDivider;
import com.alphawallet.app.widget.TestNetDialog;
import com.google.android.material.switchmaterial.SwitchMaterial;

public abstract class SelectNetworkBaseActivity extends BaseActivity {
    RecyclerView mainnetRecyclerView;
    RecyclerView testnetRecyclerView;
    SwitchMaterial mainnetSwitch;
    SwitchMaterial testnetSwitch;
    TestNetDialog testnetDialog;
    FrameLayout mainnetFrame;
    FrameLayout testnetFrame;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_select_network);

        toolbar();

        initViews();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            handleSetNetworks();
        }
        else if (item.getItemId() == R.id.action_add)
        {
            startActivity(new Intent(this, AddCustomRPCNetworkActivity.class));
        }
        return false;
    }

    @Override
    public void onBackPressed()
    {
        handleSetNetworks();
    }

    protected abstract void handleSetNetworks();

    protected void initTestNetDialog(TestNetDialog.TestNetDialogCallback callback)
    {
        testnetDialog = new TestNetDialog(this, 0, callback);
    }

    private void initViews()
    {
        mainnetFrame = findViewById(R.id.mainnet_frame);
        testnetFrame = findViewById(R.id.testnet_frame);

        mainnetSwitch = findViewById(R.id.mainnet_switch);
        testnetSwitch = findViewById(R.id.testnet_switch);

        mainnetRecyclerView = findViewById(R.id.main_list);
        testnetRecyclerView = findViewById(R.id.test_list);

        mainnetRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mainnetRecyclerView.addItemDecoration(new ListDivider(this));

        testnetRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        testnetRecyclerView.addItemDecoration(new ListDivider(this));
    }

    void hideSwitches()
    {
        testnetFrame.setVisibility(View.GONE);
        mainnetFrame.setVisibility(View.GONE);
    }

    void toggleListVisibility(boolean isMainNetActive)
    {
        if (isMainNetActive)
        {
            testnetRecyclerView.setVisibility(View.GONE);
            mainnetRecyclerView.setVisibility(View.VISIBLE);
        }
        else
        {
            testnetRecyclerView.setVisibility(View.VISIBLE);
            mainnetRecyclerView.setVisibility(View.GONE);
        }
    }
}
