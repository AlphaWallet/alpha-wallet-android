package com.alphawallet.app.viewmodel;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.alphawallet.app.C;
import com.alphawallet.app.analytics.Analytics;
import com.alphawallet.app.entity.AnalyticsProperties;
import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.ServiceException;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.service.AnalyticsServiceType;

import org.json.JSONObject;

import io.reactivex.disposables.Disposable;
import timber.log.Timber;

public class BaseViewModel extends ViewModel
{
    protected static final MutableLiveData<Integer> queueCompletion = new MutableLiveData<>();
    protected static final MutableLiveData<String> pushToastMutable = new MutableLiveData<>();
    protected static final MutableLiveData<Integer> successDialogMutable = new MutableLiveData<>();
    protected static final MutableLiveData<Integer> errorDialogMutable = new MutableLiveData<>();
    protected static final MutableLiveData<Boolean> refreshTokens = new MutableLiveData<>();
    protected final MutableLiveData<ErrorEnvelope> error = new MutableLiveData<>();
    protected final MutableLiveData<Boolean> progress = new MutableLiveData<>();
    protected Disposable disposable;
    private AnalyticsServiceType analyticsService;

    public static void onQueueUpdate(int complete)
    {
        queueCompletion.postValue(complete);
    }

    public static void onPushToast(String message)
    {
        pushToastMutable.postValue(message);
    }

    @Override
    protected void onCleared()
    {
        cancel();
    }

    private void cancel()
    {
        if (disposable != null && !disposable.isDisposed())
        {
            disposable.dispose();
        }
    }

    public LiveData<ErrorEnvelope> error()
    {
        return error;
    }

    public LiveData<Boolean> progress()
    {
        return progress;
    }

    public LiveData<Integer> queueProgress()
    {
        return queueCompletion;
    }

    public LiveData<String> pushToast()
    {
        return pushToastMutable;
    }

    public LiveData<Boolean> refreshTokens()
    {
        return refreshTokens;
    }

    protected void onError(Throwable throwable)
    {
        Timber.e(throwable);
        if (throwable instanceof ServiceException)
        {
            error.postValue(((ServiceException) throwable).error);
        }
        else
        {
            String message = TextUtils.isEmpty(throwable.getMessage()) ?
                    "Unknown Error" : throwable.getMessage();
            error.postValue(new ErrorEnvelope(C.ErrorCode.UNKNOWN, message, throwable));
        }
    }

    public void showSendToken(Context context, String address, String symbol, int decimals, Token token)
    {
        //do nothing
    }

    public void showTokenList(Activity activity, Token token)
    {
        //do nothing
    }

    public void showErc20TokenDetail(Activity context, String address, String symbol, int decimals, Token token)
    {
        //do nothing
    }

    protected void setAnalyticsService(AnalyticsServiceType analyticsService)
    {
        this.analyticsService = analyticsService;
    }

    public void identify(String uuid)
    {
        if (analyticsService != null)
        {
            analyticsService.identify(uuid);
        }
    }

    public void track(Analytics.Navigation event)
    {
        trackEvent(event.getValue());
    }

    public void track(Analytics.Navigation event, AnalyticsProperties props)
    {
        trackEventWithProps(event.getValue(), props);
    }

    public void track(Analytics.Action event)
    {
        trackEvent(event.getValue());
    }

    public void track(Analytics.Action event, AnalyticsProperties props)
    {
        trackEventWithProps(event.getValue(), props);
    }
    
    public void trackError(Analytics.Error source, String message)
    {
        AnalyticsProperties props = new AnalyticsProperties();
        props.put(Analytics.PROPS_ERROR_MESSAGE, message);
        trackEventWithProps(source.getValue(), props);
    }

    private void trackEvent(String event)
    {
        if (analyticsService != null)
        {
            analyticsService.track(event);
        }
    }

    private void trackEventWithProps(String event, AnalyticsProperties props)
    {
        if (analyticsService != null)
        {
            analyticsService.track(event, props);
        }
    }
}