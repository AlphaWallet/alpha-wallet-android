package com.alphawallet.app.ui;

import android.content.Intent;
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
import com.alphawallet.app.ui.widget.adapter.MyDappsListAdapter;
import com.alphawallet.app.util.DappBrowserUtils;

import java.util.List;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.DApp;
import com.alphawallet.app.widget.AWalletAlertDialog;


public class MyDappsFragment extends Fragment {
    private MyDappsListAdapter adapter;
    private OnDappClickListener onDappClickListener;
    private AWalletAlertDialog dialog;
    private TextView noDapps;

    void setCallbacks(OnDappClickListener listener) {
        onDappClickListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_my_dapps, container, false);
        adapter = new MyDappsListAdapter(
                getData(),
                onDappClickListener,
                this::onDappRemoved,
                this::onDappEdited);
        RecyclerView list = view.findViewById(R.id.my_dapps_list);
        list.setNestedScrollingEnabled(false);
        list.setLayoutManager(new LinearLayoutManager(getActivity()));
        list.setAdapter(adapter);
        noDapps = view.findViewById(R.id.no_dapps);
        showOrHideViews();
        return view;
    }

    private List<DApp> getData() {
        return DappBrowserUtils.getMyDapps(getContext());
    }

    private void onDappEdited(DApp dapp) {
        Intent intent = new Intent(getActivity(), AddEditDappActivity.class);
        intent.putExtra("mode", 1);
        intent.putExtra("dapp", dapp);
        getActivity().startActivity(intent);
    }

    private void onDappRemoved(DApp dapp) {
        dialog = new AWalletAlertDialog(getActivity());
        dialog.setTitle(R.string.title_remove_dapp);
        dialog.setMessage(getString(R.string.remove_from_my_dapps, dapp.getName()));
        dialog.setIcon(AWalletAlertDialog.NONE);
        dialog.setButtonText(R.string.action_remove);
        dialog.setButtonListener(v -> {
            removeDapp(dapp);
            dialog.dismiss();
        });
        dialog.setSecondaryButtonText(R.string.dialog_cancel_back);
        dialog.show();
    }

    private void removeDapp(DApp dapp) {
        try
        {
            List<DApp> myDapps = DappBrowserUtils.getMyDapps(getContext());
            for (DApp d : myDapps)
            {
                if (d.getName().equals(dapp.getName())
                        && d.getUrl().equals(dapp.getUrl()))
                {
                    myDapps.remove(d);
                    break;
                }
            }
            DappBrowserUtils.saveToPrefs(getContext(), myDapps);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            updateData();
        }
    }

    private void updateData() {
        adapter.setDapps(DappBrowserUtils.getMyDapps(getContext()));
        showOrHideViews();
    }

    private void showOrHideViews() {
        if (adapter.getItemCount() > 0) {
            noDapps.setVisibility(View.GONE);
        } else {
            noDapps.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateData();
    }
}
