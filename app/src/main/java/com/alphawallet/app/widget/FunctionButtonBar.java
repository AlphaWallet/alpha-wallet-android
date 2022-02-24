package com.alphawallet.app.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.BuyCryptoInterface;
import com.alphawallet.app.entity.ItemClick;
import com.alphawallet.app.entity.OnRampContract;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.OnRampRepositoryType;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.ui.widget.NonFungibleAdapterInterface;
import com.alphawallet.app.ui.widget.TokensAdapterCallback;
import com.alphawallet.app.util.Utils;
import com.alphawallet.token.entity.TSAction;
import com.alphawallet.token.tools.TokenDefinition;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static android.os.VibrationEffect.DEFAULT_AMPLITUDE;
import static com.alphawallet.ethereum.EthereumNetworkBase.ARBITRUM_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.BINANCE_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MATIC_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.OPTIMISTIC_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.XDAI_ID;

public class FunctionButtonBar extends LinearLayout implements AdapterView.OnItemClickListener, TokensAdapterCallback, View.OnClickListener {
    private final Context context;
    private Map<String, TSAction> functions;
    private NonFungibleAdapterInterface adapter;
    private List<BigInteger> selection = new ArrayList<>();
    private StandardFunctionInterface callStandardFunctions;
    private BuyCryptoInterface buyFunctionInterface;
    private int buttonCount;
    private Token token = null;
    private boolean showButtons = false;

    private Button primaryButton;
    private Button secondaryButton;
    private RelativeLayout primaryButtonWrapper;
    private ProgressBar primaryButtonSpinner;
    private ImageButton moreButton;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private AssetDefinitionService assetService;
    private WalletType walletType = WalletType.NOT_DEFINED;

    private BottomSheetDialog bottomSheet;
    private ListView moreActionsListView;
    private List<ItemClick> moreActionsList;
    private FunctionItemAdapter moreActionsAdapter;
    private final Semaphore functionMapComplete = new Semaphore(1);

    private boolean hasBuyFunction;
    private OnRampRepositoryType onRampRepository;

    public FunctionButtonBar(Context ctx, @Nullable AttributeSet attrs)
    {
        super(ctx, attrs);
        inflate(ctx, R.layout.layout_function_buttons, this);
        context = ctx;
        initializeViews();
    }

    private void initializeViews()
    {
        primaryButton = findViewById(R.id.primary_button);
        primaryButtonWrapper = findViewById(R.id.primary_button_wrapper);
        primaryButtonSpinner = findViewById(R.id.primary_spinner);
        secondaryButton = findViewById(R.id.secondary_button);
        moreButton = findViewById(R.id.more_button);

        bottomSheet = new BottomSheetDialog(getContext(), R.style.FullscreenBottomSheetDialogStyle);
        bottomSheet.setCancelable(true);
        bottomSheet.setCanceledOnTouchOutside(true);
        moreActionsListView = new ListView(getContext());
        moreActionsList = new ArrayList<>();
        moreActionsAdapter = new FunctionItemAdapter(getContext(),
                R.layout.item_action, moreActionsList);
        moreActionsListView.setAdapter(moreActionsAdapter);
        bottomSheet.setContentView(moreActionsListView);
        moreActionsListView.setOnItemClickListener(this);
        moreActionsListView.setDivider(new ColorDrawable(ContextCompat.getColor(context, R.color.mercury)));
        moreActionsListView.setDividerHeight(Utils.dp2px(context, 1));
    }

    private void resetButtonCount() {
        buttonCount = 0;
        primaryButtonWrapper.setVisibility(View.GONE);
        secondaryButton.setVisibility(View.GONE);
        moreButton.setVisibility(View.GONE);
        moreActionsList.clear();
        moreActionsAdapter.notifyDataSetChanged();
    }

    public void setupFunctions(StandardFunctionInterface functionInterface, List<Integer> functionResources) {
        callStandardFunctions = functionInterface;
        adapter = null;
        functions = null;
        resetButtonCount();

        for (int res : functionResources) {
            addFunction(res);
        }

        //always show buttons
        findViewById(R.id.layoutButtons).setVisibility(View.VISIBLE);
    }

    public void setupSecondaryFunction(StandardFunctionInterface functionInterface, int functionNameResource)
    {
        callStandardFunctions = functionInterface;
        adapter = null;
        functions = null;
        resetButtonCount();
        buttonCount = 1;
        addFunction(functionNameResource);

        //always show buttons
        findViewById(R.id.layoutButtons).setVisibility(View.VISIBLE);
    }

    public void setupFunctions(StandardFunctionInterface functionInterface, AssetDefinitionService assetSvs, Token token, NonFungibleAdapterInterface adp, List<BigInteger> tokenIds) {
        callStandardFunctions = functionInterface;
        adapter = adp;
        selection = tokenIds;
        this.token = token;
        functions = assetSvs.getTokenFunctionMap(token.tokenInfo.chainId, token.getAddress());
        assetService = assetSvs;
        getFunctionMap(assetSvs);
    }

    /**
     * Use only for TokenScript function list
     * @param functionInterface
     * @param functionName function
     */
    public void setupFunctionList(StandardFunctionInterface functionInterface, String functionName) {
        callStandardFunctions = functionInterface;
        if (functions == null) functions = new HashMap<>();
        functions.clear();
        resetButtonCount();

        addFunction(functionName);
        functions.put(functionName, new TSAction());

        //always show buttons
        findViewById(R.id.layoutButtons).setVisibility(View.VISIBLE);
    }

    /**
     * Adds intrinsic token functions
     *
     * @param token
     */
    private void addStandardTokenFunctions(Token token) {
        if (token == null) return;
        for (Integer i : token.getStandardFunctions()) { addFunction(i); }
    }

    public void revealButtons()
    {
        showButtons = true;
    }

    @Override
    public void onClick(View v) {
        if (v instanceof Button) { // Instance of 'primary' & 'secondary' buttons
            Button button = (Button)v;
            debounceButton(button);
            handleAction(new ItemClick(button.getText().toString(), v.getId()));
        } else if (v instanceof ImageButton) { // Instance of 'menu' button
            bottomSheet.show();
        }
    }

    private void handleAction(ItemClick action) {
        if (functions != null && functions.containsKey(action.buttonText))
        {
            handleUseClick(action);
        }
        else if (action.buttonId == R.string.action_buy_crypto)
        {
            buyFunctionInterface.handleBuyFunction(token);
        }
        else if (action.buttonId == R.string.generate_payment_request) {
            buyFunctionInterface.handleGeneratePaymentRequest(token);
        }
        else
        {
            handleStandardFunctionClick(action);
        }
    }

    private void handleStandardFunctionClick(ItemClick action) {
        if (action.buttonId == R.string.action_sell)
        {
            if (isSelectionValid(action.buttonId)) callStandardFunctions.sellTicketRouter(selection);
        }
        else if (action.buttonId == R.string.action_send)
        {
            callStandardFunctions.showSend();
        }
        else if (action.buttonId == R.string.action_receive)
        {
            callStandardFunctions.showReceive();
        }
        else if (action.buttonId == R.string.action_transfer)
        {
            if (isSelectionValid(action.buttonId)) callStandardFunctions.showTransferToken(selection);
        }
        else if (action.buttonId == R.string.action_use)
        {
            if (isSelectionValid(action.buttonId)) callStandardFunctions.selectRedeemTokens(selection);
        }
        else
        {
            callStandardFunctions.handleClick(action.buttonText, action.buttonId);
        }
    }

    private void handleUseClick(ItemClick function) {
        if (functions != null && functions.containsKey(function.buttonText)) {
            TSAction action = functions.get(function.buttonText);
            //first check for availability
            if (!TextUtils.isEmpty(action.exclude))
            {
                String denialMessage = assetService.checkFunctionDenied(token, function.buttonText, selection);
                if (!TextUtils.isEmpty(denialMessage))
                {
                    callStandardFunctions.handleFunctionDenied(denialMessage);
                    return;
                }
            }

            //ensure we have sufficient tokens for selection
            if (!hasCorrectTokens(action)) {
                callStandardFunctions.displayTokenSelectionError(action);
            } else {
                List<BigInteger> selected = selection;
                if (adapter != null) selected = adapter.getSelectedTokenIds(selection);
                callStandardFunctions.handleTokenScriptFunction(function.buttonText, selected);
            }
        }
    }

    private boolean isSelectionValid(int buttonId) {
        List<BigInteger> selected = selection;
        if (adapter != null) selected = adapter.getSelectedTokenIds(selection);
        if (token == null || token.checkSelectionValidity(selected)) {
            return true;
        }
        else {
            displayInvalidSelectionError();
            flashButton(findViewById(buttonId));
            return false;
        }
    }

    private boolean hasCorrectTokens(TSAction action) {
        //get selected tokens
        if (adapter == null)
        {
            if (action.function != null) return action.function.getTokenRequirement() <= 1; //can't use multi-token with no selection adapter.
            else return true;
        }
        List<BigInteger> selected = adapter.getSelectedTokenIds(selection);
        int groupings = adapter.getSelectedGroups();
        if (action.function != null) {
            int requiredCount = action.function.getTokenRequirement();
            if (requiredCount == 1 && selected.size() > 1 && groupings == 1) {
                BigInteger first = getSelectedTokenId(selected);
                selected.clear();
                selected.add(first);
            }

            return selected.size() == requiredCount;
        }
        return true;
    }

    @Override
    public void onTokenClick(View view, Token token, List<BigInteger> tokenIds, boolean selected) {
        int maxSelect = 1;

        if (!selected && tokenIds.containsAll(selection)) {
            selection = new ArrayList<>();
        }

        if (!selected) return;

        if (functions != null) {
            //Wait for availability to complete
            waitForMapBuild();
            populateButtons(token, getSelectedTokenId(tokenIds));

            for (TSAction action : functions.values()) {
                if (action.function != null && action.function.getTokenRequirement() > maxSelect) {
                    maxSelect = action.function.getTokenRequirement();
                }
            }
        }

        if (maxSelect <= 1) {
            selection = tokenIds;
            if (adapter != null) adapter.setRadioButtons(true);
        }
    }

    @Override
    public void onLongTokenClick(View view, Token token, List<BigInteger> tokenIds) {
        //show radio buttons of all token groups
        if (adapter != null) adapter.setRadioButtons(true);

        selection = tokenIds;
        Vibrator vb = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vb != null && vb.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                VibrationEffect vibe = VibrationEffect.createOneShot(200, DEFAULT_AMPLITUDE);
                vb.vibrate(vibe);
            } else {
                //noinspection deprecation
                vb.vibrate(200);
            }
        }

        //Wait for availability to complete
        waitForMapBuild();

        populateButtons(token, getSelectedTokenId(tokenIds));
        showButtons();
    }

    private void waitForMapBuild()
    {
        //do we need to wait for the service to finish?
        if (!functionMapComplete.tryAcquire())
        {
            callStandardFunctions.showWaitSpinner(true);
            try
            {
                functionMapComplete.acquire();
                functionMapComplete.release();
            }
            catch (InterruptedException e)
            {
                Timber.e(e);
                functionMapComplete.release();
            }
            callStandardFunctions.showWaitSpinner(false);
        }
    }

    private void displayInvalidSelectionError() {
        Toast.makeText(getContext(), "Invalid token selection", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        bottomSheet.dismiss();
        ItemClick action = moreActionsAdapter.getItem(position);
        handleAction(action);
    }

    public void setPrimaryButtonText(Integer resource) {
        if (resource != null) {
            primaryButtonWrapper.setVisibility(View.VISIBLE);
            primaryButton.setText(resource);
        } else {
            primaryButtonWrapper.setVisibility(View.GONE);
        }
    }

    public void setSecondaryButtonText(Integer resource) {
        if (resource != null) {
            secondaryButton.setVisibility(View.VISIBLE);
            secondaryButton.setText(resource);
        } else {
            secondaryButton.setVisibility(View.GONE);
        }
    }

    public void setPrimaryButtonEnabled(boolean enabled) {
        primaryButton.setEnabled(enabled);
        if (enabled) primaryButtonSpinner.setVisibility(View.GONE);
    }

    public void setPrimaryButtonWaiting() {
        primaryButton.setEnabled(false);
        primaryButtonSpinner.setVisibility(View.VISIBLE);
    }

    public void setSecondaryButtonEnabled(boolean enabled) {
        secondaryButton.setEnabled(enabled);
    }

    public void setPrimaryButtonClickListener(OnClickListener listener) {
        primaryButton.setOnClickListener(listener);
    }

    private void debounceButton(final View v)
    {
        if (v == null) return;

        v.setEnabled(false);
        handler.postDelayed(() -> {
            v.setEnabled(true);
        }, 500);
    }

    /**
     * Indicate token input error
     *
     * @param button
     */
    private void flashButton(final Button button)
    {
        if (button == null) return;
        button.setBackgroundResource(R.drawable.button_round_error);
        handler.postDelayed(() -> {
            if (button.hashCode() == primaryButton.hashCode())
            {
                button.setBackgroundResource(R.drawable.selector_round_button);
            }
            else if (button.hashCode() == secondaryButton.hashCode())
            {
                button.setBackgroundResource(R.drawable.selector_round_button_secondary);
            }
        }, 500);
    }

    private void addFunction(ItemClick function) {
        switch (buttonCount) {
            case 0: {
                primaryButton.setText(function.buttonText);
                primaryButton.setId(function.buttonId);
                primaryButtonWrapper.setVisibility(View.VISIBLE);
                primaryButton.setOnClickListener(this);
                break;
            }
            case 1: {
                secondaryButton.setText(function.buttonText);
                secondaryButton.setId(function.buttonId);
                secondaryButton.setVisibility(View.VISIBLE);
                secondaryButton.setOnClickListener(this);
                break;
            }
            default: {
                moreActionsList.add(function);
                moreActionsAdapter.notifyDataSetChanged();
                moreButton.setVisibility(View.VISIBLE);
                moreButton.setOnClickListener(this);
            }
        }
        buttonCount++;
    }

    private void addFunction(String function) {
        addFunction(new ItemClick(function, 0));
    }

    private void addFunction(int resourceId) {
        addFunction(new ItemClick(context.getString(resourceId), resourceId));
    }

    public void setWalletType(WalletType type)
    {
        walletType = type;
    }

    private static class FunctionItemAdapter extends ArrayAdapter<ItemClick>
    {
        public FunctionItemAdapter(Context context, int resource, List<ItemClick> objects) {
            super(context, resource, 0, objects);
        }

        @SuppressLint("ViewHolder") @NotNull @Override
        public View getView(int position, View convertView, @NotNull ViewGroup parent) {
            ItemClick item = getItem(position);
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.item_action, parent, false);
            ((TextView)convertView.findViewById(android.R.id.text1)).setText(item.buttonText);
            return convertView;
        }
    }

    private void populateButtons(Token token, BigInteger tokenId)
    {
        if (token == null) return;

        resetButtonCount();

        Map<String, TSAction> availableFunctions = new HashMap<>();

        //TokenScript first:
        addTokenScriptFunctions(availableFunctions, token, tokenId);

        //If Token is Non-Fungible then display the custom functions first - usually these are more frequently used
        if (!token.isNonFungible())
        {
            addStandardTokenFunctions(token);
        }

        setupCustomTokenActions();

        //Add buy function
        if (hasBuyFunction)
        {
            addBuyFunction();
        }

        //now add the standard functions for NonFungibles (since these are lower priority)
        if (token.isNonFungible())
        {
            addStandardTokenFunctions(token);
        }

        findViewById(R.id.layoutButtons).setVisibility(View.GONE);

        if (!token.isNonFungible())
        {
            addFunction(new ItemClick(context.getString(R.string.generate_payment_request), R.string.generate_payment_request));
        }
    }

    private void addTokenScriptFunctions(Map<String, TSAction> availableFunctions, Token token, BigInteger tokenId)
    {
        TokenDefinition td = assetService.getAssetDefinition(token.tokenInfo.chainId, token.getAddress());

        if (td != null && tokenId != null && functions != null)
        {
            for (String actionName : functions.keySet())
            {
                if (token.isFunctionAvailable(tokenId, actionName))
                {
                    availableFunctions.put(actionName, functions.get(actionName));
                }
            }
        }
        else
        {
            availableFunctions = functions;
        }

        if (availableFunctions != null && availableFunctions.size() > 0)
        {
            SparseArray<String> actions = new SparseArray<>();
            for (String actionName : availableFunctions.keySet()) actions.put(availableFunctions.get(actionName).order, actionName);

            for (int i = 0; i < actions.size(); i++)
            {
                addFunction(new ItemClick(actions.get(actions.keyAt(i)), 0));
            }
        }
    }

    /**
     * Adds custom commands for known tokens
     */
    private boolean setupCustomTokenActions()
    {
        if (token.tokenInfo.chainId == MATIC_ID && token.isNonFungible()) {
            return false;
        }

        if (token.tokenInfo.chainId == MAINNET_ID)
        {
            switch (token.getAddress().toLowerCase())
            {
                case C.DAI_TOKEN:
                case C.SAI_TOKEN:
                    addFunction(R.string.convert_to_xdai);
                    return true;
                default:
                    if (token.isERC20() || token.isEthereum())
                    {
                        addFunction(R.string.swap);
                    }
                    return true;
            }
        }
        else if (token.tokenInfo.chainId == BINANCE_MAIN_ID
                || token.tokenInfo.chainId == OPTIMISTIC_MAIN_ID
                || token.tokenInfo.chainId == ARBITRUM_MAIN_ID)
        {
            if (token.isERC20() || token.isEthereum())
            {
                addFunction(R.string.swap);
                return true;
            }
        }
        else if (token.tokenInfo.chainId == MATIC_ID)
        {
            addFunction(R.string.swap_with_quickswap);
            return true;
        }
        return false;
    }

    private void getFunctionMap(AssetDefinitionService assetSvs)
    {
        try
        {
            functionMapComplete.acquire();
        }
        catch (InterruptedException e)
        {
            Timber.e(e);
        }

        //get the available map for this collection
        assetSvs.fetchFunctionMap(token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(availabilityMap -> setupTokenMap(token, availabilityMap), this::onMapFetchError)
                .isDisposed();
    }

    private void onMapFetchError(Throwable throwable)
    {
        findViewById(R.id.wait_buttons).setVisibility(View.GONE);
        functionMapComplete.release();
    }

    private BigInteger getSelectedTokenId(List<BigInteger> tokenIds)
    {
        return (tokenIds != null && tokenIds.size() > 0) ? tokenIds.get(0) : BigInteger.ZERO;
    }

    private void setupTokenMap(@NotNull Token token, Map<BigInteger, List<String>> availabilityMap)
    {
        token.setFunctionAvailability(availabilityMap);
        functionMapComplete.release();
        findViewById(R.id.wait_buttons).setVisibility(View.GONE);

        if (showButtons)
        {
            BigInteger tokenId = getSelectedTokenId(selection);
            populateButtons(token, tokenId);
            showButtons();
        }
    }

    private void showButtons()
    {
        if (BuildConfig.DEBUG || walletType != WalletType.WATCH)
        {
            handler.post(() -> {
                findViewById(R.id.layoutButtons).setVisibility(View.VISIBLE);

                if (BuildConfig.DEBUG && walletType == WalletType.WATCH)
                {
                    findViewById(R.id.text_debug).setVisibility(View.VISIBLE);
                }
            });
        }
    }

    public void setupBuyFunction(BuyCryptoInterface buyCryptoInterface, OnRampRepositoryType onRampRepository)
    {
        this.hasBuyFunction = true;
        this.buyFunctionInterface = buyCryptoInterface;
        this.onRampRepository = onRampRepository;
    }

    private void addBuyFunction()
    {
        if (token.tokenInfo.chainId == MAINNET_ID
                || token.tokenInfo.chainId == XDAI_ID)
        {
            addPurchaseVerb(token, onRampRepository);
        }
    }

    private void addPurchaseVerb(Token token, OnRampRepositoryType onRampRepository)
    {
        OnRampContract contract = onRampRepository.getContract(token);
        String symbol = contract.getSymbol().isEmpty()? context.getString(R.string.crypto) : token.tokenInfo.symbol;
        addFunction(new ItemClick(context.getString(R.string.action_buy_crypto, symbol), R.string.action_buy_crypto));
    }
}
