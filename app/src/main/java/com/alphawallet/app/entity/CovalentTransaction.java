package com.alphawallet.app.entity;

import android.text.TextUtils;

import com.alphawallet.app.util.Utils;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    public LogEvent[] log_events;

    public class LogEvent
    {
        public int sender_contract_decimals;
        public String sender_name;
        public String sender_address;
        public String sender_contract_ticker_symbol;
        public LogDecode decoded;
        public String[] raw_log_topics;

        public Map<String, Param> getParams() throws Exception
        {
            Map<String, Param> params = new HashMap<>();
            if (decoded == null || decoded.params == null) return params;

            for (int index = 0; index < decoded.params.length; index++)
            {
                String rawLogValue = (index + 1) < raw_log_topics.length ? raw_log_topics[index + 1] : "";
                LogParam lp = decoded.params[index];
                Param param = new Param();
                param.type = lp.type;
                String rawValue = TextUtils.isEmpty(lp.value) || lp.value.equals("null") ? rawLogValue : lp.value;
                param.value = rawValue;
                if (lp.type.startsWith("uint") || lp.type.startsWith("int"))
                {
                    param.valueBI = Utils.stringToBigInteger(rawValue);// rawValue.startsWith("0x") ? Numeric.toBigInt(rawValue) : new BigInteger(rawValue);
                }

                params.put(lp.name, param);
            }

            return params;
        }
    }

    class Param
    {
        public String type;
        public String value;
        public BigInteger valueBI;
    }

    class LogDecode
    {
        public String name;
        public String signature;
        public LogParam[] params;

    }

    class LogParam
    {
        public String name;
        public String type;
        public String value;
    }

    public static EtherscanTransaction[] toEtherscanTransactions(CovalentTransaction[] transactions, NetworkInfo info)
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

    public String determineContractAddress()
    {
        if (log_events == null || log_events.length == 0) return "";
        for (LogEvent le : log_events)
        {
            if (le.sender_address != null)
            {
                return le.sender_address;
            }
        }

        return "";
    }

    private EtherscanEvent getEtherscanTransferEvent(LogEvent logEvent) throws Exception
    {
        if (logEvent == null || logEvent.decoded == null || !logEvent.decoded.name.equals("Transfer")) return null;

        EtherscanEvent ev = new EtherscanEvent();
        ev.tokenDecimal = String.valueOf(logEvent.sender_contract_decimals);
        ev.timeStamp = format.parse(block_signed_at).getTime() / 1000;
        ev.hash = tx_hash;
        ev.nonce = 0;
        ev.tokenName = logEvent.sender_name;
        ev.tokenSymbol = logEvent.sender_contract_ticker_symbol;
        ev.contractAddress = logEvent.sender_address;
        ev.blockNumber = block_height;

        Map<String, Param> logParams = logEvent.getParams();

        ev.from = logParams.containsKey("from") ? logParams.get("from").value : "";
        ev.to = logParams.containsKey("to") ? logParams.get("to").value : "";
        ev.tokenID = logParams.containsKey("tokenId") ? logParams.get("tokenId").valueBI.toString() : "";
        ev.value = logParams.containsKey("value") ? logParams.get("value").valueBI.toString() : "";

        ev.gasUsed = gas_spent;
        ev.gasPrice = gas_price;
        ev.gas = String.valueOf(gas_offered);

        return ev;
    }

    public static EtherscanEvent[] toEtherscanEvents(CovalentTransaction[] transactions)
    {
        List<EtherscanEvent> converted = new ArrayList<>();
        for (CovalentTransaction tx : transactions)
        {
            try
            {
                if (tx.log_events != null)
                {
                    for (LogEvent logEvent : tx.log_events)
                    {
                        if (logEvent.decoded != null && logEvent.decoded.name.equals("Transfer"))
                        {
                            EtherscanEvent ev = tx.getEtherscanTransferEvent(logEvent);
                            converted.add(ev);
                        }
                    }
                }
            }
            catch (Exception e)
            {
                //
            }
        }

        return converted.toArray(new EtherscanEvent[0]);
    }

    public static EtherscanTransaction[] toRawEtherscanTransactions(CovalentTransaction[] transactions, NetworkInfo info)
    {
        List<EtherscanTransaction> converted = new ArrayList<>();
        for (CovalentTransaction tx : transactions)
        {
            try
            {
                if (tx.log_events == null)
                {
                    Transaction rawTransaction = tx.fetchRawTransaction(info);
                    converted.add(new EtherscanTransaction(tx, rawTransaction));
                }
                else
                {
                    boolean hasTransfer = false;
                    for (LogEvent logEvent : tx.log_events)
                    {
                        if (logEvent.decoded != null && logEvent.decoded.name.equals("Transfer"))
                        {
                            hasTransfer = true;
                            break;
                        }
                    }

                    if (!hasTransfer)
                    {
                        Transaction rawTransaction = tx.fetchRawTransaction(info);
                        converted.add(new EtherscanTransaction(tx, rawTransaction));
                    }
                }
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
        return new Transaction(this, info.chainId, transactionTime);
    }
}
