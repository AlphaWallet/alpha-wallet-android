package com.alphawallet.app.ui;

import static com.alphawallet.app.C.Key.WALLET;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.walletconnect.WalletConnectSessionItem;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.ui.QRScanning.QRScanner;
import com.alphawallet.app.ui.widget.divider.ListDivider;
import com.alphawallet.app.viewmodel.WalletConnectViewModel;
import com.alphawallet.app.widget.ChainName;
import com.bumptech.glide.Glide;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;


/**
 * Created by JB on 9/09/2020.
 */
@AndroidEntryPoint
public class WalletConnectSessionActivity extends BaseActivity
{
    WalletConnectViewModel viewModel;

    private RecyclerView recyclerView;
    private Button btnConnectWallet;
    private View bottomDivider;
    private LinearLayout layoutNoActiveSessions;
    private CustomAdapter adapter;
    private Wallet wallet;
    private List<WalletConnectSessionItem> wcSessions;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private int connectionCount = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wallet_connect_sessions);
        toolbar();
        setTitle(getString(R.string.title_wallet_connect));
        wallet = getIntent().getParcelableExtra(WALLET);
        initViewModel();

        layoutNoActiveSessions = findViewById(R.id.layout_no_sessions);
        bottomDivider = findViewById(R.id.bottom_divider);
        btnConnectWallet = findViewById(R.id.btn_connect_wallet);
        btnConnectWallet.setOnClickListener(v -> openQrScanner());
    }

    private void initViewModel()
    {
        if (viewModel == null)
        {
            viewModel = new ViewModelProvider(this)
                    .get(WalletConnectViewModel.class);
            viewModel.serviceReady().observe(this, this::onServiceReady);
        }
    }

    private void onServiceReady(Boolean aBoolean)
    {
        //refresh adapter
        if (adapter != null)
        {
            adapter.notifyDataSetChanged();
        }
        else
        {
            setupList();
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        stopConnectionCheck();
    }

    private void setupList()
    {
        wcSessions = viewModel.getSessions();

        if (wcSessions != null)
        {
            if (wcSessions.isEmpty())
            {
                layoutNoActiveSessions.setVisibility(View.VISIBLE);
                bottomDivider.setVisibility(View.GONE);
            }
            else
            {
                layoutNoActiveSessions.setVisibility(View.GONE);
                bottomDivider.setVisibility(View.VISIBLE);
                recyclerView = findViewById(R.id.list);
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                adapter = new CustomAdapter();
                recyclerView.setAdapter(adapter);
                recyclerView.addItemDecoration(new ListDivider(this));
                adapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        connectionCount = -1;
        initViewModel();
        setupList();
        startConnectionCheck();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_scan_wc, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
        {
            finish();
        }
        else if (item.getItemId() == R.id.action_scan)
        {
            openQrScanner();
        }

        return super.onOptionsItemSelected(item);
    }

    private void openQrScanner()
    {
        Intent intent = new Intent(this, QRScanner.class);
        intent.putExtra("wallet", wallet);
        intent.putExtra(C.EXTRA_UNIVERSAL_SCAN, true);
        startActivity(intent);
    }

    public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.CustomViewHolder>
    {
        @Override
        public CustomAdapter.CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_wc_session, parent, false);

            return new CustomAdapter.CustomViewHolder(itemView);
        }

        class CustomViewHolder extends RecyclerView.ViewHolder
        {
            final ImageView icon;
            final ImageView statusIcon;
            final TextView peerName;
            final TextView peerUrl;
            final LinearLayout clickLayer;
            final ImageView chainIcon;

            CustomViewHolder(View view)
            {
                super(view);
                icon = view.findViewById(R.id.icon);
                statusIcon = view.findViewById(R.id.status_icon);
                peerName = view.findViewById(R.id.session_name);
                peerUrl = view.findViewById(R.id.session_url);
                clickLayer = view.findViewById(R.id.item_layout);
                chainIcon = view.findViewById(R.id.status_chain_icon);
                chainIcon.setVisibility(View.VISIBLE);
                view.findViewById(R.id.chain_icon_background).setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onBindViewHolder(CustomAdapter.CustomViewHolder holder, int position)
        {
            final WalletConnectSessionItem session = wcSessions.get(position);

            Glide.with(getApplication())
                    .load(session.icon)
                    .circleCrop()
                    .into(holder.icon);
            holder.peerName.setText(session.name);
            holder.peerUrl.setText(session.url);
            holder.chainIcon.setImageResource(EthereumNetworkRepository.getChainLogo(session.chainId));
            holder.clickLayer.setOnClickListener(v -> {
                //go to wallet connect session page
                Intent intent = new Intent(getApplication(), WalletConnectActivity.class);
                intent.putExtra("session", session.sessionId);
                startActivity(intent);
            });

            setupClient(session.sessionId, holder);

            holder.clickLayer.setOnLongClickListener(v -> {
                //delete this entry?
                dialogConfirmDelete(session);
                return true;
            });
        }

        @Override
        public int getItemCount()
        {
            return wcSessions.size();
        }
    }

    private void setupClient(final String sessionId, final CustomAdapter.CustomViewHolder holder)
    {
        viewModel.getClient(this, sessionId, client -> handler.post(() -> {
            if (client == null || !client.isConnected())
            {
                holder.statusIcon.setVisibility(View.GONE);
            }
            else
            {
                holder.statusIcon.setVisibility(View.VISIBLE);
                holder.statusIcon.setImageResource(R.drawable.ic_connected);
            }
        }));
    }

    private void dialogConfirmDelete(WalletConnectSessionItem session)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog = builder.setTitle(R.string.title_delete_session)
                .setMessage(getString(R.string.delete_session, session.name))
                .setPositiveButton(R.string.delete, (d, w) -> {
                    viewModel.deleteSession(session.sessionId);
                    setupList();
                })
                .setNegativeButton(R.string.action_cancel, (d, w) -> {
                    d.dismiss();
                })
                .setCancelable(false)
                .create();
        dialog.show();
    }

    private final BroadcastReceiver walletConnectChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(C.WALLET_CONNECT_COUNT_CHANGE))
            {
                handler.post(() -> adapter.notifyDataSetChanged());
                connectionCount = intent.getIntExtra("count", 0);
            }
        }
    };

    private void startConnectionCheck()
    {
        registerReceiver(walletConnectChangeReceiver, new IntentFilter(C.WALLET_CONNECT_COUNT_CHANGE));
    }

    private void stopConnectionCheck()
    {
        unregisterReceiver(walletConnectChangeReceiver);
    }
}
