package com.alphawallet.app.widget;


import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.alphawallet.app.R;

public class FilterDialog extends Dialog {
    private final Spinner spinnerDate;
    private final Spinner spinnerPrice;
    private final Spinner spinnerTimeframe;
    private final Spinner spinnerDistance;
    private final Button btnApply;

    public FilterDialog(Activity activity) {
        super(activity);
        setupDialog();

        spinnerDate = findViewById(R.id.spinner_date);
        spinnerDate.setAdapter(createAdapter(activity, R.array.filter_date));

        spinnerPrice = findViewById(R.id.spinner_price);
        spinnerPrice.setAdapter(createAdapter(activity, R.array.filter_price));

        spinnerDistance = findViewById(R.id.spinner_distance);
        spinnerDistance.setAdapter(createAdapter(activity, R.array.filter_distance));

        spinnerTimeframe = findViewById(R.id.spinner_timeframe);
        spinnerTimeframe.setAdapter(createAdapter(activity, R.array.filter_timeframe));

        btnApply = findViewById(R.id.btn_filter);

        btnApply.setOnClickListener(v -> {
            filter();
            dismiss();
        });
    }

    private void setupDialog() {
        setContentView(R.layout.dialog_filter);
        setCanceledOnTouchOutside(true);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setCanceledOnTouchOutside(true);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void filter() {
        //TODO: Filter action
//        int dateFilterId = (int) spinnerDate.getSelectedItemId();
//        int priceFilterId = (int) spinnerPrice.getSelectedItemId();
//        int distanceFilterId = (int) spinnerDistance.getSelectedItemId();
//        int timeframeFilterId = (int) spinnerTimeframe.getSelectedItemId();
    }

    private ArrayAdapter<CharSequence> createAdapter(Activity activity, int resId) {
        ArrayAdapter<CharSequence> adapter =  ArrayAdapter.createFromResource(activity, resId, R.layout.item_spinner);
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        return adapter;
    }
}
