package io.stormbird.wallet.interact;

/**
 * Created by James on 16/01/2018.
 */

import android.util.Log;

import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.ERC875ContractTransaction;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.TokenInfo;
import io.stormbird.wallet.entity.TokenTransaction;
import io.stormbird.wallet.entity.Transaction;
import io.stormbird.wallet.entity.TransactionContract;
import io.stormbird.wallet.entity.TransactionDecoder;
import io.stormbird.wallet.entity.TransactionInput;
import io.stormbird.wallet.entity.TransactionOperation;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.repository.TokenRepositoryType;
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

    public static final String UNKNOWN_CONTRACT = "[Unknown Contract]";
    public static final String EXPIRED_CONTRACT = "[Expired Contract]";
    public static final String INVALID_OPERATION = "[Invalid Operation]";
    public static final String CONTRACT_CONSTRUCTOR = "Contract Creation";
    public static final String RECEIVE_FROM_MAGICLINK = "Receive From MagicLink";


    public SetupTokensInteract(TokenRepositoryType tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    public Observable<TokenInfo> update(String address) {
        return tokenRepository.update(address)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }


    public void clearAll()
    {
        contractMap.clear();
        unknownContracts.clear();
        walletAddr = null;
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
            if (token == null && !unknownContracts.contains(thisTrans.to))
            {
                unknownContracts.add(thisTrans.to);
            }

            if(thisTrans.operations == null ||
                    thisTrans.operations.length == 0 || data.functionData == null) return null; //shortcut, avoid extra processing in case something slips through

            //we should already have generated the structures
            TransactionOperation[] newOps = thisTrans.operations;
            TransactionOperation op = thisTrans.operations[0];
            TransactionContract ct = op.contract;

            setupToken(token, ct, thisTrans);

            switch (data.functionData.functionFullName)
            {
                case "trade(uint256,uint16[],uint8,bytes32,bytes32)":
                    ct.interpretTradeData(walletAddr, thisTrans);
                    break;
                case "transferFrom(address,address,uint16[])":
                    ct.interpretTransferFrom(walletAddr, data);
                    break;
                case "transfer(address,uint16[])":
                    ct.interpretTransfer(walletAddr, data);
                    break;
                case "transfer(address,uint256)": //ERC20 transfer
                    op.from = thisTrans.from;
                    op.to = data.getFirstAddress();
                    op.transactionId = thisTrans.hash;
                    //value in what?
                    op.value = String.valueOf(data.getFirstValue());
                    break;
                case "loadNewTickets(bytes32[])":
                    ct.setOperation(R.string.ticket_load_new_tickets);
                    op.from = thisTrans.from;
                    op.to = token != null ? token.getAddress() : "";
                    op.transactionId = thisTrans.hash;
                    op.value = String.valueOf(data.paramValues.size());
                    break;
                case "passTo(uint256,uint16[],uint8,bytes32,bytes32,address)":
                    ct.interpretPassTo(walletAddr, data);
                    op.from = thisTrans.from;
                    op.to = data.getFirstAddress();
                    op.transactionId = thisTrans.hash;
                    //value in what?
                    op.value = String.valueOf(data.getFirstValue());
                    break;
                case "endContract()":
                    ct.setOperation(R.string.ticket_terminate_contract);
                    ct.name = thisTrans.to;
                    ct.setType(-2);
                    break;
                case CONTRACT_CONSTRUCTOR:
                    ct.name = thisTrans.to;
                    fillContractInformation(thisTrans, ct);
                    break;
                case RECEIVE_FROM_MAGICLINK:
                    //ect.operation = RECEIVE_FROM_MAGICLINK;
                    op.value = String.valueOf(data.paramValues.size());
                    break;
                case INVALID_OPERATION:
                    ct.setOperation(R.string.ticket_invalid_op);
                    break;
                default:
                    ct.setOperation(R.string.ticket_invalid_op);
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
            if (token.isBad())
            {
                ct.name = "";// EXPIRED_CONTRACT; //re-processing a contract that's been killed.
            }
            else
            {
                ct.name = token.getFullName();
            }
            ct.address = token.getAddress();
            ct.decimals = token.tokenInfo.decimals;
            ct.symbol = token.tokenInfo.symbol;
            ct.totalSupply = "0";
            ct.badTransaction = false;
        }
        else
        {
            ct.name = "";
            ct.address = t.to;
            ct.badTransaction = true;
        }
    }

    private void fillContractInformation(Transaction trans, TransactionContract tc) throws Exception
    {
        if (tc instanceof ERC875ContractTransaction)
        {
            ERC875ContractTransaction ct = (ERC875ContractTransaction) tc;
            Token token = contractMap.get(trans.to); //filled in from EtherscanTransaction

            ct.operation = R.string.ticket_contract_constructor;
            //ct.operation = CONTRACT_CONSTRUCTOR;

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
        if (trans.operations != null && trans.operations.length > 0 && trans.operations[0].walletInvolvedWithTransaction(wallet.address)) involved = true;
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
                        if (newTx != null)
                        {
                            processedTransactions.add(newTx);
                        }
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
                    if (t.isConstructor || (data != null && data.functionData != null))
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

    public void setupUnknownList(Map<String, Token> tokenMap, List<String> xmlContractAddresses)
    {
        unknownContracts.clear();
        for (String address : xmlContractAddresses)
        {
            unknownContracts.add(address);
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
            if (token.getFullName() != null)
            {
                for (Transaction t : txMap.values())
                {
                    if (t.to != null && t.to.equals(token.getAddress()))
                    {
                        TransactionInput data = transactionDecoder.decodeInput(t.input);
                        if (data != null && data.functionData != null)
                        {
                            t = parseTransaction(token, t, data);
                            processedTxList.add(t);
                            if (t != null) txMap.remove(t.hash);
                        }
                    }
                }
            }

            Log.d(TAG, "Re Processing " + processedTxList.size() + " : " + token.getFullName());
            return processedTxList.toArray(new Transaction[processedTxList.size()]);
        });
    }
}
