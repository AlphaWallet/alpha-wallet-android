package com.alphawallet.app.ui.widget.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.AddressBookContact;
import com.alphawallet.app.ui.widget.OnAddressBookItemCLickListener;

import java.util.List;

import timber.log.Timber;

public class AddressBookAdapter extends RecyclerView.Adapter<AddressBookAdapter.ViewHolder> {
    private List<AddressBookContact> data;
    private OnAddressBookItemCLickListener listener;

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name, address;
        View root;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.icon);
            name = itemView.findViewById(R.id.name);
            address = itemView.findViewById(R.id.address);
            root = itemView.findViewById(R.id.layout_list_item);
        }
    }

    public AddressBookAdapter(List<AddressBookContact> data, OnAddressBookItemCLickListener listener) {
        this.data = data;
        this.listener = listener;
    }

    public void addItem(AddressBookContact item, int position) {
        try {
            data.add(position, item);
            notifyItemInserted(position);
        } catch(Exception e) {
            Timber.e(e);
        }
    }

    public void setData(List<AddressBookContact> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_contact, viewGroup, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        AddressBookContact addressBookContact = data.get(i);
        viewHolder.name.setText(addressBookContact.getName());
        viewHolder.address.setText(addressBookContact.getWalletAddress());

        if (!addressBookContact.getEthName().isEmpty()) {
            viewHolder.address.setText(addressBookContact.getEthName() + " | " + addressBookContact.getWalletAddress());
        } else {
            viewHolder.address.setText(addressBookContact.getWalletAddress());
        }
        viewHolder.root.setOnClickListener( (v ->{
            listener.OnAddressSelected(i, data.get(i));
        }));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public AddressBookContact removeItem(int position) {
        AddressBookContact item = null;
        try
        {
            item = data.get(position);
            data.remove(position);
            notifyItemRemoved(position);
        }
        catch(Exception e)
        {
            Timber.e(e);
        }
        return item;
    }
}
