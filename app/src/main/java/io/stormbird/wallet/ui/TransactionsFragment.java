package io.stormbird.wallet.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import io.stormbird.token.tools.TokenDefinition;
import org.xml.sax.SAXException;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.ErrorEnvelope;
import io.stormbird.wallet.entity.HelpItem;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.TokenInterface;
import io.stormbird.wallet.entity.TokensReceiver;
import io.stormbird.wallet.entity.Transaction;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.ui.widget.adapter.HelpAdapter;
import io.stormbird.wallet.ui.widget.adapter.TransactionsAdapter;
import io.stormbird.wallet.util.RootUtil;
import io.stormbird.wallet.viewmodel.HelpViewModel;
import io.stormbird.wallet.viewmodel.HelpViewModelFactory;
import io.stormbird.wallet.viewmodel.TransactionsViewModel;
import io.stormbird.wallet.viewmodel.TransactionsViewModelFactory;
import io.stormbird.wallet.widget.DepositView;
import io.stormbird.wallet.widget.EmptyTransactionsView;
import io.stormbird.wallet.widget.SystemView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;

import static io.stormbird.wallet.C.ErrorCode.EMPTY_COLLECTION;

public class TransactionsFragment extends Fragment implements View.OnClickListener, TokenInterface
{
    @Inject
    TransactionsViewModelFactory transactionsViewModelFactory;
    private TransactionsViewModel viewModel;

    private TokensReceiver tokenReceiver;

    private SystemView systemView;
    private TransactionsAdapter adapter;
    private HomeActivity homeActivity; //TODO: Have a central storage for tokens shared between views. Also need mutables for completion waits
    private Dialog dialog;

    private boolean isVisible = false;
    private int networkId = 0;

    RecyclerView list;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        AndroidSupportInjection.inject(this);
        View view = inflater.inflate(R.layout.fragment_transactions, container, false);

        viewModel = ViewModelProviders.of(this, transactionsViewModelFactory).get(TransactionsViewModel.class);
        homeActivity = (HomeActivity) getActivity();

        adapter = new TransactionsAdapter(this::onTransactionClick);
        SwipeRefreshLayout refreshLayout = view.findViewById(R.id.refresh_layout);
        systemView = view.findViewById(R.id.system_view);

        list = view.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        list.setAdapter(adapter);

        systemView.attachRecyclerView(list);
        systemView.attachSwipeRefreshLayout(refreshLayout);

        systemView.showProgress(false);

        viewModel = ViewModelProviders.of(this, transactionsViewModelFactory)
                .get(TransactionsViewModel.class);
        viewModel.progress().observe(this, systemView::showProgress);
        viewModel.error().observe(this, this::onError);
        viewModel.defaultNetwork().observe(this, this::onDefaultNetwork);
        viewModel.defaultWallet().observe(this, this::onDefaultWallet);
        viewModel.transactions().observe(this, this::onTransactions);
        viewModel.showEmpty().observe(this, this::showEmptyTx);
        viewModel.clearAdapter().observe(this, this::clearAdapter);
        refreshLayout.setOnRefreshListener(() -> viewModel.forceUpdateTransactionView());

        adapter.clear();

        tokenReceiver = new TokensReceiver(getActivity(), this);

        return view;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.try_again: {
                viewModel.forceUpdateTransactionView();
            }
            break;
            case R.id.action_buy: {
                openExchangeDialog();
            }
        }
    }

    private void onTransactionClick(View view, Transaction transaction) {
        viewModel.showDetails(view.getContext(), transaction);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onResume() {
        super.onResume();
        viewModel.setVisibility(isVisible);
        viewModel.prepare();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        isVisible = isVisibleToUser;
        if (isResumed()) { // fragment created
            viewModel.setVisibility(isVisible);
            if (isVisible) {
                viewModel.startTransactionRefresh();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        //stop transaction refresh
        viewModel.setVisibility(false);
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private void onTransactions(Transaction[] transaction) {
        adapter.updateTransactions(transaction);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        getContext().unregisterReceiver(tokenReceiver);
    }

    private void onDefaultWallet(Wallet wallet)
    {
        adapter.setDefaultWallet(wallet);
    }

    private void onDefaultNetwork(NetworkInfo networkInfo)
    {
        adapter.setDefaultNetwork(networkInfo);
        networkId = networkInfo.chainId;
    }

    private void onError(ErrorEnvelope errorEnvelope) {
        if (errorEnvelope.code == EMPTY_COLLECTION || adapter.getItemCount() == 0) {
            showEmptyTx(true);
        }/* else {
            systemView.showError(getString(R.string.error_fail_load_transaction), this);
        }*/
    }

    private void showEmptyTx(boolean show) {
        if (show)
        {
            EmptyTransactionsView emptyView = new EmptyTransactionsView(getContext(), this);
            emptyView.setNetworkInfo(viewModel.defaultNetwork().getValue());
            systemView.showEmpty(emptyView);
        }
        else
        {
            systemView.hide();
        }
    }

    private void openExchangeDialog() {
        Wallet wallet = viewModel.defaultWallet().getValue();
        if (wallet == null) {
            Toast.makeText(getContext(), getString(R.string.error_wallet_not_selected), Toast.LENGTH_SHORT)
                    .show();
        } else {
            BottomSheetDialog dialog = new BottomSheetDialog(getContext());
            DepositView view = new DepositView(getContext(), wallet);
            view.setOnDepositClickListener(this::onDepositClick);
            dialog.setContentView(view);
            dialog.show();
            this.dialog = dialog;
        }
    }

    private void onDepositClick(View view, Uri uri) {
        viewModel.openDeposit(view.getContext(), uri);
    }

    @Override
    public void resetTokens()
    {
        //first abort the current operation
        viewModel.abortAndRestart(true);
        adapter.clear();
    }

    @Override
    public void addedToken()
    {

    }

    @Override
    public void changedLocale()
    {
        //need to refresh the transaction view
        adapter.clear();
        list.setAdapter(adapter);
        viewModel.abortAndRestart(true);
    }

    private void clearAdapter(Boolean aBoolean)
    {
        adapter.clear();
        list.setAdapter(adapter);
        viewModel.abortAndRestart(false);
    }
}
