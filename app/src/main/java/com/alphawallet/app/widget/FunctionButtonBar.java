package com.alphawallet.app.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.ui.widget.OnTokenClickListener;
import com.alphawallet.app.ui.widget.adapter.NonFungibleTokenAdapter;
import com.alphawallet.token.entity.TSAction;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.os.VibrationEffect.DEFAULT_AMPLITUDE;

public class FunctionButtonBar extends LinearLayout implements OnTokenClickListener, View.OnClickListener, Runnable
{
    private final Context context;
    private Map<String, TSAction> functions;
    private final Handler handler;
    private NonFungibleTokenAdapter adapter;
    private boolean activeClick;
    private List<BigInteger> selection = new ArrayList<>();
    private StandardFunctionInterface callStandardFunctions;
    private LinearLayout currentHolder = null;
    private int buttonCount;

    public FunctionButtonBar(Context ctx)
    {
        super(ctx);
        context = ctx;
        handler = new Handler();
        activeClick = false;
    }

    public FunctionButtonBar(Context ctx, @Nullable AttributeSet attrs) {
        super(ctx, attrs);
        context = ctx;
        handler = new Handler();
        activeClick = false;
    }

    public void setupFunctions(StandardFunctionInterface functionInterface, AssetDefinitionService assetSvs, Token token, NonFungibleTokenAdapter adp)
    {
        callStandardFunctions = functionInterface;
        adapter = adp;
        functions = assetSvs.getTokenFunctionMap(token.tokenInfo.chainId, token.getAddress());
        removeAllViews();

        //add a new view in
        addNewButtonLine();
        int intrinsicButtonCount = getStaticButtonCount(token);
        int functionButtonCount = getFunctionButtonCount();

        if (functions != null && functions.size() > 0)
        {
            for (String function : functions.keySet())
            {
                addButton(function);
                functionButtonCount--;
                if ((functionButtonCount + intrinsicButtonCount) == 3) addNewButtonLine();
            }
        }

        switch (token.getInterfaceSpec())
        {
            case ERC20:
            case ETHEREUM:
                addButton(R.string.action_send);
                addButton(R.string.action_receive);
                break;
            default:
                break;
            case ERC721:
            case ERC721_LEGACY:
                addButton(R.string.action_transfer);
                break;
            case ERC875:
            case ERC875LEGACY:
                addButton(R.string.action_use);
                addButton(R.string.action_transfer);
                addButton(R.string.action_sell);
                break;
        }

        findViewById(R.id.layoutButtons).setVisibility(View.GONE);
    }

    private int getFunctionButtonCount()
    {
        if (functions != null && functions.size() > 0) return functions.size();
        else return 0;
    }

    private int getStaticButtonCount(Token token)
    {
        switch (token.getInterfaceSpec())
        {
            case ERC20:
            case ETHEREUM:
                return 2;
            case ERC721:
            case ERC721_LEGACY:
                return 1;
            case ERC875:
            case ERC875LEGACY:
                return 3;
            default:
                return 0;
        }
    }

    public void revealButtons()
    {
        findViewById(R.id.layoutButtons).setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View v)
    {
        if (!(v instanceof Button)) return; //ignore non-button click

        if (!activeClick)
        {
            activeClick = true;
            handler.postDelayed(this, 500);

            //this will be the user function
            String buttonText = ((Button) v).getText().toString();
            if (functions != null && functions.containsKey(buttonText))
            {
                handleUseClick(buttonText);
            }
            else
            {
                switch (v.getId())
                {
                    case R.string.action_sell:
                        callStandardFunctions.sellTicketRouter(selection);
                        break;
                    case R.string.action_send:
                        callStandardFunctions.showSend();
                        break;
                    case R.string.action_receive:
                        callStandardFunctions.showReceive();
                        break;
                    case R.string.action_transfer:
                        callStandardFunctions.showTransferToken(selection);
                        break;
                    case R.string.action_use:
                        callStandardFunctions.selectRedeemTokens(selection);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void handleUseClick(String function)
    {
        if (functions != null && functions.containsKey(function))
        {
            TSAction action = functions.get(function);
            //ensure we have sufficient tokens for selection
            if (!hasCorrectTokens(action))
            {
                callStandardFunctions.displayTokenSelectionError(action);
            }
            else
            {
                callStandardFunctions.handleTokenScriptFunction(function, selection);
            }
        }
    }

    @Override
    public void run()
    {
        activeClick = false;
    }

    private boolean hasCorrectTokens(TSAction action)
    {
        //get selected tokens
        if (adapter == null) return true;
        List<BigInteger> selected = adapter.getSelectedTokenIds(selection);
        int groupings = adapter.getSelectedGroups();
        if (action.function != null)
        {
            int requiredCount = action.function.getTokenRequirement();
            if (requiredCount == 1 && selected.size() > 1 && groupings == 1)
            {
                BigInteger first = selected.get(0);
                selected.clear();
                selected.add(first);
            }

            return selected.size() == requiredCount;
        }
        return true;
    }

    @Override
    public void onTokenClick(View view, Token token, List<BigInteger> tokenIds, boolean selected)
    {
        int maxSelect = 1;

        if (!selected && tokenIds.containsAll(selection))
        {
            selection = new ArrayList<>();
        }

        if (!selected) return;

        if (functions != null)
        {
            for (TSAction action : functions.values())
            {
                if (action.function != null && action.function.getTokenRequirement() > maxSelect)
                {
                    maxSelect = action.function.getTokenRequirement();
                }
            }
        }

        if (maxSelect <= 1)
        {
            selection = tokenIds;
            if (adapter != null) adapter.setRadioButtons(true);
        }
    }

    @Override
    public void onLongTokenClick(View view, Token token, List<BigInteger> tokenIds)
    {
        //show radio buttons of all token groups
        if (adapter != null) adapter.setRadioButtons(true);

        selection = tokenIds;
        Vibrator vb = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vb != null && vb.hasVibrator())
        {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                VibrationEffect vibe = VibrationEffect.createOneShot(200, DEFAULT_AMPLITUDE);
                vb.vibrate(vibe);
            }
            else
            {
                //noinspection deprecation
                vb.vibrate(200);
            }
        }

        if (findViewById(R.id.layoutButtons).getVisibility() != View.VISIBLE)
        {
            findViewById(R.id.layoutButtons).setVisibility(View.VISIBLE);
        }
    }

    //Button bar and button generator code

    private void addNewButtonLine()
    {
        currentHolder = new LinearLayout(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(4,0,4,4);
        params.weight = 1.0f;
        params.gravity = Gravity.TOP;
        currentHolder.setLayoutParams(params);
        currentHolder.setOrientation(LinearLayout.HORIZONTAL);
        addView(currentHolder);
        buttonCount = 0;
    }

    private void addButton(int resourceId)
    {
        addButton(context.getString(resourceId)).setId(resourceId);
    }

    private Button addButton(String functionName)
    {
        if (buttonCount >= 3) addNewButtonLine();
        if (functionName != null && functionName.length() > 14 && buttonCount > 1) addNewButtonLine();
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0,0,4,0);
        params.weight = 1;

        Button button = new Button(context);
        button.setText(functionName);
        button.setGravity(Gravity.CENTER);
        button.setBackgroundResource(R.drawable.selector_round_button);
        button.setTextColor(context.getColor(R.color.button_text_color));
        button.setElevation(4.0f);
        button.setPadding(0,15,0,15);
        button.setLayoutParams(params);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        button.setOnClickListener(this);
        Typeface typeface = ResourcesCompat.getFont(context, R.font.font_semibold);
        button.setTypeface(typeface);
        currentHolder.addView(button);
        buttonCount++;
        if (functionName != null && functionName.length() > 14) addNewButtonLine();
        return button;
    }

    public void setupFunctionList(StandardFunctionInterface functionInterface, List<String> functionList)
    {
        callStandardFunctions = functionInterface;
        if (functions == null) functions = new HashMap<>();
        functions.clear();
        removeAllViews();
        TSAction dummyAction = new TSAction();

        //add a new view in
        addNewButtonLine();
        for (String func : functionList)
        {
            addButton(func);
            functions.put(func, dummyAction);
        }
    }
}
