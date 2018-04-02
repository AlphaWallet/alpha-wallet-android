package io.awallet.crypto.alphawallet.interact;

/**
 * Created by James on 16/01/2018.
 */

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

import org.web3j.utils.Numeric;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.awallet.crypto.alphawallet.viewmodel.TransactionsViewModel;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
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

    public final static String UNKNOWN_CONTRACT = "[Unknown Contract]";
    public final static String EXPIRED_CONTRACT = "[Expired Contract]";
    public final static String INVALID_OPERATION = "[Invalid Operation]";
    public final static String CONTRACT_CONSTRUCTOR = "Contract Creation";

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
        Transaction newTransaction = thisTrans;
        try
        {
            TransactionOperation op = new TransactionOperation();
            TransactionContract ct;
            ERC875ContractTransaction ect = null;
            String functionName = INVALID_OPERATION;

            if (data != null && data.functionData != null)
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
            }
            else
            {
                ect = new ERC875ContractTransaction();
                ct = ect;
            }

            setupToken(token, ct, thisTrans);

            TransactionOperation[] newOps = new TransactionOperation[1];
            newOps[0] = op;
            op.contract = ct;

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
            if (token.getFullName() == null)
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
        else if (deadContracts.contains(t.to))
        {
            ct.name = EXPIRED_CONTRACT;
            ct.address = t.to;
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
     * Fetch info for required tokens and remove dead contracts before returning the stream
     *
     * @return Observable stream of token info which has dead contracts removed
     */
    public Observable<TokenInfo[]> addTokens()
    {
        return tokenRepository.update(requiredContracts.toArray(new String[requiredContracts.size()]))
                .flatMap(this::removeDeleted)
                .toObservable();
    }

    /**
     * Filter the received TokenInfo array -
     * Remove any dead contracts
     *
     * @param tokenInfos List of received TokenInfo to filter
     * @return
     */
    private Single<TokenInfo[]> removeDeleted(TokenInfo[] tokenInfos)
    {
        return Single.fromCallable(() -> {
            List<TokenInfo> checkList = new ArrayList<>();
            for (TokenInfo tInfo : tokenInfos)
            {
                if ((tInfo.name == null || tInfo.name.length() < 3)
                        || (tInfo.symbol == null || tInfo.symbol.length() < 2))
                {
                    putDeadContract(tInfo.address);
                }
                else
                {
                    checkList.add(tInfo);
                }
            }

            return checkList.toArray(new TokenInfo[checkList.size()]);
        });
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
                //System.out.println(String.valueOf(txMap.size()));
                //System.out.println(String.valueOf(newTxList.size()));

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

                //System.out.println("After adding contract TX: " + String.valueOf(txMap.size()));
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
        if (data == null || data.functionData == null)
        {
            return (trans.from.equalsIgnoreCase(walletAddr)); //early return
        }
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
    public Single<Transaction[]> checkTransactions(Transaction[] txArray) {
        return Single.fromCallable(() -> {
            try {
                tokenCheckList.clear();
                txMap.clear();
                newTxList.clear(); //Probably redundant now - maybe refactor this out.

                //see if there's any ERC875 tokens
                //this is used in the viewModel to fetch all the contract transactions - this list is consumed by consumeTokenCheckList()
                for (Token t : contractMap.values())
                {
                    if (t.tokenInfo.isStormbird)
                    {
                        tokenCheckList.add(t);
                    }
                }

                //generateTestString(txArray);

                for (Transaction t : txArray) {
                    if (t.hash.contains("f8f3ef0"))
                    {
                        System.out.println("f8f3ef0");
                    }
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
                return txMap.values().toArray(new Transaction[txMap.values().size()]);
            }
            finally
            {

            }
        });
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

        System.out.println(sb.toString());
    }

    /**
     * Identify which contract this transaction involves - if we haven't cached it then add to the required fetch list
     * @param t Transaction to check
     */
    private void detectContractTransactions(Transaction t)
    {
        if (t.input != null && t.input.length() > 20)
        {
            try
            {
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

    public void checkTokens()
    {
        for (Token t : contractMap.values())
        {
            if (requiredContracts.contains(t.getAddress()))
            {
                System.out.println("Got token: " + t.getFullName());
            }
            else
            {
                System.out.println("Don't have: " + t.getFullName());
            }
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

    public int getMapSize()
    {
        return ttxMap.size();
    }
}
