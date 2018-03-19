package com.wallet.crypto.alphawallet.interact;

/**
 * Created by James on 16/01/2018.
 */

import com.wallet.crypto.alphawallet.entity.ERC875ContractTransaction;
import com.wallet.crypto.alphawallet.entity.Token;
import com.wallet.crypto.alphawallet.entity.TokenInfo;
import com.wallet.crypto.alphawallet.entity.TokenTransaction;
import com.wallet.crypto.alphawallet.entity.Transaction;
import com.wallet.crypto.alphawallet.entity.TransactionDecoder;
import com.wallet.crypto.alphawallet.entity.TransactionInput;
import com.wallet.crypto.alphawallet.entity.TransactionOperation;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.repository.TokenRepositoryType;

import org.web3j.utils.Numeric;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class SetupTokensInteract {

    private final TokenRepositoryType tokenRepository;
    private TransactionDecoder transactionDecoder = new TransactionDecoder();
    private Map<String, Token> contractMap = new ConcurrentHashMap<>();
    private Map<String, Transaction> txMap = new ConcurrentHashMap<>();
    private List<String> newTxList = new ArrayList<>();
    private Map<String, TokenTransaction> ttxMap = new ConcurrentHashMap<>();
    private List<Token> tokenCheckList = new ArrayList<>();
    private List<String> requiredContracts = new ArrayList<>();
    private List<String> deadContracts = new ArrayList<>();
    private String walletAddr;

    public SetupTokensInteract(TokenRepositoryType tokenRepository) {
        this.tokenRepository = tokenRepository;
        deadContracts.clear();
    }

    public Observable<TokenInfo> update(String address) {
        return tokenRepository.update(address)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public List<Token> getTokenCheckList() { return tokenCheckList; }
    public List<String> getRequiredContracts() { return requiredContracts; }
    public void setWalletAddr(String addr) { walletAddr = addr; }

    //If we stored any new contracts we need to re-parse the transaction list
    public void regenerateTransactionList() { txMap.clear(); }

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
        //now display the transaction in the list
        TransactionOperation op = new TransactionOperation();
        ERC875ContractTransaction ct = new ERC875ContractTransaction();
        op.contract = ct;

        if (token != null) {
            ct.address = token.getAddress();
            ct.name = token.getFullName();
        }
        else if (deadContracts.contains(thisTrans.to)) {
            ct.name = "[Expired Contract]";
            ct.address = thisTrans.to;
        }
        else {
            ct.name = "[Unknown Contract]";
            ct.address = thisTrans.to;
        }
        ct.setIndicies(data.paramValues);
        ct.operation = data.functionData.functionName;

        TransactionOperation[] newOps = new TransactionOperation[1];
        newOps[0] = op;

        Transaction newTransaction = new Transaction(thisTrans.hash,
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


        //we could ecrecover the seller here
        switch (data.functionData.functionName) {
            case "trade":
                ct.operation = "Market purchase";
                //until we can ecrecover from a signauture, we can't show our ticket as sold, but we can conclude it sold elsewhere, so this must be a buy
                ct.type = 1; //buy/receive
                break;
            case "transferFrom":
                ct.operation = "Redeem";
                if (!data.containsAddress(walletAddr)) {
                    //this must be an admin redeem
                    ct.operation = "Admin Redeem";
                }
                //one of our tickets was burned
                ct.type = -1; //redeem
                break;
            case "transfer":
                //this could be transfer to or from
                //if addresses contains our address then it must be a recieve
                if (data.containsAddress(walletAddr)) {
                    ct.operation = "Receive From";
                    ct.type = 1; //buy/receive
                    ct.otherParty = thisTrans.from;
                } else {
                    ct.operation = "Transfer To";
                    ct.type = -1; //sell
                    ct.otherParty = data.getFirstAddress();
                }
                break;
            case "Contract Creation":
                ct.name = thisTrans.hash;
                break;
            default:
                break;
        }

        return newTransaction;
    }


    /**
     * Once we have gathered all the transactions together, parse them for known contract interactions
     * This is done in the background, hence why it's on a single process
     * @param wallet
     * @return
     */
    public Single<Transaction[]> processTransactions(Wallet wallet)
    {
        return Single.fromCallable(() -> {
            try {
                System.out.println(String.valueOf(txMap.size()));
                System.out.println(String.valueOf(newTxList.size()));

                for (TokenTransaction thisTokenTrans : ttxMap.values()) {
                    Transaction thisTrans = thisTokenTrans.transaction;
                    TransactionInput data = transactionDecoder.decodeInput(thisTrans.input);

                    if (walletInvolvedInTransaction(thisTrans, data, wallet)) {
                        Transaction newTx = parseTransaction(thisTokenTrans.token, thisTrans, data);

                        //TODO: OPTIMISE: work out if we need a list update - not important right now
                        //TODO: NB need to allow for Transactions being changed during parseTransaction and any new transctions added
                        txMap.put(newTx.hash, newTx);
                    }
                }

                Transaction[] processedTransactions = txMap.values().toArray(new Transaction[txMap.size()]);

                System.out.println("After adding contract TX: " + String.valueOf(txMap.size()));
                return processedTransactions;
            }
            finally {

            }
        });
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
        if (data == null || data.functionData == null) return false; //early return
        String walletAddr = Numeric.cleanHexPrefix(wallet.address);
        if (data.containsAddress(wallet.address)) involved = true;
        if (trans.from.contains(walletAddr)) involved = true;
        return involved;
    }

    /**
     * Receive an array containing all the transactions retrieved from the user's wallet address
     * @param txArray array of transaction types
     * @return Dummy object
     */
    public Single<String> checkTransactions(Transaction[] txArray) {
        return Single.fromCallable(() -> {
            try {
                tokenCheckList.clear();
                txMap.clear();
                newTxList.clear();
                //see if there's any ERC875 tokens
                for (Token t : contractMap.values())
                {
                    if (t.tokenInfo.isStormbird)
                    {
                        tokenCheckList.add(t);
                    }
                }

                for (Transaction t : txArray) {
                    if (!txMap.containsKey(t.hash)) {
                        if (!newTxList.contains(t.hash)) {
                            newTxList.add(t.hash); //should re-check this transaction
                        }
                        txMap.put(t.hash, t);
                        //see if this transaction involves a contract we don't know about
                        detectContractTransactions(t);
                        Token localToken = contractMap.get(t.to);
                        if (localToken != null)
                        {
                            ttxMap.put(t.hash, new TokenTransaction(localToken, t));
                        }
                    }
                }
                return "null";
            } finally {

            }
        });
    }

    /**
     * Identify which contract this transaction involves - if we haven't cached it then add to the required fetch list
     * @param t Transaction to check
     */
    private void detectContractTransactions(Transaction t)
    {
        if (t.input != null && t.input.length() > 20)
        {
            try {
                TransactionInput data = transactionDecoder.decodeInput(t.input);
                if (data != null && data.functionData != null)
                {
                    Token localToken = contractMap.get(t.to);
                    if (localToken == null && !requiredContracts.contains(t.to)
                            && !deadContracts.contains(t.to)
                            && isAddressValid(t.to)) // don't add to contract watch list if we know it's a dead contract
                    {
                        //contract transaction we haven't cached
                        requiredContracts.add(t.to); //make a note we interacted with a contract, potentially
                    }

                    //pre-process the transaction since we identified this relates to a contract
                    Transaction newTx = parseTransaction(localToken, t, data);
                    txMap.put(t.hash, newTx);
                }
            }
            catch (Exception e) {

            }
        }
    }

    public int getLocalTokensCount()
    {
        return contractMap.size();
    }

    public void putDeadContract(String address)
    {
        if (requiredContracts.contains(address))
        {
            requiredContracts.remove(address);
        }
        if (!deadContracts.contains(address))
        {
            deadContracts.add(address);
        }
    }

    boolean isAddressValid(String address) {
        if (address == null) return false;
        else if (address.length() < 20) return false;
        else return true;
    }

    public void setTokens(Token[] tokens) {
        contractMap.clear();
        for (Token t : tokens)
        {
            contractMap.put(t.getAddress(), t);
        }
    }

    /**
     * Receives an array of transactions from a specific ethereum address (which is one of our cached contract tokens)
     * @param txList Array of transactions and their associated token
     */
    public void addTokenTransactions(TokenTransaction[] txList)
    {
        for (TokenTransaction tt : txList)
        {
            if (!ttxMap.containsKey(tt.transaction.hash))
            {
                ttxMap.put(tt.transaction.hash, tt);
                if (!newTxList.contains(tt.transaction.hash)) {
                    newTxList.add(tt.transaction.hash); //should re-check this transaction
                }
            }
        }
    }
}
