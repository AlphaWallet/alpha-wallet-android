package com.alphawallet.app.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.alphawallet.app.entity.BackupTokenCallback;
import com.alphawallet.app.entity.ContractResult;
import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.VisibilityFilter;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenInterface;
import com.alphawallet.app.entity.tokens.TokensReceiver;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.ui.widget.OnTokenClickListener;
import com.alphawallet.app.ui.widget.adapter.TokensAdapter;
import com.alphawallet.app.ui.widget.entity.WarningData;
import com.alphawallet.app.ui.widget.holder.TokenHolder;
import com.alphawallet.app.ui.widget.holder.WarningHolder;
import com.alphawallet.app.util.TabUtils;

import dagger.android.support.AndroidSupportInjection;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.viewmodel.WalletViewModel;
import com.alphawallet.app.viewmodel.WalletViewModelFactory;
import com.alphawallet.app.widget.ProgressView;
import com.alphawallet.app.widget.SystemView;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static com.alphawallet.app.C.ErrorCode.EMPTY_COLLECTION;
import static com.alphawallet.app.C.Key.WALLET;

/**
 * Created by justindeguzman on 2/28/18.
 */

public class WalletFragment extends Fragment implements OnTokenClickListener, View.OnClickListener, TokenInterface, Runnable, BackupTokenCallback
{
    private static final String TAG = "WFRAG";

    @Inject
    WalletViewModelFactory walletViewModelFactory;
    private WalletViewModel viewModel;

    private TokensReceiver tokenReceiver;

    private SystemView systemView;
    private ProgressView progressView;
    private TokensAdapter adapter;
    private View selectedToken;
    private Handler handler;
    private String importFileName;
    private RecyclerView listView;

    private boolean isVisible;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        AndroidSupportInjection.inject(this);
        View view = inflater.inflate(R.layout.fragment_wallet, container, false);

        SwipeRefreshLayout refreshLayout = view.findViewById(R.id.refresh_layout);
        systemView = view.findViewById(R.id.system_view);
        progressView = view.findViewById(R.id.progress_view);
        progressView.hide();
        systemView.hide();

        listView = view.findViewById(R.id.list);

        systemView.attachRecyclerView(listView);
        systemView.attachSwipeRefreshLayout(refreshLayout);

        viewModel = ViewModelProviders.of(this, walletViewModelFactory)
                .get(WalletViewModel.class);
        viewModel.progress().observe(this, systemView::showProgress);
        viewModel.error().observe(this, this::onError);
        viewModel.tokens().observe(this, this::onTokens);
        viewModel.total().observe(this, this::onTotal);
        viewModel.queueProgress().observe(this, progressView::updateProgress);
        viewModel.currentWalletBalance().observe(this, this::onBalanceChanged);
        viewModel.refreshTokens().observe(this, this::refreshTokens);
        viewModel.tokenUpdate().observe(this, this::onToken);
        viewModel.tokensReady().observe(this, this::tokensReady);
        viewModel.backupEvent().observe(this, this::backupEvent);
        viewModel.defaultWallet().observe(this, this::onDefaultWallet);

        adapter = new TokensAdapter(this, viewModel.getAssetDefinitionService(), viewModel.getTokensService(), getContext());
        adapter.setHasStableIds(true);
        listView.setLayoutManager(new LinearLayoutManager(getContext()));
        listView.setAdapter(adapter);
        if (listView.getItemAnimator() != null)
            ((SimpleItemAnimator) listView.getItemAnimator()).setSupportsChangeAnimations(false);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeCallback(adapter));
        itemTouchHelper.attachToRecyclerView(listView);

        refreshLayout.setOnRefreshListener(this::refreshList);

        tokenReceiver = new TokensReceiver(getActivity(), this);

        initTabLayout(view);

        isVisible = true;

        setImportToken();

        viewModel.clearProcess();
        return view;
    }

    private void onDefaultWallet(Wallet wallet)
    {
        //Do we display new user backup popup?
        ((HomeActivity)getActivity()).showBackupWalletDialog(wallet.lastBackupTime > 0);
    }

    public void refreshList()
    {
        adapter.clear();
        viewModel.reloadTokens();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        isVisible = isVisibleToUser;
        if (isResumed()) { // fragment created
            viewModel.setVisibility(isVisible);
            if (isVisible) viewModel.prepare();
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        viewModel.setVisibility(false);
    }

    private void onToken(Token token)
    {
        adapter.updateToken(token, false);
    }

    private void initTabLayout(View view) {
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        if (VisibilityFilter.hideTabBar())
        {
            hideTabBar(view);
        }
        else
        {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.all));
            tabLayout.addTab(tabLayout.newTab().setText(R.string.currency));
            tabLayout.addTab(tabLayout.newTab().setText(R.string.collectibles));

            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener()
            {
                @Override
                public void onTabSelected(TabLayout.Tab tab)
                {
                    switch (tab.getPosition())
                    {
                        case 0:
                            adapter.setFilterType(TokensAdapter.FILTER_ALL);
                            viewModel.fetchTokens();
                            break;
                        case 1:
                            adapter.setFilterType(TokensAdapter.FILTER_CURRENCY);
                            viewModel.fetchTokens();
                            break;
                        case 2:
                            adapter.setFilterType(TokensAdapter.FILTER_COLLECTIBLES);
                            viewModel.fetchTokens();
                            break;
                        default:
                            break;
                    }
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab)
                {

                }

                @Override
                public void onTabReselected(TabLayout.Tab tab)
                {

                }
            });
        }

        TabUtils.changeTabsFont(getContext(), tabLayout);
    }

    private void onTotal(BigDecimal totalInCurrency) {
//        adapter.setTotal(totalInCurrency);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add: {
                viewModel.showAddToken(getActivity());
            }
            break;
            case android.R.id.home: {
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTokenClick(View view, Token token, List<BigInteger> ids, boolean selected) {
        if (selectedToken == null)
        {
            selectedToken = view;
            token = viewModel.getTokenFromService(token);
            token.clickReact(viewModel, getActivity());
            handler.postDelayed(this, 700);
        }
    }

    @Override
    public void onLongTokenClick(View view, Token token, List<BigInteger> tokenId)
    {

    }

    @Override
    public void onResume() {
        super.onResume();
        if (handler == null) handler = new Handler();
        selectedToken = null;
        viewModel.setVisibility(isVisible);
        viewModel.prepare();
        listView = getActivity().findViewById(R.id.list);
    }

    private void onTokens(Token[] tokens)
    {
        if (tokens != null)
        {
            adapter.setTokens(tokens);
            checkScrollPosition();
        }
    }

    /**
     * Checks to see if the current session was started from clicking on a TokenScript notification
     * If it was, identify the contract and pass information to adapter which will identify the corresponding contract token card
     */
    private void setImportToken()
    {
        if (importFileName != null)
        {
            ContractResult importToken = viewModel.getAssetDefinitionService().getHoldingContract(importFileName);
            if (importToken != null) Toast.makeText(getContext(), importToken.name, Toast.LENGTH_LONG).show();
            if (importToken != null) adapter.setScrollToken(importToken);
            importFileName = null;
        }
    }

    /**
     * If the adapter has identified the clicked-on script update from the above call and that card is present, scroll to the card.
     */
    private void checkScrollPosition()
    {
        int scrollPos = adapter.getScrollPosition();
        if (scrollPos > 0 && listView != null)
        {
            ((LinearLayoutManager)listView.getLayoutManager()).scrollToPositionWithOffset(scrollPos, 0);
        }
    }

    private void backupEvent(GenericWalletInteract.BackupLevel backupLevel)
    {
        if (adapter.hasBackupWarning()) return;

        WarningData wData;
        switch (backupLevel)
        {
            case BACKUP_NOT_REQUIRED:
                break;
            case WALLET_HAS_LOW_VALUE:
                wData = new WarningData(this);
                wData.title = getString(R.string.time_to_backup_wallet);
                wData.detail = getString(R.string.recommend_monthly_backup);
                wData.buttonText = getString(R.string.back_up_wallet_action, viewModel.getWalletAddr().substring(0, 5));
                wData.colour = ContextCompat.getColor(getContext(), R.color.slate_grey);
                wData.buttonColour = ContextCompat.getColor(getContext(), R.color.backup_grey);
                wData.wallet = viewModel.getWallet();
                adapter.addWarning(wData);
                break;
            case WALLET_HAS_HIGH_VALUE:
                wData = new WarningData(this);
                wData.title = getString(R.string.wallet_not_backed_up);
                wData.detail = getString(R.string.not_backed_up_detail);
                wData.buttonText = getString(R.string.back_up_wallet_action, viewModel.getWalletAddr().substring(0, 5));
                wData.colour = ContextCompat.getColor(getContext(), R.color.warning_red);
                wData.buttonColour = ContextCompat.getColor(getContext(), R.color.warning_dark_red);
                wData.wallet = viewModel.getWallet();
                adapter.addWarning(wData);
                break;
        }
    }

    private void onError(ErrorEnvelope errorEnvelope) {
        if (errorEnvelope.code == EMPTY_COLLECTION) {
            systemView.showEmpty(getString(R.string.no_tokens));
        } else {
            systemView.showError(getString(R.string.error_fail_load_tokens), this);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.try_again: {
                viewModel.fetchTokens();
            }
            break;
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        getActivity().unregisterReceiver(tokenReceiver);
        viewModel.clearProcess();
    }

    private void onBalanceChanged(Map<String, String> balance) {

    }

    /**
     * This is triggered by transaction view after we have found new tokens by scanning the transaction history
     * @param aBoolean - dummy param
     */
    private void refreshTokens(Boolean aBoolean)
    {
        adapter.clear();
        viewModel.resetAndFetchTokens();
    }

    private void tokensReady(Boolean dummy)
    {
        //if (getActivity() != null) ((HomeActivity)getActivity()).TokensReady();
    }

    @Override
    public void resetTokens()
    {
        //first abort the current operation
        viewModel.clearProcess();
        adapter.clear();
        viewModel.fetchTokens();
    }

    @Override
    public void refreshTokens()
    {
        //only update the tokens in place if something has changed, using TokenSortedItem rules.
        viewModel.fetchTokens();
        systemView.showProgress(false); //indicate update complete
    }

    public void indicateFetch()
    {
        systemView.showCentralSpinner();
    }

    @Override
    public void addedToken(int[] chainIds, String[] addrs)
    {
        //token was added
        if (chainIds.length != addrs.length)
        {
            System.out.println("Receiver data mismatch");
            return;
        }

        for (int i = 0; i < chainIds.length; i++)
        {
            Token t = viewModel.getTokenFromService(chainIds[i], addrs[i]);
            if (t != null) adapter.updateToken(t, false);
        }
    }

    @Override
    public void changedLocale()
    {
        refreshList();
    }

    public void walletOutOfFocus()
    {
        if (viewModel != null) viewModel.terminateBalanceUpdate();
    }

    public void walletInFocus()
    {
        if (viewModel != null) viewModel.reloadTokens();
    }

    @Override
    public void run()
    {
        if (selectedToken != null && selectedToken.findViewById(R.id.token_layout) != null)
        {
            selectedToken.findViewById(R.id.token_layout).setBackgroundResource(R.drawable.background_marketplace_event);
        }
        selectedToken = null;
    }

    @Override
    public void BackupClick(Wallet wallet)
    {
        Intent intent = new Intent(getContext(), BackupKeyActivity.class);
        intent.putExtra(WALLET, wallet);

        switch (viewModel.getWalletType())
        {
            case HDKEY:
                intent.putExtra("TYPE", BackupKeyActivity.BackupOperationType.BACKUP_HD_KEY);
                break;
            case KEYSTORE:
                intent.putExtra("TYPE", BackupKeyActivity.BackupOperationType.BACKUP_KEYSTORE_KEY);
                break;
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        getActivity().startActivityForResult(intent, C.REQUEST_BACKUP_WALLET);
    }

    @Override
    public void remindMeLater(Wallet wallet)
    {
        if (viewModel != null) viewModel.setKeyWarningDismissTime(wallet.address).isDisposed();
        if (adapter != null) adapter.removeBackupWarning();
    }

    public void storeWalletBackupTime(String backedUpKey)
    {
        if (viewModel != null) viewModel.setKeyBackupTime(backedUpKey).isDisposed();
        if (adapter != null) adapter.removeBackupWarning();
    }

    public void setImportFilename(String fName)
    {
        importFileName = fName;
    }

    public class SwipeCallback extends ItemTouchHelper.SimpleCallback {
        private TokensAdapter mAdapter;
        private Drawable icon;
        private ColorDrawable background;

        SwipeCallback(TokensAdapter adapter) {
            super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
            mAdapter = adapter;
            if (getActivity() != null) {
                icon = ContextCompat.getDrawable(getActivity(), R.drawable.ic_hide_token);
                if (icon != null) {
                    icon.setTint(ContextCompat.getColor(getActivity(), R.color.white));
                }
                background = new ColorDrawable(ContextCompat.getColor(getActivity(), R.color.cancel_red));
            }
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
            if (viewHolder instanceof WarningHolder) {
                remindMeLater(viewModel.getWallet());
            } else {
                if (viewHolder instanceof TokenHolder) {
                    Token token = ((TokenHolder) viewHolder).token;
                    viewModel.setTokenEnabled(token, false);
                    adapter.removeToken(token);

                    if (getContext() != null) {
                        Snackbar snackbar = Snackbar
                                .make(viewHolder.itemView, token.tokenInfo.name + " " + getContext().getString(R.string.token_hidden), Snackbar.LENGTH_LONG)
                                .setAction(getString(R.string.action_snackbar_undo), view -> {
                                    viewModel.setTokenEnabled(token, true);
                                    adapter.updateToken(token, true);
                                });

                        snackbar.show();
                    }
                }
            }
        }

        @Override
        public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            if (viewHolder.getItemViewType() == TokenHolder.VIEW_TYPE)
            {
                Token t = ((TokenHolder)viewHolder).token;
                if (t.isEthereum()) return 0;
            }

            return super.getSwipeDirs(recyclerView, viewHolder);
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

            View itemView = viewHolder.itemView;
            int offset = 20;
            int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
            int iconTop = itemView.getTop() + (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
            int iconBottom = iconTop + icon.getIntrinsicHeight();

            if (dX > 0) {
                int iconLeft = itemView.getLeft() + iconMargin + icon.getIntrinsicWidth();
                int iconRight = itemView.getLeft() + iconMargin;
                icon.setBounds(iconRight, iconTop, iconLeft, iconBottom);
                background.setBounds(itemView.getLeft(), itemView.getTop(),
                        itemView.getLeft() + ((int) dX) + offset,
                        itemView.getBottom());
            } else if (dX < 0) {
                int iconLeft = itemView.getRight() - iconMargin - icon.getIntrinsicWidth();
                int iconRight = itemView.getRight() - iconMargin;
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                background.setBounds(itemView.getRight() + ((int) dX) - offset,
                        itemView.getTop(), itemView.getRight(), itemView.getBottom());
            } else {
                background.setBounds(0, 0, 0, 0);
            }

            background.draw(c);
            icon.draw(c);
        }
    }

    public Wallet getCurrentWallet()
    {
        return viewModel.getWallet();
    }

    public void hideTabBar(View view)
    {
        view.findViewById(R.id.tab_layout).setVisibility(View.GONE);
    }
}
