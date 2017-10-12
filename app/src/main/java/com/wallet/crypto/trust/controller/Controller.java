package com.wallet.crypto.trust.controller;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import com.wallet.crypto.trust.R;
import com.wallet.crypto.trust.model.ESTransaction;
import com.wallet.crypto.trust.model.ESTransactionListResponse;
import com.wallet.crypto.trust.model.VMAccount;
import com.wallet.crypto.trust.model.VMNetwork;
import com.wallet.crypto.trust.views.AccountListActivity;
import com.wallet.crypto.trust.views.CreateAccountActivity;
import com.wallet.crypto.trust.views.ExportAccountActivity;
import com.wallet.crypto.trust.views.ImportAccountActivity;
import com.wallet.crypto.trust.views.ReceiveActivity;
import com.wallet.crypto.trust.views.SettingsActivity;
import com.wallet.crypto.trust.views.TransactionListActivity;
import com.wallet.crypto.trust.views.SendActivity;

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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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

    // Services
    private Context mAppContext;
    private EtherStore mEtherStore;
    private Retrofit mRetrofit;
    private EtherscanService mEtherscanService;

    // State
    private String mKeystoreBaseDir;
    private String mCurrentAddress;
    private VMNetwork mCurrentNetwork;

    // View models
    ArrayList<VMNetwork> mNetworks;
    ArrayList<VMAccount> mAccounts;
    Map<String, List<ESTransaction>> mTransactions;
    Map<String, Long> mBalances;

    // Views
    AccountListActivity mAccountListActivity;
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

        // Create networks list
        mNetworks = new ArrayList<>();
        mNetworks.add(new VMNetwork("kovan", "https://kovan.infura.io/llyrtzQ3YhkdESt2Fzrk", "https://kovan.etherscan.io", "ZVU87DFQYV2TPJQKRJDITS42MW58GUEZ4V", 42));
        mNetworks.add(new VMNetwork("mainnet", "https://mainnet.infura.io/llyrtzQ3YhkdESt2Fzrk", "https://mainnet.etherscan.io", "?", 1));
        setCurrentNetwork(mNetworks.get(0).getName());

        // Setup service
        mRetrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(mCurrentNetwork.getEtherscanUrl())
                .build();

        mEtherscanService = mRetrofit.create(EtherscanService.class);

        mAccounts = new ArrayList<>();
        mTransactions = new HashMap<>();
        mBalances = new HashMap<>();

        // Dummy data TODO remove
        //mAccounts.add(new VMAccount(getString(R.string.default_address), "0"));
        //mAccounts.add(new VMAccount("0x5DD0b5D02cD574412Ad58dD84A2F402cc25e320a", "0"));

        loadAccounts();

        if (mAccounts.size() > 0) {
            setCurrentAddress(mAccounts.get(0).getAddress());
        }

        for (VMAccount a : mAccounts) {
            mTransactions.put(a.getAddress(), new ArrayList<ESTransaction>());
        }
    }

    private List<VMAccount> loadAccounts() {
        mAccounts = new ArrayList<>();
        try {
            List<Account> ksAccounts = mEtherStore.getAccounts();

            for (Account a: ksAccounts) {
                mAccounts.add(new VMAccount(a.getAddress().getHex(), "0"));
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        return mAccounts;
    }

    public void loadViewModels(final OnTaskCompleted listener) {
        // Get transactions
        new GetTransactionsTask(mAccounts, new OnTaskCompleted() {
            @Override
            public void onTaskCompleted() {
                Log.d(TAG, "Finished loading transactions");

                // ... and then get balances
                new GetBalancesTask(mAccounts, listener).execute();
            }
        }).execute();

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

    public void navigateToSettings(Context context) {
        Intent intent = new Intent(context, SettingsActivity.class);
        context.startActivity(intent);
    }

    public void navigateToAccountList(Context context) {
        Intent intent = new Intent(context, AccountListActivity.class);
        context.startActivity(intent);
    }

    public void navigateToCreateAccount(Context context) {
        Intent intent = new Intent(context, CreateAccountActivity.class);
        context.startActivity(intent);
    }

    public void navigateToSend(Context context) {
        Intent intent = new Intent(context, SendActivity.class);
        context.startActivity(intent);
    }

    public void navigateToReceive(Context context) {
        Intent intent = new Intent(context, ReceiveActivity.class);
        context.startActivity(intent);
    }

    public void navigateToImportAccount(Context context) {
        Intent intent = new Intent(context, ImportAccountActivity.class);
        context.startActivity(intent);
    }

    public void clickCreateAccount(Activity activity, String name, String password) throws Exception {
        Log.d(TAG, String.format("Create account '%s' with pwd '%s", name, password));
        VMAccount account = createAccount(password);

        mAccounts.add(account);
        mTransactions.put(account.getAddress(), new ArrayList<ESTransaction>());

        if (mEtherStore.getAccounts().size() == 0) {
            setCurrentAddress(account.getAddress());
        }

        activity.finish();
    }

    public void clickImport(String keystore, String password, OnTaskCompleted listener) {
        Log.d(TAG, String.format("Import account %s, %s", keystore, password));
        new ImportAccountTask(keystore, password, listener).execute();
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

    public List<VMAccount> getAccounts() {
        return mAccounts;
    }

    private String getString(int resId) {
        return mAppContext.getString(resId);
    }

    public void setCurrentAddress(String currentAddress) {
        this.mCurrentAddress = currentAddress;
    }

    public VMAccount getCurrentAccount() {
        return this.getAccount(mCurrentAddress);
    }

    public void setCurrentNetwork(String name) {
        for (VMNetwork n : mNetworks) {
            if (n.getName().equals(name)) {
                mCurrentNetwork = n;
                break;
            }
        }
    }

    public VMNetwork getCurrentNetwork() {
        return mCurrentNetwork;
    }

    public void deleteAccount(String address, String password) throws Exception {
        mEtherStore.deleteAccount(address, password);
        loadAccounts();
    }

    public void navigateToExportAccount(Context context, String address) {
        Intent intent = new Intent(context, ExportAccountActivity.class);
        intent.putExtra(getString(R.string.address_keyword), address);
        context.startActivity(intent);
    }

    public String clickExportAccount(Context context, String address, String password) {
        try {
            Account account = mEtherStore.getAccount(address);
            return mEtherStore.exportAccount(account, password);
        } catch (Exception e) {
            Toast.makeText(context, "Failed to export account " + e.toString(), Toast.LENGTH_SHORT);
        }
        return "";
    }

    public void refreshTransactions(TransactionListActivity txnList, OnTaskCompleted listener) {
        new GetTransactionsTask(mAccounts, listener).execute();
    }

    public ESTransaction findTransaction(String address, String txn_hash) {
        List<ESTransaction> txns = mTransactions.get(address);

        if (txns == null) {
            Log.e(TAG, "Can't find transactions with given address: " + address);
            return null;
        }

        for (ESTransaction txn : txns) {
            if (txn.getHash().equals(txn_hash)) {
                return txn;
            }
        }

        return null;
    }

    public static String GetDate(long time) {
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(time*1000);
        String date = DateFormat.format("MM/dd/yy H:mm:ss zzz", cal).toString();
        return date;
    }

    public List<VMNetwork> getNetworks() {
        return mNetworks;
    }

    private class GetWeb3ClientVersionTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            try {
                Web3j web3 = Web3jFactory.build(new InfuraHttpService(mCurrentNetwork.getInfuraUrl()));
                Web3ClientVersion web3ClientVersion = web3.web3ClientVersion().sendAsync().get();
                String clientVersion = web3ClientVersion.getWeb3ClientVersion();
                Log.d("INFO", "web3 client version: " + clientVersion);
            } catch (Exception e) {
                Log.d("ERROR", e.toString());
            }
            return null;
        }
    }

    private class GetBalancesTask extends AsyncTask<Void, Void, Void> {
        private List<VMAccount> mAccounts;
        private OnTaskCompleted mListener;

        public GetBalancesTask(List<VMAccount> accounts, OnTaskCompleted listener) {
            this.mAccounts = accounts;
            this.mListener = listener;
        }

        private void getBalance(VMAccount account) {
            try {
                Web3j web3 = Web3jFactory.build(new InfuraHttpService(mCurrentNetwork.getInfuraUrl()));
                EthGetBalance ethGetBalance = web3
                        .ethGetBalance(account.getAddress(), DefaultBlockParameterName.LATEST)
                        .sendAsync()
                        .get();
                BigInteger wei = ethGetBalance.getBalance();
                Log.d(TAG, "balance: " + wei);

                account.setBalance(wei);

            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }

        protected Void doInBackground(Void... params) {
            for (VMAccount a: mAccounts) {
                getBalance(a);
            }
            mListener.onTaskCompleted();
            return null;
        }
    }

    private class ImportAccountTask extends AsyncTask<String, Void, Void> {

        private final String keystoreJson;
        private final String password;
        private final OnTaskCompleted listener;

        public ImportAccountTask(String keystoreJson, String password, OnTaskCompleted listener) {
            this.keystoreJson = keystoreJson;
            this.password = password;
            this.listener = listener;
        }

        protected Void doInBackground(String... params) {
            try {
                Account account = mEtherStore.importKeyStore(keystoreJson, password);
                loadAccounts();
                Log.d("INFO", "Imported account: " + account.getAddress().getHex());
            } catch (Exception e) {
                Log.d("ERROR", e.toString());
            }
            listener.onTaskCompleted();
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

    // TODO remove
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
            Log.d(TAG, "SendTransaction %s %s %s".format(fromAddress, toAddress, wei));
        }

        protected Void doInBackground(Void... params) {
            try {
                Web3j web3j = Web3jFactory.build(new InfuraHttpService(mCurrentNetwork.getInfuraUrl()));

                Account fromAccount = mEtherStore.getAccount(fromAddress);
                if (fromAccount == null) {
                    Log.e(TAG, "Can't find account by from address: " + fromAddress);
                    return null;
                }

                EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                        fromAddress, DefaultBlockParameterName.LATEST).sendAsync().get();
                BigInteger nonce = ethGetTransactionCount.getTransactionCount();
                Log.d("INFO", "New account nonce:" + new Long(nonce.longValue()).toString());

                String hexValue = "0xDEADBEEF";
                try {
                    byte[] signedMessage = mEtherStore.signTransaction(fromAccount, password, toAddress, wei, nonce.longValue());
                    hexValue = Numeric.toHexString(signedMessage);
                } catch (Exception e) {
                    Log.e(TAG, "Error signing " + e.toString());
                }

                Log.d("INFO", "Sent transaction: " + hexValue);

                EthSendTransaction raw = web3j
                        .ethSendRawTransaction(hexValue)
                        .sendAsync()
                        .get();

                if (raw.hasError()) {
                    Log.e(TAG, raw.getError().getMessage());
                }
                String result = raw.getTransactionHash();
                Log.d(TAG, "Transaction hash " + result);

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
        private final List<VMAccount> mAccounts;
        private final OnTaskCompleted mListener;

        public GetTransactionsTask(List<VMAccount> accounts, OnTaskCompleted listener) {
            this.mAccounts = accounts;
            this.mListener = listener;
        }

        private void fetchTransactionsForAddress(VMAccount account) {
            final String address = account.getAddress();
            try {
                Call<ESTransactionListResponse> call =
                        mEtherscanService.getTransactionList(
                                "account",
                                "txlist",
                                address,
                                "0",
                                "desc",
                                mCurrentNetwork.getEtherscanApiKey()
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
        }

        protected Void doInBackground(Void... args) {
            for (VMAccount a : mAccounts) {
                fetchTransactionsForAddress(a);
            }
            mListener.onTaskCompleted();
            return null;
        }
    }

    private static String weiInEth = "1000000000000000000";
    private static String gweiInEth = "1000000000";

    public static String WeiToEth(String wei) {
        BigDecimal eth = new BigDecimal(wei).divide(new BigDecimal(weiInEth));
        return eth.toString();
    }

    public static String WeiToEth(String wei, int sigFig) {
        BigDecimal eth = new BigDecimal(wei).divide(new BigDecimal(weiInEth));
        int scale = sigFig - eth.precision() + eth.scale();
        BigDecimal eth_scaled = eth.setScale(scale, RoundingMode.HALF_UP);
        return eth_scaled.toString();
    }

    public static String EthToWei(String eth) {
        BigDecimal wei = new BigDecimal(eth).multiply(new BigDecimal(weiInEth));
        Log.d(TAG, "Eth to wei: " + wei.toBigInteger().toString());
        return wei.toBigInteger().toString();
    }

    public static String EthToGwei(String eth) {
        BigDecimal wei = new BigDecimal(eth).multiply(new BigDecimal(gweiInEth));
        Log.d(TAG, "Eth to Gwei: " + wei.toBigInteger().toString());
        return wei.toBigInteger().toString();
    }
}
