package com.alphawallet.app.entity;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.Nullable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

/**
 * Created by James on 23/02/2019.
 * Stormbird in Singapore
 */

public class QRResult implements Parcelable
{
    private String protocol;
    private String address; //becomes the token address for a transfer
    private String functionStr;
    public long chainId;
    public BigInteger weiValue;
    public String functionDetail;
    public BigInteger gasLimit;
    public BigInteger gasPrice;
    public BigDecimal tokenAmount;
    public String functionToAddress; //destination address
    public String function; //formed function hex
    public EIP681Type type;

    public QRResult(String address)
    {
        this.protocol = "address";
        this.address = address;
        defaultParams();
    }

    public QRResult(String protocol, String address)
    {
        this.protocol = protocol;
        this.address = address;
        defaultParams();
    }

    public QRResult(String data, EIP681Type type)
    {
        this.type = type;
        this.address = data;
    }

    private void defaultParams()
    {
        chainId = 1;
        type = EIP681Type.ADDRESS;
        functionStr = "";
        functionDetail = "";
        functionToAddress = "";
        tokenAmount = BigDecimal.ZERO;
        gasLimit = BigInteger.ZERO;
        gasPrice = BigInteger.ZERO;
        weiValue = BigInteger.ZERO;
    }

    protected QRResult(Parcel in)
    {
        protocol = in.readString();
        address = in.readString();
        chainId = in.readLong();
        functionStr = in.readString();
        functionDetail = in.readString();
        gasLimit = new BigInteger(in.readString(), 16);
        gasPrice = new BigInteger(in.readString(), 16);
        weiValue = new BigInteger(in.readString(), 16);
        tokenAmount = new BigDecimal(in.readString());
        functionToAddress = in.readString();
        int resultType = in.readInt();
        type = EIP681Type.values()[resultType];
    }

    public static final Creator<QRResult> CREATOR = new Creator<QRResult>()
    {
        @Override
        public QRResult createFromParcel(Parcel in)
        {
            return new QRResult(in);
        }

        @Override
        public QRResult[] newArray(int size)
        {
            return new QRResult[size];
        }
    };

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel p, int flags)
    {
        p.writeString(protocol);
        p.writeString(address);
        p.writeLong(chainId);
        p.writeString(functionStr);
        p.writeString(functionDetail);
        p.writeString(gasLimit.toString(16));
        p.writeString(gasPrice.toString(16));
        p.writeString(weiValue.toString(16));
        p.writeString(tokenAmount.toString());
        p.writeString(functionToAddress);
        p.writeInt(type.ordinal());
    }

    public String getProtocol() {
        return protocol;
    }

    public String getAddress() {
        return address;
    }

    public String getFunction() { return functionStr; }
    public void setFunction(String function) { functionStr = function; }
    public String getFunctionDetail() { return functionDetail; }

    public BigInteger getValue() { return weiValue; }
    public BigInteger getGasPrice() { return gasPrice; }
    public BigInteger getGasLimit() { return gasLimit; }

    public void createFunctionPrototype(List<EthTypeParam> params)
    {
        boolean override = false;
        if (params.size() == 0)
        {
            if (weiValue.compareTo(BigInteger.ZERO) > 0) type = EIP681Type.PAYMENT;
        }

        //TODO: Build function bytes
        StringBuilder sb = new StringBuilder();
        StringBuilder fd = new StringBuilder();

        if (functionStr != null && functionStr.length() > 0) sb.append(functionStr);
        else
        {
            //isn't a function
            if (params.size() > 0)
            {
                override = true;
                //assume transfer request
                type = EIP681Type.TRANSFER;
            }
            else
            {
                return;
            }
        }

        sb.append("(");
        fd.append(sb.toString());
        boolean first = true;
        for (EthTypeParam param : params)
        {
            if (!first)
            {
                sb.append(",");
                fd.append(",");
            }
            sb.append(param.type);
            fd.append(param.type);
            fd.append("{");
            fd.append(param.value);
            fd.append("}");
            first = false;

            //Shortcut for ERC20 sends
            switch (param.type)
            {
                case "uint":
                case "uint256":
                    tokenAmount = new BigDecimal(param.value);
                    break;
                case "address":
                    functionToAddress = param.value;
                    break;
                case "token":
                    //ignore token name for now
                    break;
                case "contractAddress":
                    //If contractAddress is explicitly defined; then this.address must have been the destination address
                    functionToAddress = address;
                    address = param.value;
                    break;
                default:
                    break;
            }
        }
        sb.append(")");
        fd.append(")");

        functionStr = sb.toString();
        functionDetail = fd.toString();

        if (!override)
        {
            if (functionStr.startsWith("transfer"))
            {
                type = EIP681Type.TRANSFER;
            }
            else
            {
                type = EIP681Type.FUNCTION_CALL;
            }
        }

        if (type == EIP681Type.FUNCTION_CALL && isEmpty(functionDetail))
        {
            type = EIP681Type.OTHER;
        }
    }

    private boolean isEmpty(String val)
    {
        return (val == null || val.length() == 0);
    }
}
