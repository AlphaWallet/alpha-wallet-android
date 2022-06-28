package com.alphawallet.app.widget;

import android.app.Activity;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.lifi.Quote;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.util.Hex;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import java.math.BigDecimal;
import java.math.BigInteger;

public class ConfirmSwapDialog extends BottomSheetDialog
{
    private LinearLayout infoLayout;
    private MaterialButton btnConfirm;
    private ConfirmSwapDialogEventListener listener;

    public ConfirmSwapDialog(@NonNull Activity activity)
    {
        super(activity);
        View view = View.inflate(getContext(), R.layout.dialog_confirm_swap, null);
        setContentView(view);
        infoLayout = view.findViewById(R.id.layout_info);
        btnConfirm = view.findViewById(R.id.btn_confirm);
    }

    public ConfirmSwapDialog(Activity activity, Quote quote, ConfirmSwapDialogEventListener listener)
    {
        this(activity, quote);
        this.listener = listener;
        btnConfirm.setOnClickListener(v -> listener.onConfirm());
    }

    public ConfirmSwapDialog(Activity activity, Quote quote)
    {
        this(activity);
        init(quote);
    }

    public void init(Quote quote)
    {
        if (quote != null)
        {
            infoLayout.removeAllViews();
            infoLayout.addView(buildNetworkDisplayWidget((int) quote.action.fromChainId));
//            infoLayout.addView(buildFeeWidget(quote)); // TODO: Determine fees
            infoLayout.addView(buildGasWidget(quote));
            infoLayout.addView(buildFromWidget(quote));
            infoLayout.addView(buildToWidget(quote));
        }
    }

    private NetworkDisplayWidget buildNetworkDisplayWidget(int chainId)
    {
        return new NetworkDisplayWidget(getContext(), chainId);
    }

    private SimpleSheetWidget buildFeeWidget(Quote quote)
    {
        SimpleSheetWidget widget = new SimpleSheetWidget(getContext(), "Fee");
        widget.setValue("TODO"); // TODO: How to calculate this?
        widget.setCaption("TODO"); // TODO
        return widget;
    }

    private SimpleSheetWidget buildGasWidget(Quote quote)
    {
        Quote.TransactionRequest request = quote.transactionRequest;
        String gas = Hex.hexToBigInteger(request.gasPrice, BigInteger.ZERO).toString();
        SimpleSheetWidget widget = new SimpleSheetWidget(getContext(), R.string.label_gas_price);
        widget.setValue(gas);
        return widget;
    }

    private SimpleSheetWidget buildFromWidget(Quote quote)
    {
        String srcAmt = BalanceUtils.getScaledValueFixed(new BigDecimal(quote.action.fromAmount), quote.action.fromToken.decimals, 4);
        String srcTkn = quote.action.fromToken.symbol;
        SimpleSheetWidget widget = new SimpleSheetWidget(getContext(), R.string.label_from);
        widget.setValue(getContext().getString(R.string.valueSymbol, srcAmt, srcTkn));
        return widget;
    }

    private SimpleSheetWidget buildToWidget(Quote quote)
    {
        String destAmt = BalanceUtils.getScaledValueFixed(new BigDecimal(quote.estimate.toAmountMin), quote.action.toToken.decimals, 4);
        String destTkn = quote.action.toToken.symbol;
        SimpleSheetWidget widget = new SimpleSheetWidget(getContext(), R.string.label_to);
        widget.setValue(getContext().getString(R.string.valueSymbol, destAmt, destTkn));
        return widget;
    }

    public void setEventListener(ConfirmSwapDialogEventListener listener)
    {
        this.listener = listener;
        btnConfirm.setOnClickListener(v -> listener.onConfirm());
    }

    public interface ConfirmSwapDialogEventListener
    {
        void onConfirm();
    }
}
