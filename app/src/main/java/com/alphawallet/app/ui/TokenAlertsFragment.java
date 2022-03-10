package com.alphawallet.app.ui;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
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
import com.alphawallet.ethereum.EthereumNetworkBase;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TokenAlertsFragment extends BaseFragment implements View.OnClickListener, PriceAlertCallback {
    public static final int REQUEST_SET_PRICE_ALERT = 4000;

    private TokenAlertsViewModel viewModel;

    private LinearLayout noAlertsLayout;
    private RecyclerView recyclerView;
    private PriceAlertAdapter adapter;
    private ActivityResultLauncher<Intent> launcher;
    private Token token;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_token_alerts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null)
        {
            viewModel = new ViewModelProvider(this)
                    .get(TokenAlertsViewModel.class);

            long chainId = getArguments().getLong(C.EXTRA_CHAIN_ID, EthereumNetworkBase.MAINNET_ID);
            token = viewModel.getTokensService().getToken(chainId, getArguments().getString(C.EXTRA_ADDRESS));

            launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result ->
                    {
                        if (result.getResultCode() == Activity.RESULT_OK)
                        {
                            if (result.getData() != null)
                            {
                                viewModel.saveAlert(result.getData().getParcelableExtra(C.EXTRA_PRICE_ALERT));
                            }
                        }
                    }
            );
            LinearLayout layoutAddPriceAlert = view.findViewById(R.id.layout_add_new_price_alert);
            layoutAddPriceAlert.setOnClickListener(this);

            recyclerView = view.findViewById(R.id.recycler_view);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeCallback());
            itemTouchHelper.attachToRecyclerView(recyclerView);

            noAlertsLayout = view.findViewById(R.id.layout_no_alerts);

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
            Intent intent = new Intent(getContext(), SetPriceAlertActivity.class);
            intent.putExtra(C.EXTRA_ADDRESS, token.getAddress());
            intent.putExtra(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);

            launcher.launch(intent);
        }
    }

    @Override
    public void onCheckChanged(boolean checked, int position)
    {
        viewModel.updateStoredAlerts(adapter.getItems());
    }

    public class SwipeCallback extends ItemTouchHelper.SimpleCallback
    {
        private Drawable icon;
        private ColorDrawable background;
        private final Paint textPaint = new TextPaint();
        private int swipeControlWidth;

        SwipeCallback()
        {
            super(0, ItemTouchHelper.LEFT);
            if (getActivity() != null)
            {
                icon = ContextCompat.getDrawable(getActivity(), R.drawable.ic_close);
                if (icon != null)
                {
                    icon.setTint(ContextCompat.getColor(getActivity(), R.color.error_inverse));
                }
                background = new ColorDrawable(ContextCompat.getColor(getActivity(), R.color.error));

                textPaint.setTextAlign(Paint.Align.CENTER);
                textPaint.setTypeface(ResourcesCompat.getFont(getContext(), R.font.font_semibold));
                textPaint.setTextSize((int) getResources().getDimension(R.dimen.sp17));
                textPaint.setColor(getResources().getColor(R.color.error_inverse, getContext().getTheme()));

                swipeControlWidth = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        120,
                        getActivity().getResources().getDisplayMetrics()
                );
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
            int iconMargin = (itemView.getHeight() / 2 - icon.getIntrinsicHeight()) / 2;
            int iconTop = itemView.getTop() + iconMargin * 2;
            int iconBottom = iconTop + icon.getIntrinsicHeight();

            if (dX < 0)
            {
                int iconRight = itemView.getRight() - (iconMargin + swipeControlWidth - icon.getIntrinsicWidth()) / 2;
                int iconLeft = iconRight - icon.getIntrinsicWidth();

                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                background.setBounds(itemView.getRight() + ((int) dX) - offset,
                        itemView.getTop(), itemView.getRight(), itemView.getBottom());
            } else
            {
                background.setBounds(0, 0, 0, 0);
            }

            background.draw(c);
            icon.draw(c);

            int xPos = itemView.getRight() - swipeControlWidth / 2;
            int yPos = (int) (itemView.getTop() + (itemView.getHeight() * 0.75));
            c.drawText(requireContext().getString(R.string.delete), xPos, yPos, textPaint);
        }
    }
}
