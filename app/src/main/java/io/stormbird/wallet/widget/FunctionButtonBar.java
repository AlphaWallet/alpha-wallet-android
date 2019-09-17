package io.stormbird.wallet.widget;

import android.content.Context;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.stormbird.token.entity.TSAction;
import io.stormbird.wallet.C;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.ERC721Token;
import io.stormbird.wallet.entity.StandardFunctionInterface;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.ui.widget.OnTokenClickListener;
import io.stormbird.wallet.ui.widget.adapter.NonFungibleTokenAdapter;

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

        findViewById(R.id.layoutButtons).setVisibility(View.GONE);

        LinearLayout auxButtons = findViewById(R.id.buttons_aux);

        final Button[] buttons = new Button[6];
        buttons[0] = findViewById(R.id.button_use);
        buttons[1] = findViewById(R.id.button_sell);
        buttons[2] = findViewById(R.id.button_action1);
        buttons[3] = findViewById(R.id.button_action2);
        buttons[4] = findViewById(R.id.button_action3);
        buttons[5] = findViewById(R.id.button_transfer);

        for (Button b : buttons) b.setOnClickListener(this);

        switch (token.getInterfaceSpec())
        {
            default:
                break;
            case ERC721:
            case ERC721_LEGACY:
                findViewById(R.id.button_sell).setVisibility(View.GONE);
                break;
            case ERC875:
            case ERC875LEGACY:
                findViewById(R.id.button_use).setVisibility(View.VISIBLE);
                break;
        }

        if (functions != null && functions.size() > 0)
        {
            int index = 0;
            for (String function : functions.keySet())
            {
                while(index < 5 && buttons[index].getVisibility() != View.GONE)
                {
                    index++;
                }
                buttons[index].setVisibility(View.VISIBLE);
                buttons[index].setText(function);
                if (index == 2) auxButtons.setVisibility(View.VISIBLE);
                if (index == 4) break;
                index++;
            }
        }
    }

    public void revealButtons()
    {
        findViewById(R.id.layoutButtons).setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View v)
    {
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
                    case R.id.button_use:
                        callStandardFunctions.selectRedeemTokens(selection);
                        break;
                    case R.id.button_sell:
                        callStandardFunctions.sellTicketRouter(selection);
                        break;
                    case R.id.button_transfer:
                        callStandardFunctions.showTransferToken(selection);
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
}
