package com.alphawallet.app.ui;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.analytics.Analytics;
import com.alphawallet.app.entity.analytics.QrScanResultType;
import com.alphawallet.app.entity.walletconnect.WalletConnectSessionItem;
import com.alphawallet.app.entity.walletconnect.WalletConnectV2SessionItem;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.ui.QRScanning.QRScannerActivity;
import com.alphawallet.app.ui.widget.divider.ListDivider;
import com.alphawallet.app.viewmodel.WalletConnectViewModel;
import com.alphawallet.app.walletconnect.AWWalletConnectClient;
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
    private final Handler handler = new Handler(Looper.getMainLooper());
    private LocalBroadcastManager broadcastManager;
    WalletConnectViewModel viewModel;
    private RecyclerView recyclerView;
    private Button btnConnectWallet;
    private LinearLayout layoutNoActiveSessions;
    private CustomAdapter adapter;
    private final BroadcastReceiver walletConnectChangeReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (action.equals(C.WALLET_CONNECT_COUNT_CHANGE))
            {
                handler.post(() -> adapter.notifyDataSetChanged());
            }
        }
    };

    @Inject
    AWWalletConnectClient awWalletConnectClient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet_connect_sessions);
        toolbar();
        setTitle(getString(R.string.title_wallet_connect));

        recyclerView = findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new ListDivider(this));
        layoutNoActiveSessions = findViewById(R.id.layout_no_sessions);
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
            viewModel.sessions().observe(this, this::onSessionsUpdated);
        }

        if (broadcastManager == null)
        {
            broadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        }
    }

    private void onServiceReady(Boolean aBoolean)
    {

    }

    private void onSessionsUpdated(List<WalletConnectSessionItem> sessionItems)
    {
        if (adapter == null)
        {
            adapter = new CustomAdapter(sessionItems);
            recyclerView.setAdapter(adapter);
        }
        else
        {
            adapter.updateList(sessionItems);
        }
        layoutNoActiveSessions.setVisibility(sessionItems.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        initViewModel();
        viewModel.updateSessions();
        startConnectionCheck();

        viewModel.track(Analytics.Navigation.WALLET_CONNECT_SESSIONS);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        stopConnectionCheck();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_wc_sessions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            finish();
        }
        else if (item.getItemId() == R.id.action_scan)
        {
            openQrScanner();
        }
        else if (item.getItemId() == R.id.action_delete)
        {
            View v = findViewById(R.id.action_delete);
            openDeleteMenu(v);
        }

        return super.onOptionsItemSelected(item);
    }

    private void openQrScanner()
    {
        Intent intent = new Intent(this, QRScannerActivity.class);
        intent.putExtra(QrScanResultType.KEY, QrScanResultType.WALLET_CONNECT.getValue());
        intent.putExtra(C.EXTRA_UNIVERSAL_SCAN, true);
        startActivity(intent);
    }

    private void openDeleteMenu(View v)
    {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.getMenuInflater().inflate(R.menu.menu_wc_sessions_delete, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_delete_empty)
            {
                viewModel.removeSessionsWithoutSignRecords(this);
                return true;
            }
            else if (item.getItemId() == R.id.action_delete_all)
            {
                viewModel.removeInactiveSessions(this);
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void setupClient(final WalletConnectSessionItem session, final CustomAdapter.CustomViewHolder holder)
    {
        if (session.wcVersion == 1)
        {
            viewModel.getClient(this, session.sessionId, client -> handler.post(() -> {
                setStatusIconActive(holder, (client != null && client.isConnected()));
            }));
        }
        else
        {
            setStatusIconActive(holder, (session.expiryTime > System.currentTimeMillis()));
        }
    }

    private void setStatusIconActive(final CustomAdapter.CustomViewHolder holder, boolean active)
    {
        if (active)
        {
            holder.statusIcon.setVisibility(View.VISIBLE);
            holder.statusIcon.setImageResource(R.drawable.ic_connected);
        }
        else
        {
            holder.statusIcon.setVisibility(View.GONE);
        }
    }

    private void dialogConfirmDelete(WalletConnectSessionItem session)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog = builder.setTitle(R.string.title_delete_session)
                .setMessage(getString(R.string.delete_session, session.name))
                .setPositiveButton(R.string.delete, (d, w) -> {
                    viewModel.deleteSession(session, new AWWalletConnectClient.WalletConnectV2Callback()
                    {
                        @Override
                        public void onSessionDisconnected()
                        {
                            runOnUiThread(() -> {
                                awWalletConnectClient.updateNotification();
                            });
                        }
                    });
                })
                .setNegativeButton(R.string.action_cancel, (d, w) -> {
                    d.dismiss();
                })
                .setCancelable(false)
                .create();
        dialog.show();
    }

    private void startConnectionCheck()
    {
        broadcastManager.registerReceiver(walletConnectChangeReceiver, new IntentFilter(C.WALLET_CONNECT_COUNT_CHANGE));
    }

    private void stopConnectionCheck()
    {
        broadcastManager.unregisterReceiver(walletConnectChangeReceiver);
    }

    public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.CustomViewHolder>
    {
        private List<WalletConnectSessionItem> list;

        public CustomAdapter(List<WalletConnectSessionItem> list)
        {
            this.list = list;
        }

        @Override
        public CustomAdapter.CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_wc_session, parent, false);

            return new CustomAdapter.CustomViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(CustomAdapter.CustomViewHolder holder, int position)
        {
            final WalletConnectSessionItem session = list.get(position);

            Glide.with(getApplication())
                    .load(session.icon)
                    .circleCrop()
                    .into(holder.icon);
            if (TextUtils.isEmpty(session.name))
            {
                holder.peerName.setText(R.string.no_title);
            }
            else
            {
                holder.peerName.setText(session.name);
            }
            holder.peerUrl.setText(session.url);
            holder.chainIcon.setImageResource(EthereumNetworkRepository.getChainLogo(session.chainId));
            holder.clickLayer.setOnClickListener(v -> {
                Context context = getApplicationContext();
                context.startActivity(newIntent(context, session));
            });

            setupClient(session, holder);

            holder.clickLayer.setOnLongClickListener(v -> {
                //delete this entry?
                dialogConfirmDelete(session);
                return true;
            });
        }

        @Override
        public int getItemCount()
        {
            return list.size();
        }

        public void updateList(List<WalletConnectSessionItem> list)
        {
            this.list = list;
            notifyDataSetChanged();
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
    }

    public static Intent newIntent(Context context, WalletConnectSessionItem session)
    {
        Intent intent;
        if (session instanceof WalletConnectV2SessionItem)
        {
            intent = new Intent(context, WalletConnectV2Activity.class);
            intent.putExtra("session", (WalletConnectV2SessionItem) session);
        }
        else
        {
            intent = new Intent(context, WalletConnectActivity.class);
            intent.putExtra("session", session.sessionId);
        }
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }
}
