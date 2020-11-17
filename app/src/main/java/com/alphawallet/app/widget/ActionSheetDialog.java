package com.alphawallet.app.widget;

import android.app.Activity;
import android.content.Context;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by JB on 17/11/2020.
 */
class ActionSheetDialog extends BottomSheetDialog implements StandardFunctionInterface
{
    private final TextView balance;
    private final TextView newBalance;
    private final TextView speedText;
    private final TextView timeEstimate;
    private final TextView recipient;
    private final TextView amount;

    private final LinearLayout editClick;
    private final ImageView recipientDetails;
    private final ImageView cancelButton;

    private final Token token;
    private final TokensService tokensService;

    public ActionSheetDialog(@NonNull Activity activity, Web3Transaction tx, Token t, String destName, TokensService ts)
    {
        super(activity);
        setContentView(R.layout.dialog_action_sheet);

        balance = findViewById(R.id.text_balance);
        newBalance = findViewById(R.id.text_new_balance);
        speedText = findViewById(R.id.text_speed);
        timeEstimate = findViewById(R.id.text_time_estimate);
        recipient = findViewById(R.id.text_recipient);
        amount = findViewById(R.id.text_amount);

        editClick = findViewById(R.id.edit_click_layer);
        recipientDetails = findViewById(R.id.image_more);
        cancelButton = findViewById(R.id.image_close);

        token = t;
        tokensService = ts;

        Token baseCurrency = tokensService.getToken(t.tokenInfo.chainId, t.getWallet());

        balance.setText(baseCurrency.getStringBalance());

        FunctionButtonBar functionBar = findViewById(R.id.layoutButtons);
        functionBar.setupFunctions(this, new ArrayList<>(Collections.singletonList(R.string.action_confirm)));
        functionBar.revealButtons();
    }

    @Override
    public void handleClick(String action, int id)
    {
        
    }
}
