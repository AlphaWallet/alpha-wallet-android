package com.wallet.crypto.trustapp.controller;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.model.CMTicker;
import com.wallet.crypto.trustapp.model.ESTransaction;
import com.wallet.crypto.trustapp.model.ESTransactionListResponse;
import com.wallet.crypto.trustapp.model.VMAccount;
import com.wallet.crypto.trustapp.model.VMNetwork;
import com.wallet.crypto.trustapp.util.KS;
import com.wallet.crypto.trustapp.views.AccountListActivity;
import com.wallet.crypto.trustapp.views.CreateAccountActivity;
import com.wallet.crypto.trustapp.views.ExportAccountActivity;
import com.wallet.crypto.trustapp.views.ImportAccountActivity;
import com.wallet.crypto.trustapp.views.RequestActivity;
import com.wallet.crypto.trustapp.views.SettingsActivity;
import com.wallet.crypto.trustapp.views.TokenListActivity;
import com.wallet.crypto.trustapp.views.TransactionListActivity;
import com.wallet.crypto.trustapp.views.SendActivity;
import com.wallet.crypto.trustapp.views.WarningBackupActivity;

import org.ethereum.geth.Account;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

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
    public static final String KEY_ADDRESS = "key_address";
    public static final String KEY_PASSWORD = "key_password";
    private static Controller mInstance;

    public static final int IMPORT_ACCOUNT_REQUEST = 1;
    public static final int UNLOCK_SCREEN_REQUEST = 1001;
    public static final int SHARE_RESULT = 2;

    private static String TAG = "CONTROLLER";

    private Context mAppContext;

    // Services
    private EtherStore mEtherStore;
    private CoinmarketService mCoinmarketService;
    private SharedPreferences mPreferences;

    // State
    private String mKeystoreBaseDir;
    private String mCurrentAddress;
    private VMNetwork mCurrentNetwork;

    // View models
    private ArrayList<VMNetwork> mNetworks;
    private ArrayList<String> mEtherscanKeys;
    private ArrayList<VMAccount> mAccounts;
    private Map<String, List<ESTransaction>> mTransactions;
    private CMTicker mEthTicker = null; // if null, no data available

    // Views
    private TransactionListActivity mTransactionListActivity;

    // Background task
    private int mInterval = 10000; // ms
    private Handler mHandler;

    public static Controller with(Context context) {
        if (mInstance == null) {
            synchronized (Controller.class) {
                if (mInstance == null) {
                    mInstance = new Controller(context.getApplicationContext());
                }
            }
        }
        return mInstance;
    }

    protected Controller(Context context) {
        mAppContext = context;
        init(context);
    }

    public void init(Context appContext) {

        mAppContext = appContext;

        mKeystoreBaseDir = mAppContext.getFilesDir() + "/keystore";

        mPreferences = mAppContext.getSharedPreferences("MyPref", 0); // 0 - for private mode

        mEtherStore = new EtherStore(mKeystoreBaseDir, this);

        // Create etherscan key list
        mEtherscanKeys = new ArrayList<>();
        mEtherscanKeys.add("{etherscan_key1}");
        mEtherscanKeys.add("{etherscan_key2}");
        mEtherscanKeys.add("{etherscan_key3}");

        // Create networks list
        mNetworks = new ArrayList<>();

        mNetworks.add(new VMNetwork("mainnet", "https://mainnet.infura.io/llyrtzQ3YhkdESt2Fzrk", "https://api.etherscan.io", 1));
        mNetworks.add(new VMNetwork("kovan", "https://kovan.infura.io/llyrtzQ3YhkdESt2Fzrk", "https://kovan.etherscan.io", 42));
        mNetworks.add(new VMNetwork("ropstein", "https://ropstein.infura.io/llyrtzQ3YhkdESt2Fzrk", "https://ropstein.etherscan.io", 3));
        mNetworks.add(new VMNetwork("rinkeby", "https://rinkeby.infura.io/llyrtzQ3YhkdESt2Fzrk", "https://rinkeby.etherscan.io", 4));

        // Load current from app preferences
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mAppContext);
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

        loadAccounts();

        mCurrentAddress = null;
        if (mAccounts.size() > 0) {
            String cachedAddress = mPreferences.getString(PREF_CURRENT_ADDRESS, null);
            if (getAccount(cachedAddress) == null) {
                setCurrentAddress(mAccounts.get(0).getAddress());
            } else {
                setCurrentAddress(cachedAddress);
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

    public void setTransactionListActivity(TransactionListActivity activity) {
        mTransactionListActivity = activity;
    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                Log.d(TAG, "Periodic task");
                mTransactionListActivity.fetchModelsAndReinit();
            } catch (Exception e) {
                Log.e(TAG, "Unable to fetch update mTransactionListActivity");
            } finally {
                mHandler.postDelayed(mStatusChecker, mInterval);
            }

            fetchEthereumTicker();
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
                mAccounts.add(new VMAccount(a.getAddress().getHex().toLowerCase(), "0"));
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

    public void navigateToTokenList(Context context) {
        Intent intent = new Intent(context, TokenListActivity.class);
        intent.putExtra(KEY_ADDRESS, getCurrentAccount().getAddress());
        context.startActivity(intent);
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

    public void navigateToSend(Context context, String to_address) {
        Intent intent = new Intent(context, SendActivity.class);
        intent.putExtra(getString(R.string.address_keyword), to_address);
        context.startActivity(intent);
    }

    public void navigateToReceive(Context context) {
        Intent intent = new Intent(context, RequestActivity.class);
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

        Intent intent = new Intent(activity.getApplicationContext(), WarningBackupActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(KEY_ADDRESS, account.getAddress());
        intent.putExtra(KEY_PASSWORD, password);
        activity.getApplicationContext().startActivity(intent);

        if (firstAccount) {
            setCurrentAddress(account.getAddress());
        }
        activity.finish();
    }

    public void clickImport(String keystore, String password, OnTaskCompleted listener) {
        Log.d(TAG, String.format("Import account %s", keystore));
        try {
            JsonElement el = new JsonParser().parse(keystore);
            JsonObject obj = el.getAsJsonObject();
            String address = obj.get("address").getAsString();
            Log.d(TAG, "address : " + address);
            if (mEtherStore.hasAddress("0x" + address)) {
                listener.onTaskCompleted(new TaskResult(TaskStatus.FAILURE, getString(R.string.account_already_imported)));
                return;
            }
        } catch (Exception e) {
            listener.onTaskCompleted(new TaskResult(TaskStatus.FAILURE, getString(R.string.error_import) + ": " + e.getMessage()));
            return;
        }
        new ImportAccountTask(keystore, password, listener).execute();
    }

    public void clickImportPrivateKey(Activity activity, String privateKey, OnTaskCompleted listener) {
        Log.d(TAG, "Import account by private key");
        final String password = Controller.generatePassphrase();

        new ImportPrivateKeyTask(activity, privateKey, password, listener).execute();
    }

    public void clickSend(String from, String to, String ethAmount, String gasLimit, String gasPrice, OnTaskCompleted listener) throws ServiceErrorException {
        Log.d(TAG, String.format("Send ETH: %s, %s, %s", from, to, ethAmount));
        try {
	        String wei = EthToWei(ethAmount);
//            String password = PasswordManager.getPassword(from, mAppContext);
	        String password = new String(KS.get(mAppContext, from.toLowerCase()));
	        new SendTransactionTask(from, to, wei, gasLimit, gasPrice, password, null, listener).execute();
        } catch (ServiceErrorException ex) {
	        Log.e(TAG, "Error sending transaction: ", ex);
        	throw ex;
        } catch (Exception e) {
            Log.e(TAG, "Error sending transaction: ", e);
        }
    }

    public void clickSendTokens(String from, String to, String contractAddress, String tokenAmount, String gasLimit, String gasPrice, int decimals, OnTaskCompleted listener) throws ServiceErrorException {
        Log.d(TAG, String.format("Send tokens: %s, %s, %s", from, to, tokenAmount));
        try {
	        BigInteger nTokens = new BigDecimal(tokenAmount).multiply(BigDecimal.valueOf((long) Math.pow(10, decimals))).toBigInteger();

	        List<Type> params = Arrays.<Type>asList(new Address(to), new Uint256(nTokens));

	        List<TypeReference<?>> returnTypes = Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {
	        });

	        Function function = new Function("transfer", params, returnTypes);
	        String encodedFunction = FunctionEncoder.encode(function);
	        byte[] data = Numeric.hexStringToByteArray(Numeric.cleanHexPrefix(encodedFunction));

//            String password = PasswordManager.getPassword(from, mAppContext);
	        String password = new String(KS.get(mAppContext, from.toLowerCase()));
	        new SendTransactionTask(from, contractAddress, "0", gasLimit, gasPrice, password, data, listener).execute();
        } catch (ServiceErrorException ex) {
        	throw ex;
        } catch (Exception e) {
            Log.e(TAG, "Error sending transaction: ", e);
        }
    }

    public VMAccount createAccount(String password) throws Exception {
	    Log.d("INFO", "Trying to generate wallet in " + mKeystoreBaseDir);
	    String address = null;
	    VMAccount result = null;
	    try {
		    Account account = mEtherStore.createAccount(password);
		    address = account.getAddress().getHex().toLowerCase();
		    KS.put(mAppContext, address, password);
		    result = new VMAccount(address, "0");
		    return result;
	    } finally {
		    if (result == null && !TextUtils.isEmpty(address)) {
			    try {
				    mEtherStore.deleteAccount(address, password);
			    } catch (Exception e) { /* Quietly */ }
		    }
	    }
    }

    public List<VMAccount> getAccounts() {
        return mAccounts;
    }

    public int getNumberOfAccounts() {
        return mAccounts == null ? 0 : mAccounts.size();
    }

    private String getString(int resId) {
        return mAppContext.getString(resId);
    }

    public void setCurrentAddress(String currentAddress) {
        this.mCurrentAddress = currentAddress.toLowerCase();

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
            if (mTransactionListActivity != null) { // may not be set yet
                mTransactionListActivity.fetchModelsAndReinit();
            }
        }
    }

    public VMNetwork getCurrentNetwork() {
        return mCurrentNetwork;
    }

    public void deleteAccount(String address) throws Exception {
//        String password = PasswordManager.getPassword(address, mAppContext);
	    String password = new String(KS.get(mAppContext, address.toLowerCase()));
        mEtherStore.deleteAccount(address, password);
        loadAccounts();
        if (address.equals(mCurrentAddress)) {
            if (mAccounts.size() > 0) {
                mCurrentAddress = mAccounts.get(0).getAddress();
            }
        }
    }

    public void navigateToExportAccount(Activity parent, String address) {
        Intent intent = new Intent(parent, ExportAccountActivity.class);
        intent.putExtra(getString(R.string.address_keyword), address);
        parent.startActivityForResult(intent, SHARE_RESULT);
    }

    public String clickExportAccount(Context context, String address, String new_password) throws ServiceErrorException {
        try {
	        Account account = mEtherStore.getAccount(address);
//            String account_password = PasswordManager.getPassword(address, mAppContext);
	        String account_password = new String(KS.get(mAppContext, address.toLowerCase()));
	        return mEtherStore.exportAccount(account, account_password, new_password);
        } catch (ServiceErrorException ex) {
        	throw ex;
        } catch (Exception e) {
            Toast.makeText(context, "Failed to export account " + e.getMessage(), Toast.LENGTH_SHORT);
        }
        return "";
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

    public void shareKeystore(Activity parent, String keystoreJson) {
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Keystore");
        sharingIntent.putExtra(Intent.EXTRA_TEXT, keystoreJson);

        parent.startActivityForResult(Intent.createChooser(sharingIntent, "Share via"), SHARE_RESULT);
    }

    public static String generatePassphrase() {
        return UUID.randomUUID().toString();
    }

    public String getVersion() {
        String version = "N/A";
        try {
            PackageInfo pInfo = mAppContext.getPackageManager().getPackageInfo(mAppContext.getPackageName(), 0);
            version = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return version;
    }

    private class SendTokensTask extends AsyncTask<Void, Void, Void> {
        String encodedFunction;

        public SendTokensTask(String encodedFunction) {
            this.encodedFunction = encodedFunction;
        }

        protected Void doInBackground(Void... params) {
            Web3j web3 = Web3jFactory.build(new InfuraHttpService(mCurrentNetwork.getInfuraUrl()));
            /*
            Transaction transaction = Transaction.createFunctionCallTransaction(
                    from, gasPrice, gasLimit, contractAddress, amount, encodedFunction);

            org.web3j.protocol.core.methods.response.EthSendTransaction transactionResponse =
                    web3.ethSendTransaction(transaction).sendAsync().get();

            String transactionHash = transactionResponse.getTransactionHash();
            String password = PasswordManager.getPassword(from, mAppContext);
            */
            return null;
        }
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

    private class ImportPrivateKeyTask extends AsyncTask<Void, Void, Void> {

        private final String privateKey;
        private final String password;
        private final OnTaskCompleted listener;
        private ProgressDialog dialog;


        public ImportPrivateKeyTask(Activity activity, String privateKey, String password, OnTaskCompleted listener) {
            this.privateKey = privateKey;
            this.password = password;
            this.listener = listener;
            dialog = new ProgressDialog(activity);
            dialog.setCancelable(false);
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    ImportPrivateKeyTask.this.cancel(true);
                }
            });
        }

        @Override
        protected void onPreExecute() {
            dialog.setMessage(getString(R.string.message_importing_private_key));
            dialog.show();
        }

        protected Void doInBackground(Void... params) {
            //android.os.Process.setThreadPriority(Thread.MAX_PRIORITY);
            String keystore = "";
            try {
                keystore = EtherStoreUtils.convertPrivateKeyToKeystoreJson(privateKey, password);
            } catch (Exception e) {
                listener.onTaskCompleted(new TaskResult(TaskStatus.FAILURE, getString(R.string.error_import_private_key)));
            }

            clickImport(keystore, password, listener);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // do UI work here
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
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
        	String address = null;
            try {
	            Account account = mEtherStore.importKeyStore(keystoreJson, password);
	            address = account.getAddress().getHex().toLowerCase();
	            KS.put(mAppContext, address, password);
	            loadAccounts();
	            Log.d("INFO", "Imported account: " + account.getAddress().getHex());
	            listener.onTaskCompleted(new TaskResult(TaskStatus.SUCCESS, "Imported wallet."));
            } catch (ServiceErrorException e) {

            	if (!TextUtils.isEmpty(address)) {
            		try {
			            mEtherStore.deleteAccount(address, password);
		            } catch (Exception ex) {};
	            }

            	if (e.code == ServiceErrorException.USER_NOT_AUTHENTICATED) {
		            listener.onTaskCompleted(new TaskResult(TaskStatus.FAILURE, ""));
	            }
            } catch (Exception e) {
                Log.d("ERROR", e.toString());
                listener.onTaskCompleted(new TaskResult(TaskStatus.FAILURE, "Failed to import wallet: '%s'".format(e.getMessage())));
            }
            return null;
        }
    }

//    private class CreateAccountTask extends AsyncTask<String, Void, String> {
//	    private final OnTaskCompleted listener;
//
//	    public CreateAccountTask(OnTaskCompleted listener) {
//		    this.listener = listener;
//	    }
//
//        protected String doInBackground(String... passwords) {
//            Log.d("INFO", "Trying to generate wallet in " + mKeystoreBaseDir);
//            String address = "";
//            try {
//	            Account account = mEtherStore.createAccount(passwords[0]);
//	            address = account.getAddress().getHex().toString().toLowerCase();
//	            KS.put(mAppContext, address, passwords[0]);
//            } catch (ServiceErrorException ex) {
//	            if (!TextUtils.isEmpty(address)) {
//		            try {
//			            mEtherStore.deleteAccount(address, passwords[0]);
//		            } catch (Exception e) { /* Quietly */ }
//	            }
//
//	            if (ex.code == ServiceErrorException.USER_NOT_AUTHENTICATED) {
//		            listener.onTaskCompleted(new TaskResult(TaskStatus.FAILURE, "USER_NOT_AUTHENTICATED"));
//	            }
//	            address = null;
//            } catch (Exception e) {
//                Log.d("ERROR", "Error generating wallet: " + e.toString());
//                address = null;
//            }
//            return address;
//        }
//    }

    // Randomize etherscan key selection stay below rate limit
    private String getRandomEtherscanKey() {
        assert(mEtherscanKeys.size() > 0);

        final int random = new Random().nextInt(mEtherscanKeys.size());
        return mEtherscanKeys.get(random);
    }

    private class SendTransactionTask extends AsyncTask<Void, Void, Void> {
        private String fromAddress;
        private String toAddress;
        private String wei;
        private String gasLimit;
        private String gasPrice;
        private String password;
        private byte[] data;
        private OnTaskCompleted listener;

        public SendTransactionTask(String fromAddress, String toAddress, String wei, String gasLimit, String gasPrice, String password, byte[] data, OnTaskCompleted listener) {
            this.fromAddress = fromAddress;
            this.toAddress = toAddress;
            this.wei = wei;
            this.gasLimit = gasLimit;
            this.gasPrice = gasPrice;
            this.password = password;
            this.data = data;
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
                    byte[] signedMessage = mEtherStore.signTransaction(fromAccount, password, toAddress, wei, gasLimit, gasPrice, data, nonce.longValue());
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

    private void fetchEthereumTicker() {
        try {

            Retrofit mRetrofit = new Retrofit.Builder()
                    .addConverterFactory(GsonConverterFactory.create())
                    .baseUrl("https://api.coinmarketcap.com")
                    .build();

            CoinmarketService service = mRetrofit.create(CoinmarketService.class);

            Call<List<CMTicker>> call =
                    service.getEthereumPrice();

            Log.d("INFO", "Request query:" + call.request().url().query());
            call.enqueue(new Callback<List<CMTicker>>() {

                @Override
                public void onResponse(Call<List<CMTicker>> call, Response<List<CMTicker>> response) {
                    try {
                        List<CMTicker> tickers = response.body();
                        Log.d("INFO", "Number of transactions: " + tickers.size());
                        if (tickers.size() == 1) {
                            mEthTicker = tickers.get(0);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, e.getLocalizedMessage());
                    }
                }

                @Override
                public void onFailure(Call<List<CMTicker>> call, Throwable t) {
                    Log.e("ERROR", t.toString());
                    Toast.makeText(mAppContext, "Error contacting ether price service. Check internet connection.", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
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

                final String etherscanKey = getRandomEtherscanKey();
                Log.d(TAG, "Using etherscan service: " + mCurrentNetwork.getName() + ", " + mCurrentNetwork.getEtherscanUrl() + ", " + etherscanKey);

                Call<ESTransactionListResponse> call =
                        service.getTransactionList(
                                "account",
                                "txlist",
                                address,
                                "0",
                                "desc",
                                etherscanKey
                        );


                Log.d("INFO", "Request query:" + call.request().url().query());
                call.enqueue(new Callback<ESTransactionListResponse>() {

                    @Override
                    public void onResponse(Call<ESTransactionListResponse> call, Response<ESTransactionListResponse> response) {
                        try {
                            List<ESTransaction> transactions = response.body().getTransactionList();
                            mTransactions.put(address, transactions);
                            Log.d("INFO", "Number of transactions: " + transactions.size());
                            if (transactions.size() > 0) {
                                Log.d("INFO", "Last transaction block number: " + transactions.get(0).getBlockNumber());
                                Log.d("INFO", "First transaction block number: " + transactions.get(transactions.size() - 1).getBlockNumber());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, e.getLocalizedMessage());
                        }
                    }

                    @Override
                    public void onFailure(Call<ESTransactionListResponse> call, Throwable t) {
                        Log.e("ERROR", t.toString());
                        Toast.makeText(mAppContext, "Error contacting RPC service. Check internet connection.", Toast.LENGTH_SHORT).show();
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

    private static String weiInEth  = "1000000000000000000";
    private static String gweiInEth = "1000000000";
    private static String weiInGwei = "1000000000";

    public static String WeiToEth(String wei) {
        BigDecimal eth = new BigDecimal(wei).divide(new BigDecimal(weiInEth));
        return eth.toString();
    }

    public static String WeiToEth(String wei, int sigFig) throws Exception {
        BigDecimal eth = new BigDecimal(wei).divide(new BigDecimal(weiInEth));
        int scale = sigFig - eth.precision() + eth.scale();
        BigDecimal eth_scaled = eth.setScale(scale, RoundingMode.HALF_UP);
        return eth_scaled.toString();
    }

    public String EthToUsd(String balance) {
        if (mEtherStore == null) {
            return null;
        }
        try {
            BigDecimal usd = new BigDecimal(balance).multiply(new BigDecimal(mEthTicker.getPriceUsd()));
            usd = usd.setScale(2, RoundingMode.CEILING);
            return usd.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error converting ETH to USD");
        }
        return null;
    }

    public static String WeiToGwei(String wei) {
        BigDecimal gwei = new BigDecimal(wei).divide(new BigDecimal(weiInGwei));
        return gwei.toString();
    }

    public static String EthToWei(String eth) throws Exception {
        BigDecimal wei = new BigDecimal(eth).multiply(new BigDecimal(weiInEth));
        Log.d(TAG, "Eth to wei: " + wei.toBigInteger().toString());
        return wei.toBigInteger().toString();
    }

    public static String EthToGwei(String eth) throws Exception {
        BigDecimal wei = new BigDecimal(eth).multiply(new BigDecimal(gweiInEth));
        Log.d(TAG, "Eth to Gwei: " + wei.toBigInteger().toString());
        return wei.toBigInteger().toString();
    }
}
