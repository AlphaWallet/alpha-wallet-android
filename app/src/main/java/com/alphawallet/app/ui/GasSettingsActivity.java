package com.alphawallet.app.ui;


import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.GasPriceSpread;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.app.repository.entity.RealmGasSpread;
import com.alphawallet.app.repository.entity.RealmTokenTicker;
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.ui.widget.divider.ListDivider;
import com.alphawallet.app.ui.widget.entity.GasSettingsCallback;
import com.alphawallet.app.ui.widget.entity.GasSpeed;
import com.alphawallet.app.ui.widget.entity.GasWarningLayout;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.GasSettingsViewModel;
import com.alphawallet.app.widget.GasSliderView;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.realm.Realm;
import io.realm.RealmQuery;

@AndroidEntryPoint
public class GasSettingsActivity extends BaseActivity implements GasSettingsCallback
{
    private static final int GAS_PRECISION = 5; //5 dp for gas

    GasSettingsViewModel viewModel;

    private GasSliderView gasSliderView;
    private RecyclerView recyclerView;
    private CustomAdapter adapter;
    private RealmGasSpread realmGasSpread;

    private final List<GasSpeed> gasSpeeds = new ArrayList<>();
    private int currentGasSpeedIndex = -1;
    private long chainId;
    private BigDecimal presetGasLimit;
    private BigDecimal customGasLimit;
    private BigDecimal availableBalance;
    private BigDecimal sendAmount;
    private BigInteger customGasPriceFromWidget;
    private GasWarningLayout gasWarning;
    private GasWarningLayout insufficientWarning;
    private long minGasPrice;
    private boolean gasWarningShown;

    private int customIndex = -1;

    private enum Warning
    {
        OFF,
        LOW,
        HIGH,
        INSUFFICIENT
    }

    private Warning warningType = Warning.OFF;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_gas_settings);
        toolbar();
        setTitle(R.string.set_speed_title);

        gasSliderView = findViewById(R.id.gasSliderView);
        recyclerView = findViewById(R.id.list);
        gasWarning = findViewById(R.id.gas_warning_bubble);
        insufficientWarning = findViewById(R.id.insufficient_bubble);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        viewModel = new ViewModelProvider(this)
                .get(GasSettingsViewModel.class);

        minGasPrice = getIntent().getLongExtra(C.EXTRA_MIN_GAS_PRICE, -1);
        if (minGasPrice > 0)
        {
            gasSliderView.setupResendSettings(minGasPrice);
            FrameLayout resendNote = findViewById(R.id.layout_resend_note);
            resendNote.setVisibility(View.VISIBLE);
        }

        currentGasSpeedIndex = getIntent().getIntExtra(C.EXTRA_SINGLE_ITEM, -1);
        chainId = getIntent().getLongExtra(C.EXTRA_CHAIN_ID, MAINNET_ID);
        customGasLimit = new BigDecimal(getIntent().getStringExtra(C.EXTRA_CUSTOM_GAS_LIMIT));
        presetGasLimit = new BigDecimal(getIntent().getStringExtra(C.EXTRA_GAS_LIMIT_PRESET));
        availableBalance = new BigDecimal(getIntent().getStringExtra(C.EXTRA_TOKEN_BALANCE));
        sendAmount = new BigDecimal(getIntent().getStringExtra(C.EXTRA_AMOUNT));
        gasSliderView.setNonce(getIntent().getLongExtra(C.EXTRA_NONCE, -1));
        gasSliderView.initGasLimit(customGasLimit.toBigInteger());
        customGasPriceFromWidget = new BigInteger(getIntent().getStringExtra(C.EXTRA_GAS_PRICE));

        gasSliderView.initGasPrice(customGasPriceFromWidget);
        adapter = new CustomAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new ListDivider(this));
        gasSliderView.setCallback(this);

        // start listening for gas price updates
        setupGasSpeeds();
        startGasListener();
        gasWarningShown = false;
    }

    private RealmQuery<RealmGasSpread> getGasQuery()
    {
        return viewModel.getTickerRealm().where(RealmGasSpread.class)
                .equalTo("chainId", chainId);
    }

    private void startGasListener()
    {
        if (realmGasSpread != null) realmGasSpread.removeAllChangeListeners();
        realmGasSpread = getGasQuery().findFirstAsync();
        realmGasSpread.addChangeListener(realmToken -> {
            if (realmGasSpread.isValid())
            {
                GasPriceSpread gs = ((RealmGasSpread) realmToken).getGasPrice();
                initGasSpeeds(gs);
                gasSettingsUpdate(gasSpeeds.get(customIndex).gasPrice, customGasLimit.toBigInteger());
            }
        });
    }

    private void setupGasSpeeds()
    {
        gasSpeeds.add(new GasSpeed(getString(R.string.speed_custom), GasPriceSpread.FAST_SECONDS, customGasPriceFromWidget, true));

        RealmGasSpread getGas = getGasQuery().findFirst();
        if (getGas != null)
        {
            initGasSpeeds(getGas.getGasPrice());
        }
    }

    private void initGasSpeeds(GasPriceSpread gs)
    {
        currentGasSpeedIndex = gs.setupGasSpeeds(this, gasSpeeds, currentGasSpeedIndex);
        customIndex = gs.getCustomIndex();
        gasSliderView.initGasPriceMax(gasSpeeds.get(0).gasPrice);
        if (customGasPriceFromWidget.equals(BigInteger.ZERO))
        {
            //use slow or average
            customGasPriceFromWidget = gs.standard;
            updateCustomElement(customGasPriceFromWidget, customGasLimit.toBigInteger());
            gasSliderView.initGasPrice(customGasPriceFromWidget);
        }

        //if we have mainnet then show timings, otherwise no timing, if the token has fiat value, show fiat value of gas, so we need the ticker
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed()
    {
        Intent result = new Intent();
        GasSpeed gs = gasSpeeds.get(currentGasSpeedIndex);
        result.putExtra(C.EXTRA_SINGLE_ITEM, currentGasSpeedIndex);
        result.putExtra(C.EXTRA_GAS_LIMIT, customGasLimit.toString());
        result.putExtra(C.EXTRA_NONCE, gasSliderView.getNonce());
        result.putExtra(C.EXTRA_AMOUNT, gs.seconds);
        if (customIndex >= 0 && customIndex < gasSpeeds.size())
        {
            result.putExtra(C.EXTRA_GAS_PRICE, gasSpeeds.get(customIndex).gasPrice.toString());
        }
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (realmGasSpread != null)
        {
            realmGasSpread.removeAllChangeListeners();
            if (!realmGasSpread.getRealm().isClosed()) realmGasSpread.getRealm().close();
        }
    }

    @Override
    public void gasSettingsUpdate(BigInteger gasPrice, BigInteger gasLimit)
    {
        if (customIndex < 0) return;
        updateCustomElement(gasPrice, gasLimit);
        adapter.notifyItemChanged(customIndex);
    }

    public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.CustomViewHolder>
    {
        private final Token baseCurrency;
        private final Context context;

        @Override
        public CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_gas_speed, parent, false);

            return new CustomViewHolder(itemView);
        }

        class CustomViewHolder extends RecyclerView.ViewHolder {
            final ImageView checkbox;
            final TextView speedName;
            final TextView speedGwei;
            final TextView speedCostEth;
            final TextView speedCostFiat;
            final TextView speedTime;
            final View itemLayout;

            final LinearLayout warning;
            final TextView warningText;

            CustomViewHolder(View view)
            {
                super(view);
                checkbox = view.findViewById(R.id.checkbox);
                speedName = view.findViewById(R.id.text_speed);
                speedCostFiat = view.findViewById(R.id.text_speed_cost);
                speedCostEth = view.findViewById(R.id.text_speed_cost_eth);
                speedTime = view.findViewById(R.id.text_speed_time);
                itemLayout = view.findViewById(R.id.layout_list_item);
                speedGwei = view.findViewById(R.id.text_gwei);

                warning = view.findViewById(R.id.layout_speed_warning);
                warningText = view.findViewById(R.id.text_speed_warning);
            }
        }

        private CustomAdapter(Context ctx)
        {
            baseCurrency = viewModel.getBaseCurrencyToken(chainId);
            context = ctx;
        }

        @Override
        public void onBindViewHolder(CustomAdapter.CustomViewHolder holder, int p)
        {
            BigDecimal useGasLimit = presetGasLimit;
            int position = holder.getAbsoluteAdapterPosition();
            GasSpeed gs = gasSpeeds.get(position);
            holder.speedName.setText(gs.speed);

            holder.speedName.setVisibility(View.VISIBLE);
            holder.warning.setVisibility(View.GONE);

            holder.checkbox.setSelected(position == currentGasSpeedIndex);
            holder.itemLayout.setOnClickListener(v -> {
                if (position == customIndex && currentGasSpeedIndex != customIndex)
                {
                    gasSliderView.initGasLimit(customGasLimit.toBigInteger());
                    gasSliderView.reportPosition();
                }
                else if (position != customIndex && currentGasSpeedIndex == customIndex)
                {
                    hideGasWarning();
                }
                notifyItemChanged(currentGasSpeedIndex);
                currentGasSpeedIndex = position;
                notifyItemChanged(position);
            });

            String speedGwei = BalanceUtils.weiToGweiBI(gs.gasPrice).toBigInteger().toString();

            if (position == customIndex)
            {
                if (gs.seconds == 0)
                {
                    blankCustomHolder(holder);
                    setCustomGasDetails(position);
                    return;
                }
                else
                {
                    //recalculate the custom speed every time it's updated
                    gs.seconds = getExpectedTransactionTime(gs.gasPrice);
                    speedGwei = context.getString(R.string.bracketed, context.getString(R.string.set_your_speed));
                    useGasLimit = customGasLimit;
                }

                holder.speedName.setVisibility(View.GONE);
                holder.warning.setVisibility(View.VISIBLE);

                switch (warningType)
                {
                    case OFF:
                        holder.warning.setVisibility(View.GONE);
                        holder.speedName.setVisibility(View.VISIBLE);
                        break;
                    case LOW:
                        holder.warningText.setText(R.string.speed_too_low);
                        break;
                    case HIGH:
                        holder.warningText.setText(R.string.speed_high_gas);
                        break;
                    case INSUFFICIENT:
                        holder.warningText.setText(R.string.insufficient_gas);
                        break;
                }
            }

            BigDecimal gasFee = new BigDecimal(gs.gasPrice).multiply(useGasLimit);

            String gasAmountInBase = BalanceUtils.getScaledValueScientific(gasFee, baseCurrency.tokenInfo.decimals, GAS_PRECISION);
            if (gasAmountInBase.equals("0")) gasAmountInBase = "0.00001"; //NB no need to allow for zero gas chains; this activity wouldn't appear
            String displayStr = context.getString(R.string.gas_amount, gasAmountInBase, baseCurrency.getSymbol());
            String displayTime = context.getString(R.string.gas_time_suffix,
                    Utils.shortConvertTimePeriodInSeconds(gs.seconds, context));
            String fiatStr = getGasCost(gasAmountInBase);

            holder.speedGwei.setText(context.getString(R.string.gas_price_widget, speedGwei));
            holder.speedCostEth.setText(context.getString(R.string.gas_fiat_suffix, gasAmountInBase, baseCurrency.getSymbol()));
            holder.speedTime.setText(displayTime);

            if (fiatStr.length() > 0)
            {
                holder.speedCostFiat.setVisibility(View.VISIBLE);
                holder.speedCostFiat.setText(fiatStr);
            }
            else
            {
                holder.speedCostFiat.setVisibility(View.GONE);
            }

            setCustomGasDetails(position);

            //This collapses the view if it's not required, eg for re-send transaction
            //This hides the views that aren't selectable due to gas too low
            if(minGasPrice > 0)
            {
                if(!gs.isCustom && gs.gasPrice.longValue() < minGasPrice)
                {
                    ViewGroup.LayoutParams params = holder.itemLayout.getLayoutParams();
                    params.height = 0;
                    holder.itemLayout.setLayoutParams(params);
                    holder.itemLayout.requestLayout();
                }
            }

            //determine if this amount can be used
            BigDecimal txCost = gasFee.add(sendAmount);
            checkInsufficientGas(txCost);
        }

        private void blankCustomHolder(CustomViewHolder holder)
        {
            holder.speedGwei.setText(context.getString(R.string.bracketed, context.getString(R.string.set_your_speed)));
            holder.speedCostEth.setText("");
            holder.speedCostFiat.setText("");
            holder.speedTime.setText("");

            holder.speedName.setVisibility(View.VISIBLE);
            holder.warning.setVisibility(View.GONE);
        }

        private String getGasCost(String gasAmountInBase)
        {
            String costStr = "";
            try (Realm realm = viewModel.getTickerRealm())
            {
                RealmTokenTicker rtt = realm.where(RealmTokenTicker.class)
                        .equalTo("contract", TokensRealmSource.databaseKey(chainId, "eth"))
                        .findFirst();

                if (rtt != null)
                {
                    //calculate equivalent fiat
                    double cryptoRate = Double.parseDouble(rtt.getPrice());
                    double cryptoAmount = Double.parseDouble(gasAmountInBase);
                    costStr = TickerService.getCurrencyString(cryptoAmount * cryptoRate);
                }
            }
            catch (Exception e)
            {
                //
            }

            return costStr;
        }

        private void setCustomGasDetails(int position)
        {
            if (position == currentGasSpeedIndex)
            {
                TextView notice = findViewById(R.id.text_notice);
                if (currentGasSpeedIndex == customIndex)
                {
                    notice.setVisibility(View.GONE);
                    gasSliderView.setVisibility(View.VISIBLE);
                }
                else
                {
                    GasSpeed gs = gasSpeeds.get(position);
                    gasSliderView.initGasPriceMax(gs.gasPrice);
                    gasSliderView.setVisibility(View.GONE);
                    hideGasWarning();

                    setGasMessage(notice);
                }
            }
        }

        @Override
        public int getItemCount()
        {
            return gasSpeeds.size();
        }
    }

    private void setGasMessage(TextView notice)
    {
        String oracleAPI = EthereumNetworkRepository.getGasOracle(chainId);
        if (!TextUtils.isEmpty(oracleAPI))
        {
            Pattern extractDomain = Pattern.compile("(https:\\/\\/)(api-?\\S?\\S?)(\\.)([a-z.]+)(\\/api\\?)", Pattern.MULTILINE);
            Matcher matcher = extractDomain.matcher(oracleAPI);
            if (matcher.find())
            {
                notice.setVisibility(View.VISIBLE);
                notice.setText(getString(R.string.gas_message, matcher.group(4)));
            }
        }
    }

    public long getExpectedTransactionTime(BigInteger customGasPriceBI)
    {
        long expectedTime = gasSpeeds.get(0).seconds;
        if (gasSpeeds.size() > 2)
        {
            double dGasPrice = customGasPriceBI.doubleValue();
            //Extrapolate between adjacent price readings
            for (int index = 0; index < gasSpeeds.size() - 2; index++)
            {
                GasSpeed ug = gasSpeeds.get(index);
                GasSpeed lg = gasSpeeds.get(index + 1);
                double lowerBound = lg.gasPrice.doubleValue();
                double upperBound = ug.gasPrice.doubleValue();
                if (lowerBound <= dGasPrice && (upperBound >= dGasPrice))
                {
                    double timeDiff = lg.seconds - ug.seconds;
                    double extrapolateFactor = (dGasPrice - lowerBound) / (upperBound - lowerBound);
                    expectedTime = (long) ((double) lg.seconds - extrapolateFactor * timeDiff);
                    break;
                }
                else if (lg.speed.equals(getString(R.string.speed_slow))) { //final entry
                    //danger zone - transaction may not complete
                    double dangerAmount = lowerBound / 2.0;
                    long dangerTime = 12 * DateUtils.HOUR_IN_MILLIS / 1000;

                    if (dGasPrice < (lowerBound*0.95)) //only show gas warning if less than 95% of slow
                    {
                        showGasWarning(false);
                    }

                    if (dGasPrice < dangerAmount) {
                        expectedTime = -1; //never
                    } else {
                        expectedTime = extrapolateTime(dangerTime, lg.seconds, dGasPrice, dangerAmount, lowerBound);
                    }

                    return expectedTime;
                }
                else if (ug.speed.equals(getString(R.string.speed_rapid)) && dGasPrice >= upperBound)
                {
                    if (dGasPrice > 1.4 * upperBound)
                    {
                        showGasWarning(true); //only show gas warning if greater than 140% of max needed gas
                    }
                    else
                    {
                        hideGasWarning();
                    }
                    return expectedTime; //exit here so we don't hit the speed_slow catcher
                }
            }
            hideGasWarning(); // Didn't need a gas warning: custom gas is within bounds
        }

        return expectedTime;
    }

    private long extrapolateTime(long longTime, long shortTime, double customPrice, double lowPrice, double highPrice)
    {
        double timeDiff = longTime - shortTime;
        double extrapolateFactor = (customPrice - lowPrice) / (highPrice - lowPrice);
        return (long) ((double) longTime - extrapolateFactor * timeDiff);
    }

    private void updateCustomElement(BigInteger gasPrice, BigInteger gasLimit)
    {
        if (customIndex < 0)
        {
            return;
        }
        GasSpeed gs = gasSpeeds.get(customIndex);
        //new settings from the slider widget
        gs = new GasSpeed(gs.speed, getExpectedTransactionTime(gasPrice), gasPrice, true);
        gasSpeeds.remove(customIndex);
        gasSpeeds.add(gs);

        this.customGasLimit = new BigDecimal(gasLimit);
    }

    private void showGasWarning(boolean high)
    {
        if (currentGasSpeedIndex != customIndex)
        {
            return;
        }

        displayGasWarning(); //if gas warning already showing, no need to take focus from user input

        TextView heading = findViewById(R.id.bubble_title);
        TextView body = findViewById(R.id.bubble_body);
        if (high)
        {
            warningType = Warning.HIGH;
            heading.setText(getString(R.string.high_gas_setting));
            body.setText(getString(R.string.body_high_gas));
        }
        else
        {
            warningType = Warning.LOW;
            heading.setText(getString(R.string.low_gas_setting));
            body.setText(getString(R.string.body_low_gas));
        }
    }

    private void displayGasWarning()
    {
        if (gasWarning.getVisibility() != View.VISIBLE) //no need to re-apply
        {
            gasWarningShown = true;
            gasWarning.setVisibility(View.VISIBLE);

            EditText gas_price_entry = findViewById(R.id.gas_price_entry);
            gas_price_entry.setTextColor(getColor(R.color.danger));
            gas_price_entry.setBackground(ContextCompat.getDrawable(this, R.drawable.background_text_edit_error));
        }
    }

    private void hideGasWarning()
    {
        if (gasWarningShown) //leave gas warning space, so we don't do a jump-scroll while user operates slider if warning is visible
        {
            gasWarning.setVisibility(View.INVISIBLE);
        }
        else
        {
            gasWarning.setVisibility(View.GONE);
        }

        warningType = Warning.OFF;

        EditText gas_price_entry = findViewById(R.id.gas_price_entry);
        gas_price_entry.setTextColor(getColor(R.color.dove));
        gas_price_entry.setBackground(AppCompatResources.getDrawable(this, R.drawable.background_password_entry));
    }

    private void checkInsufficientGas(BigDecimal txCost)
    {
        if (txCost.compareTo(availableBalance) > 0)
        {
            warningType = Warning.INSUFFICIENT;
            insufficientWarning.setVisibility(View.VISIBLE);
        }
        else
        {
            insufficientWarning.setVisibility(View.GONE);
        }

        if (insufficientWarning.getVisibility() == View.VISIBLE && gasWarning.getVisibility() == View.VISIBLE)
        {
            gasWarning.setVisibility(View.GONE);
        }
    }

}
