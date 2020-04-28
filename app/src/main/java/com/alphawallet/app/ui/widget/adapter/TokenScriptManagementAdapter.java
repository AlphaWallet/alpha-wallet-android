package com.alphawallet.app.ui.widget.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.TokenLocator;

import java.util.List;

public class TokenScriptManagementAdapter extends RecyclerView.Adapter<TokenScriptManagementAdapter.TokenHolder> {

    private Context context;
    private LayoutInflater inflater;
    private List<TokenLocator> tokenLocators;

    public TokenScriptManagementAdapter(Context context, List<TokenLocator> tokenLocators) {
        this.context = context;
        this.tokenLocators = tokenLocators;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public TokenHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new TokenHolder(inflater.inflate(R.layout.item_tokenscript_management, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull TokenHolder tokenHolder, int pos) {

        TokenLocator tokenLocator = tokenLocators.get(pos);

        tokenHolder.txtToken.setText(tokenLocator.name);
        tokenHolder.txtTokenAddress.setText(tokenLocator.getAddress());
        tokenHolder.txtTokenFile.setText(tokenLocator.getFileName());
    }

    @Override
    public int getItemCount() {
        return tokenLocators.size();
    }

    class TokenHolder extends RecyclerView.ViewHolder {

        TextView txtToken;
        TextView txtTokenFile;
        TextView txtTokenAddress;

        public TokenHolder(@NonNull View itemView) {
            super(itemView);

            txtToken = itemView.findViewById(R.id.token_name);
            txtTokenFile = itemView.findViewById(R.id.token_file);
            txtTokenAddress = itemView.findViewById(R.id.token_address);
        }
    }
}
