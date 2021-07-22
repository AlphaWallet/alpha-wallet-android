package com.alphawallet.app.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.BackupOperationType;
import com.alphawallet.app.entity.BackupTokenCallback;
import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.CustomViewSettings;
import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletPage;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.ui.widget.OnTokenClickListener;
import com.alphawallet.app.ui.widget.adapter.TokensAdapter;
import com.alphawallet.app.ui.widget.entity.WarningData;
import com.alphawallet.app.ui.widget.holder.ManageTokensHolder;
import com.alphawallet.app.ui.widget.holder.TokenGridHolder;
import com.alphawallet.app.ui.widget.holder.TokenHolder;
import com.alphawallet.app.ui.widget.holder.WarningHolder;
import com.alphawallet.app.util.Blockies;
import com.alphawallet.app.util.TabUtils;
import com.alphawallet.app.viewmodel.WalletViewModel;
import com.alphawallet.app.viewmodel.WalletViewModelFactory;
import com.alphawallet.app.widget.NotificationView;
import com.alphawallet.app.widget.ProgressView;
import com.alphawallet.app.widget.SystemView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import io.realm.Realm;
import io.realm.RealmResults;

import static android.app.Activity.RESULT_OK;
import static com.alphawallet.app.C.ErrorCode.EMPTY_COLLECTION;
import static com.alphawallet.app.C.Key.WALLET;
import static com.alphawallet.app.repository.TokensRealmSource.ADDRESS_FORMAT;

/**
 * Created by justindeguzman on 2/28/18.
 */

public class WalletFragment extends BaseFragment implements
        OnTokenClickListener,
        View.OnClickListener,
        Runnable,
        BackupTokenCallback
{
    private static final String TAG = "WFRAG";
    private static final int TAB_ALL = 0;
    private static final int TAB_CURRENCY = 1;
    private static final int TAB_COLLECTIBLES = 2;
    private static final int TAB_ATTESTATIONS = 3;

    @Inject
    WalletViewModelFactory walletViewModelFactory;
    private WalletViewModel viewModel;

    private SystemView systemView;
    private ProgressView progressView;
    private TokensAdapter adapter;
    private ImageView addressBlockie;
    private View selectedToken;
    private final Handler handler = new Handler();
    private String importFileName;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout refreshLayout;
    private boolean isVisible;
    private int currentTabPos = -1;
    private Realm realm;
    private RealmResults<RealmToken> realmUpdates;
    private String realmId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        AndroidSupportInjection.inject(this);

        View view = inflater.inflate(R.layout.fragment_wallet, container, false);

        if (CustomViewSettings.canAddTokens()) {
            toolbar(view, R.menu.menu_wallet, this::onMenuItemClick);
        } else {
            toolbar(view);
        }

        initViews(view);

        initViewModel();

        isVisible = true;

        initList();

        initTabLayout(view);

        initNotificationView(view);

        setImportToken();

        return view;
    }

    private void initList() {
        adapter = new TokensAdapter(this, viewModel.getAssetDefinitionService(), viewModel.getTokensService(), getContext());
        adapter.setHasStableIds(true);
        setLinearLayoutManager(TAB_ALL);
        recyclerView.setAdapter(adapter);
        if (recyclerView.getItemAnimator() != null)
            ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeCallback(adapter));
        itemTouchHelper.attachToRecyclerView(recyclerView);

        refreshLayout.setOnRefreshListener(this::refreshList);
        recyclerView.setRecyclerListener(holder -> adapter.onRViewRecycled(holder));
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this, walletViewModelFactory)
                .get(WalletViewModel.class);
        viewModel.progress().observe(getViewLifecycleOwner(), systemView::showProgress);
        viewModel.tokens().observe(getViewLifecycleOwner(), this::onTokens);
        viewModel.backupEvent().observe(getViewLifecycleOwner(), this::backupEvent);
        viewModel.defaultWallet().observe(getViewLifecycleOwner(), this::onDefaultWallet);
    }

    private void initViews(View view) {
        refreshLayout = view.findViewById(R.id.refresh_layout);
        systemView = view.findViewById(R.id.system_view);
        progressView = view.findViewById(R.id.progress_view);
        recyclerView = view.findViewById(R.id.list);
        addressBlockie = view.findViewById(R.id.user_address_blockie);

        progressView.hide();
        systemView.showProgress(true);

        systemView.attachRecyclerView(recyclerView);
        systemView.attachSwipeRefreshLayout(refreshLayout);
    }

    private void onDefaultWallet(Wallet wallet)
    {
        if (CustomViewSettings.showManageTokens()) {
            adapter.setWalletAddress(wallet.address);
        }

        addressBlockie.setImageBitmap(Blockies.createIcon(wallet.address.toLowerCase()));
        addressBlockie.setVisibility(View.VISIBLE);

        //Do we display new user backup popup?
        ((HomeActivity) getActivity()).showBackupWalletDialog(wallet.lastBackupTime > 0);
        startRealmListener(wallet);
    }

    private void startRealmListener(Wallet wallet)
    {
        if (realmId == null || !realmId.equalsIgnoreCase(wallet.address))
        {
            realmId = wallet.address;
            realm = viewModel.getRealmInstance(wallet);
            setRealmListener();
        }
    }

    private void setRealmListener()
    {
        realmUpdates = realm.where(RealmToken.class).equalTo("isEnabled", true)
                .like("address", ADDRESS_FORMAT).findAllAsync();
        realmUpdates.addChangeListener(realmTokens -> {
            if (!isVisible && realmTokens.size() == 0) return;
            List<TokenCardMeta> metas = new ArrayList<>();
            //make list
            for (RealmToken t : realmTokens)
            {
                if (!viewModel.getTokensService().getNetworkFilters().contains(t.getChainId())) continue;
                if (viewModel.isChainToken(t.getChainId(), t.getTokenAddress())) continue;

                String balance = TokensRealmSource.convertStringBalance(t.getBalance(), t.getContractType());

                TokenCardMeta meta = new TokenCardMeta(t.getChainId(), t.getTokenAddress(), balance,
                        t.getUpdateTime(), viewModel.getAssetDefinitionService(), t.getName(), t.getSymbol(), t.getContractType());
                meta.lastTxUpdate = t.getLastTxTime();
                metas.add(meta);
            }

            updateMetas(metas);
        });
    }

    private void updateMetas(List<TokenCardMeta> metas)
    {
        handler.post(() -> {
            if (metas.size() > 0)
            {
                adapter.setTokens(metas.toArray(new TokenCardMeta[0]));
                systemView.hide();
            }

            if (viewModel.getWallet().type != WalletType.WATCH && isVisible)
            {
                viewModel.checkBackup();
            }
        });
    }

    private void refreshList()
    {
        handler.post(() -> {
            adapter.clear();
            viewModel.prepare();
            viewModel.notifyRefresh();
        });
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        isVisible = isVisibleToUser;
        if (isResumed()) { // fragment created
            if (isVisible) viewModel.prepare();
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
    }

    private void initTabLayout(View view)
    {
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        if (CustomViewSettings.hideTabBar())
        {
            tabLayout.setVisibility(View.GONE);
            return;
        }
        tabLayout.addTab(tabLayout.newTab().setText(R.string.all));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.currency));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.collectibles));
        //tabLayout.addTab(tabLayout.newTab().setText(R.string.attestations));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener()
        {
            @Override
            public void onTabSelected(TabLayout.Tab tab)
            {
                switch (tab.getPosition())
                {
                    case TAB_ALL:
                        setLinearLayoutManager(tab.getPosition());
                        adapter.setFilterType(TokensAdapter.FILTER_ALL);
                        viewModel.prepare();
                        break;
                    case TAB_CURRENCY:
                        setLinearLayoutManager(tab.getPosition());
                        adapter.setFilterType(TokensAdapter.FILTER_CURRENCY);
                        viewModel.prepare();
                        break;
                    case TAB_COLLECTIBLES:
                        setGridLayoutManager(tab.getPosition());
                        adapter.setFilterType(TokensAdapter.FILTER_COLLECTIBLES);
                        viewModel.prepare();
                        break;
                    case TAB_ATTESTATIONS: // TODO: Filter Attestations
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

        TabUtils.decorateTabLayout(getContext(), tabLayout);
    }

    private void setGridLayoutManager(int tab)
    {
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup()
        {
            @Override
            public int getSpanSize(int position)
            {
                if (adapter.getItemViewType(position) == TokenGridHolder.VIEW_TYPE)
                {
                    return 1;
                }
                return 2;
            }
        });
        recyclerView.setLayoutManager(gridLayoutManager);
        currentTabPos = tab;
    }

    private void setLinearLayoutManager(int tab)
    {
        if (currentTabPos != TAB_ALL && currentTabPos != TAB_CURRENCY)
        {
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        }
        currentTabPos = tab;
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
        currentTabPos = -1;
        selectedToken = null;
        if (viewModel == null)
        {
            ((HomeActivity)getActivity()).resetFragment(WalletPage.WALLET);
        }
        else
        {
            viewModel.prepare();
        }
    }

    private void onTokens(TokenCardMeta[] tokens)
    {
        if (tokens != null)
        {
            adapter.setTokens(tokens);
            checkScrollPosition();
        }
        systemView.showProgress(false);
    }

    /**
     * Checks to see if the current session was started from clicking on a TokenScript notification
     * If it was, identify the contract and pass information to adapter which will identify the corresponding contract token card
     */
    private void setImportToken()
    {
        if (importFileName != null)
        {
            ContractLocator importToken = viewModel.getAssetDefinitionService().getHoldingContract(importFileName);
            if (importToken != null) Toast.makeText(getContext(), importToken.address, Toast.LENGTH_LONG).show();
            if (importToken != null && adapter != null) adapter.setScrollToken(importToken);
            importFileName = null;
        }
    }

    /**
     * If the adapter has identified the clicked-on script update from the above call and that card is present, scroll to the card.
     */
    private void checkScrollPosition()
    {
        int scrollPos = adapter.getScrollPosition();
        if (scrollPos > 0 && recyclerView != null)
        {
            ((LinearLayoutManager) recyclerView.getLayoutManager()).scrollToPositionWithOffset(scrollPos, 0);
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
                wData.colour = R.color.slate_grey;
                wData.buttonColour = R.color.backup_grey;
                wData.wallet = viewModel.getWallet();
                adapter.addWarning(wData);
                break;
            case WALLET_HAS_HIGH_VALUE:
                wData = new WarningData(this);
                wData.title = getString(R.string.wallet_not_backed_up);
                wData.detail = getString(R.string.not_backed_up_detail);
                wData.buttonText = getString(R.string.back_up_wallet_action, viewModel.getWalletAddr().substring(0, 5));
                wData.colour = R.color.warning_red;
                wData.buttonColour = R.color.warning_dark_red;
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
                viewModel.prepare();
            }
            break;
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        //viewModel.clearProcess();
        if (realmUpdates != null) realmUpdates.removeAllChangeListeners();
        if (realm != null && !realm.isClosed()) realm.close();
        if (adapter != null && recyclerView != null) adapter.onDestroy(recyclerView);
    }

    public void resetTokens()
    {
        //first abort the current operation
        if (viewModel != null && adapter != null)
        {
            adapter.clear();
        }
    }

    public void refreshTokens()
    {
        //only update the tokens in place if something has changed, using TokenSortedItem rules.
        if (viewModel != null)
        {
            viewModel.prepare();
            systemView.showProgress(false); //indicate update complete
        }
    }

    public void indicateFetch()
    {
        systemView.showCentralSpinner();
    }

    public void changedLocale()
    {
        refreshList();
    }

    public void walletOutOfFocus()
    {
        if (viewModel != null) viewModel.getTokensService().walletHidden();
    }

    public void walletInFocus()
    {
        if (viewModel != null) viewModel.getTokensService().walletShowing();
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

    ActivityResultLauncher<Intent> handleBackupClick = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                String keyBackup = null;
                boolean noLockScreen = false;
                Intent data = result.getData();
                if (data != null) keyBackup = data.getStringExtra("Key");
                if (data != null) noLockScreen = data.getBooleanExtra("nolock", false);
                if (result.getResultCode() == RESULT_OK)
                {
                    ((HomeActivity)getActivity()).backupWalletSuccess(keyBackup);
                }
                else
                {
                    ((HomeActivity)getActivity()).backupWalletFail(keyBackup, noLockScreen);
                }
    });

    @Override
    public void BackupClick(Wallet wallet)
    {
        Intent intent = new Intent(getContext(), BackupKeyActivity.class);
        intent.putExtra(WALLET, wallet);

        switch (viewModel.getWalletType())
        {
            case HDKEY:
                intent.putExtra("TYPE", BackupOperationType.BACKUP_HD_KEY);
                break;
            case KEYSTORE:
                intent.putExtra("TYPE", BackupOperationType.BACKUP_KEYSTORE_KEY);
                break;
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        handleBackupClick.launch(intent);
    }

    @Override
    public void remindMeLater(Wallet wallet)
    {
        handler.post(() -> {
            if (viewModel != null) viewModel.setKeyWarningDismissTime(wallet.address);
            if (adapter != null) adapter.removeBackupWarning();
        });
    }

    public void storeWalletBackupTime(String backedUpKey)
    {
        handler.post(() -> {
            if (viewModel != null) viewModel.setKeyBackupTime(backedUpKey);
            if (adapter != null) adapter.removeBackupWarning();
        });
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
                    adapter.removeToken(token.tokenInfo.chainId, token.getAddress());

                    if (getContext() != null) {
                        Snackbar snackbar = Snackbar
                                .make(viewHolder.itemView, token.tokenInfo.name + " " + getContext().getString(R.string.token_hidden), Snackbar.LENGTH_LONG)
                                .setAction(getString(R.string.action_snackbar_undo), view -> {
                                    viewModel.setTokenEnabled(token, true);
                                    //adapter.updateToken(token.tokenInfo.chainId, token.getAddress(), true);
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
            else if (viewHolder.getItemViewType() == ManageTokensHolder.VIEW_TYPE ||
                    viewHolder.getItemViewType() == TokenGridHolder.VIEW_TYPE)
            {
                return 0;
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

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.action_my_wallet) {
            viewModel.showMyAddress(getContext());
        }
        if (menuItem.getItemId() == R.id.action_scan) {
            viewModel.showQRCodeScanning(getActivity());
        }
        return super.onMenuItemClick(menuItem);
    }

    private void initNotificationView(View view) {
        final String key = "marshmallow_version_support_warning_shown";
        NotificationView notificationView = view.findViewById(R.id.notification);
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean hasShownWarning = pref.getBoolean(key, false);

        if (!hasShownWarning && android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            notificationView.setNotificationBackgroundColor(R.color.indigo);
            notificationView.setTitle(getContext().getString(R.string.title_version_support_warning));
            notificationView.setMessage(getContext().getString(R.string.message_version_support_warning));
            notificationView.setPrimaryButtonText(getContext().getString(R.string.hide_notification));
            notificationView.setPrimaryButtonListener(() -> {
                notificationView.setVisibility(View.GONE);
                pref.edit().putBoolean(key, true).apply();
            });
        } else {
            notificationView.setVisibility(View.GONE);
        }
    }
}
