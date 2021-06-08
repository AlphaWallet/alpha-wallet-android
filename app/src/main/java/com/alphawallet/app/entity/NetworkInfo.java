package com.alphawallet.app.entity;

import android.net.Uri;

import androidx.annotation.Nullable;

import static com.alphawallet.app.repository.EthereumNetworkBase.COVALENT;

public class NetworkInfo extends com.alphawallet.ethereum.NetworkInfo {
    private final String BLOCKSCOUT_API = "blockscout";
    private final String MATIC_API = "maticvigil.com/api/v2/transactions";

    public  String backupNodeUrl = null;
    public  String etherscanTxUrl = null; //This is used by the API call to fetch transactions

    public NetworkInfo(
            String name,
            String symbol,
            String rpcServerUrl,
            String etherscanUrl,
            int chainId,
            String backupNodeUrl,
            String etherscanTxUrl) {
        super(name, symbol, rpcServerUrl, etherscanUrl, chainId);
        this.backupNodeUrl = backupNodeUrl;
        this.etherscanTxUrl = etherscanTxUrl;
    }

    public String getShortName()
    {
        int index = this.name.indexOf(" (Test)");
        if (index > 0) return this.name.substring(0, index);
        else if (this.name.length() > 10) return this.symbol;
        else return this.name;
    }

    public boolean usesSeparateNFTTransferQuery()
    {
        return (!etherscanTxUrl.contains(BLOCKSCOUT_API) && !etherscanTxUrl.contains(MATIC_API) && !etherscanTxUrl.contains(COVALENT));
    }

    @Nullable
    public Uri getEtherscanUri(String transactionHash) {
        if (etherscanUrl != null)
        {
            return Uri.parse(etherscanUrl)
                    .buildUpon()
                    .appendEncodedPath(transactionHash)
                    .build();
        }
        else
        {
            return Uri.EMPTY;
        }
    }

    public Uri getEtherscanAddressUri(String toAddress)
    {
        if (etherscanUrl != null)
        {
            return Uri.parse(etherscanUrl)
                    .buildUpon()
                    .appendEncodedPath("address")
                    .appendEncodedPath(toAddress)
                    .build();
        }
        else
        {
            return Uri.EMPTY;
        }
    }
}
