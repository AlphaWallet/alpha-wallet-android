package com.alphawallet.app.widget;

/**
 * Created by JB on 15/01/2021.
 */

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.ActionSheetInterface;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionInput;
import com.alphawallet.app.web3.entity.Web3Transaction;

import org.jetbrains.annotations.NotNull;

public class TransactionDetailWidget extends LinearLayout
{
    private final TextView textTransactionSummary;
    private final TextView textFullDetails;
    private final LinearLayout layoutDetails;
    private final LinearLayout layoutHolder;
    private final LinearLayout layoutSummary;
    private ActionSheetInterface sheetInterface;

    public TransactionDetailWidget(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        inflate(context, R.layout.transaction_detail_widget, this);
        textTransactionSummary = findViewById(R.id.text_transaction_summary);
        textFullDetails = findViewById(R.id.text_full_details);
        layoutDetails = findViewById(R.id.layout_detail);
        layoutHolder = findViewById(R.id.layout_holder);
        layoutSummary = findViewById(R.id.layout_summary);
    }

    public void setupTransaction(Web3Transaction w3tx, long chainId, String walletAddress, String symbol,
                                 @NotNull ActionSheetInterface asIf)
    {
        layoutHolder.setVisibility(View.VISIBLE);
        textFullDetails.setText(w3tx.getFormattedTransaction(getContext(), chainId, symbol));
        sheetInterface = asIf;

        if (!TextUtils.isEmpty(w3tx.description))
        {
            textTransactionSummary.setText(w3tx.description);
        }
        else
        {
            TransactionInput transactionInput = Transaction.decoder.decodeInput(w3tx, chainId, walletAddress);
            textTransactionSummary.setText(transactionInput.buildFunctionCallText());
        }

        layoutHolder.setOnClickListener(v -> {
            if (layoutDetails.getVisibility() == View.GONE)
            {
                layoutDetails.setVisibility(View.VISIBLE);
                layoutSummary.setVisibility(View.GONE);
                sheetInterface.lockDragging(true);
            }
            else
            {
                layoutDetails.setVisibility(View.GONE);
                layoutSummary.setVisibility(View.VISIBLE);
                sheetInterface.lockDragging(false);
            }
        });
    }
}
