package com.wallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.wallet.crypto.alphawallet.C;
import com.wallet.crypto.alphawallet.entity.ErrorEnvelope;
import com.wallet.crypto.alphawallet.entity.ServiceException;
import com.wallet.crypto.alphawallet.entity.Token;
import com.wallet.crypto.alphawallet.entity.TradeInstance;

import io.reactivex.disposables.Disposable;

public class BaseViewModel extends ViewModel
{

	protected final MutableLiveData<ErrorEnvelope> error = new MutableLiveData<>();
	protected final MutableLiveData<Boolean> progress = new MutableLiveData<>();
	protected Disposable disposable;
	protected static final MutableLiveData<Integer> queueCompletion = new MutableLiveData<>();
	protected static final MutableLiveData<String> pushToastMutable = new MutableLiveData<>();

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

	protected void onError(Throwable throwable)
	{
		Log.d("TAG", "Err", throwable);
		if (throwable instanceof ServiceException)
		{
			error.postValue(((ServiceException) throwable).error);
		}
		else
		{
			if (throwable.getCause() == null || TextUtils.isEmpty(throwable.getCause().getMessage()))
			{
				error.postValue(new ErrorEnvelope(C.ErrorCode.UNKNOWN, null, throwable));
			}
			else
			{
				error.postValue(new ErrorEnvelope(C.ErrorCode.UNKNOWN, throwable.getCause().getMessage(), throwable));
			}
		}
	}

	public static void onQueueUpdate(int complete)
	{
		queueCompletion.postValue(complete);
	}

	public static void onPushToast(String message)
	{
		pushToastMutable.postValue(message);
	}

	public void showSendToken(Context context, String address, String symbol, int decimals) {
		//do nothing
	}

	public void showUseToken(Context context, Token token) {
		//do nothing
	}
}