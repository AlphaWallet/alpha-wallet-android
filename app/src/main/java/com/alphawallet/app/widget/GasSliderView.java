package com.alphawallet.app.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import androidx.appcompat.widget.AppCompatSeekBar;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.ui.widget.entity.GasSettingsCallback;
import com.alphawallet.app.util.BalanceUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

public class GasSliderView extends RelativeLayout
{
    private float maxDefaultPrice = 8.0f * 10.0f; //8 Gwei

    private final EditText gasPriceValue;
    private final EditText gasLimitValue;
    private final EditText nonceValue;

    private final AppCompatSeekBar gasPriceSlider;
    private final AppCompatSeekBar gasLimitSlider;

    private float scaleFactor; //used to convert slider value (0-100) into gas price
    private float minimumPrice;  //minimum for slider
    private float gasLimitScaleFactor;
    private boolean limitInit = false;
    private final Handler handler = new Handler();
    private final FrameLayout note;

    private GasSettingsCallback gasCallback;

    public GasSliderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.item_gas_slider, this);

        calculateStaticScaleFactor();

        gasPriceSlider = findViewById(R.id.gas_price_slider);
        gasLimitSlider = findViewById(R.id.gas_limit_slider);
        gasLimitValue = findViewById(R.id.gas_limit_entry);
        gasPriceValue = findViewById(R.id.gas_price_entry);
        nonceValue = findViewById(R.id.nonce_entry);
        note = findViewById(R.id.layout_resend_note);
        minimumPrice = BalanceUtils.weiToGweiBI(BigInteger.valueOf(C.GAS_PRICE_MIN)).multiply(BigDecimal.TEN).floatValue();
        bindViews();
    }

    private void bindViews()
    {
        gasPriceSlider.setProgress(1);
        gasPriceSlider.setMax(100);
        gasLimitSlider.setMax(100);

        gasPriceSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                if (!fromUser) return;
                /*
                As seekbar only works on Integer values, and to support with decimal value,
                value which is set to seek bar is from 0 to 990.
                The selected progress then be divided with 10 and adding 1 to value will make the proper expected result.
                Adding 1 is necessary because value is not below 0.1

                For example, progress on seekbar is 150, then expected result is 16.0
                 */
                BigDecimal scaledGasPrice = BigDecimal.valueOf((progress * scaleFactor) + minimumPrice)
                        .divide(BigDecimal.TEN) //divide by ten because price from API is x10
                        .setScale(1, RoundingMode.HALF_DOWN); //to 1 dp

                gasPriceValue.setText(scaledGasPrice.toString());
                limitInit = true;
                updateGasControl();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        gasLimitSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser)
                {
                    BigDecimal scaledGasLimit = BigDecimal.valueOf((progress * gasLimitScaleFactor) + C.GAS_LIMIT_MIN)
                            .setScale(2, RoundingMode.HALF_DOWN); //to 2 dp

                    gasLimitValue.setText(scaledGasLimit.toBigInteger().toString());
                    limitInit = true;
                    updateGasControl();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        TextWatcher tw = new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void afterTextChanged(Editable editable)
            {
                if (gasLimitValue.hasFocus() || gasPriceValue.hasFocus())
                {
                    limitInit = true;
                    handler.removeCallbacks(null);
                    handler.postDelayed(() -> {
                        updateSliderSettingsFromText(); //ensure sliders reflect new values
                        updateGasControl();
                    },2000);
                }
            }
        };

        gasLimitValue.addTextChangedListener(tw);
        gasPriceValue.addTextChangedListener(tw);
    }

    public void setupResendSettings(long minPrice)
    {

        float candidateMinPrice = BalanceUtils.weiToGweiBI(BigInteger.valueOf(minPrice)).multiply(BigDecimal.TEN).floatValue();
        if (candidateMinPrice > minimumPrice)
        {
            minimumPrice = candidateMinPrice;
            calculateStaticScaleFactor();
        }
        //nonce must be fixed
        nonceValue.setEnabled(false);

        note.setVisibility(View.VISIBLE);
        bindViews();
    }

    private void updateGasControl()
    {
        //calculate wei price
        String gasPriceStr = gasPriceValue.getText().toString();
        String gasLimitStr = gasLimitValue.getText().toString();
        if (!TextUtils.isEmpty(gasPriceStr) && !TextUtils.isEmpty(gasLimitStr))
        {
            try
            {
                gasCallback.gasSettingsUpdate(BalanceUtils.gweiToWei(new BigDecimal(gasPriceStr)),
                        new BigInteger(gasLimitStr));
            }
            catch (Exception e)
            {
                //
            }
        }
    }

    //After user updates the gas settings, reflect the new values in the sliders
    private void updateSliderSettingsFromText()
    {
        String gasPriceStr = gasPriceValue.getText().toString();
        String gasLimitStr = gasLimitValue.getText().toString();

        try
        {
            BigDecimal gweiPrice = new BigDecimal(gasPriceStr);
            setPriceSlider(gweiPrice);
        }
        catch (Exception e)
        {
            // - user typed a number that couldn't be converted
        }

        try
        {
            BigDecimal gasLimitGwei = new BigDecimal(gasLimitStr);
            int progress = (int)((float)(gasLimitGwei.longValue() - C.GAS_LIMIT_MIN)/gasLimitScaleFactor);
            gasLimitSlider.setProgress(progress);
        }
        catch (Exception e)
        {
            // - no need to act
        }
    }

    @SuppressLint("SetTextI18n")
    public void initGasPrice(BigInteger price)
    {
        if (!limitInit)
        {
            BigDecimal gweiPrice = BalanceUtils.weiToGweiBI(price);
            gasPriceValue.setText(gweiPrice.setScale(1, RoundingMode.HALF_DOWN).toString());
            setPriceSlider(gweiPrice);
        }
    }

    public void initGasPriceMax(BigInteger maxPrice)
    {
        String gasPriceStr = gasPriceValue.getText().toString();
        if (!TextUtils.isEmpty(gasPriceStr) && !TextUtils.isDigitsOnly(gasPriceStr))
        {
            BigDecimal gweiPrice = new BigDecimal(gasPriceStr);
            BigDecimal maxDefault = BalanceUtils.weiToGweiBI(maxPrice).multiply(BigDecimal.valueOf(15.0));
            maxDefaultPrice = Math.max(maxDefaultPrice, maxDefault.floatValue());
            calculateStaticScaleFactor();
            setPriceSlider(gweiPrice);
        }
    }

    private void setPriceSlider(BigDecimal gweiPrice)
    {
        int progress = (int) (((gweiPrice.floatValue() * 10.0f) - minimumPrice) / scaleFactor);
        gasPriceSlider.setProgress(progress);
    }

    @SuppressLint("SetTextI18n")
    public void initGasLimit(BigInteger limit)
    {
        if (limitInit) return;
        gasLimitValue.setText(limit.toString());
        int progress = (int)((float)(limit.longValue() - C.GAS_LIMIT_MIN)/gasLimitScaleFactor);
        gasLimitSlider.setProgress(progress);
    }

    private void calculateStaticScaleFactor()
    {
        scaleFactor = (maxDefaultPrice - minimumPrice)/100.0f; //default scale factor
        gasLimitScaleFactor = (float)(C.GAS_LIMIT_MAX - C.GAS_LIMIT_MIN)/100.0f;
    }

    public void setCallback(GasSettingsCallback callback)
    {
        gasCallback = callback;
    }

    public long getNonce()
    {
        String nonce = nonceValue.getText().toString();
        if (!TextUtils.isEmpty(nonce) && TextUtils.isDigitsOnly(nonce))
        {
            return Long.parseLong(nonce);
        }
        else
        {
            return -1;
        }
    }

    public void setNonce(long nonce)
    {
        if (nonce >= 0)
        {
            nonceValue.setText(String.valueOf(nonce));
        }
    }

    public void reportPosition()
    {
        updateGasControl();
    }
}
