package io.video.weapp.viewmodel;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.util.Log;

import io.reactivex.disposables.Disposable;
import io.video.weapp.entity.ApiException;
import io.video.weapp.entity.ErrorEnvelope;


public class BaseViewModel extends ViewModel {

	public final MutableLiveData<ErrorEnvelope> error = new MutableLiveData<>();
	public final MutableLiveData<Boolean> progress = new MutableLiveData<>();
	protected Disposable disposable;

	@Override
	protected void onCleared() {
		cancel();
	}

	protected void cancel() {
		if (disposable != null && !disposable.isDisposed()) {
			disposable.dispose();
		}
	}

	protected void onError(Throwable t) {
		if (t instanceof ApiException) {
			error.setValue(((ApiException) t).error);
		} else {
			Log.d("SESSION", "Err", t);
		}
	}
}
