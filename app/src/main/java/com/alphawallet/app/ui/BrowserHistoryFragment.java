package com.alphawallet.app.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alphawallet.app.ui.widget.OnDappClickListener;
import com.alphawallet.app.ui.widget.OnHistoryItemRemovedListener;
import com.alphawallet.app.ui.widget.adapter.BrowserHistoryAdapter;
import com.alphawallet.app.util.DappBrowserUtils;

import java.util.List;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.DApp;
import com.alphawallet.app.widget.AWalletAlertDialog;


public class BrowserHistoryFragment extends Fragment {
    private BrowserHistoryAdapter adapter;
    private OnDappClickListener onDappClickListener;
    private OnHistoryItemRemovedListener onHistoryItemRemovedListener;
    private AWalletAlertDialog dialog;
    private TextView clear;
    private TextView noHistory;

    void setCallbacks(OnDappClickListener onDappClickListener,
                      OnHistoryItemRemovedListener onHistoryItemRemovedListener) {
        this.onDappClickListener = onDappClickListener;
        this.onHistoryItemRemovedListener = onHistoryItemRemovedListener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_browser_history, container, false);
        adapter = new BrowserHistoryAdapter(
                getData(),
                onDappClickListener,
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
        onHistoryItemRemovedListener.onHistoryItemRemoved(dapp);
        DappBrowserUtils.removeFromHistory(getContext(), dapp);
        adapter.setDapps(getData());
        showOrHideViews();
    }

    private List<DApp> getData() {
        return DappBrowserUtils.getBrowserHistory(getContext());
    }
}
