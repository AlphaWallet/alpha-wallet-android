package io.stormbird.wallet.entity;

import java.math.BigInteger;
import java.util.List;

/**
 * Created by James on 23/02/2019.
 * Stormbird in Singapore
 */

public class QrUrlResult {
    private String protocol;
    private String address;
    private String functionStr;
    public int chainId;
    public BigInteger weiValue;
    public String functionDetail;
    public BigInteger gasLimit;
    public BigInteger gasPrice;
    public byte[] function; //formed function bytes

    public QrUrlResult(String address)
    {
        this.protocol = "address";
        this.address = address;
    }

    public QrUrlResult(String protocol, String address) {
        this.protocol = protocol;
        this.address = address;
        chainId = 1;
        functionStr = "";
        functionDetail = "";
        gasLimit = null;
        gasPrice = null;
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
        if (params.size() == 0) return;
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
        }
        sb.append(")");
        fd.append(")");

        functionStr = sb.toString();
        functionDetail = fd.toString();
    }
}
