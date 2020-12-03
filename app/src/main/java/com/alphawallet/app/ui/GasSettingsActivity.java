package com.alphawallet.app.ui;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.GasPriceSpread;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.app.repository.entity.RealmGasSpread;
import com.alphawallet.app.repository.entity.RealmTokenTicker;
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.ui.widget.divider.ListDivider;
import com.alphawallet.app.ui.widget.entity.GasSettingsCallback;
import com.alphawallet.app.ui.widget.entity.GasSpeed;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.GasSettingsViewModel;
import com.alphawallet.app.viewmodel.GasSettingsViewModelFactory;
import com.alphawallet.app.widget.GasSliderView;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.realm.Realm;
import io.realm.Sort;

import static com.alphawallet.app.repository.EthereumNetworkBase.MAINNET_ID;
import static com.alphawallet.app.repository.EthereumNetworkBase.POA_ID;

public class GasSettingsActivity extends BaseActivity implements GasSettingsCallback
{
    @Inject
    GasSettingsViewModelFactory viewModelFactory;
    GasSettingsViewModel viewModel;

    private GasSliderView gasSliderView;
    private RecyclerView recyclerView;
    private CustomAdapter adapter;
    private RealmGasSpread realmGasSpread;
    private final Handler handler = new Handler();

    private final List<GasSpeed> gasSpeeds = new ArrayList<>();
    private int currentGasSpeedIndex = -1;
    private int chainId;
    private BigDecimal gasLimit;

    private BigDecimal customGasLimit = BigDecimal.ZERO;
    private BigDecimal customGasPrice = BigDecimal.ZERO;
    private int customIndex = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_gas_settings);
        toolbar();
        setTitle(R.string.set_speed_title);

        gasSliderView = findViewById(R.id.gasSliderView);
        recyclerView = findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(GasSettingsViewModel.class);

        currentGasSpeedIndex = getIntent().getIntExtra(C.EXTRA_SINGLE_ITEM, -1);
        chainId = getIntent().getIntExtra(C.EXTRA_CHAIN_ID, MAINNET_ID);
        gasLimit = new BigDecimal(getIntent().getStringExtra(C.EXTRA_GAS_LIMIT));
        customGasPrice = new BigDecimal(getIntent().getStringExtra(C.EXTRA_GAS_PRICE));
        gasSliderView.setNonce(getIntent().getLongExtra(C.EXTRA_NONCE, -1));
        gasSliderView.initGasLimit(gasLimit.toBigInteger());
        gasSliderView.initGasPrice(customGasPrice.toBigInteger());

        adapter = new CustomAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new ListDivider(this));
        gasSliderView.setCallback(this);

        // start listening for gas price updates
        startGasListener();
    }

    private void startGasListener()
    {
        realmGasSpread = viewModel.getTickerRealm().where(RealmGasSpread.class)
                .equalTo("chainId", chainId)
                .sort("timeStamp", Sort.DESCENDING)
                .findFirstAsync();

        realmGasSpread.addChangeListener(realmToken -> {
            RealmGasSpread rgs = (RealmGasSpread) realmToken;
            GasPriceSpread gs = rgs.getGasPrice();
            currentGasSpeedIndex = gs.setupGasSpeeds(this, gasSpeeds, currentGasSpeedIndex);
            gasSliderView.initGasPriceMax(gasSpeeds.get(0).gasPrice);
            //if we have mainnet then show timings, otherwise no timing, if the token has fiat value, show fiat value of gas, so we need the ticker
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onResume()
    {
        super.onResume();
        viewModel.prepare();
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
        result.putExtra(C.EXTRA_GAS_PRICE, customGasPrice.toString());
        result.putExtra(C.EXTRA_GAS_LIMIT, customGasLimit.toString());
        result.putExtra(C.EXTRA_NONCE, gasSliderView.getNonce());
        result.putExtra(C.EXTRA_AMOUNT, gs.seconds);
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
        if (realmGasSpread != null) realmGasSpread.removeAllChangeListeners();
    }

    @Override
    public void gasSettingsUpdate(BigInteger gasPrice, BigInteger gasLimit)
    {
        if (customIndex < 0) return;
        GasSpeed gs = gasSpeeds.get(customIndex);
        //new settings from the slider widget
        gs = new GasSpeed(gs.speed, getExpectedTransactionTime(gasPrice), gasPrice);
        gasSpeeds.remove(customIndex);
        gasSpeeds.add(gs);

        customGasPrice = new BigDecimal(gasPrice);
        customGasLimit = new BigDecimal(gasLimit);
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
            final TextView speedCost;
            final TextView speedTime;
            final View itemLayout;

            CustomViewHolder(View view)
            {
                super(view);
                checkbox = view.findViewById(R.id.checkbox);
                speedName = view.findViewById(R.id.text_speed);
                speedCost = view.findViewById(R.id.text_speed_cost);
                speedTime = view.findViewById(R.id.text_speed_time);
                itemLayout = view.findViewById(R.id.layout_list_item);
                speedGwei = view.findViewById(R.id.text_gwei);
            }
        }

        private CustomAdapter(Context ctx)
        {
            baseCurrency = viewModel.getBaseCurrencyToken(chainId);
            context = ctx;
        }

        @Override
        public void onBindViewHolder(CustomAdapter.CustomViewHolder holder, int position)
        {
            BigDecimal useGasLimit = gasLimit;
            GasSpeed gs = gasSpeeds.get(position);
            holder.speedName.setText(gs.speed);
            holder.checkbox.setSelected(position == currentGasSpeedIndex);
            holder.itemLayout.setOnClickListener(v -> {
                if (position == customIndex && currentGasSpeedIndex != customIndex)
                {
                    gasSliderView.initGasLimit(gasLimit.toBigInteger());
                    gasSliderView.reportPosition();
                }
                currentGasSpeedIndex = position;
                notifyDataSetChanged();
            });

            String speedGwei = context.getString(R.string.bracketed, BalanceUtils.weiToGweiBI(gs.gasPrice).toBigInteger().toString());

            if (gs.speed.equals(context.getString(R.string.speed_custom)))
            {
                customIndex = position;
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
            }

            String gasAmountInBase = BalanceUtils.getScaledValueScientific(new BigDecimal(gs.gasPrice).multiply(useGasLimit), baseCurrency.tokenInfo.decimals);
            if (gasAmountInBase.equals("0")) gasAmountInBase = "0.0001";
            String displayStr = context.getString(R.string.gas_amount, gasAmountInBase, baseCurrency.getSymbol());
            String displayTime = context.getString(R.string.gas_time_suffix,
                    Utils.shortConvertTimePeriodInSeconds(gs.seconds, context));
            displayStr += getGasCost(gasAmountInBase);

            holder.speedGwei.setText(speedGwei);
            holder.speedCost.setText(displayStr);
            holder.speedTime.setText(displayTime);

            setCustomGasDetails(position);
        }

        private void blankCustomHolder(CustomViewHolder holder)
        {
            holder.speedGwei.setText(context.getString(R.string.bracketed, context.getString(R.string.set_your_speed)));
            holder.speedCost.setText("");
            holder.speedTime.setText("");
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
                    costStr = context.getString(R.string.gas_fiat_suffix,
                            TickerService.getCurrencyString(cryptoAmount * cryptoRate),
                            rtt.getCurrencySymbol());
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
                    gasSliderView.initGasPrice(gs.gasPrice);
                    notice.setVisibility(View.VISIBLE);
                    gasSliderView.setVisibility(View.GONE);
                }
            }
        }

        @Override
        public int getItemCount()
        {
            return gasSpeeds.size();
        }
    }

    public long getExpectedTransactionTime(BigInteger customGasPriceBI)
    {
        long expectedTime = gasSpeeds.get(0).seconds;
        if (gasSpeeds.size() > 2)
        {
            double customGasPrice = customGasPriceBI.doubleValue();
            //Extrapolate between adjacent price readings
            for (int index = 0; index < gasSpeeds.size() - 2; index++)
            {
                GasSpeed ug = gasSpeeds.get(index);
                GasSpeed lg = gasSpeeds.get(index + 1);
                double lowerBound = lg.gasPrice.doubleValue();
                double upperBound = ug.gasPrice.doubleValue();
                if (lowerBound <= customGasPrice && (upperBound >= customGasPrice))
                {
                    expectedTime = extrapolateTime(lg.seconds, ug.seconds, customGasPrice, lowerBound, upperBound);
                    double timeDiff = lg.seconds - ug.seconds;
                    double extrapolateFactor = (customGasPrice - lowerBound) / (upperBound - lowerBound);
                    expectedTime = (long) ((double) lg.seconds - extrapolateFactor * timeDiff);
                    break;
                }
                else if (lg.speed.equals(getString(R.string.speed_slow)) && customGasPrice < lowerBound)
                {
                    //danger zone - transaction may not complete
                    double dangerAmount = lowerBound / 2.0;
                    long dangerTime = 12 * DateUtils.HOUR_IN_MILLIS / 1000;

                    if (customGasPrice < dangerAmount)
                    {
                        expectedTime = -1; //never
                    }
                    else
                    {
                        expectedTime = extrapolateTime(dangerTime, lg.seconds, customGasPrice, dangerAmount, lowerBound);
                    }
                    break;
                }
            }
        }

        return expectedTime;
    }

    private long extrapolateTime(long longTime, long shortTime, double customPrice, double lowPrice, double highPrice)
    {
        double timeDiff = longTime - shortTime;
        double extrapolateFactor = (customPrice - lowPrice) / (highPrice - lowPrice);
        return (long) ((double) longTime - extrapolateFactor * timeDiff);
    }
}
