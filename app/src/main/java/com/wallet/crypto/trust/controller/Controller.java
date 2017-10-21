package com.wallet.crypto.trust.controller;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
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
    private static final String PREF_CURRENT_ADDRESS = "pref_current_address";
    private static Controller mInstance;
    private static boolean mInited = false;

    public static final int IMPORT_ACCOUNT_REQUEST = 1;

    private static String TAG = "CONTROLLER";

    // Services
    private TransactionListActivity mMainActivity;
    private EtherStore mEtherStore;
    private Map<String, EtherscanService> mEtherscanServices;
    private SharedPreferences mPreferences;

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

    // Backgroud task
    private int mInterval = 10000;
    private Handler mHandler;

    public static Controller get() {
        if (mInstance == null) {
            mInstance = new Controller();
        }
        return mInstance;
    }

    protected Controller() { }

    public void init(Context appContext) {

        if (mInited) {
            return;
        }
        mInited = true;

        mMainActivity = (TransactionListActivity) appContext;

        mKeystoreBaseDir = mMainActivity.getFilesDir() + "/keystore";

        mPreferences = mMainActivity.getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode

        mEtherStore = new EtherStore(mKeystoreBaseDir);

        // Create networks list
        mNetworks = new ArrayList<>();

        mNetworks.add(new VMNetwork("kovan", "https://kovan.infura.io/llyrtzQ3YhkdESt2Fzrk", "https://kovan.etherscan.io", "ZVU87DFQYV2TPJQKRJDITS42MW58GUEZ4V", 42));
        mNetworks.add(new VMNetwork("mainnet", "https://mainnet.infura.io/llyrtzQ3YhkdESt2Fzrk", "https://api.etherscan.io", "ZVU87DFQYV2TPJQKRJDITS42MW58GUEZ4V", 1));
        mNetworks.add(new VMNetwork("ropstein", "https://ropstein.infura.io/llyrtzQ3YhkdESt2Fzrk", "https://ropstein.etherscan.io", "ZVU87DFQYV2TPJQKRJDITS42MW58GUEZ4V", 3));
        mNetworks.add(new VMNetwork("rinkeby", "https://rinkeby.infura.io/llyrtzQ3YhkdESt2Fzrk", "https://rinkeby.etherscan.io", "ZVU87DFQYV2TPJQKRJDITS42MW58GUEZ4V", 4));

        // Load current from app preferences
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mMainActivity);
        String rpcServerName = sharedPref.getString("pref_rpcServer", "");

        if (rpcServerName != null && existsNetwork(rpcServerName)) {
            Log.d(TAG, "Loaded rpcServerName from preferences " + rpcServerName);
            setCurrentNetwork(rpcServerName);
        } else {
            Log.d(TAG, "Setting default rpc server " + mNetworks.get(0).getName());
            setCurrentNetwork(mNetworks.get(0).getName());
        }

        mAccounts = new ArrayList<>();
        mTransactions = new HashMap<>();
        mBalances = new HashMap<>();

        loadAccounts();

        mCurrentAddress = null;
        if (mAccounts.size() > 0) {
            mCurrentAddress = mPreferences.getString(PREF_CURRENT_ADDRESS, null);
            if (mCurrentAddress == null) {
                setCurrentAddress(mAccounts.get(0).getAddress());
            }
        }

        for (VMAccount a : mAccounts) {
            mTransactions.put(a.getAddress(), new ArrayList<ESTransaction>());
        }

        mHandler = new Handler();
    }

    public void onResume() {
        startRepeatingTask();
    }

    public void onStop() {
        stopRepeatingTask();
    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                Log.d(TAG, "Periodic task");
                mMainActivity.fetchModelsAndReinit();
            } finally {
                mHandler.postDelayed(mStatusChecker, mInterval);
            }
        }
    };

    void startRepeatingTask() {
        mStatusChecker.run();
    }

    void stopRepeatingTask() {
        mHandler.removeCallbacks(mStatusChecker);
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
            public void onTaskCompleted(TaskResult result) {
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

    public void navigateToAccountList() {
        Intent intent = new Intent(mMainActivity, AccountListActivity.class);
        mMainActivity.startActivity(intent);
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

    public void navigateToSend(Context context, String to_address) {
        Intent intent = new Intent(context, SendActivity.class);
        intent.putExtra(getString(R.string.address_keyword), to_address);
        context.startActivity(intent);
    }

    public void navigateToReceive(Context context) {
        Intent intent = new Intent(context, ReceiveActivity.class);
        context.startActivity(intent);
    }

    public void navigateToImportAccount(CreateAccountActivity parentActivity) {
        Intent intent = new Intent(parentActivity, ImportAccountActivity.class);
        parentActivity.startActivityForResult(intent, IMPORT_ACCOUNT_REQUEST);
    }

    public void clickCreateAccount(Activity activity, String name, String password) throws Exception {
        Log.d(TAG, String.format("Create account '%s'", name));
        boolean firstAccount = mAccounts.size() == 0;
        VMAccount account = createAccount(password);

        mAccounts.add(account);
        mTransactions.put(account.getAddress(), new ArrayList<ESTransaction>());

        if (firstAccount) {
            setCurrentAddress(account.getAddress());
            Intent intent = new Intent(activity.getApplicationContext(), TransactionListActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.getApplicationContext().startActivity(intent);
        }
        activity.finish();
    }

    public void clickImport(String keystore, String password, OnTaskCompleted listener) {
        Log.d(TAG, String.format("Import account %s", keystore));
        new ImportAccountTask(keystore, password, listener).execute();
    }

    public void clickSend(SendActivity sendActivity, String from, String to, String ethAmount, String password, OnTaskCompleted listener) {
        Log.d(TAG, String.format("Send ETH: %s, %s, %s", from, to, ethAmount));
        new SendTransactionTask(from, to, EthToWei(ethAmount), password, listener).execute();
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
        return mMainActivity.getString(resId);
    }

    public void setCurrentAddress(String currentAddress) {
        this.mCurrentAddress = currentAddress;

        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(PREF_CURRENT_ADDRESS, currentAddress);
        editor.commit();
    }

    public VMAccount getCurrentAccount() {
        return this.getAccount(mCurrentAddress);
    }

    public boolean existsNetwork(String name) {
        boolean exists = false;
        for (VMNetwork n : mNetworks) {
            if (n.getName().equals(name)) {
                exists = true;
                break;
            }
        }
        return exists;
    }

    public void setCurrentNetwork(String name) {
        VMNetwork previous = mCurrentNetwork;
        for (VMNetwork n : mNetworks) {
            if (n.getName().equals(name)) {
                mCurrentNetwork = n;
                break;
            }
        }
        assert(mCurrentNetwork != null);
        if (previous != mCurrentNetwork) {
            mMainActivity.fetchModelsAndReinit();
        }
    }

    public VMNetwork getCurrentNetwork() {
        return mCurrentNetwork;
    }

    public void deleteAccount(String address, String password) throws Exception {
        mEtherStore.deleteAccount(address, password);
        loadAccounts();
        if (address.equals(mCurrentAddress)) {
            if (mAccounts.size() > 0) {
                mCurrentAddress = mAccounts.get(0).getAddress();
            }
        }
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
            mListener.onTaskCompleted(new TaskResult(TaskStatus.SUCCESS, ""));
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
                listener.onTaskCompleted(new TaskResult(TaskStatus.SUCCESS, "Imported wallet."));
            } catch (Exception e) {
                Log.d("ERROR", e.toString());
                listener.onTaskCompleted(new TaskResult(TaskStatus.FAILURE, "Failed to import wallet: '%s'".format(e.getMessage())));
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

    private class SendTransactionTask extends AsyncTask<Void, Void, Void> {
        private String fromAddress;
        private String toAddress;
        private String wei;
        private String password;
        private OnTaskCompleted listener;

        public SendTransactionTask(String fromAddress, String toAddress, String wei, String password, OnTaskCompleted listener) {
            this.fromAddress = fromAddress;
            this.toAddress = toAddress;
            this.wei = wei;
            this.password = password;
            this.listener = listener;
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
                    listener.onTaskCompleted(new TaskResult(TaskStatus.FAILURE, "Error signing: " + e.toString()));
                    return null;
                }

                Log.d("INFO", "Sent transaction: " + hexValue);

                EthSendTransaction raw = web3j
                        .ethSendRawTransaction(hexValue)
                        .sendAsync()
                        .get();

                String result = raw.getTransactionHash();
                Log.d(TAG, "Transaction hash " + result);

                if (raw.hasError()) {
                    Log.d(TAG, "Transaction error message: " + raw.getError().getMessage());
                    Log.d(TAG, "Transaction error data: " + raw.getError().getData());
                    listener.onTaskCompleted(new TaskResult(TaskStatus.FAILURE, "Transaction error: " + raw.getError().getMessage()));
                    return null;
                }
                Log.d(TAG, "Transaction JSON-RPC"+ raw.getJsonrpc());
                Log.d(TAG, "Transaction result: " + raw.getResult());
                Log.d(TAG, "Transaction hash: " + raw.getTransactionHash());
            } catch (Exception e) {
                Log.e(TAG, e.toString());
                listener.onTaskCompleted(new TaskResult(TaskStatus.FAILURE, "Transaction error: " + e.toString()));
                return null;
            }
            listener.onTaskCompleted(new TaskResult(TaskStatus.SUCCESS, "Payment sent"));
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

                Retrofit mRetrofit = new Retrofit.Builder()
                        .addConverterFactory(GsonConverterFactory.create())
                        .baseUrl(mCurrentNetwork.getEtherscanUrl())
                        .build();

                EtherscanService service = mRetrofit.create(EtherscanService.class);

                Log.d(TAG, "Using etherscan service: " + mCurrentNetwork.getName() + ", " + mCurrentNetwork.getEtherscanUrl() + ", " + mCurrentNetwork.getEtherscanApiKey());

                Call<ESTransactionListResponse> call =
                        service.getTransactionList(
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
                        Toast.makeText(mMainActivity, "Error contacting RPC service. Check internet connection.", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        protected Void doInBackground(Void... args) {
            if (mAccounts == null) {
                return null;
            }
            for (VMAccount a : mAccounts) {
                fetchTransactionsForAddress(a);
            }
            mListener.onTaskCompleted(new TaskResult(TaskStatus.SUCCESS, "Fetched transactions for all accounts."));
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
