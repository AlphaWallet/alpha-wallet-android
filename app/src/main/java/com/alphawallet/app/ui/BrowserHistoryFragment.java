package com.alphawallet.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.DApp;
import com.alphawallet.app.ui.widget.OnDappClickListener;
import com.alphawallet.app.ui.widget.adapter.BrowserHistoryAdapter;
import com.alphawallet.app.util.DappBrowserUtils;
import com.alphawallet.app.widget.AWalletAlertDialog;

import java.util.List;

import static com.alphawallet.app.ui.DappBrowserFragment.DAPP_CLICK;
import static com.alphawallet.app.ui.DappBrowserFragment.DAPP_REMOVE_HISTORY;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class BrowserHistoryFragment extends Fragment {
    private BrowserHistoryAdapter adapter;
    private AWalletAlertDialog dialog;
    private TextView clear;
    private TextView noHistory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_browser_history, container, false);
        adapter = new BrowserHistoryAdapter(
                getData(),
                (OnDappClickListener) dapp -> setFragmentResult(DAPP_CLICK, dapp),
                this::onHistoryItemRemoved);
        RecyclerView list = view.findViewById(R.id.my_dapps_list);
        list.setNestedScrollingEnabled(false);
        list.setLayoutManager(new LinearLayoutManager(getActivity()));
        list.setAdapter(adapter);

        noHistory = view.findViewById(R.id.no_history);
        clear = view.findViewById(R.id.clear);
        clear.setOnClickListener(v -> {
            dialog = new AWalletAlertDialog(getActivity());
            dialog.setTitle(R.string.dialog_title_clear);
            dialog.setMessage(R.string.dialog_message_clear);
            dialog.setIcon(AWalletAlertDialog.NONE);
            dialog.setButtonText(R.string.action_clear);
            dialog.setButtonListener(v1 -> {
                clearHistory();
                dialog.dismiss();
            });
            dialog.setSecondaryButtonText(R.string.dialog_cancel_back);
            dialog.show();
        });

        showOrHideViews();
        return view;
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        adapter.clear();
    }

    private void showOrHideViews() {
        if (adapter.getItemCount() > 0) {
            clear.setVisibility(View.VISIBLE);
            noHistory.setVisibility(View.GONE);
        } else {
            clear.setVisibility(View.GONE);
            noHistory.setVisibility(View.VISIBLE);
        }
    }

    private void clearHistory() {
        DappBrowserUtils.clearHistory(getContext());
        adapter.setDapps(getData());
        showOrHideViews();
    }

    private void onHistoryItemRemoved(DApp dapp) {
        DappBrowserUtils.removeFromHistory(getContext(), dapp);
        adapter.setDapps(getData());
        showOrHideViews();
        setFragmentResult(DAPP_REMOVE_HISTORY, dapp);
    }

    private void setFragmentResult(String key, DApp dapp)
    {
        Bundle result = new Bundle();
        result.putParcelable(key, dapp);
        getParentFragmentManager().setFragmentResult(DAPP_CLICK, result);
    }

    private List<DApp> getData() {
        return DappBrowserUtils.getBrowserHistory(getContext());
    }
}
