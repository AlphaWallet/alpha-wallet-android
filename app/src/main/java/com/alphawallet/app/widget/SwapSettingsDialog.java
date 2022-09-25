package com.alphawallet.app.widget;

import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.lifi.Chain;
import com.alphawallet.app.entity.lifi.SwapProvider;
import com.alphawallet.app.ui.SelectSwapProvidersActivity;
import com.alphawallet.app.ui.widget.adapter.ChainFilter;
import com.alphawallet.app.ui.widget.adapter.SelectChainAdapter;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;
import java.util.Set;

public class SwapSettingsDialog extends BottomSheetDialog
{
    private RecyclerView chainList;
    private SelectChainAdapter adapter;
    private List<SwapProvider> swapProviders;
    private SlippageWidget slippageWidget;
    private StandardHeader preferredExchangesHeader;
    private FlexboxLayout preferredSwapProviders;

    public SwapSettingsDialog(@NonNull Activity activity)
    {
        super(activity);
        View view = View.inflate(getContext(), R.layout.dialog_swap_settings, null);
        setContentView(view);

        setOnShowListener(dialogInterface -> {
            view.setMinimumHeight(Resources.getSystem().getDisplayMetrics().heightPixels);
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from((View) view.getParent());
            behavior.setState(STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        });

        chainList = view.findViewById(R.id.chain_list);
        slippageWidget = view.findViewById(R.id.slippage_widget);

        ImageView closeBtn = findViewById(R.id.image_close);
        closeBtn.setOnClickListener(v -> dismiss());

        preferredExchangesHeader = findViewById(R.id.header_exchanges);
        preferredExchangesHeader.getTextControl().setOnClickListener(v -> {
            Intent intent = new Intent(activity, SelectSwapProvidersActivity.class);
            activity.startActivity(intent);
        });

        preferredSwapProviders = findViewById(R.id.layout_exchanges);
    }

    public SwapSettingsDialog(Activity activity,
                              List<Chain> chains,
                              List<SwapProvider> swapProviders,
                              Set<String> preferredSwapProviders,
                              SwapSettingsInterface swapSettingsInterface)
    {
        this(activity);
        ChainFilter filter = new ChainFilter(chains);
        adapter = new SelectChainAdapter(activity, filter.getSupportedChains(), swapSettingsInterface);
        chainList.setLayoutManager(new LinearLayoutManager(getContext()));
        chainList.setAdapter(adapter);
        this.swapProviders = swapProviders;
        setSwapProviders(preferredSwapProviders);
    }

    private TextView createTextView(String name)
    {
        int margin = (int) getContext().getResources().getDimension(R.dimen.tiny_8);
        FlexboxLayout.LayoutParams params =
                new FlexboxLayout.LayoutParams(FlexboxLayout.LayoutParams.WRAP_CONTENT, FlexboxLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(margin, margin, margin, margin);

        TextView exchange = new TextView(getContext(), null);
        exchange.setText(name);
        exchange.setLayoutParams(params);
        return exchange;
    }

    public void setSwapProviders(Set<String> swapProviders)
    {
        preferredSwapProviders.removeAllViews();
        for (SwapProvider provider : this.swapProviders)
        {
            if (swapProviders.contains(provider.key))
            {
                preferredSwapProviders.addView(createTextView(provider.name));
            }
        }
        preferredSwapProviders.invalidate();
    }

    public void setChains(List<Chain> chains)
    {
        adapter.setChains(chains);
    }

    public void setSelectedChain(long selectedChainId)
    {
        adapter.setSelectedChain(selectedChainId);
    }

    public long getSelectedChainId()
    {
        return adapter.getSelectedChain();
    }

    public String getSlippage()
    {
        return slippageWidget.getSlippage();
    }

    public interface SwapSettingsInterface
    {
        void onChainSelected(Chain chain);
    }
}
