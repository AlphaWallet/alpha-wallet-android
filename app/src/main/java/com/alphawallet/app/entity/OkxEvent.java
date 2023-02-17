package com.alphawallet.app.entity;

import com.alphawallet.app.util.BalanceUtils;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class OkxEvent
{

    @SerializedName("txId")
    @Expose
    public String txId;

    @SerializedName("methodId")
    @Expose
    public String methodId;

    @SerializedName("blockHash")
    @Expose
    public String blockHash;

    @SerializedName("height")
    @Expose
    public String height;

    @SerializedName("transactionTime")
    @Expose
    public String transactionTime;

    @SerializedName("from")
    @Expose
    public String from;

    @SerializedName("to")
    @Expose
    public String to;

    @SerializedName("isFromContract")
    @Expose
    public boolean isFromContract;

    @SerializedName("isToContract")
    @Expose
    public boolean isToContract;

    @SerializedName("amount")
    @Expose
    public String amount;

    @SerializedName("transactionSymbol")
    @Expose
    public String transactionSymbol;

    @SerializedName("txFee")
    @Expose
    public String txFee;

    @SerializedName("state")
    @Expose
    public String state;

    @SerializedName("tokenId")
    @Expose
    public String tokenId;

    @SerializedName("tokenContractAddress")
    @Expose
    public String tokenContractAddress;

    @SerializedName("challengeStatus")
    @Expose
    public String challengeStatus;

    @SerializedName("l1OriginHash")
    @Expose
    public String l1OriginHash;

    public EtherscanEvent getEtherscanTransferEvent(boolean isNft) throws Exception
    {
//        final Web3j web3j = TokenRepository.getWeb3jService(OKX_ID);
//        final EthTransaction eTx = web3j.ethGetTransactionByHash(txId.trim()).send();

        EtherscanEvent ev = new EtherscanEvent();
        //ev.tokenDecimal = String.valueOf(logEvent.sender_contract_decimals); // TODO:
        ev.timeStamp = Long.parseLong(transactionTime) / 1000;
        ev.hash = txId;
        ev.nonce = 0; // TODO:
        ev.tokenName = "";
        ev.tokenSymbol = transactionSymbol;
        ev.contractAddress = tokenContractAddress;
        ev.blockNumber = String.valueOf(height);
        ev.from = from;
        ev.to = to;
        ev.tokenID = String.valueOf(tokenId);

        if (!isNft)
        {
            int decimals = 18; // TODO: decimals can be found via OkLinkService.getTokenDetails();
            BigDecimal bi = new BigDecimal(amount);
            ev.value = BalanceUtils.getScaledValueMinimal(bi,
                decimals, 5);
            ev.tokenDecimal = "18";
        }
        else
        {
            ev.value = amount;
            ev.tokenDecimal = "";
            ev.tokenValue = amount;
        }

        ev.gasUsed = "0"; // TODO:
        ev.gasPrice = "0"; // TODO:
        ev.gas = "0"; // TODO:

        return ev;
    }
}
