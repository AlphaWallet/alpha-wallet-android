package com.alphawallet.app.ui;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.ui.widget.adapter.PriceAlertAdapter;
import com.alphawallet.app.ui.widget.entity.PriceAlert;
import com.alphawallet.app.ui.widget.entity.PriceAlertCallback;
import com.alphawallet.app.viewmodel.TokenAlertsViewModel;
import com.alphawallet.app.viewmodel.TokenAlertsViewModelFactory;

import java.util.List;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;

public class TokenAlertsFragment extends BaseFragment implements View.OnClickListener, PriceAlertCallback {
    public static final int REQUEST_SET_PRICE_ALERT = 4000;

    @Inject
    TokenAlertsViewModelFactory viewModelFactory;
    private TokenAlertsViewModel viewModel;

    private LinearLayout layoutAddPriceAlert;
    private LinearLayout noAlertsLayout;
    private RecyclerView recyclerView;
    private PriceAlertAdapter adapter;

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
            Token token = getArguments().getParcelable(C.EXTRA_TOKEN_ID);

            layoutAddPriceAlert = view.findViewById(R.id.layout_add_new_price_alert);
            layoutAddPriceAlert.setOnClickListener(this);

            recyclerView = view.findViewById(R.id.recycler_view);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeCallback());
            itemTouchHelper.attachToRecyclerView(recyclerView);

            noAlertsLayout = view.findViewById(R.id.layout_no_alerts);

            viewModel = new ViewModelProvider(this, viewModelFactory)
                    .get(TokenAlertsViewModel.class);
            viewModel.priceAlerts().observe(getViewLifecycleOwner(), this::onPriceAlertsUpdated);
            viewModel.fetchStoredPriceAlerts(token);
        }
    }

    private void onPriceAlertsUpdated(List<PriceAlert> priceAlerts)
    {
        adapter = new PriceAlertAdapter(getContext(), priceAlerts, this);
        recyclerView.setAdapter(adapter);
        noAlertsLayout.setVisibility(priceAlerts.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void addAlert(PriceAlert alert)
    {
        viewModel.saveAlert(alert);
    }

    private void removeAlert(int position)
    {
        adapter.remove(position);
        viewModel.updateStoredAlerts(adapter.getItems());
    }

    @Override
    public void onClick(View v)
    {
        if (v.getId() == R.id.layout_add_new_price_alert)
        {
            viewModel.openAddPriceAlertMenu(this, REQUEST_SET_PRICE_ALERT);
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
                    addAlert(data.getParcelableExtra(C.EXTRA_PRICE_ALERT));
                }
            }
        }
        else
        {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onCheckChanged(boolean checked, int position)
    {
        viewModel.updateStoredAlerts(adapter.getItems());
    }

    public class SwipeCallback extends ItemTouchHelper.SimpleCallback {
        private Drawable icon;
        private ColorDrawable background;

        SwipeCallback()
        {
            super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
            if (getActivity() != null)
            {
                icon = ContextCompat.getDrawable(getActivity(), R.drawable.ic_hide_token);
                if (icon != null)
                {
                    icon.setTint(ContextCompat.getColor(getActivity(), R.color.white));
                }
                background = new ColorDrawable(ContextCompat.getColor(getActivity(), R.color.cancel_red));
            }
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1)
        {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i)
        {
            removeAlert(viewHolder.getAbsoluteAdapterPosition());
        }

        @Override
        public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder)
        {
            return super.getSwipeDirs(recyclerView, viewHolder);
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive)
        {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            View itemView = viewHolder.itemView;
            int offset = 20;
            int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
            int iconTop = itemView.getTop() + (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
            int iconBottom = iconTop + icon.getIntrinsicHeight();

            if (dX > 0)
            {
                int iconLeft = itemView.getLeft() + iconMargin + icon.getIntrinsicWidth();
                int iconRight = itemView.getLeft() + iconMargin;
                icon.setBounds(iconRight, iconTop, iconLeft, iconBottom);
                background.setBounds(itemView.getLeft(), itemView.getTop(),
                        itemView.getLeft() + ((int) dX) + offset,
                        itemView.getBottom());
            }
            else if (dX < 0)
            {
                int iconLeft = itemView.getRight() - iconMargin - icon.getIntrinsicWidth();
                int iconRight = itemView.getRight() - iconMargin;
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                background.setBounds(itemView.getRight() + ((int) dX) - offset,
                        itemView.getTop(), itemView.getRight(), itemView.getBottom());
            }
            else
            {
                background.setBounds(0, 0, 0, 0);
            }

            background.draw(c);
            icon.draw(c);
        }
    }
}
