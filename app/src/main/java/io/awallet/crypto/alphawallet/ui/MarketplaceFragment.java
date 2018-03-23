package io.awallet.crypto.alphawallet.ui;

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
import android.widget.RelativeLayout;

import io.awallet.crypto.alphawallet.R;
import io.awallet.crypto.alphawallet.entity.MarketplaceEvent;
import io.awallet.crypto.alphawallet.ui.widget.adapter.MarketplaceEventAdapter;
import io.awallet.crypto.alphawallet.viewmodel.MarketplaceViewModel;
import io.awallet.crypto.alphawallet.viewmodel.MarketplaceViewModelFactory;
import io.awallet.crypto.alphawallet.widget.SearchDialog;

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
        MarketplaceEvent testEvent1 = new MarketplaceEvent(getString(R.string.my_listings));
        MarketplaceEvent testEvent2 = new MarketplaceEvent(getString(R.string.fifa_2018));
        MarketplaceEvent[] testEvents = { testEvent1, testEvent2 };
        adapter.setMarketplaceEvents(testEvents);

        list.setAdapter(adapter);

        setupSearchBar(view);
        return view;
    }

    private void setupSearchBar(View view) {
        SearchDialog dialog = new SearchDialog(getActivity());
        RelativeLayout searchLayout = view.findViewById(R.id.search_container);
        searchLayout.setOnClickListener(v -> {
            dialog.show();
        });
    }

    private void onMarketplaceEventClick(View view, MarketplaceEvent marketplaceEvent) {
        viewModel.showMarketplace(getContext(), marketplaceEvent);
    }

    @Override
    public void onClick(View v) {

    }
}
