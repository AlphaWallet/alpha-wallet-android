package com.alphawallet.app.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.walletconnect.WalletConnectSessionItem;
import com.alphawallet.app.ui.widget.divider.ListDivider;
import com.alphawallet.app.ui.zxing.QRScanningActivity;
import com.alphawallet.app.viewmodel.WalletConnectViewModel;
import com.alphawallet.app.viewmodel.WalletConnectViewModelFactory;
import com.alphawallet.app.walletconnect.WCClient;
import com.bumptech.glide.Glide;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

import static com.alphawallet.app.C.Key.WALLET;

/**
 * Created by JB on 9/09/2020.
 */
public class WalletConnectSessionActivity extends BaseActivity
{
    @Inject
    WalletConnectViewModelFactory viewModelFactory;
    WalletConnectViewModel viewModel;

    private RecyclerView recyclerView;
    private CustomAdapter adapter;
    private Wallet wallet;
    private List<WalletConnectSessionItem> wcSessions;

    private final Handler handler = new Handler();

    @Nullable
    private Disposable connectionCheck;

    private int connectionCount = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        AndroidInjection.inject(this);

        setContentView(R.layout.activity_list);
        toolbar();
        setTitle(getString(R.string.title_wallet_connect));
        wallet = getIntent().getParcelableExtra(WALLET);
        initViewModel();
    }

    private void initViewModel()
    {
        if (viewModel == null)
        {
            viewModel = ViewModelProviders.of(this, viewModelFactory)
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
            recyclerView = findViewById(R.id.list);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new CustomAdapter();
            recyclerView.setAdapter(adapter);
            recyclerView.addItemDecoration(new ListDivider(this));
            adapter.notifyDataSetChanged();
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
        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
                break;
            }
            case R.id.action_scan: {
                Intent intent = new Intent(this, QRScanningActivity.class);
                intent.putExtra("wallet", wallet);
                intent.putExtra(C.EXTRA_UNIVERSAL_SCAN, true);
                startActivityForResult(intent, C.REQUEST_UNIVERSAL_SCAN);
            }
            break;
        }

        return super.onOptionsItemSelected(item);
    }

    public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.CustomViewHolder>
    {
        @Override
        public CustomAdapter.CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
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

            CustomViewHolder(View view)
            {
                super(view);
                icon = view.findViewById(R.id.icon);
                statusIcon = view.findViewById(R.id.status_icon);
                peerName = view.findViewById(R.id.session_name);
                peerUrl = view.findViewById(R.id.session_url);
                clickLayer = view.findViewById(R.id.item_layout);
            }
        }

        @Override
        public void onBindViewHolder(CustomAdapter.CustomViewHolder holder, int position)
        {
            final WalletConnectSessionItem session = wcSessions.get(position);

            Glide.with(getApplication())
                    .load(session.icon)
                    .into(holder.icon);
            holder.peerName.setText(session.name);
            holder.peerUrl.setText(session.url);
            holder.clickLayer.setOnClickListener(v -> {
                //go to wallet connect session page
                Intent intent = new Intent(getApplication(), WalletConnectActivity.class);
                intent.putExtra("session", session.sessionId);
                startActivity(intent);
            });

            WCClient client = viewModel.getClient(session.sessionId);
            if (client == null || !client.isConnected())
            {
                holder.statusIcon.setVisibility(View.GONE);
            }
            else
            {
                holder.statusIcon.setVisibility(View.VISIBLE);
                holder.statusIcon.setImageResource(R.drawable.ic_connected);
            }

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case C.REQUEST_UNIVERSAL_SCAN:
                if(data != null && resultCode == Activity.RESULT_OK)
                {
                    if (data.hasExtra(C.EXTRA_QR_CODE))
                    {
                        String qrCode = data.getStringExtra(C.EXTRA_QR_CODE);
                        if (qrCode.startsWith("wc:"))
                        {
                            Intent intent = new Intent(this, WalletConnectActivity.class);
                            intent.putExtra("qrCode", qrCode);
                            startActivity(intent);
                            setResult(RESULT_OK);
                            finish();
                        }
                    }
                }
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private void startConnectionCheck()
    {
        if (connectionCheck != null && !connectionCheck.isDisposed()) connectionCheck.dispose();

        connectionCheck = Observable.interval(0, 10, TimeUnit.SECONDS)
                .doOnNext(l -> checkConnections()).subscribe();
    }

    private void checkConnections()
    {
        int connections = viewModel.getConnectionCount();
        if (connectionCount >= 0 && connections != connectionCount)
        {
            handler.post(() -> adapter.notifyDataSetChanged());
        }

        connectionCount = connections;
    }

    private void stopConnectionCheck()
    {
        if (connectionCheck != null && !connectionCheck.isDisposed()) connectionCheck.dispose();
    }
}
