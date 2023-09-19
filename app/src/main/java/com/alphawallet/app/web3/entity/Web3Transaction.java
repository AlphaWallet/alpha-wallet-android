package com.alphawallet.app.web3.entity;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.text.style.StyleSpan;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.analytics.ActionSheetMode;
import com.alphawallet.app.entity.walletconnect.SignType;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.util.Hex;
import com.alphawallet.app.util.StyledStringBuilder;
import com.alphawallet.app.walletconnect.entity.WCEthereumTransaction;

import org.web3j.protocol.core.methods.request.Transaction;

import java.math.BigDecimal;
import java.math.BigInteger;


public class Web3Transaction implements Parcelable
{
    public final Address recipient;
    public final Address contract;
    public final Address sender;
    public final BigInteger value;
    public final BigInteger gasPrice;
    public final BigInteger gasLimit;

    // EIP1559
    public BigInteger maxFeePerGas;
    public BigInteger maxPriorityFeePerGas;

    public final long nonce;
    public final String payload;
    public final long leafPosition;
    public final String description;

    public Web3Transaction(
            Address sender,
            Address contract,
            BigInteger value,
            BigInteger gasPrice,
            BigInteger gasLimit,
            long nonce,
            String payload)
    {
        this.recipient = contract;
        this.sender = sender;
        this.contract = contract;
        this.value = value;
        this.gasPrice = gasPrice;
        this.gasLimit = gasLimit;
        this.nonce = nonce;
        this.payload = payload;
        this.leafPosition = 0;
        this.description = null;
        this.maxPriorityFeePerGas = BigInteger.ZERO;
        this.maxFeePerGas = BigInteger.ZERO;
    }

    public Web3Transaction(
            Address recipient,
            Address contract,
            BigInteger value,
            BigInteger gasPrice,
            BigInteger gasLimit,
            long nonce,
            String payload,
            String description)
    {
        this.sender = null;
        this.recipient = recipient;
        this.contract = contract;
        this.value = value;
        this.gasPrice = gasPrice;
        this.gasLimit = gasLimit;
        this.nonce = nonce;
        this.payload = payload;
        this.leafPosition = 0;
        this.description = description;
        this.maxFeePerGas = BigInteger.ZERO;
        this.maxPriorityFeePerGas = BigInteger.ZERO;
    }

    public Web3Transaction(
            Address recipient,
            Address contract,
            BigInteger value,
            BigInteger maxFee,
            BigInteger maxPriorityFee,
            BigInteger gasLimit,
            long nonce,
            String payload,
            String description)
    {
        this.recipient = recipient;
        this.contract = contract;
        this.sender = null;
        this.value = value;
        this.gasPrice = BigInteger.ZERO;
        this.gasLimit = gasLimit;
        this.nonce = nonce;
        this.payload = payload;
        this.leafPosition = 0;
        this.description = description;
        this.maxFeePerGas = maxFee;
        this.maxPriorityFeePerGas = maxPriorityFee;
    }

    public Web3Transaction(
            Address recipient,
            Address contract,
            BigInteger value,
            BigInteger gasPrice,
            BigInteger gasLimit,
            long nonce,
            String payload,
            long leafPosition)
    {
        this.recipient = recipient;
        this.contract = contract;
        this.sender = null;
        this.value = value;
        this.gasPrice = gasPrice;
        this.gasLimit = gasLimit;
        this.nonce = nonce;
        this.payload = payload;
        this.leafPosition = leafPosition;
        this.description = null;
        this.maxFeePerGas = BigInteger.ZERO;
        this.maxPriorityFeePerGas = BigInteger.ZERO;
    }

    public Web3Transaction(
            Address recipient,
            Address contract,
            Address sender,
            BigInteger value,
            BigInteger gasPrice,
            BigInteger gasLimit,
            long nonce,
            String payload,
            long leafPosition)
    {
        this.recipient = recipient;
        this.contract = contract;
        this.sender = sender;
        this.value = value;
        this.gasPrice = gasPrice;
        this.gasLimit = gasLimit;
        this.nonce = nonce;
        this.payload = payload;
        this.leafPosition = leafPosition;
        this.description = null;
        this.maxFeePerGas = BigInteger.ZERO;
        this.maxPriorityFeePerGas = BigInteger.ZERO;
    }

    public Web3Transaction(
            Address recipient,
            Address contract,
            Address sender,
            BigInteger value,
            BigInteger maxFee,
            BigInteger maxPriorityFee,
            BigInteger gasLimit,
            long nonce,
            String payload,
            long leafPosition)
    {
        this.recipient = recipient;
        this.contract = contract;
        this.sender = sender;
        this.value = value;
        this.gasPrice = BigInteger.ZERO;
        this.gasLimit = gasLimit;
        this.nonce = nonce;
        this.payload = payload;
        this.leafPosition = leafPosition;
        this.description = null;
        this.maxFeePerGas = maxFee;
        this.maxPriorityFeePerGas = maxPriorityFee;
    }

    /**
     * Initialise from WalletConnect Transaction
     *
     * @param wcTx
     * @param callbackId
     */
    public Web3Transaction(WCEthereumTransaction wcTx, long callbackId, SignType signType)
    {
        String gasPrice = wcTx.getGasPrice() != null ? wcTx.getGasPrice() : "0";
        String gasLimit = wcTx.getGasLimit() != null ? wcTx.getGasLimit() : "0";
        String nonce = wcTx.getNonce() != null ? wcTx.getNonce() : "";

        this.recipient = TextUtils.isEmpty(wcTx.getTo()) ? Address.EMPTY : new Address(wcTx.getTo());
        this.contract = null;
        this.sender = TextUtils.isEmpty(wcTx.getFrom()) ? Address.EMPTY : new Address(wcTx.getFrom());
        this.value = wcTx.getValue() == null ? BigInteger.ZERO : Hex.hexToBigInteger(wcTx.getValue(), BigInteger.ZERO);
        this.gasPrice = Hex.hexToBigInteger(gasPrice, BigInteger.ZERO);
        this.gasLimit = Hex.hexToBigInteger(gasLimit, BigInteger.ZERO);
        this.maxFeePerGas = Hex.hexToBigInteger(wcTx.getMaxFeePerGas(), BigInteger.ZERO);
        this.maxPriorityFeePerGas = Hex.hexToBigInteger(wcTx.getMaxPriorityFeePerGas(), BigInteger.ZERO);
        this.nonce = Hex.hexToLong(nonce, -1);
        this.payload = wcTx.getData();
        this.leafPosition = callbackId;
        this.description = String.valueOf(signType.ordinal());
    }

    public SignType getSignType()
    {
        if (description != null && description.length() == 1 && Character.isDigit(description.charAt(0)))
        {
            int ordinal = Integer.parseInt(description);
            return SignType.values()[ordinal];
        }
        else
        {
            return SignType.SEND_TX;
        }
    }

    /**
     * Initialise from previous Transaction for Resending (Speeding up or cancelling)
     *
     * @param tx
     * @param mode
     * @param minGas
     */
    public Web3Transaction(com.alphawallet.app.entity.Transaction tx, ActionSheetMode mode, BigInteger minGas)
    {
        recipient = new Address(tx.to);
        contract = new Address(tx.to);
        value = (mode == ActionSheetMode.CANCEL_TRANSACTION) ? BigInteger.ZERO : new BigInteger(tx.value);
        gasPrice = minGas;
        gasLimit = new BigInteger(tx.gasUsed);
        nonce = tx.nonce;
        payload = (mode == ActionSheetMode.CANCEL_TRANSACTION) ? "0x" : tx.input;
        leafPosition = -1;
        description = null;
        maxFeePerGas = BigInteger.ZERO;
        maxPriorityFeePerGas = BigInteger.ZERO;
        sender = new Address(tx.from);
    }

    Web3Transaction(Parcel in)
    {
        recipient = in.readParcelable(Address.class.getClassLoader());
        contract = in.readParcelable(Address.class.getClassLoader());
        sender = in.readParcelable(Address.class.getClassLoader());
        value = new BigInteger(in.readString());
        gasPrice = new BigInteger(in.readString());
        gasLimit = new BigInteger(in.readString());
        maxFeePerGas = new BigInteger(in.readString());
        maxPriorityFeePerGas = new BigInteger(in.readString());
        nonce = in.readLong();
        payload = in.readString();
        leafPosition = in.readLong();
        description = in.readString();
    }

    public static final Creator<Web3Transaction> CREATOR = new Creator<Web3Transaction>()
    {
        @Override
        public Web3Transaction createFromParcel(Parcel in)
        {
            return new Web3Transaction(in);
        }

        @Override
        public Web3Transaction[] newArray(int size)
        {
            return new Web3Transaction[size];
        }
    };

    public Address getTransactionDestination()
    {
        if (this.contract != null)
        {
            return contract;
        }
        else
        {
            return recipient;
        }
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeParcelable(recipient, flags);
        dest.writeParcelable(contract, flags);
        dest.writeParcelable(sender, flags);
        dest.writeString((value == null ? BigInteger.ZERO : value).toString());
        dest.writeString((gasPrice == null ? BigInteger.ZERO : gasPrice).toString());
        dest.writeString((gasLimit == null ? BigInteger.ZERO : gasLimit).toString());
        dest.writeString((maxFeePerGas == null ? BigInteger.ZERO : maxFeePerGas).toString());
        dest.writeString((maxPriorityFeePerGas == null ? BigInteger.ZERO : maxPriorityFeePerGas).toString());
        dest.writeLong(nonce);
        dest.writeString(payload);
        dest.writeLong(leafPosition);
        dest.writeString(description);
    }

    public boolean isConstructor()
    {
        return (recipient.equals(Address.EMPTY) && payload != null);
    }

    public boolean isBaseTransfer()
    {
        return payload == null || payload.equals("0x");
    }

    /**
     * Can be used anywhere to generate an 'instant' human readable transaction dump
     *
     * @param ctx
     * @param chainId
     * @return
     */
    public CharSequence getFormattedTransaction(Context ctx, long chainId, String symbol)
    {
        StyledStringBuilder sb = new StyledStringBuilder();
        sb.startStyleGroup().append(ctx.getString(R.string.recipient)).append(": \n");
        sb.setStyle(new StyleSpan(Typeface.BOLD));
        sb.append(recipient.toString()).append("\n");

        sb.startStyleGroup().append("\n").append(ctx.getString(R.string.value)).append(": \n");
        sb.setStyle(new StyleSpan(Typeface.BOLD));
        sb.append(BalanceUtils.getScaledValueWithLimit(new BigDecimal(value), 18));
        sb.append(" ").append(symbol).append("\n");

        sb.startStyleGroup().append("\n").append(ctx.getString(R.string.label_gas_limit)).append(": \n");
        sb.setStyle(new StyleSpan(Typeface.BOLD));
        sb.append(gasLimit.toString()).append("\n");

        if (nonce >= 0)
        {
            sb.startStyleGroup().append("\n").append(ctx.getString(R.string.label_nonce)).append(": \n");
            sb.setStyle(new StyleSpan(Typeface.BOLD));
            sb.append(String.valueOf(nonce)).append("\n");
        }

        if (!TextUtils.isEmpty(payload))
        {
            sb.startStyleGroup().append("\n").append(ctx.getString(R.string.payload)).append(": \n");
            sb.setStyle(new StyleSpan(Typeface.BOLD));
            sb.append(payload).append("\n");
        }

        sb.startStyleGroup().append("\n").append(ctx.getString(R.string.subtitle_network)).append(": \n");
        sb.setStyle(new StyleSpan(Typeface.BOLD));
        sb.append(EthereumNetworkBase.getNetworkInfo(chainId).getShortName()).append("\n");

        if (isLegacyTransaction())
        {
            if (gasPrice.compareTo(BigInteger.ZERO) > 0)
            {
                sb.startStyleGroup().append("\n").append(ctx.getString(R.string.label_gas_price)).append(": \n");
                sb.setStyle(new StyleSpan(Typeface.BOLD));
                sb.append(BalanceUtils.weiToGwei(gasPrice)).append("\n");
            }
        }
        else
        {
            sb.startStyleGroup().append("\n").append("Max Priority").append(": \n");
            sb.setStyle(new StyleSpan(Typeface.BOLD));
            sb.append(BalanceUtils.weiToGwei(maxPriorityFeePerGas)).append("\n");

            sb.startStyleGroup().append("\n").append(ctx.getString(R.string.label_gas_price_max)).append(": \n");
            sb.setStyle(new StyleSpan(Typeface.BOLD));
            sb.append(BalanceUtils.weiToGwei(maxFeePerGas)).append("\n");
        }

        sb.applyStyles();

        return sb;
    }

    /**
     * Use this for debugging; it's sometimes handy to dump these transactions
     *
     * @param chainId
     * @return
     */
    public CharSequence getTxDump(long chainId)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Recipient: ").append(recipient.toString()).append(" : ");
        sb.append("Value: ").append(BalanceUtils.getScaledValueWithLimit(new BigDecimal(value), 18)).append(" : ");
        sb.append("Gas Limit: ").append(gasLimit.toString()).append(" : ");
        sb.append("Nonce: ").append(nonce).append(" : ");
        if (!TextUtils.isEmpty(payload))
        {
            sb.append("Payload: ").append(payload).append(" : ");
        }

        sb.append("Network: ").append(EthereumNetworkBase.getNetworkInfo(chainId).getShortName()).append(" : ");

        if (isLegacyTransaction())
        {
            sb.append("Gas Price: ").append(BalanceUtils.weiToGwei(gasPrice)).append(" : ");
        }
        else
        {
            sb.append("Max Priority: ").append(BalanceUtils.weiToGwei(maxPriorityFeePerGas)).append(" : ");
            sb.append("Max Fee Gas: ").append(BalanceUtils.weiToGwei(maxFeePerGas)).append(" : ");
        }

        return sb;
    }

    public Transaction getWeb3jTransaction(String walletAddress, long nonce)
    {
        return new Transaction(
                walletAddress,
                BigInteger.valueOf(nonce),
                gasPrice,
                gasLimit,
                recipient.toString(),
                value,
                payload);
    }

    public boolean isLegacyTransaction()
    {
        return !gasPrice.equals(BigInteger.ZERO) || maxFeePerGas.compareTo(BigInteger.ZERO) <= 0;
    }
}
