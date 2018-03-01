package com.wallet.crypto.alphawallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wallet.crypto.alphawallet.R;
import com.wallet.crypto.alphawallet.entity.MarketplaceEvent;
import com.wallet.crypto.alphawallet.ui.widget.adapter.MarketplaceEventAdapter;
import com.wallet.crypto.alphawallet.viewmodel.MarketplaceViewModel;
import com.wallet.crypto.alphawallet.viewmodel.MarketplaceViewModelFactory;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;

public class MarketplaceFragment extends Fragment implements View.OnClickListener {
    @Inject
    MarketplaceViewModelFactory marketplaceViewModelFactory;
    private MarketplaceViewModel viewModel;

    private MarketplaceEventAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        AndroidSupportInjection.inject(this);
        View view = inflater.inflate(R.layout.fragment_marketplace, container, false);
        viewModel = ViewModelProviders.of(this, marketplaceViewModelFactory).get(MarketplaceViewModel.class);

        RecyclerView list = view.findViewById(R.id.list_marketplace);
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MarketplaceEventAdapter(this::onMarketplaceEventClick);

        /* Placeholder only */
        MarketplaceEvent testEvent1 = new MarketplaceEvent("My Listings");
        MarketplaceEvent testEvent2 = new MarketplaceEvent("FIFA 2018");
        MarketplaceEvent[] testEvents = { testEvent1, testEvent2 };
        adapter.setMarketplaceEvents(testEvents);

        list.setAdapter(adapter);
        return view;
    }

    private void onMarketplaceEventClick(View view, MarketplaceEvent marketplaceEvent) {
        viewModel.showMarketplace(getContext());
    }

    @Override
    public void onClick(View v) {

    }
}
