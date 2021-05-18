package com.alphawallet.app.ui;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.ui.widget.adapter.PriceAlertAdapter;
import com.alphawallet.app.ui.widget.entity.PriceAlertItem;
import com.alphawallet.app.viewmodel.TokenAlertsViewModel;
import com.alphawallet.app.viewmodel.TokenAlertsViewModelFactory;

import java.util.ArrayList;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;

public class TokenAlertsFragment extends BaseFragment implements View.OnClickListener {
    @Inject
    TokenAlertsViewModelFactory viewModelFactory;

    private TokenAlertsViewModel viewModel;

    private RecyclerView recyclerView;

    private LinearLayout layoutAddPriceAlert;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        AndroidSupportInjection.inject(this);
        return inflater.inflate(R.layout.fragment_token_alerts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        layoutAddPriceAlert = view.findViewById(R.id.layout_add_new_price_alert);
        layoutAddPriceAlert.setOnClickListener(this);
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(TokenAlertsViewModel.class);
        viewModel.priceAlerts().observe(getViewLifecycleOwner(), this::onPriceAlertsFetched);

        viewModel.fetchStoredPriceAlerts();
    }

    private void onPriceAlertsFetched(ArrayList<PriceAlertItem> priceAlerts)
    {
        // TODO: populate recylerView
        PriceAlertAdapter adapter = new PriceAlertAdapter(getContext(), priceAlerts);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onClick(View v)
    {
        if (v.getId() == R.id.layout_add_new_price_alert) {
            viewModel.openAddPriceAlertMenu();
        }
    }
}
