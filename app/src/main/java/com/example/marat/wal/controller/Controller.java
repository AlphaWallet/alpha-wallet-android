package com.example.marat.wal.controller;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.marat.wal.R;
import com.example.marat.wal.model.ESTransaction;
import com.example.marat.wal.model.ESTransactionListResponse;
import com.example.marat.wal.model.VMAccount;
import com.example.marat.wal.views.AccountListActivity;
import com.example.marat.wal.views.CreateAccountActivity;
import com.example.marat.wal.views.TransactionListActivity;
import com.example.marat.wal.views.SendActivity;

import org.ethereum.geth.Account;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.infura.InfuraHttpService;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by marat on 9/26/17.
 * Controller class which contains all business logic
 */

public class Controller {
    private static Controller mInstance;

    private static String TAG = "CONTROLLER";

    private Context mAppContext;
    private EtherStore mEtherStore;
    private String mKeystoreBaseDir;
    private Retrofit mRetrofit;
    private EtherscanService mEtherscanService;

    // View models
    ArrayList<VMAccount> mAccounts;
    Map<String, List<ESTransaction>> mTransactions;
    Map<String, Long> mBalances;

    // Views
    AccountListActivity mHomeActivity;
    TransactionListActivity mWalletActivity;

    public static Controller get() {
        if (mInstance == null) {
            mInstance = new Controller();
        }
        return mInstance;
    }

    protected Controller() { }

    public void init(Context appContext) {

        mAppContext = appContext;

        mKeystoreBaseDir = mAppContext.getFilesDir() + "/keystore";

        mEtherStore = new EtherStore(mKeystoreBaseDir);

        mRetrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(getString(R.string.etherscan_url))
                .build();

        mEtherscanService = mRetrofit.create(EtherscanService.class);

        mAccounts = new ArrayList<>();
        mTransactions = new HashMap<>();
        mBalances = new HashMap<>();

        // Dummy data TODO remove
        //mAccounts.add(new VMAccount(getString(R.string.default_address), "0"));
        //mAccounts.add(new VMAccount("0x5DD0b5D02cD574412Ad58dD84A2F402cc25e320a", "0"));

        try {
            List<Account> ksAccounts = mEtherStore.getAccounts();

            for (Account a: ksAccounts) {
                mAccounts.add(new VMAccount(a.getAddress().getHex(), "0"));
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        for (VMAccount a : mAccounts) {
            mTransactions.put(a.getAddress(), new ArrayList<ESTransaction>());
        }
    }

    public void loadViewModels() {
        // Get transactions
        for (VMAccount a : mAccounts) {
            new GetTransactionsTask(a.getAddress()).execute();
        }

        // Get balances
        for (VMAccount a : mAccounts) {
            new GetBalanceTask(a).execute();
        }
    }

    public VMAccount getAccount(String address) {
        VMAccount out = null;
        for (VMAccount a : mAccounts) {
            if (a.getAddress().equals(address)) {
                out = a;
            }
        }
        return out;
    }

    public List<ESTransaction> getTransactions(String address) {
        List<ESTransaction> txns = mTransactions.get(address);
        if (txns == null) {
            return new ArrayList<>();
        }
        return txns;
    }

    public void navigateToWallet(Context context, View view) {
        TextView b = (TextView) view;
        String address = (String) b.getText();
        Intent intent = new Intent(context, TransactionListActivity.class);
        intent.putExtra("address", address);
        context.startActivity(intent);
    }

    public void navigateToCreateWallet(Context context) {
        Intent intent = new Intent(context, CreateAccountActivity.class);
        context.startActivity(intent);
    }

    public void navigateToSend(Context context) {
        Intent intent = new Intent(context, SendActivity.class);
        context.startActivity(intent);
    }

    public void navigateToImportWallet(Context context) {
        /*TextView b = (TextView) view;
        String address = (String) b.getText();
        Intent intent = new Intent(context, TransactionListActivity.class);
        context.startActivity(intent);*/
    }

    public void clickCreateAccount(Activity activity, String name, String password) {
        Log.d(TAG, String.format("Create account '%s' with pwd '%s", name, password));
        VMAccount account = createAccount(password);

        mAccounts.add(account);
        mTransactions.put(account.getAddress(), new ArrayList<ESTransaction>());

        activity.finish();
    }

    public void clickSend(SendActivity sendActivity, String from, String to, String ethAmount, String password) {
        Log.d(TAG, String.format("Send ETH: %s, %s, %s, %s", from, to, ethAmount, password));
        new SendTransactionTask(from, to, EthToWei(ethAmount), password).execute();
        sendActivity.finish();
    }

    public VMAccount createAccount(String password) {
        try {
            String address = new CreateAccountTask().execute(password).get();
            return new VMAccount(address, "0");
        } catch (Exception e) {
            Log.d(TAG, e.toString());
        }
        return null;
    }

    public void importAccount(View view) { new ImportAccountTask().execute(); }

    public void exportAccount(View view) {
        new ExportAccountTask().execute();
    }

    public List<VMAccount> getAccounts() {
        return mAccounts;
    }

    private String getString(int resId) {
        return mAppContext.getString(resId);
    }

    private class GetWeb3ClientVersionTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            try {
                Web3j web3 = Web3jFactory.build(new InfuraHttpService(getString(R.string.infura_url)));
                Web3ClientVersion web3ClientVersion = web3.web3ClientVersion().sendAsync().get();
                String clientVersion = web3ClientVersion.getWeb3ClientVersion();
                Log.d("INFO", "web3 client version: " + clientVersion);
            } catch (Exception e) {
                Log.d("ERROR", e.toString());
            }
            return null;
        }
    }

    private class GetBalanceTask extends AsyncTask<Void, Void, Void> {
        private VMAccount mAccount;

        public GetBalanceTask(VMAccount account) {
            this.mAccount = account;
        }

        protected Void doInBackground(Void... params) {
            try {
                Web3j web3 = Web3jFactory.build(new InfuraHttpService(getString(R.string.infura_url)));
                EthGetBalance ethGetBalance = web3
                        .ethGetBalance(mAccount.getAddress(), DefaultBlockParameterName.LATEST)
                        .sendAsync()
                        .get();
                BigInteger wei = ethGetBalance.getBalance();
                Log.d(TAG, "balance: " + wei);

                mAccount.setBalance(wei);

            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
            return null;
        }
    }

    private class ImportAccountTask extends AsyncTask<String, Void, Void> {
        protected Void doInBackground(String... params) {
            try {
                String storeJson = "{\"address\":\"aa3cc54d7f10fa3a1737e4997ba27c34f330ce16\",\"crypto\":{\"cipher\":\"aes-128-ctr\",\"ciphertext\":\"94119190a98a3e6fd0512c1e170d2a632907192a54d4a355768dec5eb0818db7\",\"cipherparams\":{\"iv\":\"4e5fea1dbb06694c6809d379f736c2e2\"},\"kdf\":\"scrypt\",\"kdfparams\":{\"dklen\":32,\"n\":4096,\"p\":6,\"r\":8,\"salt\":\"0b92da3c8548156453b2a5960f16cdef9f365c49e44c3f3f9a9ee3544a0ef16b\"},\"mac\":\"08700b32aad5ca0b0ffd55001db36606ff52ee3d94f762176bb1269d27074bb9\"},\"id\":\"1e7a1a79-9ce9-47c9-b764-fed548766c65\",\"version\":3}";
                Account account = mEtherStore.importKeyStore(storeJson, getString(R.string.default_password));
                Log.d("INFO", "Imported account: " + account.getAddress().getHex());
            } catch (Exception e) {
                Log.d("ERROR", e.toString());
            }
            return null;
        }
    }

    private class CreateAccountTask extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... passwords) {
            Log.d("INFO", "Trying to generate wallet in " + mKeystoreBaseDir);
            String address = "";
            try {
                Account account = mEtherStore.createAccount(passwords[0]);
                address = account.getAddress().getHex().toString();
            } catch (Exception e) {
                Log.d("ERROR", "Error generating wallet: " + e.toString());
            }
            return address;
        }
    }

    private class ExportAccountTask extends AsyncTask<String, Void, Void> {
        protected Void doInBackground(String... params) {
            try {
                Account account = mEtherStore.createAccount(getString(R.string.default_password));
                String accountJson = mEtherStore.exportAccount(account, getString(R.string.default_password));
                Log.d("INFO", accountJson);
            } catch (Exception e) {
                Log.d("ERROR", e.toString());
            }
            return null;
        }
    }

    // "{\"address\":\"aa3cc54d7f10fa3a1737e4997ba27c34f330ce16\",\"crypto\":{\"cipher\":\"aes-128-ctr\",\"ciphertext\":\"94119190a98a3e6fd0512c1e170d2a632907192a54d4a355768dec5eb0818db7\",\"cipherparams\":{\"iv\":\"4e5fea1dbb06694c6809d379f736c2e2\"},\"kdf\":\"scrypt\",\"kdfparams\":{\"dklen\":32,\"n\":4096,\"p\":6,\"r\":8,\"salt\":\"0b92da3c8548156453b2a5960f16cdef9f365c49e44c3f3f9a9ee3544a0ef16b\"},\"mac\":\"08700b32aad5ca0b0ffd55001db36606ff52ee3d94f762176bb1269d27074bb9\"},\"id\":\"1e7a1a79-9ce9-47c9-b764-fed548766c65\",\"version\":3}"

    private class SendTransactionTask extends AsyncTask<Void, Void, Void> {
        private String fromAddress;
        private String toAddress;
        private String wei;
        private String password;

        public SendTransactionTask(String fromAddress, String toAddress, String wei, String password) {
            this.fromAddress = fromAddress;
            this.toAddress = toAddress;
            this.wei = wei;
            this.password = password;
        }

        protected Void doInBackground(Void... params) {
            try {
                Web3j web3j = Web3jFactory.build(new InfuraHttpService(getString(R.string.infura_url)));

                Account fromAccount = mEtherStore.getAccount(fromAddress);
                if (fromAccount == null) {
                    Log.e(TAG, "Can't find account by from address: " + fromAddress);
                    return null;
                }

                EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                        fromAddress, DefaultBlockParameterName.LATEST).sendAsync().get();
                BigInteger nonce = ethGetTransactionCount.getTransactionCount();
                Log.d("INFO", "New account nonce:" + new Long(nonce.longValue()).toString());

                byte[] signedMessage = mEtherStore.signTransaction(fromAccount, password, toAddress, nonce.longValue());
                String hexValue = Numeric.toHexString(signedMessage);

                Log.d("INFO", "Sent transaction: " + hexValue);

                EthSendTransaction raw = web3j
                        .ethSendRawTransaction(hexValue)
                        .sendAsync()
                        .get();

                String result = raw.getTransactionHash();
                Log.d(TAG, "Transaction hash %s".format(result));

                if (raw.hasError()) {
                    Log.d(TAG, "Transaction error message: " + raw.getError().getMessage());
                    Log.d(TAG, "Transaction error data: " + raw.getError().getData());
                }
                Log.d(TAG, "Transaction JSON-RPC"+ raw.getJsonrpc());
                Log.d(TAG, "Transaction result: " + raw.getResult());
                Log.d(TAG, "Transaction hash: " + raw.getTransactionHash());
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
            return null;
        }
    }

    private class GetTransactionsTask extends AsyncTask<Void, Void, Void> {
        private final String address;

        public GetTransactionsTask(String address) {
            this.address = address;
        }

        protected Void doInBackground(Void... args) {
            try {
                Call<ESTransactionListResponse> call =
                        mEtherscanService.getTransactionList(
                                "account",
                                "txlist",
                                address,
                                "0",
                                "desc",
                                getString(R.string.etherscan_api_key)
                        );


                Log.d("INFO", "Request query:" + call.request().url().query());
                call.enqueue(new Callback<ESTransactionListResponse>() {

                    @Override

                    public void onResponse(Call<ESTransactionListResponse> call, Response<ESTransactionListResponse> response) {

                        List<ESTransaction> transactions = response.body().getTransactionList();

                        mTransactions.put(address, transactions);

                        //recyclerView.setAdapter(new MoviesAdapter(movies, R.layout.list_item_movie, getApplicationContext()));

                        Log.d("INFO", "Number of transactions: " + transactions.size());
                        if (transactions.size() > 0) {
                            Log.d("INFO", "Last transaction block number: " + transactions.get(0).getBlockNumber());
                            Log.d("INFO", "First transaction block number: " + transactions.get(transactions.size() - 1).getBlockNumber());
                        }
                    }

                    @Override
                    public void onFailure(Call<ESTransactionListResponse> call, Throwable t) {
                        Log.e("ERROR", t.toString());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private static String weiInEth = "1000000000000000000";

    public static String WeiToEth(String wei) {
        BigDecimal eth = new BigDecimal(wei).divide(new BigDecimal(weiInEth));
        return eth.toString();
    }

    public static String EthToWei(String eth) {
        BigDecimal wei = new BigDecimal(eth).multiply(new BigDecimal(weiInEth));
        return wei.toString();
    }
}
