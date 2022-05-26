package com.alphawallet.app.widget;

import android.app.Activity;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.lifi.Chain;
import com.alphawallet.app.ui.widget.adapter.SelectChainAdapter;
import com.alphawallet.app.ui.widget.divider.ListDivider;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;

public class SwapSettingsDialog extends BottomSheetDialog
{
    private RecyclerView chainList;
    private SelectChainAdapter adapter;
    private List<Chain> chains;
    private SlippageWidget slippageWidget;
    private long selectedChainId;

    public SwapSettingsDialog(@NonNull Activity activity)
    {
        super(activity);
        View view = View.inflate(getContext(), R.layout.dialog_swap_settings, null);
        setContentView(view);

//        view.setMinimumHeight(Resources.getSystem().getDisplayMetrics().heightPixels);
//        BottomSheetBehavior<View>behavior = BottomSheetBehavior.from((View) view.getParent());
//        behavior.setState(STATE_EXPANDED);
//        behavior.setSkipCollapsed(true);

        chainList = view.findViewById(R.id.chain_list);
        slippageWidget = view.findViewById(R.id.slippage_widget);

        ImageView closeBtn = findViewById(R.id.image_close);
        closeBtn.setOnClickListener(v -> dismiss());
    }

    public SwapSettingsDialog(Activity activity, List<Chain> chains, SwapSettingsInterface swapSettingsInterface)
    {
        this(activity);

        adapter = new SelectChainAdapter(activity, chains, swapSettingsInterface);
        chainList.setLayoutManager(new LinearLayoutManager(getContext()));
        chainList.addItemDecoration(new ListDivider(getContext()));
        chainList.setAdapter(adapter);
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
