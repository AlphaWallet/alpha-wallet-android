package com.wallet.crypto.alphawallet.ui.widget.holder;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wallet.crypto.alphawallet.R;
import com.wallet.crypto.alphawallet.entity.SalesOrder;
import com.wallet.crypto.alphawallet.entity.TicketDecode;
import com.wallet.crypto.alphawallet.ui.widget.OnSalesOrderClickListener;
import com.wallet.crypto.alphawallet.ui.widget.OnTicketIdClickListener;
import com.wallet.crypto.alphawallet.ui.widget.entity.TicketRange;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by James on 21/02/2018.
 */

public class OrderHolder extends BinderViewHolder<SalesOrder> implements View.OnClickListener {

    public static final int VIEW_TYPE = 1166;

    private SalesOrder thisData;
    private OnSalesOrderClickListener onOrderClickListener;

    private final TextView price;
    private final TextView count;
    private final TextView date;
    private final TextView name;
    private final TextView ticketIds;
    private final TextView ticketCat;
    private final TextView ticketTypeText;

    public OrderHolder(int resId, ViewGroup parent) {
        super(resId, parent);
        name = findViewById(R.id.name);
        price = findViewById(R.id.price);
        count = findViewById(R.id.ticket_count);
        date = findViewById(R.id.date);
        ticketIds = findViewById(R.id.tickettext);
        ticketCat = findViewById(R.id.cattext);
        ticketTypeText = findViewById(R.id.ticket_type);
        itemView.setOnClickListener(this);
    }

    @Override
    public void bind(@Nullable SalesOrder data, @NonNull Bundle addition) {
        this.thisData = data;
        try {
            int seatStart = TicketDecode.getSeatIdInt(data.ticketStart);
            String seatRange = String.valueOf(seatStart);
            if (data.ticketCount > 1) seatRange = seatStart + "-" + (seatStart+(data.ticketCount-1));
            price.setText(String.valueOf(data.price));
            name.setText(data.tokenInfo.name);
            setBalance(data);
            date.setText(TicketDecode.getDate(data.ticketStart));
            ticketIds.setText(seatRange);
            ticketCat.setText(TicketDecode.getZone(data.ticketStart));
        } catch (Exception ex) {
            fillEmpty();
        }
    }

    private void fillEmpty() {
        name.setText(R.string.NA);
        price.setText(R.string.NA);
    }

    @Override
    public void onClick(View v) {
        if (onOrderClickListener != null) {
            onOrderClickListener.onOrderClick(v, thisData);
        }
    }

    public void setOnOrderClickListener(OnSalesOrderClickListener onTokenClickListener) {
        this.onOrderClickListener = onTokenClickListener;
    }

    private void setBalance(SalesOrder data)
    {
        if (data.balanceInfo != null)
        {
            //check if the required tickets are actually here
            List<Integer> newBalance = new ArrayList<>();
            boolean allIndicesUnsold = true;
            for (Integer index : data.tickets) //SalesOrder tickets member contains the list of ticket indices we're importing?
            {
                if (data.balanceInfo.size() > index)
                {
                    Integer ticketId = data.balanceInfo.get(index);
                    if (ticketId > 0)
                    {
                        newBalance.add(ticketId);
                    }
                    else
                    {
                        allIndicesUnsold = false;
                    }
                }
            }

            if (newBalance.size() > 0 && allIndicesUnsold)
            {
                //ticket bundle has been validated - it's currently available for purchase
                ticketTypeText.setText(R.string.tickets);
                count.setText(String.valueOf(newBalance.size()));
            }
            else
            {
                //received balance but this bundle is now unavailable
                count.setText(R.string.ticket_unavailable);
                ticketTypeText.setText("");
            }
        }
        else {
            //Waiting for balance (display waiting progress indicator)
            count.setText(R.string.NA);
            ticketTypeText.setText("");
        }
    }
}
