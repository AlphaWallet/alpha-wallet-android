package com.alphawallet.app.widget;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.alphawallet.app.R;
import com.alphawallet.app.ui.widget.entity.OnQuantityChangedListener;
import com.alphawallet.app.ui.widget.entity.QuantitySelectorDialogInterface;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

public class QuantitySelectorDialog extends BottomSheetDialog implements OnQuantityChangedListener
{
    private final QuantitySelector quantitySelector;
    private final TextView maxText;
    private final TextView btnMax;
    private final MaterialButton confirmButton;
    private final ImageView closeImage;
    private final Context context;
    private final QuantitySelectorDialogInterface callback;

    public QuantitySelectorDialog(@NonNull Context context, QuantitySelectorDialogInterface callback)
    {
        super(context);
        View view = View.inflate(getContext(), R.layout.dialog_quantity_selector, null);
        setContentView(view);
        this.context = context;
        this.callback = callback;
        maxText = view.findViewById(R.id.max_text);
        btnMax = view.findViewById(R.id.btn_max);
        confirmButton = view.findViewById(R.id.btn_confirm);
        quantitySelector = view.findViewById(R.id.quantity_selector);
        closeImage = view.findViewById(R.id.image_close);
    }

    public int getQuantity()
    {
        return quantitySelector.getQuantity();
    }

    public void init(int balance, int position)
    {
        quantitySelector.init(balance, this);
        maxText.setText(context.getString(R.string.input_amount_max, String.valueOf(balance)));
        closeImage.setOnClickListener(v -> cancel());
        confirmButton.setOnClickListener(v -> confirm(position));
        btnMax.setOnClickListener(v -> quantitySelector.set(balance));
        setOnCancelListener(dialogInterface -> cancel(position));

        if (quantitySelector.getQuantity() > balance)
        {
            quantitySelector.reset();
        }
    }

    private void confirm(int position)
    {
        callback.onConfirm(position, getQuantity());
        dismiss();
    }

    private void cancel(int position)
    {
        callback.onCancel(position);
    }

    private boolean isValid(int quantity)
    {
        return quantitySelector.isValid(quantity);
    }

    @Override
    public void onQuantityChanged(int quantity)
    {
        confirmButton.setEnabled(isValid(quantity));
    }
}
