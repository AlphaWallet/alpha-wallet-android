package io.stormbird.wallet.entity;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import io.stormbird.wallet.R;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.entity.opensea.Asset;
import io.stormbird.wallet.ui.widget.holder.TokenHolder;
import io.stormbird.wallet.viewmodel.BaseViewModel;

/**
 * Created by James on 3/10/2018.
 * Stormbird in Singapore
 */
public class ERC721Token extends Token implements Parcelable
{
    public List<Asset> tokenBalance;

    public ERC721Token(TokenInfo tokenInfo, List<Asset> balanceList, long blancaTime) {
        super(tokenInfo, BigDecimal.ZERO, blancaTime);
        if (balanceList != null)
        {
            tokenBalance = balanceList;
        }
        else
        {
            tokenBalance = new ArrayList<>();
        }
        setTokenNetwork(1); //current only have ERC721 on mainnet
    }

    private ERC721Token(Parcel in) {
        super(in);
        tokenBalance = new ArrayList<>();
        //read in the element list
        int size = in.readInt();
        for (; size > 0; size--)
        {
            Asset asset = in.readParcelable(Asset.class.getClassLoader());
            tokenBalance.add(asset);
        }
    }

    public static final Creator<ERC721Token> CREATOR = new Creator<ERC721Token>() {
        @Override
        public ERC721Token createFromParcel(Parcel in) {
            return new ERC721Token(in);
        }

        @Override
        public ERC721Token[] newArray(int size) {
            return new ERC721Token[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(tokenBalance.size());
        for (Asset asset : tokenBalance)
        {
            dest.writeParcelable(asset, flags);
        }
    }

    @Override
    public boolean hasPositiveBalance()
    {
        return (tokenBalance.size() > 0);
    }

    @Override
    public boolean independentUpdate()
    {
        return true;
    }

    @Override
    public void setupContent(TokenHolder holder, AssetDefinitionService definition)
    {
        //721 Balance
        int balance = tokenBalance.size();

        holder.balanceEth.setText(String.valueOf(balance));
        holder.issuer.setText(R.string.ethereum);
        holder.layoutValueDetails.setVisibility(View.GONE);

        holder.contractType.setVisibility(View.VISIBLE);
        holder.contractSeparator.setVisibility(View.VISIBLE);
        holder.contractType.setText(R.string.erc721);

        holder.balanceEth.setVisibility(View.VISIBLE);
        holder.arrayBalance.setVisibility(View.GONE);
    }

    @Override
    public void clickReact(BaseViewModel viewModel, Context context)
    {
        viewModel.showRedeemToken(context, this);
    }

    @Override
    public int getTicketCount()
    {
        return tokenBalance.size();
    }

    @Override
    public String getFullBalance()
    {
        return String.valueOf(tokenBalance.size());
    }
}
