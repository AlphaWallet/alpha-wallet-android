package com.alphawallet.app.ui.widget.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokenscript.TokenScriptFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TokenScriptManagementAdapter extends RecyclerView.Adapter<TokenScriptManagementAdapter.TokenHolder> {

    private Context context;
    private LayoutInflater inflater;
    private Map<String, TokenScriptFile> values;
    private List<String> keyValues;

    public TokenScriptManagementAdapter(Context context, Map<String, TokenScriptFile> values) {
        this.context = context;
        this.values = values;
        inflater = LayoutInflater.from(context);
        keyValues = new ArrayList<>(values.keySet());
    }

    @Override
    public TokenHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i)
    {
        return new TokenHolder(inflater.inflate(R.layout.item_tokenscript_management,viewGroup,false));
    }

    @Override
    public void onBindViewHolder(@NonNull TokenHolder tokenHolder, int pos) {

        String tokenAddress = keyValues.get(pos);

        TokenScriptFile tokenFile = values.get(tokenAddress);

        tokenHolder.txtToken.setText(tokenFile.getTokenName());
        tokenHolder.txtTokenAddress.setText(tokenAddress);
        tokenHolder.txtTokenFile.setText(values.get(tokenAddress).getName());
    }

    @Override
    public int getItemCount() {
        return values.size();
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
