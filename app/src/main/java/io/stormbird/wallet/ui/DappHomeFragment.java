package io.stormbird.wallet.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.List;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.DApp;
import io.stormbird.wallet.ui.widget.OnDappClickListener;
import io.stormbird.wallet.ui.widget.OnDappHomeNavClickListener;
import io.stormbird.wallet.ui.widget.adapter.MyDappsGridAdapter;
import io.stormbird.wallet.util.DappBrowserUtils;


public class DappHomeFragment extends Fragment {
    private static final String ON_DAPP_HOME_NAV_CLICK_LISTENER = "onDappHomeNavClickListener";
    private static final String ON_DAPP_CLICK_LISTENER = "onDappClickListener";
    private MyDappsGridAdapter adapter;
    private OnDappClickListener onDappClickListener;
    private OnDappHomeNavClickListener onDappHomeNavClickListener;

    public static DappHomeFragment newInstance(OnDappHomeNavClickListener onDappHomeNavClickListener,
                                               OnDappClickListener onDappClickListener) {
        DappHomeFragment f = new DappHomeFragment();
        Bundle args = new Bundle();
        args.putSerializable(ON_DAPP_HOME_NAV_CLICK_LISTENER, onDappHomeNavClickListener);
        args.putSerializable(ON_DAPP_CLICK_LISTENER, onDappClickListener);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            onDappClickListener =
                    (OnDappClickListener) getArguments().get(ON_DAPP_CLICK_LISTENER);
            onDappHomeNavClickListener =
                    (OnDappHomeNavClickListener) getArguments().get(ON_DAPP_HOME_NAV_CLICK_LISTENER);

        }
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_dapp_home, container, false);

        LinearLayout myDappsLayout = view.findViewById(R.id.my_dapps);
        myDappsLayout.setOnClickListener(v ->
                onDappHomeNavClickListener.onDappHomeNavClick(0));

        LinearLayout discoverDappsLayout = view.findViewById(R.id.discover_dapps);
        discoverDappsLayout.setOnClickListener(v ->
                onDappHomeNavClickListener.onDappHomeNavClick(1));

        LinearLayout historyLayout = view.findViewById(R.id.history);
        historyLayout.setOnClickListener(v ->
                onDappHomeNavClickListener.onDappHomeNavClick(2));

        adapter = new MyDappsGridAdapter(getData(), onDappClickListener);
        RecyclerView grid = view.findViewById(R.id.my_dapps_grid);
        grid.setNestedScrollingEnabled(false);
        grid.setLayoutManager(new GridLayoutManager(getActivity(), 3));
        grid.setAdapter(adapter);
        return view;
    }

    private List<DApp> getData() {
        return DappBrowserUtils.getMyDapps(getContext());
    }
}
