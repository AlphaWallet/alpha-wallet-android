package com.wallet.crypto.trustapp.views;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.design.widget.CollapsingToolbarLayout;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.controller.Controller;
import com.wallet.crypto.trustapp.model.ESTransaction;

public class TransactionDetailFragment extends Fragment implements View.OnClickListener {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_TXN_HASH = "item_id";
    public static final String ARG_ADDRESS = "address";

    private static final String TAG = "TXN_DETAIL_FRAG";

    /**
     * The dummy content this fragment is presenting.
     */
    private ESTransaction mItem;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public TransactionDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Controller mController = Controller.with(getActivity());

        Log.d(TAG, getArguments().toString());

        if (getArguments().containsKey(ARG_TXN_HASH)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            String txn_hash = getArguments().getString(ARG_TXN_HASH);
            String address = mController.getCurrentAccount().getAddress();

            mItem = mController.findTransaction(address, txn_hash);

            if (mItem == null) {
                Log.e(TAG, "Can't find transaction");
            }

            Activity activity = this.getActivity();
            CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout) activity.findViewById(R.id.toolbar_layout);

            boolean isSent = mItem.getFrom().toLowerCase().equals(address.toLowerCase());
            String wei = mItem.getValue();

            String sign = "+";
            int color = Color.GREEN;

            if (isSent) {
                color = Color.RED;
                sign = "-";
            } else {
                sign = "+";
                color = Color.GREEN;
            }

            String eth = "0";
            try {
                eth = Controller.WeiToEth(wei, 5);
                Log.d(TAG, eth);
            } catch (Exception e) {
                Log.e(TAG, "Error WeiToEth");
            }

            if (!eth.equals("0")) {
                eth = sign + eth;
            }

            if (appBarLayout != null) {
                appBarLayout.setTitle(eth + " " + mController.getCurrentNetwork().getSymbol());
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.transaction_detail, container, false);

        // Show the dummy content as text in a TextView.
        if (mItem != null) {
            ((TextView) rootView.findViewById(R.id.from)).setText(mItem.getFrom());
            ((TextView) rootView.findViewById(R.id.to)).setText(mItem.getTo());
            ((TextView) rootView.findViewById(R.id.gas_fee)).setText(Controller.WeiToGwei(mItem.getGasUsed()));
            ((TextView) rootView.findViewById(R.id.confirmation)).setText(mItem.getConfirmations());
            ((TextView) rootView.findViewById(R.id.txn_hash)).setText(mItem.getHash());
            ((TextView) rootView.findViewById(R.id.txn_time)).setText(Controller.GetDate(Long.decode(mItem.getTimeStamp())));
            ((TextView) rootView.findViewById(R.id.block_hash)).setText(mItem.getBlockHash());
            rootView.findViewById(R.id.more_detail).setOnClickListener(this);
        }

        return rootView;
    }

    public void goToMoreDetails () {
        Uri uriUrl = Uri.parse(Controller.with(getActivity()).getCurrentNetwork().getEtherscanUrl() + "/tx/" + mItem.getHash());
        Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
        startActivity(launchBrowser);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.more_detail:
                goToMoreDetails();
                break;
        }
    }
}
