package com.alphawallet.app.widget;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.GasTransactionResponse;
import com.alphawallet.app.util.BalanceUtils;
import com.google.gson.Gson;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class GasSliderView extends RelativeLayout implements LifecycleObserver {

    /**
    Below object is used to set Animation duration for expand/collapse and rotate
     */
    private final int ANIMATION_DURATION = 300;

    /**
     * Duration to call Gas Price API
     */
    private final int API_CALL_PERIOD = 30;

    private Context context;
    private ImageView imgExpandCollapse;
    private TextView gasPriceValue;
    private TextView gasLimitValue;
    private TextView networkFeeValue;
    private TextView estimateTimeValue;

    private View layoutValueDetails;
    private AppCompatSeekBar gasPriceSlider;

    /**
     * Below object will identify whether GasSlider detail view is expanded or collapsed
     * If true, Expanded else Collapsed
     */
    private boolean isViewExpanded = true;

    /**
    Used to store gas price selected from Slider
     */
    private MutableLiveData<BigDecimal> gasPrice = new MutableLiveData<>();

    /**
     * Used to store gas limit
     */
    private MutableLiveData<BigInteger> gasLimit = new MutableLiveData<>();

    /**
     * Used to call API call of Gas Price Range
     */
    private OkHttpClient okHttpClient;

    /**
     * Used to drop existing API operation if lifecyle STOP event is executed
     */
    private Disposable disposable;

    /**
     * This is temporary storage of response so that user can have recent updated time whenever slider is moved
     */
    private GasTransactionResponse gasTransactionResponse;

    public GasSliderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        /*
        Below code will attache current view with activity/fragment lifecycle
         */
        ((LifecycleOwner)context).getLifecycle().addObserver(this);

        inflate(context, R.layout.item_gas_slider, this);

        bindViews();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void start() {
        disposable = Observable.interval(0, API_CALL_PERIOD, TimeUnit.SECONDS, Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .map(tick -> getGasTransactionTime(tick)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribe(response -> updateDetails(response, tick), this::showError).isDisposed())
                .doOnError(this::showError)
                .subscribe();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void stop() {
        if(disposable != null && !disposable.isDisposed()){
            disposable.dispose();
        }
    }

    private void bindViews() {
        imgExpandCollapse = findViewById(R.id.img_expand_collapse);
        gasPriceValue = findViewById(R.id.gas_price_value);
        gasLimitValue = findViewById(R.id.gas_limit_value);
        networkFeeValue = findViewById(R.id.network_fee_value);
        estimateTimeValue = findViewById(R.id.estimated_time_value);
        layoutValueDetails = findViewById(R.id.layout_value_details);
        gasPriceSlider = findViewById(R.id.gas_price_slider1);

        gasPrice.observe((FragmentActivity) context, this::setGasPrice);
        gasLimit.observe((FragmentActivity) context, this::setGasLimit);

        gasPrice.setValue(BigDecimal.ONE);
        gasLimit.setValue(BigInteger.valueOf(C.GAS_LIMIT_DEFAULT));

        gasPriceSlider.setProgress(gasPrice.getValue().intValue());

        imgExpandCollapse.setOnClickListener(v -> expandCollapseView());

        gasPriceSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                /*
                As seekbar only works on Integer values, and to support with decimal value,
                value which is set to seek bar is from 0 to 990.
                The selected progress then be divided with 10 and adding 1 to value will make the proper expected result.
                Adding 1 is necessary because value is not below 0.1

                For example, progress on seekbar is 150, then expected result is 16.0
                 */
                BigDecimal scaledProgress = BigDecimal.valueOf(progress / 10.0 + 1);
                gasPrice.setValue(scaledProgress);
                updateTimings();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        okHttpClient = new OkHttpClient.Builder()
                //.addInterceptor(new LogInterceptor())
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .build();
    }

    private void showError(Throwable throwable) {
        Log.d("Home","Error : " + throwable.getMessage());
    }

    /**
     * Method will update the details of Gas Estimated timing each interval the API got response.
     *
     * @param response API response holds the Gas Price Range and time factors
     * @param tick A count value by Observer used to indicate if it's a first call or subsequential
     */
    private void updateDetails(GasTransactionResponse response, Long tick) {
        //When interval start, it starts with Tick 0. So at this point, we can initialize the
        //Component with safelow and fastest value to slider
        if(tick == 0){
            initializeComponent(response);
        }

        this.gasTransactionResponse = response;
        updateTimings();
    }

    /**
     * Only initialize or set the progress when API is called for first time.
     * This method will set the progress to SafeLow Gas price
     * @param response
     */
    private void initializeComponent(GasTransactionResponse response)
    {
        float safeLow = response.getSafeLow() - 10;
        gasPriceSlider.setProgress((int)safeLow);
    }

    /**
     * This method will check for current progress and validate in which gap it is falling.
     * Based on the falling price range, it will take the estimated time factor.
     */
    private void updateTimings()
    {
        float currentProgress = gasPrice.getValue().floatValue();
        String estimatedTime = "";

        if(gasTransactionResponse != null) {
            Map<String, Float> priceRange = gasTransactionResponse.getResult();

            Float smallestFall = 0.0f;

            for (String key : priceRange.keySet()){
                Float floatKey = Float.parseFloat(key) / 10.0f;
                if(currentProgress <= floatKey){
                    if(smallestFall == 0.0f || floatKey < smallestFall){
                        smallestFall = floatKey;
                    }
                }
            }

            String resultKey = "" + (int) (smallestFall * 10);
            estimatedTime = convertGasEstimatedTime((int) (priceRange.get(resultKey) * 60));
        }

        estimateTimeValue.setText(estimatedTime);
    }

    /**
     * Used to convert seconds into formatted Minutes and Seconds
     *
     * @param seconds Value got from price range list
     * @return Formatted string to display
     */
    private String convertGasEstimatedTime(int seconds) {
        int minutes = seconds / 60;
        seconds = seconds % 60;

        if(minutes != 0) {
            if(seconds == 0) {
                return String.format("%d min 0 sec", minutes);
            }else{
                return String.format("%d min %02d sec", minutes, seconds);
            }
        }else{
            return String.format("%02d sec", seconds);
        }
    }

    /**
     * Used to check if given progress fall between lower and upper cap
     *
     * @param number Current progress
     * @param lower Lower value of price range
     * @param upper Upper value of price range
     * @return
     */
    public boolean isBetween(float number, float lower, float upper) {
        return lower <= number && number<= upper;
    }

    /**
     * Below method will call API to have Gas price estimated time values.
     * @param tick : this tick mark is used to identify whether to set constants on slider or not
     * @return GasTransactionResponse which holds time values
     */
    public Single<GasTransactionResponse> getGasTransactionTime(Long tick)
    {
        return Single.fromCallable(() -> {
            try
            {
                Request request = new Request.Builder()
                        .url("https://ethgasstation.info/json/ethgasAPI.json")
                        .get()
                        .build();
                okhttp3.Response response = okHttpClient.newCall(request).execute();
                String responseStr = response.body().string();

                return new Gson().fromJson(responseStr, GasTransactionResponse.class);
            }
            catch (Exception e)
            {
                return null;
            }
        });
    }

    /**
     * This is setter method when price is changed by the slider
     * @param price
     */
    public void setGasPrice(BigDecimal price) {
        String priceStr = price + " " + C.GWEI_UNIT;
        gasPriceValue.setText(priceStr);
        updateNetworkFee();
    }

    /**
     * This is setter method to apply Limit change
     * @param limit
     */
    public void setGasLimit(BigInteger limit) {
        gasLimitValue.setText(limit.toString());
        updateNetworkFee();
    }

    /**
     * Method used to format and set the network fee after calculation
     */
    private void updateNetworkFee() {
        BigDecimal gasPriceInEth = new BigDecimal(BalanceUtils.gweiToWei(gasPrice.getValue().multiply(new BigDecimal(gasLimit.getValue()))));
        String fee = "~" + BalanceUtils.weiToEth(gasPriceInEth).toPlainString() + " " + C.ETH_SYMBOL;
        networkFeeValue.setText(fee);
    }

    /**
     * This method is used to get Gas Price
     * @return BigDecimal Gas Price value selected from Gas Slider
     */
    public BigDecimal getGasPrice(){
        return gasPrice.getValue();
    }

    /**
     * This method is used to get Gas Limit
     * @return BigInteger Gas Limit for now it is constant to 90000
     */
    public BigInteger getGasLimit(){
        return gasLimit.getValue();
    }

    /**
     * Used to expand or collapse the view
     */
    private void expandCollapseView()
    {
        //Collapse view
        if(isViewExpanded)
        {
            int finalHeight = layoutValueDetails.getHeight();
            ValueAnimator valueAnimator = slideAnimator(finalHeight, 0, layoutValueDetails);
            valueAnimator.addListener(animatorListener);
            valueAnimator.start();
            rotateAnimator();
        }
        //Expand view
        else
        {
            layoutValueDetails.setVisibility(View.VISIBLE);

            int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

            layoutValueDetails.measure(widthSpec, heightSpec);
            int height = layoutValueDetails.getMeasuredHeight();
            ValueAnimator valueAnimator = slideAnimator(0, height, layoutValueDetails);
            valueAnimator.addListener(animatorListener);
            valueAnimator.start();
            rotateAnimator();
        }
    }

    private ValueAnimator slideAnimator(int start, int end, final View view) {

        final ValueAnimator animator = ValueAnimator.ofInt(start, end);

        animator.addUpdateListener(valueAnimator -> {
            // Update Height
            int value = (Integer) valueAnimator.getAnimatedValue();

            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.height = value;
            view.setLayoutParams(layoutParams);
        });
        animator.setDuration(ANIMATION_DURATION);
        return animator;
    }

    /**
     * This method will rotate the angle view which is clicked to have
     * some real time effect of collapsing and expanding
     */
    private void rotateAnimator()
    {
        if(isViewExpanded)
        {
            imgExpandCollapse.animate().rotation(180).setDuration(ANIMATION_DURATION);
        }
        else
        {
            imgExpandCollapse.animate().rotation(0).setDuration(ANIMATION_DURATION);
        }
    }

    private Animator.AnimatorListener animatorListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if(isViewExpanded)
            {
                layoutValueDetails.setVisibility(View.GONE);
                isViewExpanded = false;
            }
            else
            {
                layoutValueDetails.setVisibility(View.VISIBLE);
                isViewExpanded = true;
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    };
}
