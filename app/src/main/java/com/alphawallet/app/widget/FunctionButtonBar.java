package com.alphawallet.app.widget;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.ui.widget.OnTokenClickListener;
import com.alphawallet.app.ui.widget.adapter.NonFungibleTokenAdapter;
import com.alphawallet.app.util.Utils;
import com.alphawallet.token.entity.TSAction;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.os.VibrationEffect.DEFAULT_AMPLITUDE;

public class FunctionButtonBar extends LinearLayout implements AdapterView.OnItemClickListener, OnTokenClickListener, View.OnClickListener {
    private final Context context;
    private Map<String, TSAction> functions;
    private NonFungibleTokenAdapter adapter;
    private List<BigInteger> selection = new ArrayList<>();
    private StandardFunctionInterface callStandardFunctions;
    private int buttonCount;
    private Token token = null;

    private Button primaryButton;
    private Button secondaryButton;
    private ImageButton moreButton;

    private BottomSheetDialog bottomSheet;
    private ListView moreActionsListView;
    private List<String> moreActionsList;
    ArrayAdapter<String> moreActionsAdapter;

    public FunctionButtonBar(Context ctx) {
        this(ctx, null);
    }

    public FunctionButtonBar(Context ctx, @Nullable AttributeSet attrs) {
        super(ctx, attrs);
        inflate(ctx, R.layout.layout_function_buttons, this);
        context = ctx;
        initializeViews();
    }

    private void initializeViews() {
        primaryButton = findViewById(R.id.primary_button);
        secondaryButton = findViewById(R.id.secondary_button);
        moreButton = findViewById(R.id.more_button);

        bottomSheet = new BottomSheetDialog(getContext());
        bottomSheet.setCancelable(true);
        bottomSheet.setCanceledOnTouchOutside(true);
        moreActionsListView = new ListView(getContext());
        moreActionsList = new ArrayList<>();
        moreActionsAdapter = new ArrayAdapter<>(getContext(),
                R.layout.item_action, android.R.id.text1, moreActionsList);
        moreActionsListView.setAdapter(moreActionsAdapter);
        bottomSheet.setContentView(moreActionsListView);
        moreActionsListView.setOnItemClickListener(this);
        moreActionsListView.setDivider(new ColorDrawable(ContextCompat.getColor(context, R.color.mercury)));
        moreActionsListView.setDividerHeight(Utils.dp2px(context, 1));
    }

    private void resetButtonCount() {
        buttonCount = 0;
        moreActionsList.clear();
        moreActionsAdapter.notifyDataSetChanged();
    }

    private void addFunction(String resource) {
        switch (buttonCount) {
            case 0: {
                primaryButton.setText(resource);
                primaryButton.setVisibility(View.VISIBLE);
                primaryButton.setOnClickListener(this);
                break;
            }
            case 1: {
                secondaryButton.setText(resource);
                secondaryButton.setVisibility(View.VISIBLE);
                secondaryButton.setOnClickListener(this);
                break;
            }
            default: {
                moreActionsList.add(resource);
                moreActionsAdapter.notifyDataSetChanged();
                moreButton.setVisibility(View.VISIBLE);
                moreButton.setOnClickListener(this);
            }
        }
        buttonCount++;
    }

    private void addFunction(int resourceId) {
        addFunction(context.getString(resourceId));
    }

    public void setupFunctions(StandardFunctionInterface functionInterface, List<Integer> functionResources) {
        callStandardFunctions = functionInterface;
        adapter = null;
        functions = null;
        resetButtonCount();

        for (int res : functionResources) {
            addFunction(res);
        }
    }

    public void setupFunctions(StandardFunctionInterface functionInterface, AssetDefinitionService assetSvs, Token token, NonFungibleTokenAdapter adp) {
        callStandardFunctions = functionInterface;
        adapter = adp;
        this.token = token;
        functions = assetSvs.getTokenFunctionMap(token.tokenInfo.chainId, token.getAddress());
        resetButtonCount();

        if (!token.isNonFungible()) addStandardTokenFunctions(token);

        if (functions != null && functions.size() > 0) {
            for (String function : functions.keySet()) {
                addFunction(function);
            }
        }

        if (token.isNonFungible()) addStandardTokenFunctions(token); //For non-fungible, include custom functions first - usually these are more frequently used

        findViewById(R.id.layoutButtons).setVisibility(View.GONE);
    }

    public void setupFunctionList(StandardFunctionInterface functionInterface, List<String> functionList) {
        callStandardFunctions = functionInterface;
        if (functions == null) functions = new HashMap<>();
        functions.clear();
        resetButtonCount();

        TSAction dummyAction = new TSAction();
        for (String func : functionList) {
            addFunction(func);
            functions.put(func, dummyAction);
        }
    }

    /**
     * Adds intrinsic token functions
     *
     * @param token
     */
    private void addStandardTokenFunctions(Token token) {
        switch (token.getInterfaceSpec()) {
            case ERC20:
            case ETHEREUM:
                addFunction(R.string.action_send);
                addFunction(R.string.action_receive);
                break;
            case ERC721:
            case ERC721_LEGACY:
                addFunction(R.string.action_transfer);
                break;
            case ERC721_TICKET:
                addFunction(R.string.action_use);
                addFunction(R.string.action_transfer);
                break;
            case ERC875:
            case ERC875_LEGACY:
                addFunction(R.string.action_use);
                addFunction(R.string.action_transfer);
                addFunction(R.string.action_sell);
                break;
            default:
                addFunction(R.string.action_receive);
                break;
        }
    }

    public void revealButtons() {
        findViewById(R.id.layoutButtons).setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        String action;
        if (v instanceof Button) { // Instance of 'primary' & 'secondary' buttons
            action = ((Button) v).getText().toString();
            handleAction(action);
        } else if (v instanceof ImageButton) { // Instance of 'menu' button
            bottomSheet.show();
        }
    }

    private void handleAction(String action) {
        if (functions != null && functions.containsKey(action)) {
            handleUseClick(action);
        } else {
            handleStandardFunctionClick(action);
        }
    }

    private void handleStandardFunctionClick(String action) {
        if (action.equals(context.getString(R.string.action_sell))) { //ERC875 only
            if (isSelectionValid()) callStandardFunctions.sellTicketRouter(selection);
        } else if (action.equals(context.getString(R.string.action_send))) { //Eth + ERC20
            callStandardFunctions.showSend();
        } else if (action.equals(context.getString(R.string.action_receive))) { //Everything
            callStandardFunctions.showReceive();
        } else if (action.equals(context.getString(R.string.action_transfer))) { //Any NFT
            if (isSelectionValid()) callStandardFunctions.showTransferToken(selection);
        } else if (action.equals(context.getString(R.string.action_use))) { //NFT with Redeem
            if (isSelectionValid()) callStandardFunctions.selectRedeemTokens(selection);
        } else {
            callStandardFunctions.handleClick(action);
        }
    }

    private void handleUseClick(String function) {
        if (functions != null && functions.containsKey(function)) {
            TSAction action = functions.get(function);
            //ensure we have sufficient tokens for selection
            if (!hasCorrectTokens(action)) {
                callStandardFunctions.displayTokenSelectionError(action);
            } else {
                callStandardFunctions.handleTokenScriptFunction(function, selection);
            }
        }
    }

    private boolean isSelectionValid() {
        List<BigInteger> selected = selection;
        if (adapter != null) selected = adapter.getSelectedTokenIds(selection);
        if (token == null || token.checkSelectionValidity(selected)) {
            return true;
        }
        else {
            displayInvalidSelectionError();
            return false;
        }
    }

    private boolean hasCorrectTokens(TSAction action) {
        //get selected tokens
        if (adapter == null) return true;
        List<BigInteger> selected = adapter.getSelectedTokenIds(selection);
        int groupings = adapter.getSelectedGroups();
        if (action.function != null) {
            int requiredCount = action.function.getTokenRequirement();
            if (requiredCount == 1 && selected.size() > 1 && groupings == 1) {
                BigInteger first = selected.get(0);
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

        if (findViewById(R.id.layoutButtons).getVisibility() != View.VISIBLE) {
            findViewById(R.id.layoutButtons).setVisibility(View.VISIBLE);
        }
    }

    private void displayInvalidSelectionError() {
        Toast.makeText(getContext(), "Invalid token selection", Toast.LENGTH_SHORT).show();
    }

    public void setSelection(List<BigInteger> idList) {
        selection = idList;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String action = moreActionsAdapter.getItem(position);
        handleAction(action);
        bottomSheet.hide();
    }

    public void setPrimaryButtonText(Integer resource) {
        if (resource != null) {
            primaryButton.setVisibility(View.VISIBLE);
            primaryButton.setText(resource);
        } else {
            primaryButton.setVisibility(View.GONE);
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
    }

    public void setSecondaryButtonEnabled(boolean enabled) {
        secondaryButton.setEnabled(enabled);
    }

    public void setPrimaryButtonClickListener(OnClickListener listener) {
        primaryButton.setOnClickListener(listener);
    }

    public void setSecondaryButtonClickListener(OnClickListener listener) {
        secondaryButton.setOnClickListener(listener);
    }
}
