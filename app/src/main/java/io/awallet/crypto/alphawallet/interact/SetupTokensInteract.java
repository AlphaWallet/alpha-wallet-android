package io.awallet.crypto.alphawallet.interact;

/**
 * Created by James on 16/01/2018.
 */

import android.util.Log;

import org.web3j.utils.Numeric;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.awallet.crypto.alphawallet.entity.ERC875ContractTransaction;
import io.awallet.crypto.alphawallet.entity.Token;
import io.awallet.crypto.alphawallet.entity.TokenInfo;
import io.awallet.crypto.alphawallet.entity.TokenTransaction;
import io.awallet.crypto.alphawallet.entity.Transaction;
import io.awallet.crypto.alphawallet.entity.TransactionContract;
import io.awallet.crypto.alphawallet.entity.TransactionDecoder;
import io.awallet.crypto.alphawallet.entity.TransactionInput;
import io.awallet.crypto.alphawallet.entity.TransactionOperation;
import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.repository.TokenRepositoryType;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class SetupTokensInteract {

    private final static String TAG = "STI";
    private final TokenRepositoryType tokenRepository;
    private TransactionDecoder transactionDecoder = new TransactionDecoder();
    private Map<String, Token> contractMap = new ConcurrentHashMap<>();
    private List<String> unknownContracts = new ArrayList<>();
    private String walletAddr;

    public final static String UNKNOWN_CONTRACT = "[Unknown Contract]";
    public final static String EXPIRED_CONTRACT = "[Expired Contract]";
    public final static String INVALID_OPERATION = "[Invalid Operation]";
    public final static String CONTRACT_CONSTRUCTOR = "Contract Creation";
    public final static String RECEIVE_FROM_MAGICLINK = "Receive From MagicLink";

    public SetupTokensInteract(TokenRepositoryType tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    public Observable<TokenInfo> update(String address) {
        return tokenRepository.update(address)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public void setWalletAddr(String addr) { walletAddr = addr; }

    /**
     * Parse a transaction to fill in contract interaction information.
     * This is used to display the fuil transaction information on the list view
     *
     * @param token The token associated with the contract interaction
     * @param thisTrans The actual transaction
     * @param data The transaction input parsed by the TransactionDecoder class
     * @return New transaction that has the interaction information added. This has to be re-created since the Contract data is final.
     */
    private Transaction parseTransaction(Token token, Transaction thisTrans, TransactionInput data)
    {
        Transaction newTransaction = thisTrans;
        try
        {
            TransactionOperation op;
            TransactionOperation[] newOps;
            TransactionContract ct;
            ERC875ContractTransaction ect = null;
            String functionName = INVALID_OPERATION;

            if (token == null && !unknownContracts.contains(thisTrans.to))
            {
                unknownContracts.add(thisTrans.to);
            }

            //already has contract info
            if (thisTrans.operations.length > 0 &&
                    thisTrans.operations[0].contract instanceof ERC875ContractTransaction)
            {
                op = thisTrans.operations[0];
                ect = (ERC875ContractTransaction) thisTrans.operations[0].contract;
                ct = ect;
                functionName = ect.operation;
                newOps = thisTrans.operations;
                if (ect.type == -5)
                {
                    ect.type = -2;
                }
            }
            else if (data != null && data.functionData != null)
            {
                if (data.functionData.isERC875() || data.functionData.isConstructor()
                        || (token != null && token.tokenInfo.isStormbird))
                {
                    ect = new ERC875ContractTransaction();
                    ct = ect;

                    if (data.functionData != null)
                    {
                        ect.setIndicies(data.paramValues);
                    }
                    ect.operation = functionName;
                }
                else
                {
                    ct = new TransactionContract();
                }

                functionName = data.functionData.functionFullName;
                op = new TransactionOperation();
                newOps = new TransactionOperation[1];
                newOps[0] = op;
                op.contract = ct;
            }
            else
            {
                op = new TransactionOperation();
                ect = new ERC875ContractTransaction();
                newOps = new TransactionOperation[1];
                newOps[0] = op;
                ct = ect;
                op.contract = ct;
            }

            setupToken(token, ct, thisTrans);

            //we could ecrecover the seller here
            switch (functionName)
            {
                case "trade(uint256,uint16[],uint8,bytes32,bytes32)":
                    ect.operation = "Market purchase";
                    //until we can ecrecover from a signauture, we can't show our ticket as sold, but we can conclude it sold elsewhere, so this must be a buy
                    ect.type = 1; // buy/receive
                    break;
                case "transferFrom(address,address,uint16[])":
                    ect.operation = "Redeem";
                    if (!data.containsAddress(walletAddr))
                    {
                        //this must be an admin redeem
                        ect.operation = "Admin Redeem";
                    }
                    //one of our tickets was burned
                    ect.type = -1; //redeem
                    break;
                case "transfer(address,uint16[])":
                    //this could be transfer to or from
                    //if addresses contains our address then it must be a recieve
                    if (data.containsAddress(walletAddr))
                    {
                        ect.operation = "Receive From";
                        ect.type = 1; //buy/receive
                        ect.otherParty = thisTrans.from;
                    }
                    else
                    {
                        ect.operation = "Transfer To";
                        ect.type = -1; //sell
                        ect.otherParty = data.getFirstAddress();
                    }
                    break;
                case "transfer(address,uint256)":
                    //ERC20 transfer
                    op.from = thisTrans.from;
                    op.to = data.getFirstAddress();
                    op.transactionId = thisTrans.hash;
                    //value in what?
                    op.value = String.valueOf(data.getFirstValue());
                    break;
                case "loadNewTickets(bytes32[])":
                    ect.operation = data.functionData.functionName;
                    op.from = thisTrans.from;
                    op.to = token != null ? token.getAddress() : UNKNOWN_CONTRACT;
                    op.transactionId = thisTrans.hash;
                    op.value = String.valueOf(data.paramValues.size());
                    break;
                case "passTo(uint256,uint16[],uint8,bytes32,bytes32,address)":
                    if (data.containsAddress(walletAddr))
                    {
                        ect.operation = "Pass From";
                        ect.type = 1;
                    }
                    else
                    {
                        ect.operation = "Pass To";
                        ect.type = -1;
                    }
                    op.from = thisTrans.from;
                    op.to = data.getFirstAddress();
                    op.transactionId = thisTrans.hash;
                    //value in what?
                    op.value = String.valueOf(data.getFirstValue());
                    break;
                case "endContract()":
                    ect.operation = "Terminate";
                    ct.name = thisTrans.to;
                    ect.type = -2;
                    break;
                case CONTRACT_CONSTRUCTOR:
                    ct.name = thisTrans.to;
                    fillContractInformation(thisTrans, ct);
                    break;
                case INVALID_OPERATION:
                    if (ect != null)
                    {
                        ect.operation = INVALID_OPERATION;
                    }
                    break;
                default:
                    break;
            }

            newTransaction = new Transaction(thisTrans.hash,
                                             thisTrans.error,
                                             thisTrans.blockNumber,
                                             thisTrans.timeStamp,
                                             thisTrans.nonce,
                                             thisTrans.from,
                                             thisTrans.to,
                                             thisTrans.value,
                                             thisTrans.gas,
                                             thisTrans.gasPrice,
                                             thisTrans.input,
                                             thisTrans.gasUsed,
                                             newOps);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return newTransaction;
    }

    private void setupToken(Token token, TransactionContract ct, Transaction t) throws Exception
    {
        if (token != null)
        {
            if (token.isTerminated())
            {
                ct.name = EXPIRED_CONTRACT; //re-processing a contract that's been killed.
            }
            else
            {
                ct.name = token.getFullName();
            }
            ct.address = token.getAddress();
            ct.decimals = token.tokenInfo.decimals;
            ct.symbol = token.tokenInfo.symbol;
            ct.totalSupply = "0";
        }
        else
        {
            ct.name = UNKNOWN_CONTRACT;
            ct.address = t.to;
        }
    }

    private void fillContractInformation(Transaction trans, TransactionContract tc) throws Exception
    {
        if (tc instanceof ERC875ContractTransaction)
        {
            ERC875ContractTransaction ct = (ERC875ContractTransaction) tc;
            Token token = contractMap.get(trans.to); //filled in from EtherscanTransaction

            ct.operation = CONTRACT_CONSTRUCTOR;

            if (token != null)
            {
                ct.name = token.tokenInfo.name + " (" + token.getAddress() + ")";
                if (token.tokenInfo.isStormbird)
                {
                    ct.type = -3;

                }
                else
                {
                    ct.type = -2;
                }
            }
        }
    }

    /**
     * Is the user's wallet involved in this contract's transaction?
     * (we may have obtained the transaction by peeking at the list of transactions associated with a contract)
     *
     * @param trans
     * @param data
     * @param wallet
     * @return
     */
    private boolean walletInvolvedInTransaction(Transaction trans, TransactionInput data, Wallet wallet)
    {
        boolean involved = false;
        if (data == null || data.functionData == null)
        {
            return (trans.from.equalsIgnoreCase(walletAddr)); //early return
        }
        String walletAddr = Numeric.cleanHexPrefix(wallet.address);
        if (data.containsAddress(wallet.address)) involved = true;
        if (trans.from.contains(walletAddr)) involved = true;
        return involved;
    }

    //use this function to generate unit test string
    private void generateTestString(Transaction[] txList)
    {
        StringBuilder sb = new StringBuilder();

        sb.append("String[] inputTestList = {");
        boolean first = true;
        for (Transaction t : txList) {
            if (!first) {
                sb.append("\n,");
            }
            first = false;
            sb.append("\"");
            sb.append(t.input);
            sb.append("\"");
        }

        sb.append("};");
    }

    public void addTokenToMap(Token token)
    {
        contractMap.put(token.getAddress(), token);
    }

    /**
     * Transforms an array of Token - Transaction pairs into a processed list of transactions
     * Bascially just processes each set of transactions if they involve the wallet
     * @param txList
     * @param wallet
     * @return
     */
    public Observable<Transaction[]> processTokenTransactions(Wallet wallet, TokenTransaction[] txList)
    {
        return Observable.fromCallable(() -> {
            List<Transaction> processedTransactions = new ArrayList<Transaction>();
            try {
                for (TokenTransaction thisTokenTrans : txList) {
                    Transaction thisTrans = thisTokenTrans.transaction;
                    TransactionInput data = transactionDecoder.decodeInput(thisTrans.input);

                    if (walletInvolvedInTransaction(thisTrans, data, wallet)) {
                        Transaction newTx = parseTransaction(thisTokenTrans.token, thisTrans, data);
                        processedTransactions.add(newTx);
                    }
                }
                //System.out.println("After adding contract TX: " + String.valueOf(txMap.size()));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return processedTransactions.toArray(new Transaction[processedTransactions.size()]);
        });
    }

    /**
     * Parse all transactions not associated with known tokens and pick up unknown contracts
     * @param transactions
     * @param tokenMap
     * @return
     */
    public Observable<Transaction[]> processRemainingTransactions(Transaction[] transactions, Map<String, Token> tokenMap)
    {
        return Observable.fromCallable(() -> {
            List<Transaction> processedTxList = new ArrayList<>();
            //process the remaining transactions
            for (Transaction t : transactions)
            {
                if (t.input != null && t.input.length() > 20)
                {
                    TransactionInput data = transactionDecoder.decodeInput(t.input);
                    if (data != null && data.functionData != null)
                    {
                        Token localToken = tokenMap.get(t.to);
                        if (localToken == null && !unknownContracts.contains(t.to)) unknownContracts.add(t.to);
                        t = parseTransaction(localToken, t, data);
                    }
                }
                processedTxList.add(t);
            }

            return processedTxList.toArray(new Transaction[processedTxList.size()]);
        });
    }

    public Observable<TokenInfo> addToken(String address)
    {
        return tokenRepository.update(address);
    }

    public void setupUnknownList(Map<String, Token> tokenMap, String xmlContractAddress)
    {
        unknownContracts.clear();
        if (!tokenMap.containsKey(xmlContractAddress))
        {
            unknownContracts.add(xmlContractAddress);
        }
    }

    public List<String> getUnknownContracts(Transaction[] transactionList)
    {
        Log.d(TAG, "New unknown size: " + unknownContracts.size());
        return unknownContracts;
    }

    /**
     * Go back over the transactions list
     * @param token
     * @param txMap
     * @return
     */
    public Observable<Transaction[]> reProcessTokens(Token token, Map<String, Transaction> txMap)
    {
        Log.d(TAG, "Re Processing " + token.getFullName());
        return Observable.fromCallable(() -> {
            List<Transaction> processedTxList = new ArrayList<>();
            for (Transaction t : txMap.values())
            {
                if (t.to != null && t.to.equals(token.getAddress()) && t.input != null && t.input.length() > 20)
                {
                    TransactionInput data = transactionDecoder.decodeInput(t.input);
                    if (data != null && data.functionData != null)
                    {
                        t = parseTransaction(token, t, data);
                        processedTxList.add(t);
                        txMap.remove(t.hash);
                    }
                }
            }

            Log.d(TAG, "Re Processing " + processedTxList.size() + " : " + token.getFullName());
            return processedTxList.toArray(new Transaction[processedTxList.size()]);
        });
    }
}
