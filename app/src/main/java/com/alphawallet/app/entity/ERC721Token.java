package com.alphawallet.app.entity;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

import com.alphawallet.app.entity.opensea.Asset;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.ui.widget.holder.TokenHolder;
import com.alphawallet.app.viewmodel.BaseViewModel;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.alphawallet.app.R;

/**
 * Created by James on 3/10/2018.
 * Stormbird in Singapore
 */
public class ERC721Token extends Token implements Parcelable
{
    public List<Asset> tokenBalance;

    public ERC721Token(TokenInfo tokenInfo, List<Asset> balanceList, long blancaTime, String networkName, ContractType type) {
        super(tokenInfo, BigDecimal.ZERO, blancaTime, networkName, type);
        if (balanceList != null)
        {
            tokenBalance = balanceList;
        }
        else
        {
            tokenBalance = new ArrayList<>();
        }
        setInterfaceSpec(type);
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
    }

    @Override
    public Function getTransferFunction(String to, String tokenId)
    {
        Function function = null;
        try
        {
            BigInteger tokenIdBI = new BigInteger(tokenId);
            List<Type> params;
            List<TypeReference<?>> returnTypes = Collections.emptyList();
            if (tokenUsesLegacyTransfer())
            {
                params = Arrays.asList(new Address(to), new Uint256(tokenIdBI));
                function = new Function("transfer", params, returnTypes);
            }
            else
            {
                //function safeTransferFrom(address _from, address _to, uint256 _tokenId) external payable;
                params = Arrays.asList(new Address(getWallet()), new Address(to), new Uint256(tokenIdBI));
                function = new Function("safeTransferFrom", params, returnTypes);
            }
        }
        catch (NumberFormatException e)
        {
            e.printStackTrace();
        }

        return function;
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
        boolean firstItem = true;
        StringBuilder sb = new StringBuilder();
        for (Asset item : tokenBalance)
        {
            if (!firstItem) sb.append(",");
            sb.append(item.getTokenId());
            firstItem = false;
        }
        return sb.toString();
    }

    @Override
    public boolean isToken() {
        return false;
    }

    @Override
    public String getTransactionValue(Transaction transaction, Context ctx)
    {
        if (transaction.operations != null && transaction.operations.length > 0)
        {
            TransactionOperation operation = transaction.operations[0];
            return "#" + operation.value;
        }
        else
        {
            return "-"; //Placeholder - should never see this
        }
    }

    @Override
    protected String addSuffix(String result, Transaction transaction)
    {
        return result;
    }

    @Override
    public boolean checkIntrinsicType()
    {
        return (contractType == ContractType.ERC721);
    }

    @Override
    public boolean hasArrayBalance()
    {
        return true;
    }

    @Override
    public boolean hasPositiveBalance()
    {
        return tokenBalance != null && tokenBalance.size() > 0;
    }

    @Override
    protected float calculateBalanceUpdateWeight()
    {
        return 0.0f;
    }

    @Override
    public void zeroiseBalance()
    {
        tokenBalance.clear();
    }

    @Override
    public boolean requiresTransactionRefresh()
    {
        return false;
    }

    /**
     * This is a list of legacy contracts which are known to use the old ERC721 source,
     * which only had 'transfer' as the transfer function.
     * @return
     */
    private boolean tokenUsesLegacyTransfer()
    {
        switch (tokenInfo.address.toLowerCase())
        {
            case "0x06012c8cf97bead5deae237070f9587f8e7a266d":
            case "0xabc7e6c01237e8eef355bba2bf925a730b714d5f":
            case "0x71c118b00759b0851785642541ceb0f4ceea0bd5":
            case "0x16baf0de678e52367adc69fd067e5edd1d33e3bf":
            case "0x7fdcd2a1e52f10c28cb7732f46393e297ecadda1":
                return true;

            default:
                return false;
        }
    }
}
