package com.alphawallet.app.ui.widget.entity;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.web3.entity.Address;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import wallet.core.jni.Hash;

import static com.alphawallet.ethereum.EthereumNetworkBase.AVALANCHE_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.BINANCE_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.CLASSIC_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.FANTOM_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MATIC_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.XDAI_ID;

public class HistoryChart extends View {

    static OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build();

    public enum Range {

        Day(1),
        Week(7),
        Month(30),
        ThreeMonth(91),
        Year(365);

        private final int value;

        Range(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

    }

    // store tokens mapping and chart data
    static class Cache {
        static class CoinGeckoToken {
            public String id;
            public String symbol;
            public String name;
            public Map<String, String> platforms;
        }

        Range range;
        TokenInfo tokenInfo = null;
        List<CoinGeckoToken> coinGeckoTokens;

        Map<Range, Datasource> datasourceMap = new HashMap<>();

        Datasource getCurrentDatasource() {
            if (datasourceMap.containsKey(range)) {
                return datasourceMap.get(range);
            }
            return null;
        }

        // Mapping created by examining CoinGecko API output empirically
        boolean platformMatches(String platform, int chainId) {
            switch (chainId) {
                case MAINNET_ID:
                    return platform.equals("ethereum");
                case CLASSIC_ID:
                    return platform.equals("ethereum-classic");
                case XDAI_ID:
                    return platform.equals("xdai");
                case BINANCE_MAIN_ID:
                    return platform.equals("binance-smart-chain");
                case AVALANCHE_ID:
                    return platform.equals("avalanche");
                case MATIC_ID:
                    return platform.equals("polygon-pos");
                case FANTOM_ID:
                    return platform.equals("fantom");
                //TODO: case ARBITRUM_ID: return platform == "arbitrum-one"
                default:
                    return false;
            }
        }

        boolean isServerSupported(int chainId) {
            switch (chainId) {
                case MAINNET_ID:
                    return true;
                case CLASSIC_ID:
                    return true;
                case XDAI_ID:
                    return true;
                case BINANCE_MAIN_ID:
                    return true;
                case AVALANCHE_ID:
                    return true;
                case MATIC_ID:
                    return true;
                case FANTOM_ID:
                    return true;
                //TODO: case ARBITRUM_ID: return true
                default:
                    return false;
            }
        }

        static Single<List<CoinGeckoToken>> fetchList() {
            return Single.fromCallable(() -> {
                List<CoinGeckoToken> coinGeckoTokens = new ArrayList<>();
                Request request = new Request.Builder()
                        .url("https://api.coingecko.com/api/v3/coins/list?include_platform=true")
                        .get()
                        .build();
                okhttp3.Response response = httpClient.newCall(request)
                        .execute();
                if (response.code() / 200 == 1) {
                    coinGeckoTokens = new Gson().fromJson(response.body().string(), new TypeToken<List<CoinGeckoToken>>() {
                    }.getType());
                }
                return coinGeckoTokens;
            });
        }
    }

    static class Datasource {
        ArrayList<Pair<Long, Float>> getEntries() {
            return entries;
        }

        // precalculated
        float minValue = 0.0f;
        float maxValue = 0.0f;

        ArrayList<Pair<Long, Float>> entries = new ArrayList<>();

        static Single<Datasource> fetchHistory(Range range, String tokenId) {
            return Single.fromCallable(() -> {
                ArrayList<Pair<Long, Float>> entries = new ArrayList<>();
                try {
                    Request request = new Request.Builder()
                            .url("https://api.coingecko.com/api/v3/coins/" + tokenId + "/market_chart?days=" + range.value + "&vs_currency=USD")
                            .get()
                            .build();
                    okhttp3.Response response = httpClient.newCall(request)
                            .execute();
                    if (response.code() / 200 == 1) {
                        JSONArray prices = new JSONObject(response.body().string()).getJSONArray("prices");
                        float minValue = Float.MAX_VALUE;
                        float maxValue = 0;
                        for (int i = 0; i < prices.length(); i++) {
                            JSONArray entry = prices.getJSONArray(i);
                            long timestamp = entry.getLong(0);
                            float value = (float) entry.getDouble(1);
                            entries.add(Pair.create(timestamp, value));
                            if (minValue > value) {
                                minValue = value;
                            }
                            if (maxValue < value) {
                                maxValue = value;
                            }
                        }
                        Datasource ds = new Datasource();
                        ds.entries = entries;
                        ds.minValue = minValue;
                        ds.maxValue = maxValue;
                        return ds;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            });
        }

        long minTime() {
            return entries.get(0).first;
        }

        long maxTime() {
            return entries.get(entries.size() - 1).first;
        }

        float minValue() {
            return minValue;
        }

        float maxValue() {
            return maxValue;
        }
    }

    Cache cache = new Cache();

    Paint paint = new Paint();
    Paint textPaint = new Paint();
    Path path = new Path();
    LinearGradient gradient = null;

    private void init() {
        paint.setColor(getResources().getColor(R.color.nasty_green, getContext().getTheme()));


        Resources r = getResources();
        int strokeWidth = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                5,
                r.getDisplayMetrics()
        );

        paint.setStrokeWidth(strokeWidth);
        paint.setDither(true);

        textPaint.setTextAlign(Paint.Align.CENTER);
        int textSize = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                14,
                r.getDisplayMetrics()
        );
        textPaint.setTextSize(textSize);
        textPaint.setColor(getResources().getColor(R.color.black, getContext().getTheme()));
    }

    public HistoryChart(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Datasource datasource = cache.getCurrentDatasource();
        if (datasource == null || datasource.entries.size() == 0) {
            // draw no chart data available message
            int xPos = (getWidth() / 2);
            int yPos = (int) ((getHeight() / 2) - ((textPaint.descent() + textPaint.ascent()) / 2));
            canvas.drawText(getContext().getString(R.string.no_chart_data_available), xPos, yPos, textPaint);
            return;
        }

        path.reset();

        // draw chart
        float width = getWidth();
        float height = getHeight();

        float xScale = width / (datasource.maxTime() - datasource.minTime());
        float yScale = (height * 0.9f) / (datasource.maxValue() - datasource.minValue());

        for (int i = 0; i < datasource.entries.size(); i++) {
            Pair<Long, Float> entry = datasource.entries.get(i);

            float x = (entry.first - datasource.minTime()) * xScale;
            float y = height - (entry.second - datasource.minValue()) * yScale;

            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setShader(null);
        canvas.drawPath(path, paint);

        if (gradient == null) {
            // create gradient fill on the first run
            gradient = new LinearGradient(0, 0, 0, height, 0xFFf7fbf4, 0xffffffff, Shader.TileMode.CLAMP);
        }

        paint.setShader(gradient);

        // add bottom points
        paint.setStyle(Paint.Style.FILL);
        path.lineTo(width, height);
        path.lineTo(0, height);
        canvas.drawPath(path, paint);
    }

    public void fetchHistory(TokenInfo info, Range range) {
        cache.tokenInfo = info;
        cache.range = range;
        if (cache.coinGeckoTokens == null) {
            Cache.fetchList()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onTokens, this::onError).isDisposed();
        } else {
            getCoingeckoTokenIdAndFetchHistory();
        }
    }


    private void getCoingeckoTokenIdAndFetchHistory() {

        // use cache
        if (cache.getCurrentDatasource() != null) {
            invalidate();
            return;
        }

        if (!cache.isServerSupported(cache.tokenInfo.chainId)) {
            return;
        }

        final Address polygonMaticContract = new Address("0x0000000000000000000000000000000000001010");

        String coingeckoTokenId = null;

        for (Cache.CoinGeckoToken cgToken : cache.coinGeckoTokens) {
            for (Map.Entry<String, String> entry : cgToken.platforms.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    if (cache.platformMatches(entry.getKey(), cache.tokenInfo.chainId)) {
                        if (new Address(entry.getValue()).equals(Address.EMPTY) &&
                                cgToken.symbol.equalsIgnoreCase(cache.tokenInfo.symbol)
                        ) {
                            coingeckoTokenId = cgToken.id;
                            break;
                        } else if (new Address(entry.getValue()).equals(new Address(cache.tokenInfo.address))) {
                            coingeckoTokenId = cgToken.id;
                            break;
                        } else if (cache.tokenInfo.chainId == MATIC_ID && new Address(cache.tokenInfo.address).equals(Address.EMPTY) &&
                                new Address(entry.getValue()).equals(polygonMaticContract)) {
                            coingeckoTokenId = cgToken.id;
                            break;
                        }
                    }
                }
            }

            if (coingeckoTokenId == null) {
                if (cgToken.symbol.equalsIgnoreCase(cache.tokenInfo.symbol)) {
                    coingeckoTokenId = cgToken.id;
                    break;
                }
            }
        }

        if (coingeckoTokenId != null) {
            Datasource.fetchHistory(cache.range, coingeckoTokenId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onEntries, this::onError).isDisposed();
        }
    }


    private void onTokens(List<Cache.CoinGeckoToken> tokens) {
        // invalidate
        this.cache.coinGeckoTokens = tokens;
        getCoingeckoTokenIdAndFetchHistory();
    }

    private void onEntries(Datasource datasource) {
        // invalidate
        cache.datasourceMap.put(cache.range, datasource);
        invalidate();
    }

    private void onError(Throwable throwable) {
        throwable.printStackTrace();
    }
}
