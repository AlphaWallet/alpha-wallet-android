package com.alphawallet.app.ui.widget.entity;

import static com.alphawallet.app.service.TickerService.chainPairs;
import static com.alphawallet.app.service.TickerService.coinGeckoChainIdToAPIName;
import static java.lang.Math.floor;
import static io.realm.Realm.getApplicationContext;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Pair;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.util.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class HistoryChart extends View
{

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

        Range(int value)
        {
            this.value = value;
        }

        public int getValue()
        {
            return value;
        }
    }

    public void showIntercept(float x)
    {
        Datasource datasource = cache.getCurrentDatasource(cache.range);

        float width = getWidth();
        float height = getHeight();

        float xScale = width / (datasource.maxTime() - datasource.minTime());
        float yScale = ((height * 0.9f) / (datasource.maxValue() - datasource.minValue()));

        int color = datasource.isGreen() ? R.color.green : R.color.danger;
        interceptPaint.setColor(getResources().getColor(color,getContext().getTheme()));
        long timeStamp = (long) floor(x/xScale + datasource.minTime());

        float price = 0;
        float y = -1;

        //find from timeStamp
        for(int i = (int) floor((x/width)*datasource.entries.size()); i < datasource.entries.size(); i++)
        {
            System.out.println(String.format("i ratio is %d // %d",x,width));
            if(i < datasource.entries.size()-1)
            {
                Pair<Long, Float> entry_lower = datasource.entries.get(i);
                Pair<Long, Float> entry_higher = datasource.entries.get(i+1);
                if(entry_lower.first < timeStamp && entry_higher.first >= timeStamp)
                {
                    long diff = (timeStamp - entry_lower.first) - (entry_higher.first - timeStamp);
                    price = diff >= 0 ? entry_lower.second: entry_higher.second;
                    y = height - (price - datasource.minValue()) * yScale;

                    break;
                }
            }

        }
        System.out.println(String.format("boop coord %f,%f",x,y));

        //draw intercepts
        try
        {
            interceptLines.reset();
            interceptLines.moveTo(0,y);
            interceptLines.lineTo(width,y);

            interceptLines.moveTo(x,0);
            interceptLines.moveTo(x,height);
            canvas.drawPath(interceptLines,interceptPaint);

            canvas.drawTextOnPath(Utils.localiseUnixDate(getApplicationContext(),timeStamp),
                    interceptLines,
                    0,0,
                    edgeValPaint);
        }
        catch (Exception e)
        {
            //do nothing
        }


    }

    public void hideIntercept()
    {
        canvas.restore();
    }

    // store tokens mapping and chart data
    static class Cache {
        Range range;
        Map<Range, Datasource> datasourceMap = new HashMap<>();

        Datasource getCurrentDatasource(Range range) {
            if (datasourceMap.containsKey(range)) {
                return datasourceMap.get(range);
            }
            return null;
        }
    }

    static class Datasource
    {
        ArrayList<Pair<Long, Float>> getEntries()
        {
            return entries;
        }

        // precalculated
        float minValue = 0.0f;
        float maxValue = 0.0f;

        ArrayList<Pair<Long, Float>> entries = new ArrayList<>();

        static Single<Datasource> fetchHistory(Range range, String tokenId)
        {
            return Single.fromCallable(() -> {
                ArrayList<Pair<Long, Float>> entries = new ArrayList<>();
                try
                {
                    Request request = new Request.Builder()
                            .url("https://api.coingecko.com/api/v3/coins/" + tokenId + "/market_chart?days=" + range.value + "&vs_currency=" + TickerService.getCurrencySymbolTxt())
                            .get()
                            .build();
                    okhttp3.Response response = httpClient.newCall(request)
                            .execute();
                    if (response.code() / 200 == 1) {
                        JSONArray prices = new JSONObject(response.body().string()).getJSONArray("prices");
                        float minValue = Float.MAX_VALUE;
                        float maxValue = 0;
                        for (int i = 0; i < prices.length(); i++)
                        {
                            JSONArray entry = prices.getJSONArray(i);
                            long timestamp = entry.getLong(0);
                            float value = (float) entry.getDouble(1);
                            entries.add(Pair.create(timestamp, value));
                            if (minValue > value)
                            {
                                minValue = value;
                            }
                            if (maxValue < value)
                            {
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

        long minTime()
        {
            return entries.get(0).first;
        }

        long maxTime()
        {
            return entries.get(entries.size() - 1).first;
        }

        boolean isGreen()
        {
            return (entries.get(0).second - entries.get(entries.size() - 1).second) < 0;
        }

        float minValue()
        {
            return minValue;
        }

        float maxValue()
        {
            return maxValue;
        }
    }

    Cache cache = new Cache();
    Paint paint = new Paint();
    Paint noDataTextPaint = new Paint();
    Path path = new Path();
    Paint greyPaint = new Paint();
    Path greyLines = new Path();
    Paint edgeValPaint = new Paint();
    Paint interceptPaint = new Paint();
    Path interceptLines = new Path();
    Canvas canvas;

    private void init()
    {
        paint.setColor(getResources().getColor(R.color.green, getContext().getTheme()));

        Resources r = getResources();
        int strokeWidth = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                3,
                r.getDisplayMetrics()
        );

        paint.setStrokeWidth(strokeWidth);
        paint.setDither(true);

        interceptPaint.setColor(getResources().getColor(R.color.green,getContext().getTheme()));
        interceptPaint.setStrokeWidth(1);


        greyPaint.setColor(getResources().getColor(R.color.black_12,getContext().getTheme()));
        greyPaint.setStrokeWidth(1);

        noDataTextPaint.setTextAlign(Paint.Align.CENTER);
        int textSize = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                14,
                r.getDisplayMetrics()
        );
        noDataTextPaint.setTextSize(textSize);
        noDataTextPaint.setColor(getResources().getColor(R.color.black_12, getContext().getTheme()));

        edgeValPaint.setTextAlign(Paint.Align.RIGHT);
        edgeValPaint.setTextSize(
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_SP,
                        12,
                        r.getDisplayMetrics()
                )
        );
        edgeValPaint.setColor(getResources().getColor(R.color.black, getContext().getTheme()));

    }

    public HistoryChart(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        Datasource datasource = cache.getCurrentDatasource(cache.range);
        if (datasource == null || datasource.entries.size() == 0)
        {
            // draw no chart data available message
            int xPos = (getWidth() / 2);
            int yPos = (int) ((getHeight() / 2) - ((noDataTextPaint.descent() + noDataTextPaint.ascent()) / 2));
            canvas.drawText(getContext().getString(R.string.no_chart_data_available), xPos, yPos, noDataTextPaint);
            return;
        }


        // draw chart
        float width = getWidth();
        float height = getHeight();

        //colour changes depending on first and last values
        path.reset();
        int color = datasource.isGreen() ? R.color.green : R.color.danger;
        paint.setColor(getResources().getColor(color,getContext().getTheme()));


        float xScale = width / (datasource.maxTime() - datasource.minTime());
        float yScale = ((height * 0.9f) / (datasource.maxValue() - datasource.minValue()));

        for (float i = datasource.minValue();
             i <= datasource.maxValue();
             i = i + (datasource.maxValue() - datasource.minValue())/4) {
            float lineVal = height - (i - datasource.minValue()) * yScale;
            greyLines.moveTo(0, lineVal);
            greyLines.lineTo(width, lineVal);
        }
        greyPaint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(greyLines,greyPaint);

        for (int i = 0; i < datasource.entries.size(); i++)
        {
            Pair<Long, Float> entry = datasource.entries.get(i);

            float x = (entry.first - datasource.minTime()) * xScale;
            float y = height - (entry.second - datasource.minValue()) * yScale;

            if (i == 0)
            {
                path.moveTo(x, y);
            } else
            {
                path.lineTo(x, y);
            }
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setShader(null);
        canvas.drawPath(path, paint);

        // add min/max values to chart
        canvas.drawText(String.format("%.02f", datasource.minValue()),width,height,edgeValPaint);
        canvas.drawText(String.format("%.02f",datasource.maxValue()),width,0.05f*height,edgeValPaint);
    }

    public void fetchHistory(Token token, final Range range)
    {
        // use cache
        cache.range = range;
        if (cache.getCurrentDatasource(range) != null) {
            invalidate();
            return;
        }

        if (!TickerService.validateCoinGeckoAPI(token)) { return; } //wouldn't have tickers

        String coingeckoTokenId = token.isEthereum() ? chainPairs.get(token.tokenInfo.chainId)
                : coinGeckoChainIdToAPIName.get(token.tokenInfo.chainId) + "/contract/" + token.getAddress().toLowerCase();

        if (coingeckoTokenId != null)
        {
            Datasource.fetchHistory(range, coingeckoTokenId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(datasource -> onEntries(range, datasource), this::onError).isDisposed();
        }
    }

    private void onEntries(Range range, Datasource datasource)
    {
        // invalidate
        cache.datasourceMap.put(range, datasource);
        invalidate();
    }

    private void onError(Throwable throwable) {
        throwable.printStackTrace();
    }
}
