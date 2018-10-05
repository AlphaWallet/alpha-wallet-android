package io.stormbird.wallet.entity;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import io.stormbird.wallet.R;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.ui.widget.holder.TokenHolder;
import io.stormbird.wallet.viewmodel.BaseViewModel;

/**
 * Created by James on 3/10/2018.
 * Stormbird in Singapore
 */
public class ERC721Token extends Token implements Parcelable
{
    public List<OpenseaElement> tokenBalance;

    public ERC721Token(TokenInfo tokenInfo, List<OpenseaElement> balanceList, long blancaTime) {
        super(tokenInfo, BigDecimal.ZERO, blancaTime);
        if (balanceList != null)
        {
            tokenBalance = balanceList;
        }
        else
        {
            tokenBalance = new ArrayList<>();
        }
    }

    private ERC721Token(Parcel in) {
        super(in);
        tokenBalance = new ArrayList<>();
        //read in the element list
        int size = in.readInt();
        for (; size > 0; size--)
        {
            OpenseaElement element = in.readParcelable(OpenseaElement.class.getClassLoader());
            tokenBalance.add(element);
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
        for (OpenseaElement element : tokenBalance)
        {
            dest.writeParcelable(element, flags);
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
        int balance = 0;
        for (OpenseaElement element : tokenBalance)
        {
            if (element.attributes.size() > 0 && element.imageURL != null && element.tokenId > 0)
            {
                balance++;
            }
        }

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
}
