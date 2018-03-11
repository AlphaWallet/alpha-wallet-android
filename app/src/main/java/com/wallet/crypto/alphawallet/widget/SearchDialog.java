package com.wallet.crypto.alphawallet.widget;


import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.wallet.crypto.alphawallet.R;

public class SearchDialog extends Dialog {
    private Spinner spinnerMatch;
    private Spinner spinnerClass;
    private Spinner spinnerSeats;
    private Spinner spinnerDate;
    private Button btnApply;

    public SearchDialog(Activity activity) {
        super(activity);
        setupDialog();

        spinnerMatch = findViewById(R.id.spinner_match);
        spinnerMatch.setAdapter(createAdapter(activity, R.array.filter_match));

        spinnerClass = findViewById(R.id.spinner_class);
        spinnerClass.setAdapter(createAdapter(activity, R.array.filter_class));

        spinnerSeats = findViewById(R.id.spinner_seats);
        spinnerSeats.setAdapter(createAdapter(activity, R.array.filter_seats));

        spinnerDate = findViewById(R.id.spinner_date);
        spinnerDate.setAdapter(createAdapter(activity, R.array.filter_search_date));

        btnApply = findViewById(R.id.btn_filter);

        btnApply.setOnClickListener(v -> {
            search();
            dismiss();
        });
    }

    private void setupDialog() {
        setContentView(R.layout.dialog_search);
        setCanceledOnTouchOutside(true);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setCanceledOnTouchOutside(true);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void search() {
        //TODO: Search action
    }

    private ArrayAdapter<CharSequence> createAdapter(Activity activity, int resId) {
        ArrayAdapter<CharSequence> adapter =  ArrayAdapter.createFromResource(activity, resId, R.layout.item_spinner);
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        return adapter;
    }
}
