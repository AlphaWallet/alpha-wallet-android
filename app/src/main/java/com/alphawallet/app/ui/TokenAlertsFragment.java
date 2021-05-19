package com.alphawallet.app.ui;


import android.app.Activity;
import android.content.Intent;
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

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.ui.widget.adapter.PriceAlertAdapter;
import com.alphawallet.app.ui.widget.entity.PriceAlert;
import com.alphawallet.app.viewmodel.TokenAlertsViewModel;
import com.alphawallet.app.viewmodel.TokenAlertsViewModelFactory;

import java.util.ArrayList;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;

public class TokenAlertsFragment extends BaseFragment implements View.OnClickListener {
    public static final int REQUEST_SET_PRICE_ALERT = 4000;

    @Inject
    TokenAlertsViewModelFactory viewModelFactory;
    private TokenAlertsViewModel viewModel;

    private LinearLayout layoutAddPriceAlert;
    private LinearLayout noAlertsLayout;
    private RecyclerView recyclerView;
    private PriceAlertAdapter adapter;
    private Token token;

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

        if (getArguments() != null)
        {
            token = getArguments().getParcelable(C.EXTRA_TOKEN_ID);

            layoutAddPriceAlert = view.findViewById(R.id.layout_add_new_price_alert);
            layoutAddPriceAlert.setOnClickListener(this);

            recyclerView = view.findViewById(R.id.recycler_view);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

            noAlertsLayout = view.findViewById(R.id.layout_no_alerts);

            viewModel = new ViewModelProvider(this, viewModelFactory)
                    .get(TokenAlertsViewModel.class);
            viewModel.priceAlerts().observe(getViewLifecycleOwner(), this::onPriceAlertsFetched);

            viewModel.fetchStoredPriceAlerts();
        }
    }

    private void onPriceAlertsFetched(ArrayList<PriceAlert> priceAlerts)
    {
        adapter = new PriceAlertAdapter(getContext(), priceAlerts);
        recyclerView.setAdapter(adapter);
        noAlertsLayout.setVisibility(priceAlerts.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onClick(View v)
    {
        if (v.getId() == R.id.layout_add_new_price_alert)
        {
            viewModel.openAddPriceAlertMenu(this, token, REQUEST_SET_PRICE_ALERT);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        if (requestCode == REQUEST_SET_PRICE_ALERT)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                if (data != null)
                {
                    PriceAlert alert = data.getParcelableExtra(C.EXTRA_PRICE_ALERT);
                    adapter.add(alert);
                    noAlertsLayout.setVisibility(View.GONE);
                    viewModel.saveAlert(alert);
                }
            }
        }
        else
        {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
