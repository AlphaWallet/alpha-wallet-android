package com.alphawallet.app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.lifi.Route;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.service.SwapService;
import com.alphawallet.app.ui.widget.entity.ProgressInfo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

@HiltViewModel
public class SelectRouteViewModel extends BaseViewModel
{
    private final PreferenceRepositoryType preferenceRepository;
    private final SwapService swapService;
    private final MutableLiveData<List<Route>> routes = new MutableLiveData<>();
    private final MutableLiveData<ProgressInfo> progressInfo = new MutableLiveData<>();
    private Disposable routeDisposable;

    @Inject
    public SelectRouteViewModel(
            PreferenceRepositoryType preferenceRepository,
            SwapService swapService)
    {
        this.preferenceRepository = preferenceRepository;
        this.swapService = swapService;
    }

    public LiveData<List<Route>> routes()
    {
        return routes;
    }

    public LiveData<ProgressInfo> progressInfo()
    {
        return progressInfo;
    }

    public void getRoutes(String fromChainId,
                          String toChainId,
                          String fromTokenAddress,
                          String toTokenAddress,
                          String fromAddress,
                          String fromAmount,
                          String slippage,
                          Set<String> exchanges)
    {
        progressInfo.postValue(new ProgressInfo(true, R.string.message_fetching_routes));

        routeDisposable = swapService
                .getRoutes(fromChainId, toChainId, fromTokenAddress, toTokenAddress, fromAddress, fromAmount, slippage, exchanges)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onRoutes, this::onRoutesError);
    }

    private void onRoutes(String result)
    {
        progressInfo.postValue(new ProgressInfo(false));

        try
        {
            JSONObject obj = new JSONObject(result);
            if (obj.has("routes"))
            {
                JSONArray json = obj.getJSONArray("routes");
                List<Route> routeList = new Gson().fromJson(json.toString(), new TypeToken<List<Route>>()
                {
                }.getType());
                routes.postValue(routeList);
            }
            else
            {
//                postError(C.ErrorCode.SWAP_CONNECTIONS_ERROR, result);
            }
        }
        catch (JSONException e)
        {
//            postError(C.ErrorCode.SWAP_CONNECTIONS_ERROR, Objects.requireNonNull(e.getMessage()));
        }
    }

    private void onRoutesError(Throwable throwable)
    {
        // TODO:
    }

    public Set<String> getPreferredExchanges()
    {
        return preferenceRepository.getSelectedSwapProviders();
    }

    @Override
    protected void onCleared()
    {
        if (routeDisposable != null && !routeDisposable.isDisposed())
        {
            routeDisposable.dispose();
        }
        super.onCleared();
    }
}
