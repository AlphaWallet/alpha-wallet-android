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

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class SetupTokensInteract {

    private final TokenRepositoryType tokenRepository;
    private TransactionDecoder transactionDecoder = new TransactionDecoder();
    private List<String> contractInteractionAddresses = new ArrayList<>();
    private Map<String, Token> contractMap = new HashMap<>();
    private Map<String, Transaction> txMap = new HashMap<>();
    private List<String> newTxList = new ArrayList<>();
    private Map<String, TokenTransaction> ttxMap = new HashMap<>();
    private List<Token> tokenCheckList = new ArrayList<>();
    private List<String> requiredContracts = new ArrayList<>();

    public SetupTokensInteract(TokenRepositoryType tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    public Observable<TokenInfo> update(String address) {
        return tokenRepository.update(address)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public List<String> getDetectedContracts() { return contractInteractionAddresses; }
    public List<Token> getTokenCheckList() { return tokenCheckList; }
    public List<String> getRequiredContracts() { return requiredContracts; }

    //Temporarily in this file until we move it into its own service
    public Single<Transaction[]> processTransactions(Wallet wallet)
    {
        return Single.fromCallable(() -> {
            try
            {
                Transaction[] txList = onContractTokenTransactions(wallet);
                return txList;
            }
            finally {

            }
        });
    }


    //receive a list of transactions for the contract
    private Transaction[] onContractTokenTransactions(Wallet wallet)
    {

        System.out.println(String.valueOf(txMap.size()));
        System.out.println(String.valueOf(newTxList.size()));

        for (TokenTransaction thisTokenTrans : ttxMap.values())
        {
            Transaction thisTrans = thisTokenTrans.transaction;
            TransactionInput data = transactionDecoder.decodeInput(thisTrans.input);

            if (newTxList.contains(thisTrans.hash) && walletInvolvedInTransaction(thisTrans, data, wallet))
            {
                //now display the transaction in the list
                TransactionOperation op = new TransactionOperation();
                ERC875ContractTransaction ct = new ERC875ContractTransaction();
                op.contract = ct;

                ct.address = thisTokenTrans.token.getAddress();
                ct.setIndicies(data.paramValues);
                ct.name = thisTokenTrans.token.getFullName();
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

                if (txMap.containsKey(newTransaction.hash))
                {
                    txMap.remove(newTransaction.hash);
                }

                txMap.put(newTransaction.hash, newTransaction);

                //we could ecrecover the seller here
                switch (data.functionData.functionName)
                {
                    case "trade":
                        ct.operation = "Market purchase";
                        //until we can ecrecover from a signauture, we can't show our ticket as sold, but we can conclude it sold elsewhere, so this must be a buy
                        ct.type = 1; //buy/receive
                        break;
                    case "transferFrom":
                        ct.operation = "Redeem";
                        if (!data.containsAddress(wallet.address))
                        {
                            //this must be an admin redeem
                            ct.operation = "Admin Redeem";
                        }
                        //one of our tickets was burned
                        ct.type = -1; //redeem
                        break;
                    case "transfer":
                        //this could be transfer to or from
                        //if addresses contains our address then it must be a recieve
                        if (data.containsAddress(wallet.address))
                        {
                            ct.operation = "Receive From";
                            ct.type = 1; //buy/receive
                            ct.otherParty = thisTrans.from;
                        }
                        else
                        {
                            ct.operation = "Transfer To";
                            ct.type = -1; //sell
                            ct.otherParty = data.getFirstAddress();
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        Transaction[] processedTransactions = txMap.values().toArray(new Transaction[txMap.size()]);

        return processedTransactions;
    }

    private boolean walletInvolvedInTransaction(Transaction trans, TransactionInput data, Wallet wallet)
    {
        boolean involved = false;
        if (data == null || data.functionData == null) return false; //early return
        String walletAddr = Numeric.cleanHexPrefix(wallet.address);
        if (data.containsAddress(wallet.address)) involved = true;
        if (trans.from.contains(walletAddr)) involved = true;
        return involved;
    }

    public Single<String> checkTransactions(Transaction[] txArray) {
        return Single.fromCallable(() -> {
            try {
                tokenCheckList.clear();
                //txMap.clear();
                newTxList.clear();
                contractInteractionAddresses.clear();

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
                        newTxList.add(t.hash);
                        txMap.put(t.hash, t);
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

    private void detectContractTransactions(Transaction t)
    {
        if (t.input != null && t.input.length() > 20)
        {
            try {
                TransactionInput data = transactionDecoder.decodeInput(t.input);
                if (data != null && data.functionData != null)
                {
                    if (!contractInteractionAddresses.contains(t.to))
                    {
                        contractInteractionAddresses.add(t.to);
                    }
                    Token localToken = contractMap.get(t.to);
                    if (localToken == null && !requiredContracts.contains(t.to))
                    {
                        //contract transaction we haven't cached
                        requiredContracts.add(t.to); //make a note we interacted with a contract, potentially
                    }
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
    }

    public void setTokens(Token[] tokens) {
        contractMap.clear();
        for (Token t : tokens)
        {
            contractMap.put(t.getAddress(), t);
        }
    }

    public void addTokenTransactions(TokenTransaction[] txList)
    {
        for (TokenTransaction tt : txList)
        {
            if (!ttxMap.containsKey(tt.transaction.hash))
            {
                ttxMap.put(tt.transaction.hash, tt);
                if (!txMap.containsKey(tt.transaction.hash)) {
                    newTxList.add(tt.transaction.hash);
                }
            }
        }
    }
}
