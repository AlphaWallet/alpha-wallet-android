package com.alphawallet.app.ui;

import static android.app.Activity.RESULT_OK;
import static com.alphawallet.app.C.ADDED_TOKEN;
import static com.alphawallet.app.C.ErrorCode.EMPTY_COLLECTION;
import static com.alphawallet.app.C.Key.WALLET;
import static com.alphawallet.app.repository.TokensRealmSource.ADDRESS_FORMAT;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.BackupOperationType;
import com.alphawallet.app.entity.BackupTokenCallback;
import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.CustomViewSettings;
import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.ServiceSyncCallback;
import com.alphawallet.app.entity.TokenFilter;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletPage;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.widget.TokensAdapterCallback;
import com.alphawallet.app.ui.widget.adapter.TokensAdapter;
import com.alphawallet.app.ui.widget.entity.AvatarWriteCallback;
import com.alphawallet.app.ui.widget.entity.WarningData;
import com.alphawallet.app.ui.widget.holder.ManageTokensHolder;
import com.alphawallet.app.ui.widget.holder.TokenGridHolder;
import com.alphawallet.app.ui.widget.holder.TokenHolder;
import com.alphawallet.app.ui.widget.holder.WarningHolder;
import com.alphawallet.app.util.LocaleUtils;
import com.alphawallet.app.viewmodel.WalletViewModel;
import com.alphawallet.app.widget.LargeTitleView;
import com.alphawallet.app.widget.NotificationView;
import com.alphawallet.app.widget.ProgressView;
import com.alphawallet.app.widget.SystemView;
import com.alphawallet.app.widget.UserAvatar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by justindeguzman on 2/28/18.
 */
@AndroidEntryPoint
public class WalletFragment extends BaseFragment implements
        TokensAdapterCallback,
        View.OnClickListener,
        Runnable,
        BackupTokenCallback,
        AvatarWriteCallback,
        ServiceSyncCallback
{
    private static final String TAG = "WFRAG";

    public static final String SEARCH_FRAGMENT = "w_search";

    private WalletViewModel viewModel;

    private SystemView systemView;
    private TokensAdapter adapter;
    private UserAvatar addressAvatar;
    private View selectedToken;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String importFileName;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout refreshLayout;
    private boolean isVisible;
    private TokenFilter currentTabPos = TokenFilter.ALL;
    private Realm realm = null;
    private RealmResults<RealmToken> realmUpdates;
    private LargeTitleView largeTitleView;
    private long realmUpdateTime;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {

        View view = inflater.inflate(R.layout.fragment_wallet, container, false);
        LocaleUtils.setActiveLocale(getContext()); // Can't be placed before above line

        if (CustomViewSettings.canAddTokens()) {
            toolbar(view, R.menu.menu_wallet, this::onMenuItemClick);
        } else {
            toolbar(view);
        }

        initViews(view);

        initViewModel();

        initList();

        initTabLayout(view);

        initNotificationView(view);

        setImportToken();

        viewModel.prepare();

        addressAvatar.setWaiting();

        getChildFragmentManager()
                .setFragmentResultListener(SEARCH_FRAGMENT, this, (requestKey, bundle) -> {
                    Fragment fragment = getChildFragmentManager().findFragmentByTag(SEARCH_FRAGMENT);
                    if (fragment != null && fragment.isVisible() && !fragment.isDetached()) {
                        fragment.onDetach();
                        getChildFragmentManager().beginTransaction()
                                .remove(fragment)
                                .commitAllowingStateLoss();
                    }
                });

        return view;
    }

    private void initList() {
        adapter = new TokensAdapter(this, viewModel.getAssetDefinitionService(), viewModel.getTokensService(),
                tokenManagementLauncher);
        adapter.setHasStableIds(true);
        setLinearLayoutManager(TokenFilter.ALL.ordinal());
        recyclerView.setAdapter(adapter);
        if (recyclerView.getItemAnimator() != null)
            ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeCallback(adapter));
        itemTouchHelper.attachToRecyclerView(recyclerView);

        refreshLayout.setOnRefreshListener(this::refreshList);
        recyclerView.addRecyclerListener(holder -> adapter.onRViewRecycled(holder));
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this)
                .get(WalletViewModel.class);
        viewModel.progress().observe(getViewLifecycleOwner(), systemView::showProgress);
        viewModel.tokens().observe(getViewLifecycleOwner(), this::onTokens);
        viewModel.backupEvent().observe(getViewLifecycleOwner(), this::backupEvent);
        viewModel.defaultWallet().observe(getViewLifecycleOwner(), this::onDefaultWallet);
        viewModel.onFiatValues().observe(getViewLifecycleOwner(), this::updateValue);
        viewModel.getTokensService().startWalletSync(this);
    }

    private void initViews(@NonNull View view) {
        refreshLayout = view.findViewById(R.id.refresh_layout);
        systemView = view.findViewById(R.id.system_view);
        recyclerView = view.findViewById(R.id.list);
        addressAvatar = view.findViewById(R.id.user_address_blockie);
        addressAvatar.setVisibility(View.VISIBLE);

        systemView.showProgress(true);

        systemView.attachRecyclerView(recyclerView);
        systemView.attachSwipeRefreshLayout(refreshLayout);

        largeTitleView = view.findViewById(R.id.large_title_view);

        ((ProgressView)view.findViewById(R.id.progress_view)).hide();
    }

    private void onDefaultWallet(Wallet wallet)
    {
        if (CustomViewSettings.showManageTokens()) {
            adapter.setWalletAddress(wallet.address);
        }

        addressAvatar.bind(wallet, this);
        addressAvatar.setVisibility(View.VISIBLE);

        addressAvatar.setOnClickListener(v -> {
            // open wallets activity
            viewModel.showManageWallets(getContext(), false);
        });

        //Do we display new user backup popup?
        Bundle result = new Bundle();
        result.putBoolean(C.SHOW_BACKUP, wallet.lastBackupTime > 0);
        getParentFragmentManager().setFragmentResult(C.SHOW_BACKUP, result); //reset tokens service and wallet page with updated filters

        addressAvatar.setWaiting();
    }

    private void setRealmListener(final long updateTime)
    {
        if (realm == null || realm.isClosed()) realm = viewModel.getRealmInstance();
        if (realmUpdates != null)
        {
            realmUpdates.removeAllChangeListeners();
            realm.removeAllChangeListeners();
        }

        realmUpdates = realm.where(RealmToken.class).equalTo("isEnabled", true)
                .like("address", ADDRESS_FORMAT)
                .greaterThan("addedTime", (updateTime + 1))
                .findAllAsync();
        realmUpdates.addChangeListener(realmTokens -> {
            long lastUpdateTime = updateTime;
            List<TokenCardMeta> metas = new ArrayList<>();
            //make list
            for (RealmToken t : realmTokens)
            {
                if (t.getUpdateTime() > lastUpdateTime) lastUpdateTime = t.getUpdateTime();
                if (!viewModel.getTokensService().getNetworkFilters().contains(t.getChainId())) continue;
                if (viewModel.isChainToken(t.getChainId(), t.getTokenAddress())) continue;

                String balance = TokensRealmSource.convertStringBalance(t.getBalance(), t.getContractType());

                TokenCardMeta meta = new TokenCardMeta(t.getChainId(), t.getTokenAddress(), balance,
                        t.getUpdateTime(), viewModel.getAssetDefinitionService(), t.getName(), t.getSymbol(), t.getContractType(),
                        viewModel.getTokenGroup(t.getChainId(), t.getTokenAddress()));
                meta.lastTxUpdate = t.getLastTxTime();
                meta.isEnabled = t.isEnabled();
                metas.add(meta);
            }

            if (metas.size() > 0)
            {
                realmUpdateTime = lastUpdateTime;
                updateMetas(metas);
                handler.postDelayed(() -> setRealmListener(realmUpdateTime), 500);
            }
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
        });
    }

    //Refresh value of wallet once sync is complete
    @Override
    public void syncComplete(TokensService svs, int syncCount)
    {
        if (syncCount > 0) handler.post(() -> addressAvatar.finishWaiting());
        if (viewModel.getTokensService().isMainNetActive())
        {
            svs.getFiatValuePair()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::updateValue)
                    .isDisposed();
        }

        if (syncCount > 0)
        {
            //now refresh the tokens to pick up any new ticker updates
            viewModel.getTokensService().getTickerUpdateList()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(adapter::notifyTickerUpdate)
                    .isDisposed();
        }
    }

    //Could the view have been destroyed?
    private void updateValue(Pair<Double, Double> fiatValues)
    {
        try
        {
            // to avoid NaN
            double changePercent = fiatValues.first != 0 ? ((fiatValues.first - fiatValues.second) / fiatValues.second) * 100.0 : 0.0;
            largeTitleView.subtitle.setText(getString(R.string.wallet_total_change, TickerService.getCurrencyString(fiatValues.first - fiatValues.second),
                    TickerService.getPercentageConversion(changePercent)));
            largeTitleView.title.setText(TickerService.getCurrencyString(fiatValues.first));
            int color = ContextCompat.getColor(requireContext(), changePercent < 0 ? R.color.red : R.color.green);
            largeTitleView.subtitle.setTextColor(color);

            if (viewModel.getWallet() != null && viewModel.getWallet().type != WalletType.WATCH && isVisible)
            {
                viewModel.checkBackup(fiatValues.first);
            }
        }
        catch (Exception e)
        {
            // empty: expected if view has terminated before we can shut down the service return
        }
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
    public void comeIntoFocus()
    {
        isVisible = true;
        if (viewModel.getWallet() != null && !TextUtils.isEmpty(viewModel.getWallet().address))
        {
            setRealmListener(realmUpdateTime);
        }
    }

    @Override
    public void leaveFocus()
    {
        if (realmUpdates != null)
        {
            realmUpdates.removeAllChangeListeners();
            realmUpdates = null;
        }
        if (realm != null && !realm.isClosed()) realm.close();
        softKeyboardGone();
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
        tabLayout.addTab(tabLayout.newTab().setText(R.string.assets));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.collectibles));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.defi_header));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.governance_header));
        //tabLayout.addTab(tabLayout.newTab().setText(R.string.attestations));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener()
        {
            @Override
            public void onTabSelected(TabLayout.Tab tab)
            {
                TokenFilter newFilter = setLinearLayoutManager(tab.getPosition());
                adapter.setFilterType(newFilter);
                switch (newFilter)
                {
                    case ALL:
                    case ASSETS:
                    case DEFI:
                    case GOVERNANCE:
                        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
                        viewModel.prepare();
                        break;
                    case COLLECTIBLES:
                        setGridLayoutManager(TokenFilter.COLLECTIBLES);
                        viewModel.prepare();
                        break;
                    case ATTESTATIONS: // TODO: Filter Attestations
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

    private void setGridLayoutManager(TokenFilter tab)
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

    private TokenFilter setLinearLayoutManager(int selectedTab)
    {
        currentTabPos = TokenFilter.values()[selectedTab];
        return currentTabPos;
    }

    @Override
    public void onTokenClick(View view, Token token, List<BigInteger> ids, boolean selected) {
        if (selectedToken == null)
        {
            getParentFragmentManager().setFragmentResult(C.TOKEN_CLICK, new Bundle());
            selectedToken = view;
            Token clickOrigin = viewModel.getTokenFromService(token);
            if (clickOrigin == null) clickOrigin = token;
            viewModel.showTokenDetail(getActivity(), clickOrigin);
            handler.postDelayed(this, 700);
        }
    }

    @Override
    public void onLongTokenClick(View view, Token token, List<BigInteger> tokenId)
    {

    }

    @Override
    public void reloadTokens()
    {
        viewModel.reloadTokens();
    }

    @Override
    public void onBuyToken() {
        Intent intent = viewModel.getBuyIntent(getCurrentWallet().address);
        ((HomeActivity)getActivity()).onActivityResult(C.TOKEN_SEND_ACTIVITY, RESULT_OK, intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        currentTabPos = TokenFilter.ALL;
        selectedToken = null;
        if (viewModel == null)
        {
            ((HomeActivity)getActivity()).resetFragment(WalletPage.WALLET);
        }
        else if (largeTitleView != null)
        {
            largeTitleView.setVisibility(viewModel.getTokensService().isMainNetActive() ? View.VISIBLE : View.GONE); //show or hide Fiat summary
        }
    }

    private void onTokens(TokenCardMeta[] tokens)
    {
        if (tokens != null)
        {
            adapter.setTokens(tokens);
            checkScrollPosition();
            viewModel.calculateFiatValues();
        }
        systemView.showProgress(false);

        realmUpdateTime = 0;
        for (TokenCardMeta tcm : tokens)
        {
            if (tcm.lastUpdate > realmUpdateTime) realmUpdateTime = tcm.lastUpdate;
        }

        if (isVisible)
        {
            setRealmListener(realmUpdateTime);
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
        if (view.getId() == R.id.try_again) {
            viewModel.prepare();
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (realmUpdates != null)
        {
            realmUpdates.removeAllChangeListeners();
        }
        if (realm != null && !realm.isClosed()) realm.close();
        if (adapter != null && recyclerView != null) adapter.onDestroy(recyclerView);
    }

    public void resetTokens()
    {
        if (viewModel != null && adapter != null)
        {
            //reload tokens
            viewModel.reloadTokens();

            handler.post(() -> {
                //first abort the current operation
                adapter.clear();
                //show syncing
                addressAvatar.setWaiting();
            });
        }
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

    final ActivityResultLauncher<Intent> tokenManagementLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getData() == null) return;
                ArrayList<ContractLocator> tokenData = result.getData().getParcelableArrayListExtra(ADDED_TOKEN);
                Bundle b = new Bundle();
                b.putParcelableArrayList(C.ADDED_TOKEN, tokenData);
                getParentFragmentManager().setFragmentResult(C.ADDED_TOKEN, b);
            });

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

    @Override
    public void avatarFound(Wallet wallet)
    {
        //write to database
        viewModel.saveAvatar(wallet);
    }

    public class SwipeCallback extends ItemTouchHelper.SimpleCallback {
        private final TokensAdapter mAdapter;
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
            if (viewHolder instanceof WarningHolder)
            {
                remindMeLater(viewModel.getWallet());
            }
            else if (viewHolder instanceof TokenHolder)
            {
                Token token = ((TokenHolder) viewHolder).token;
                viewModel.setTokenEnabled(token, false);
                adapter.removeToken(token.tokenInfo.chainId, token.getAddress());

                if (getContext() != null)
                {
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

        @Override
        public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            if (viewHolder.getItemViewType() == TokenHolder.VIEW_TYPE)
            {
                Token t = ((TokenHolder)viewHolder).token;
                if (t != null && t.isEthereum()) return 0;
            }
            else
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
        NotificationView notificationView = view.findViewById(R.id.notification);
        boolean hasShownWarning = viewModel.isMarshMallowWarningShown();

        if (!hasShownWarning && android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            notificationView.setNotificationBackgroundColor(R.color.indigo);
            notificationView.setTitle(getContext().getString(R.string.title_version_support_warning));
            notificationView.setMessage(getContext().getString(R.string.message_version_support_warning));
            notificationView.setPrimaryButtonText(getContext().getString(R.string.hide_notification));
            notificationView.setPrimaryButtonListener(() -> {
                notificationView.setVisibility(View.GONE);
                viewModel.setMarshMallowWarning(true);
            });
        } else {
            notificationView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSearchClicked()
    {
        if (getView() != null && getHost() != null && getChildFragmentManager().findFragmentByTag(SEARCH_FRAGMENT) == null)
        {
            getView().clearFocus();
            Fragment f = new TokenSearchFragment();
            getChildFragmentManager().beginTransaction()
                    .add(R.id.coordinator, f, SEARCH_FRAGMENT)
                    .commit();
        }
    }
}
