package com.alphawallet.app.ui.widget.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.TokenLocator;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.util.Utils;
import com.alphawallet.token.entity.ContractInfo;
import com.alphawallet.token.entity.SigReturnType;
import com.alphawallet.token.entity.XMLDsigDescriptor;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class TokenScriptManagementAdapter extends RecyclerView.Adapter<TokenScriptManagementAdapter.TokenHolder> {

    private final Context context;
    private final LayoutInflater inflater;
    private final List<TokenLocator> tokenLocators;
    private final AssetDefinitionService assetDefinitionService;

    public TokenScriptManagementAdapter(Context context, List<TokenLocator> locators, AssetDefinitionService assetDefinitionService) {
        this.context = context;
        this.tokenLocators = new ArrayList<>(locators);
        this.assetDefinitionService = assetDefinitionService;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public TokenHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new TokenHolder(inflater.inflate(R.layout.item_tokenscript_management, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull TokenHolder tokenHolder, int pos) {

        TokenLocator tokenLocator = tokenLocators.get(pos);

        tokenHolder.txtToken.setText(tokenLocator.getDefinitionName());
        tokenHolder.txtTokenFile.setText(tokenLocator.getFileName());
        ContractInfo originContract = tokenLocator.getContracts();
        int chainId;
        String address;

        //sweep to see if there's a mainnet holding contract
        if (originContract.addresses.get(EthereumNetworkBase.MAINNET_ID) != null)
        {
            chainId = EthereumNetworkBase.MAINNET_ID;
            address = originContract.addresses.get(chainId).get(0);
        }
        else
        {
            chainId = originContract.addresses.keySet().iterator().next();
            address = originContract.addresses.get(chainId).iterator().next();
        }

        Token t = assetDefinitionService.getTokenFromService(chainId, address);
        if (t != null)
        {
            tokenHolder.chainName.setVisibility(View.VISIBLE);
            tokenHolder.chainName.setText(t.getNetworkName());
            Utils.setChainColour(tokenHolder.chainName, t.tokenInfo.chainId);
            String tokenSpec = context.getString(R.string.token_spec, address, originContract.contractInterface);
            tokenHolder.txtTokenAddress.setText(tokenSpec);
            tokenHolder.tokenFullName.setText(t.getFullName());
        }

        assetDefinitionService.getSignatureData(chainId, address)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(sig -> onSigData(sig, tokenHolder), Throwable::printStackTrace).isDisposed();
    }

    @Override
    public int getItemCount() {
        return tokenLocators.size();
    }

    public void setTokenScriptInfo(List<TokenLocator> tokenList)
    {
        tokenLocators.addAll(tokenList);
    }

    class TokenHolder extends RecyclerView.ViewHolder {

        TextView txtToken;
        TextView txtTokenFile;
        TextView txtTokenAddress;
        TextView tokenFullName;
        TextView chainName;
        ImageView imgLock;

        public TokenHolder(@NonNull View itemView) {
            super(itemView);

            txtToken = itemView.findViewById(R.id.token_definition_name);
            txtTokenFile = itemView.findViewById(R.id.token_file);
            txtTokenAddress = itemView.findViewById(R.id.token_address);
            tokenFullName = itemView.findViewById(R.id.token_name);
            chainName = itemView.findViewById(R.id.text_chain_name);
            imgLock = itemView.findViewById(R.id.image_lock);
        }
    }

    //TODO: Move this into a separate class to deduplicate
    private void onSigData(final XMLDsigDescriptor sigData, final TokenHolder tokenHolder)
    {
        SigReturnType type = sigData.type != null ? sigData.type : SigReturnType.NO_TOKENSCRIPT;
        tokenHolder.imgLock.setVisibility(View.VISIBLE);

        switch (type)
        {
            case NO_TOKENSCRIPT:
                tokenHolder.imgLock.setVisibility(View.GONE);
                break;
            case DEBUG_SIGNATURE_INVALID:
            case DEBUG_NO_SIGNATURE:
                tokenHolder.imgLock.setImageResource(R.mipmap.ic_unlocked_debug);
                break;
            case DEBUG_SIGNATURE_PASS:
                tokenHolder.imgLock.setImageResource(R.mipmap.ic_locked_debug);
                break;
            case SIGNATURE_INVALID:
            case NO_SIGNATURE:
                tokenHolder.imgLock.setImageResource(R.mipmap.ic_unverified);
                break;
            case SIGNATURE_PASS:
                tokenHolder.imgLock.setImageResource(R.mipmap.ic_locked);
                break;
        }
    }
}
