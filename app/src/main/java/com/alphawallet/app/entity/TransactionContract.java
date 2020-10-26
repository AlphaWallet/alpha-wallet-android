package com.alphawallet.app.entity;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.ui.widget.entity.StatusType;
import com.alphawallet.app.ui.widget.holder.TransactionHolder;
import com.alphawallet.app.util.BalanceUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static com.alphawallet.app.entity.TransactionOperation.NORMAL_CONTRACT_TYPE;

public class TransactionContract implements Parcelable {
    public String address;
    public String name;
    public String totalSupply;
    public int decimals;
    public String symbol;
    public boolean badTransaction;

    public TransactionContract() {
        badTransaction = false;
    }

    public int contractType()
    {
        return NORMAL_CONTRACT_TYPE;
    }

    public TransactionType getOperationType()
    {
        return TransactionType.UNKNOWN;
    }

    private TransactionContract(Parcel in) {
        address = in.readString();
        name = in.readString();
        totalSupply = in.readString();
        decimals = in.readInt();
        symbol = in.readString();
        badTransaction = in.readInt() == 1;
    }

    public static final Creator<TransactionContract> CREATOR = new Creator<TransactionContract>() {
        @Override
        public TransactionContract createFromParcel(Parcel in) {
            return new TransactionContract(in);
        }

        @Override
        public TransactionContract[] newArray(int size) {
            return new TransactionContract[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(address);
        parcel.writeString(name);
        parcel.writeString(totalSupply);
        parcel.writeInt(decimals);
        parcel.writeString(symbol);
        parcel.writeInt(badTransaction ? 1 : 0);
    }

    public boolean checkAddress(String address)
    {
        return false; //this style of contract doesn't involve indirect addresses (only ERC875 'trade' and 'passTo' functions).
    }

    public void interpretTradeData(String walletAddr, Transaction thisTrans)
    {
        //do nothing for ERC20
    }

    public void interpretTransferFrom(String walletAddr, Transaction trans)
    {

    }

    public void interpretTransfer(String walletAddr)
    {

    }

    public void setOtherParty(String otherParty)
    {

    }

    public void setOperation(TransactionType operation)
    {
        if (operation == TransactionType.INVALID_OPERATION)
        {
            badTransaction = true;
        }
    }

    public void completeSetup(String currentAddress, Transaction trans)
    {

    }

    public void interpretPassTo(String walletAddr, Transaction transaction)
    {

    }

    public void setType(int type)
    {

    }

    public void setIndicies(List<BigInteger> indicies)
    {

    }

    public String getIndicesString()
    {
        return "";
    }

    public String getOperationName(Context ctx)
    {
        int operationId = getOperationId();
        if (operationId > -1)
        {
            return ctx.getString(TransactionLookup.typeToName(TransactionType.values()[operationId]));
        }
        else
        {
            return name;
        }
    }

    private int getOperationId()
    {
        int operationId = -1;
        if (name != null && name.length() > 0 && name.charAt(0) == '*')
        {
            operationId = Integer.parseInt(name.substring(1));
        }

        return operationId;
    }

    public String getIndicesSize()
    {
        return "0";
    }

    /**
     * Most likely ERC20 contract transaction result
     *
     * @param token
     * @param operation
     * @return
     */
    public String getOperationResult(Token token, TransactionOperation operation, Transaction tx)
    {
        try
        {
            if (!token.isNonFungible() && (operation.value == null || operation.value.length() > 0 && Character.isDigit(operation.value.charAt(0))))
            {
                BigDecimal value = new BigDecimal(operation.value);
                if (getOperationId() == TransactionType.APPROVE.ordinal() && value.abs().compareTo(token.balance.multiply(BigDecimal.TEN)) > 0) return "All";
                else return tx.getPrefix(token) + BalanceUtils.getScaledValueFixed(value, token.tokenInfo.decimals, TransactionHolder.TRANSACTION_BALANCE_PRECISION); //appears to be a number; try to produce number with prefix
            }
            else
            {
                return operation.value;
            }
        }
        catch (NumberFormatException e)
        {
            return operation.value;
        }
    }

    public StatusType getOperationImage(Token token, Transaction tx)
    {
        return token.ethereumTxImage(tx);
    }

    String getSupplimentalInfo(Transaction tx, String walletAddress, String networkName)
    {
        String supplimentalTxt;
        //is this sending or receiving value?
        if (tx.value.equals("0") || tx.value.equals("0x0") || (!TextUtils.isEmpty(tx.value) && (new BigDecimal(tx.value).compareTo(BigDecimal.ZERO) == 0)))
        {
            supplimentalTxt = "";
        }
        else
        {
            //simple heuristic: if value is attached to a transaction from the user, then it's outgoing
            if (tx.from.equalsIgnoreCase(walletAddress))
            {
                supplimentalTxt = "-" + BalanceUtils.getScaledValue(tx.value, C.ETHER_DECIMALS) + " " + networkName;
            }
            else
            {
                supplimentalTxt = "+" + BalanceUtils.getScaledValue(tx.value, C.ETHER_DECIMALS) + " " + networkName;
            }
        }

        return supplimentalTxt;
    }
}
