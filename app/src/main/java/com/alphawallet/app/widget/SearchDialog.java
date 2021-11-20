package com.alphawallet.app.widget;


import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.alphawallet.app.R;

import java.util.Locale;

public class SearchDialog extends Dialog {
    private final Spinner spinnerMatch;
    private final Spinner spinnerClass;
    private final Spinner spinnerSeats;
    private final Spinner spinnerDate;
    private final SeekBar priceSeekBar;
    private final Button btnApply;
    private final TextView seekBarValue;

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

        seekBarValue = findViewById(R.id.seek_bar_value);

        priceSeekBar = findViewById(R.id.seek_bar_price);

        priceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double value = progress * 100.00;
                String val = String.format(Locale.getDefault(),"$%.2f", value);
                seekBarValue.setText(val);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
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
