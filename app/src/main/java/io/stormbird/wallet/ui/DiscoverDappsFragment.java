package io.stormbird.wallet.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.DApp;
import io.stormbird.wallet.ui.widget.OnDappClickListener;
import io.stormbird.wallet.ui.widget.adapter.DiscoverDappsListAdapter;
import io.stormbird.wallet.util.DappBrowserUtils;
import io.stormbird.wallet.util.Utils;


public class DiscoverDappsFragment extends Fragment {
    private DiscoverDappsListAdapter adapter;
    private OnDappClickListener onDappClickListener;

    void setCallbacks(OnDappClickListener listener) {
        onDappClickListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_discover_dapps, container, false);
        adapter = new DiscoverDappsListAdapter(
                getData(),
                onDappClickListener,
                this::onDappAdded,
                this::onDappRemoved);
        RecyclerView list = view.findViewById(R.id.discover_dapps_list);
        list.setNestedScrollingEnabled(false);
        list.setLayoutManager(new LinearLayoutManager(getActivity()));
        list.setAdapter(adapter);
        return view;
    }

    private void onDappAdded(DApp dapp) {
        List<DApp> myDapps = DappBrowserUtils.getMyDapps(getContext());
        dapp.setAdded(true);
        myDapps.add(dapp);
        DappBrowserUtils.saveToPrefs(getContext(), myDapps);
    }

    private void onDappRemoved(DApp dapp) {
        try
        {
            List<DApp> myDapps = DappBrowserUtils.getMyDapps(getContext());
            for (DApp d : myDapps)
            {
                if (d.getName().equals(dapp.getName())
                        && d.getUrl().equals(dapp.getUrl()))
                {
                    myDapps.remove(d);
                    break;
                }
            }
            DappBrowserUtils.saveToPrefs(getContext(), myDapps);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private List<DApp> getData() {
        List<DApp> dapps;
        dapps = DappBrowserUtils.getDappsList(getContext());

        try
        {
            for (DApp d : dapps)
            {
                for (DApp myDapp : DappBrowserUtils.getMyDapps(getContext()))
                {
                    if (d.getName().equals(myDapp.getName()) && myDapp.isAdded())
                    {
                        d.setAdded(true);
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return dapps;
    }
}
