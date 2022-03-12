package com.alphawallet.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.DApp;
import com.alphawallet.app.ui.widget.OnDappClickListener;
import com.alphawallet.app.ui.widget.adapter.DiscoverDappsListAdapter;
import com.alphawallet.app.util.DappBrowserUtils;

import java.util.List;

import static com.alphawallet.app.ui.DappBrowserFragment.DAPP_CLICK;

import timber.log.Timber;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class DiscoverDappsFragment extends Fragment implements OnDappClickListener {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_discover_dapps, container, false);
        DiscoverDappsListAdapter adapter = new DiscoverDappsListAdapter(
                getData(),
                this,
                this::onDappAdded,
                this::onDappRemoved);
        RecyclerView list = view.findViewById(R.id.discover_dapps_list);
        list.setNestedScrollingEnabled(false);
        list.setLayoutManager(new LinearLayoutManager(getActivity()));
        list.setAdapter(adapter);
        return view;
    }

    @Override
    public void onDappClick(DApp dapp)
    {
        Bundle result = new Bundle();
        result.putParcelable(DAPP_CLICK, dapp);
        getParentFragmentManager().setFragmentResult(DAPP_CLICK, result);
    }

    private void onDappAdded(DApp dapp) {
        List<DApp> myDapps = DappBrowserUtils.getMyDapps(getContext());
        dapp.setAdded(true);
        for (DApp d : myDapps)
        {
            if (d.getName() != null && d.getUrl() != null && d.getName().equals(dapp.getName())
                    && d.getUrl().equals(dapp.getUrl()))
            {
                return;
            }
        }
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
            Timber.e(e);
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
            Timber.e(e);
        }

        return dapps;
    }
}
