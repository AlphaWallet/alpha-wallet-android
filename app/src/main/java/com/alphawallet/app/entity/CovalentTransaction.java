package com.alphawallet.app.entity;

import com.alphawallet.app.entity.tokenscript.EventUtils;

import org.web3j.protocol.Web3j;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.alphawallet.app.repository.TokenRepository.getWeb3jService;

/**
 * Created by JB on 17/05/2021.
 */
public class CovalentTransaction
{
    public String block_signed_at;
    public String block_height;
    public String tx_hash;
    public int tx_offset;
    public boolean successful;
    public String from_address;
    public String from_address_label;
    public String to_address;
    public String to_address_label;
    public String value;
    public double value_quote;
    public long gas_offered;
    public String gas_spent;
    public String gas_price;
    public String input;
    public double gas_quote;
    public double gas_quote_rate;

    public static EtherscanTransaction[] toEtherscanTransactions(List<CovalentTransaction> transactions, NetworkInfo info)
    {
        List<EtherscanTransaction> converted = new ArrayList<>();
        for (CovalentTransaction tx : transactions)
        {
            try
            {
                Transaction rawTransaction = tx.fetchRawTransaction(info);
                converted.add(new EtherscanTransaction(tx, rawTransaction));
            }
            catch (Exception e)
            {
                //
            }
        }

        return converted.toArray(new EtherscanTransaction[0]);
    }

    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

    private Transaction fetchRawTransaction(NetworkInfo info) throws Exception
    {
        long transactionTime = format.parse(block_signed_at).getTime() / 1000;
        Web3j web3j = getWeb3jService(info.chainId);

        return EventUtils.getTransactionDetails(tx_hash, web3j)
                .map(ethTx -> new Transaction(ethTx.getResult(), info.chainId, true, transactionTime)).blockingGet();
    }
}
