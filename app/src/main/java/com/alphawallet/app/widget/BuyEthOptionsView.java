package com.alphawallet.app.widget;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.LayoutRes;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.DialogDismissInterface;


public class BuyEthOptionsView extends FrameLayout implements View.OnClickListener
{
    private OnClickListener onBuyWithCoinbasePayListener;
    private OnClickListener onBuyWithRampListener;
    private DialogDismissInterface dismissInterface;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public BuyEthOptionsView(Context context)
    {
        this(context, R.layout.dialog_buy_eth_options);
    }

    public BuyEthOptionsView(Context context, @LayoutRes int layoutId)
    {
        super(context);
        init(layoutId);
    }

    private void init(@LayoutRes int layoutId)
    {
        LayoutInflater.from(getContext()).inflate(layoutId, this, true);
        findViewById(R.id.buy_with_coinbase_pay).setOnClickListener(this);
        findViewById(R.id.buy_with_ramp).setOnClickListener(this);

        //close after 30 seconds of inactivity
        handler.postDelayed(closePopup, C.STANDARD_POPUP_INACTIVITY_DISMISS);
    }

    private final Runnable closePopup = () -> dismissInterface.dismissDialog();

    @Override
    public void onClick(View view)
    {
        handler.removeCallbacks(closePopup);
        if (view.getId() == R.id.buy_with_coinbase_pay)
        {
            if (onBuyWithCoinbasePayListener != null)
            {
                onBuyWithCoinbasePayListener.onClick(view);
            }
        }
        else if (view.getId() == R.id.buy_with_ramp)
        {
            if (onBuyWithRampListener != null)
            {
                onBuyWithRampListener.onClick(view);
            }
        }
    }

    public void setOnBuyWithCoinbasePayListener(OnClickListener onClickListener)
    {
        this.onBuyWithCoinbasePayListener = onClickListener;
    }

    public void setOnBuyWithRampListener(OnClickListener onClickListener)
    {
        this.onBuyWithRampListener = onClickListener;
    }

    public void setDismissInterface(DialogDismissInterface dismissInterface)
    {
        this.dismissInterface = dismissInterface;
    }
}
