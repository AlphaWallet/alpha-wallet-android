package com.alphawallet.app.entity;

import android.os.Parcel;
import android.os.Parcelable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

/**
 * Created by James on 23/02/2019.
 * Stormbird in Singapore
 */

public class QrUrlResult implements Parcelable
{
    private String protocol;
    private String address;
    private String functionStr;
    public int chainId;
    public BigInteger weiValue;
    public String functionDetail;
    public BigInteger gasLimit;
    public BigInteger gasPrice;
    public BigDecimal tokenAmount;
    public String functionToAddress;
    public String function; //formed function hex
    public EIP681Type type;

    public QrUrlResult(String address)
    {
        this.protocol = "address";
        this.address = address;
        defaultParams();
    }

    public QrUrlResult(String protocol, String address) {
        this.protocol = protocol;
        this.address = address;
        defaultParams();
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

    protected QrUrlResult(Parcel in)
    {
        protocol = in.readString();
        address = in.readString();
        chainId = in.readInt();
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

    public static final Creator<QrUrlResult> CREATOR = new Creator<QrUrlResult>()
    {
        @Override
        public QrUrlResult createFromParcel(Parcel in)
        {
            return new QrUrlResult(in);
        }

        @Override
        public QrUrlResult[] newArray(int size)
        {
            return new QrUrlResult[size];
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
        p.writeInt(chainId);
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
        if (params.size() == 0)
        {
            if (weiValue.compareTo(BigInteger.ZERO) > 0) type = EIP681Type.PAYMENT;
            return;
        }

        //TODO: Build function bytes
        StringBuilder sb = new StringBuilder();
        StringBuilder fd = new StringBuilder();

        if (functionStr != null) sb.append(functionStr);

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
                default:
                    break;
            }
        }
        sb.append(")");
        fd.append(")");

        functionStr = sb.toString();
        functionDetail = fd.toString();

        if (functionStr.startsWith("transfer"))
        {
            type = EIP681Type.TRANSFER;
        }
        else if (params.size() > 0)
        {
            type = EIP681Type.FUNCTION_CALL;
        }
        else
        {
            type = EIP681Type.OTHER;
        }
    }
}
