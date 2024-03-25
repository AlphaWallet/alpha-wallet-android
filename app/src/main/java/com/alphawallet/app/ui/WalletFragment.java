package com.alphawallet.app.ui;

import static android.app.Activity.RESULT_OK;
import static com.alphawallet.app.C.ADDED_TOKEN;
import static com.alphawallet.app.C.ErrorCode.EMPTY_COLLECTION;
import static com.alphawallet.app.C.Key.WALLET;
import static com.alphawallet.app.ui.HomeActivity.RESET_TOKEN_SERVICE;
import static com.alphawallet.app.ui.MyAddressActivity.KEY_ADDRESS;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.analytics.Analytics;
import com.alphawallet.app.entity.BackupOperationType;
import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.CustomViewSettings;
import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.ServiceSyncCallback;
import com.alphawallet.app.entity.TokenFilter;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.widget.TokensAdapterCallback;
import com.alphawallet.app.ui.widget.adapter.TokensAdapter;
import com.alphawallet.app.ui.widget.entity.AvatarWriteCallback;
import com.alphawallet.app.ui.widget.entity.SortedItem;
import com.alphawallet.app.ui.widget.entity.WarningData;
import com.alphawallet.app.ui.widget.holder.TokenGridHolder;
import com.alphawallet.app.ui.widget.holder.TokenHolder;
import com.alphawallet.app.ui.widget.holder.WarningHolder;
import com.alphawallet.app.util.LocaleUtils;
import com.alphawallet.app.viewmodel.WalletViewModel;
import com.alphawallet.app.walletconnect.AWWalletConnectClient;
import com.alphawallet.app.widget.BuyEthOptionsView;
import com.alphawallet.app.widget.LargeTitleView;
import com.alphawallet.app.widget.NotificationView;
import com.alphawallet.app.widget.ProgressView;
import com.alphawallet.app.widget.SystemView;
import com.alphawallet.app.widget.UserAvatar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import org.web3j.crypto.Keys;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by justindeguzman on 2/28/18.
 */
@AndroidEntryPoint
public class WalletFragment extends BaseFragment implements
        TokensAdapterCallback,
        View.OnClickListener,
        Runnable,
        AvatarWriteCallback,
        ServiceSyncCallback
{
    public static final String SEARCH_FRAGMENT = "w_search";
    private static final String TAG = "WFRAG";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private WalletViewModel viewModel;
    private SystemView systemView;
    private TokensAdapter adapter;
    private UserAvatar addressAvatar;
    private View selectedToken;
    private String importFileName;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout refreshLayout;
    private boolean isVisible;
    private TokenFilter currentTabPos = TokenFilter.ALL;
    private LargeTitleView largeTitleView;
    private ActivityResultLauncher<Intent> handleBackupClick;
    private ActivityResultLauncher<Intent> tokenManagementLauncher;
    private boolean completed = false;
    private boolean hasWCSession = false;

    @Inject
    AWWalletConnectClient awWalletConnectClient;

    private final ActivityResultLauncher<Intent> networkSettingsHandler = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result ->
            {
                //send instruction to restart tokenService
                getParentFragmentManager().setFragmentResult(RESET_TOKEN_SERVICE, new Bundle());
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {

        View view = inflater.inflate(R.layout.fragment_wallet, container, false);
        LocaleUtils.setActiveLocale(getContext()); // Can't be placed before above line

        if (CustomViewSettings.canAddTokens())
        {
            toolbar(view, R.menu.menu_wallet, this::onMenuItemClick);
        }
        else
        {
            toolbar(view);
        }

        initResultLaunchers();

        initViews(view);

        initViewModel();

        initList();

        initTabLayout(view);

        initNotificationView(view);

        setImportToken();

        viewModel.prepare();

        addressAvatar.setWaiting();

        getChildFragmentManager()
                .setFragmentResultListener(SEARCH_FRAGMENT, this, (requestKey, bundle) ->
                {
                    Fragment fragment = getChildFragmentManager().findFragmentByTag(SEARCH_FRAGMENT);
                    if (fragment != null && fragment.isVisible() && !fragment.isDetached())
                    {
                        fragment.onDetach();
                        getChildFragmentManager().beginTransaction()
                                .remove(fragment)
                                .commitAllowingStateLoss();
                    }
                });

        return view;
    }

    private void initResultLaunchers()
    {
        tokenManagementLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result ->
                {
                    if (result.getData() == null) return;
                    ArrayList<ContractLocator> tokenData = result.getData().getParcelableArrayListExtra(ADDED_TOKEN);
                    Bundle b = new Bundle();
                    b.putParcelableArrayList(C.ADDED_TOKEN, tokenData);
                    getParentFragmentManager().setFragmentResult(C.ADDED_TOKEN, b);
                });

        handleBackupClick = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result ->
                {
                    String keyBackup = null;
                    boolean noLockScreen = false;
                    Intent data = result.getData();
                    if (data != null)
                    {
                        keyBackup = data.getStringExtra("Key");
                        data.getBooleanExtra("nolock", false);
                    }
                    Bundle backup = new Bundle();
                    backup.putBoolean(C.HANDLE_BACKUP, result.getResultCode() == RESULT_OK);
                    backup.putString("Key", keyBackup);
                    backup.putBoolean("nolock", noLockScreen);
                    getParentFragmentManager().setFragmentResult(C.HANDLE_BACKUP, backup);
                });
    }

    class CompletionLayoutListener extends LinearLayoutManager
    {
        public CompletionLayoutListener(Context context)
        {
            super(context);
        }

        public CompletionLayoutListener(FragmentActivity activity, int orientation, boolean reverseLayout)
        {
            super(activity, orientation, reverseLayout);
        }

        @Override
        public void onLayoutCompleted(RecyclerView.State state)
        {
            super.onLayoutCompleted(state);
            final int firstVisibleItemPosition = findFirstVisibleItemPosition();
            final int lastVisibleItemPosition = findLastVisibleItemPosition();
            int itemsShown = lastVisibleItemPosition - firstVisibleItemPosition + 1;
            if (!completed && itemsShown > 1)
            {
                completed = true;
                viewModel.startUpdateListener();
                viewModel.getTokensService().startUpdateCycleIfRequired();
            }
        }
    }

    private void initList()
    {
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
        recyclerView.setLayoutManager(new CompletionLayoutListener(getActivity(), LinearLayoutManager.VERTICAL, false));
    }

    private void initViewModel()
    {
        viewModel = new ViewModelProvider(this)
                .get(WalletViewModel.class);
        viewModel.progress().observe(getViewLifecycleOwner(), systemView::showProgress);
        viewModel.tokens().observe(getViewLifecycleOwner(), this::onTokens);
        viewModel.backupEvent().observe(getViewLifecycleOwner(), this::backupEvent);
        viewModel.defaultWallet().observe(getViewLifecycleOwner(), this::onDefaultWallet);
        viewModel.onFiatValues().observe(getViewLifecycleOwner(), this::updateValue);
        viewModel.onUpdatedTokens().observe(getViewLifecycleOwner(), this::updateMetas);
        viewModel.removeDisplayTokens().observe(getViewLifecycleOwner(), this::removeTokens);
        viewModel.getTokensService().startWalletSync(this);
        viewModel.activeWalletConnectSessions().observe(getViewLifecycleOwner(), walletConnectSessionItems -> {
            hasWCSession = !walletConnectSessionItems.isEmpty();
            adapter.showActiveWalletConnectSessions(walletConnectSessionItems);
        });
    }

    private void initViews(@NonNull View view)
    {
        refreshLayout = view.findViewById(R.id.refresh_layout_wallet);
        systemView = view.findViewById(R.id.system_view);
        recyclerView = view.findViewById(R.id.list);
        addressAvatar = view.findViewById(R.id.user_address_blockie);
        addressAvatar.setVisibility(View.VISIBLE);

        systemView.showProgress(true);

        systemView.attachRecyclerView(recyclerView);
        systemView.attachSwipeRefreshLayout(refreshLayout);

        largeTitleView = view.findViewById(R.id.large_title_view);

        ((ProgressView) view.findViewById(R.id.progress_view)).hide();
    }

    private void onDefaultWallet(Wallet wallet)
    {
        if (CustomViewSettings.showManageTokens())
        {
            adapter.setWalletAddress(wallet.address);
        }

        addressAvatar.bind(wallet, this);
        addressAvatar.setVisibility(View.VISIBLE);

        addressAvatar.setOnClickListener(v ->
        {
            // open wallets activity
            viewModel.showManageWallets(getContext(), false);
        });

        //Do we display new user backup popup?
        Bundle result = new Bundle();
        result.putBoolean(C.SHOW_BACKUP, wallet.lastBackupTime > 0);
        getParentFragmentManager().setFragmentResult(C.SHOW_BACKUP, result); //reset tokens service and wallet page with updated filters

        addressAvatar.setWaiting();
    }

    private void updateMetas(TokenCardMeta[] metas)
    {
        if (metas.length > 0)
        {
            adapter.updateTokenMetas(metas);
            systemView.hide();
            viewModel.checkDeleteMetas(metas);
            viewModel.calculateFiatValues();
        }
    }

    public void updateAttestationMeta(TokenCardMeta tcm)
    {
        updateMetas(new TokenCardMeta[]{tcm});
        viewModel.checkRemovedMetas();
    }

    //Refresh value of wallet once sync is complete
    @Override
    public void syncComplete(TokensService svs, int syncCount)
    {
        if (syncCount > 0) handler.post(() -> addressAvatar.finishWaiting());
        svs.getFiatValuePair()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateValue)
                .isDisposed();

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
            int color = ContextCompat.getColor(requireContext(), changePercent < 0 ? R.color.negative : R.color.positive);
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
        handler.post(() ->
        {
            adapter.clear();
            viewModel.prepare();
            viewModel.notifyRefresh();
        });
    }

    private void removeTokens(Token[] tokensToRemove)
    {
        for (Token remove : tokensToRemove)
        {
            adapter.removeToken(remove.getDatabaseKey());
        }
    }

    @Override
    public void comeIntoFocus()
    {
        isVisible = true;
        if (completed)
        {
            viewModel.startUpdateListener();
            viewModel.getTokensService().startUpdateCycleIfRequired();
        }
        checkWalletConnect();
    }

    @Override
    public void leaveFocus()
    {
        isVisible = false;
        viewModel.stopUpdateListener();
        softKeyboardGone();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (isVisible)
        {
            viewModel.stopUpdateListener();
        }
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
                        recyclerView.setLayoutManager(new CompletionLayoutListener(getActivity()));
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
    public void onTokenClick(View view, Token token, List<BigInteger> ids, boolean selected)
    {
        if (selectedToken == null)
        {
            getParentFragmentManager().setFragmentResult(C.TOKEN_CLICK, new Bundle());
            selectedToken = view;
            /*Token clickOrigin = viewModel.getTokenFromService(token);
            if (clickOrigin == null || token.getInterfaceSpec() == ContractType.ATTESTATION)
            {
                clickOrigin = token;
            }*/
            viewModel.showTokenDetail(getActivity(), token);
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
    public void onBuyToken()
    {
        final BottomSheetDialog buyEthDialog = new BottomSheetDialog(getActivity());
        BuyEthOptionsView buyEthOptionsView = new BuyEthOptionsView(getActivity());
        buyEthOptionsView.setOnBuyWithRampListener(v -> {
            Intent intent = viewModel.getBuyIntent(getCurrentWallet().address);
            ((HomeActivity) getActivity()).onActivityResult(C.TOKEN_SEND_ACTIVITY, RESULT_OK, intent);
            viewModel.track(Analytics.Action.BUY_WITH_RAMP);
            buyEthDialog.dismiss();
        });
        buyEthOptionsView.setOnBuyWithCoinbasePayListener(v -> {
            viewModel.showBuyEthOptions(getActivity());
        });
        buyEthOptionsView.setDismissInterface(() -> {
            if (buyEthDialog != null && buyEthDialog.isShowing())
            {
                buyEthDialog.dismiss();
            }
        });
        buyEthDialog.setContentView(buyEthOptionsView);
        buyEthDialog.show();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        currentTabPos = TokenFilter.ALL;
        selectedToken = null;
        if (viewModel == null)
        {
            requireActivity().recreate();
            return;
        }
        else
        {
            viewModel.track(Analytics.Navigation.WALLET);
            if (largeTitleView != null)
            {
                largeTitleView.setVisibility(View.VISIBLE); //show or hide Fiat summary
            }
        }

        if (isVisible)
        {
            viewModel.startUpdateListener();
            viewModel.getTokensService().startUpdateCycleIfRequired();
        }

        checkWalletConnect();
    }

    private void checkWalletConnect()
    {
        if (adapter != null)
        {
            adapter.checkWalletConnect();
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

        if (currentTabPos.equals(TokenFilter.ALL))
        {
            checkWalletConnect();
        }
        else
        {
            adapter.showActiveWalletConnectSessions(Collections.emptyList());
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
            if (importToken != null)
                Toast.makeText(getContext(), importToken.address, Toast.LENGTH_LONG).show();
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
                wData.buttonText = getString(R.string.back_up_now);
                wData.colour = R.color.text_secondary;
                wData.wallet = viewModel.getWallet();
                adapter.addWarning(wData);
                break;
            case WALLET_HAS_HIGH_VALUE:
                wData = new WarningData(this);
                wData.title = getString(R.string.wallet_not_backed_up);
                wData.detail = getString(R.string.not_backed_up_detail);
                wData.buttonText = getString(R.string.back_up_now);
                wData.colour = R.color.error;
                wData.wallet = viewModel.getWallet();
                adapter.addWarning(wData);
                break;
        }
    }

    private void onError(ErrorEnvelope errorEnvelope)
    {
        if (errorEnvelope.code == EMPTY_COLLECTION)
        {
            systemView.showEmpty(getString(R.string.no_tokens));
        }
        else
        {
            systemView.showError(getString(R.string.error_fail_load_tokens), this);
        }
    }

    @Override
    public void onClick(View view)
    {
        if (view.getId() == R.id.try_again)
        {
            viewModel.prepare();
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        viewModel.stopUpdateListener();
        if (adapter != null && recyclerView != null) adapter.onDestroy(recyclerView);
    }

    @Override
    public void resetTokens()
    {
        if (viewModel != null && adapter != null)
        {
            //reload tokens
            refreshList();

            handler.post(() ->
            {
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
//        if (selectedToken != null && selectedToken.findViewById(R.id.token_layout) != null)
//        {
//            selectedToken.findViewById(R.id.token_layout).setBackgroundResource(R.drawable.background_marketplace_event);
//        }
        selectedToken = null;
    }

    @Override
    public void backUpClick(Wallet wallet)
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
        handler.post(() ->
        {
            if (viewModel != null) viewModel.setKeyWarningDismissTime(wallet.address);
            if (adapter != null) adapter.removeItem(WarningHolder.VIEW_TYPE);
        });
    }

    @Override
    public void storeWalletBackupTime(String backedUpKey)
    {
        handler.post(() ->
        {
            if (viewModel != null) viewModel.setKeyBackupTime(backedUpKey);
            if (adapter != null) adapter.removeItem(WarningHolder.VIEW_TYPE);
        });
    }

    @Override
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

    public Wallet getCurrentWallet()
    {
        return viewModel.getWallet();
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem)
    {
        if (menuItem.getItemId() == R.id.action_my_wallet)
        {
            viewModel.showMyAddress(requireActivity());
        }
        if (menuItem.getItemId() == R.id.action_scan)
        {
            Bundle b = new Bundle();
            b.putParcelableArrayList(C.QRCODE_SCAN, null);
            getParentFragmentManager().setFragmentResult(C.QRCODE_SCAN, b);
        }
        return super.onMenuItemClick(menuItem);
    }

    private void initNotificationView(View view)
    {
        NotificationView notificationView = view.findViewById(R.id.notification);
        notificationView.setVisibility(View.GONE);
    }

    @Override
    public void onSearchClicked()
    {
        Intent intent = new Intent(getActivity(), SearchActivity.class);
        networkSettingsHandler.launch(intent);
        //startActivity(intent);
    }

    @Override
    public void onWCClicked()
    {
        Intent intent = awWalletConnectClient.getSessionIntent(getContext());
        startActivity(intent);
    }

    @Override
    public boolean hasWCSession()
    {
        return hasWCSession || (awWalletConnectClient != null && awWalletConnectClient.hasWalletConnectSessions());
    }

    @Override
    public void onSwitchClicked()
    {
        Intent intent = new Intent(getActivity(), NetworkToggleActivity.class);
        intent.putExtra(C.EXTRA_SINGLE_ITEM, false);
        networkSettingsHandler.launch(intent);
    }

    public class SwipeCallback extends ItemTouchHelper.SimpleCallback
    {
        private final TokensAdapter mAdapter;
        private Drawable icon;
        private ColorDrawable background;

        SwipeCallback(TokensAdapter adapter)
        {
            super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
            mAdapter = adapter;
            if (getActivity() != null)
            {
                icon = ContextCompat.getDrawable(getActivity(), R.drawable.ic_hide_token);
                if (icon != null)
                {
                    icon.setTint(ContextCompat.getColor(getActivity(), R.color.error_inverse));
                }
                background = new ColorDrawable(ContextCompat.getColor(getActivity(), R.color.error));
            }
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1)
        {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int position)
        {
            if (viewHolder instanceof WarningHolder)
            {
                remindMeLater(viewModel.getWallet());
            }
            else if (viewHolder instanceof TokenHolder)
            {
                Token token = ((TokenHolder) viewHolder).token;
                SortedItem<TokenCardMeta> removedToken;
                String tokenName = "";
                if (token == null)
                {
                    //just delete this entry
                    String tokenKey = ((TokenHolder) viewHolder).tokenKey;
                    viewModel.removeTokenMetaItem(tokenKey);
                    removedToken = adapter.removeEntry(tokenKey);
                }
                else if (token.getInterfaceSpec() == ContractType.ATTESTATION)
                {
                    viewModel.deleteToken(token);
                    removedToken = adapter.removeToken(token.getDatabaseKey());
                    tokenName = token.tokenInfo.name;
                }
                else
                {
                    viewModel.setTokenEnabled(token, false);
                    removedToken = adapter.removeToken(token.getDatabaseKey());
                    tokenName = token.tokenInfo.name;
                }

                if (getContext() != null)
                {
                    Snackbar snackbar = Snackbar
                            .make(viewHolder.itemView, tokenName + " " + getContext().getString(R.string.token_hidden), Snackbar.LENGTH_LONG)
                            .setAction(getString(R.string.action_snackbar_undo), view ->
                            {
                                if (token != null)
                                {
                                    viewModel.setTokenEnabled(token, true);
                                    adapter.addToken(removedToken);
                                }
                            });

                    snackbar.show();
                }
            }
        }

        @Override
        public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder)
        {
            if (viewHolder.getItemViewType() == TokenHolder.VIEW_TYPE)
            {
                Token t = ((TokenHolder) viewHolder).token;
                if (t != null && t.isEthereum()) return 0;
            }
            else
            {
                return 0;
            }

            return super.getSwipeDirs(recyclerView, viewHolder);
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive)
        {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

            View itemView = viewHolder.itemView;
            int offset = 20;
            int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
            int iconTop = itemView.getTop() + (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
            int iconBottom = iconTop + icon.getIntrinsicHeight();

            if (dX > 0)
            {
                int iconLeft = itemView.getLeft() + iconMargin + icon.getIntrinsicWidth();
                int iconRight = itemView.getLeft() + iconMargin;
                icon.setBounds(iconRight, iconTop, iconLeft, iconBottom);
                background.setBounds(itemView.getLeft(), itemView.getTop(),
                        itemView.getLeft() + ((int) dX) + offset,
                        itemView.getBottom());
            }
            else if (dX < 0)
            {
                int iconLeft = itemView.getRight() - iconMargin - icon.getIntrinsicWidth();
                int iconRight = itemView.getRight() - iconMargin;
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                background.setBounds(itemView.getRight() + ((int) dX) - offset,
                        itemView.getTop(), itemView.getRight(), itemView.getBottom());
            }
            else
            {
                background.setBounds(0, 0, 0, 0);
            }

            background.draw(c);
            icon.draw(c);
        }
    }

    @Override
    public void onToolbarClicked(View view)
    {
        //can we do it this way?
        //copy address
        ClipboardManager clipboard = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(KEY_ADDRESS, Keys.toChecksumAddress(viewModel.getWalletAddr()));
        if (clipboard != null)
        {
            clipboard.setPrimaryClip(clip);
        }
    }
}
