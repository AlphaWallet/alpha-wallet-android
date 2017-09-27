package com.example.marat.wal;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.example.marat.wal.model.ESTransaction;
import com.example.marat.wal.model.ESTransactionListResponse;

import org.ethereum.geth.Account;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthAccounts;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.infura.InfuraHttpService;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;

public class MainActivity extends AppCompatActivity {

    private TextView mTextMessage;
    private EtherStore etherStore;
    private String walletFile;
    private String keystoreBaseDir;
    private AsyncTask lastTask;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    mTextMessage.setText(R.string.title_home);
                    return true;
                case R.id.navigation_import:
                    mTextMessage.setText(R.string.title_import);
                    Intent intent = new Intent(MainActivity.this, ItemListActivity.class);
                    startActivity(intent);
                    return true;
                case R.id.navigation_notifications:
                    mTextMessage.setText(R.string.title_notifications);
                    return true;
            }
            return false;
        }

    };

    public void navigate(View view) {
        //TODO: Start wallet detail page with selected wallet id
        Intent intent = new Intent(MainActivity.this, ItemListActivity.class);
        startActivity(intent);
    }

    public void showBalance(View view) {
        lastTask = new GetBalanceTask().execute();
    }

    public void createAccount(View view) {
        lastTask = new CreateAccountTask().execute();
    }

    public void importAccount(View view) { lastTask = new ImportAccountTask().execute(); }

    public void exportAccount(View view) {
        lastTask = new ExportAccountTask().execute();
    }

    public void sendTransaction(View view) {
        lastTask = new SendTransactionTask().execute();
    }

    public void checkStatus(View view) {
        if (lastTask != null) {
            Log.d("INFO", "Last task status: " + lastTask.getStatus().toString());
        } else {
            Log.d("INFO", "no last task");
        }
    }

    public void getTransactions(View view) {
        lastTask = new GetTransactionsTask().execute();
    }

    public void getAccounts(View view) {
        lastTask = new GetAccountsTask().execute();
    }

    private class GetBalanceTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            try {
                Web3j web3 = Web3jFactory.build(new InfuraHttpService(getString(R.string.default_network)));
                EthGetBalance ethGetBalance = web3
                        .ethGetBalance(getString(R.string.default_address), DefaultBlockParameterName.LATEST)
                        .sendAsync()
                        .get();
                BigInteger wei = ethGetBalance.getBalance();
                Log.d("INFO", "balance: " + wei);
            } catch (Exception e) {
                Log.d("ERROR", e.toString());
            }
            return null;
        }
    }

    private class ImportAccountTask extends AsyncTask<String, Void, Void> {
        protected Void doInBackground(String... params) {
            try {
                String storeJson = "{\"address\":\"aa3cc54d7f10fa3a1737e4997ba27c34f330ce16\",\"crypto\":{\"cipher\":\"aes-128-ctr\",\"ciphertext\":\"94119190a98a3e6fd0512c1e170d2a632907192a54d4a355768dec5eb0818db7\",\"cipherparams\":{\"iv\":\"4e5fea1dbb06694c6809d379f736c2e2\"},\"kdf\":\"scrypt\",\"kdfparams\":{\"dklen\":32,\"n\":4096,\"p\":6,\"r\":8,\"salt\":\"0b92da3c8548156453b2a5960f16cdef9f365c49e44c3f3f9a9ee3544a0ef16b\"},\"mac\":\"08700b32aad5ca0b0ffd55001db36606ff52ee3d94f762176bb1269d27074bb9\"},\"id\":\"1e7a1a79-9ce9-47c9-b764-fed548766c65\",\"version\":3}";
                Account account = etherStore.importKeyStore(storeJson, getString(R.string.default_password));
                Log.d("INFO", "Imported account: " + account.getAddress().getHex());
            } catch (Exception e) {
                Log.d("ERROR", e.toString());
            }
            return null;
        }
    }

    private class CreateAccountTask extends AsyncTask<String, Void, Void> {
        protected Void doInBackground(String... params) {
                Log.d("INFO", "Trying to generate wallet in " + keystoreBaseDir);
            try {
                Account account = etherStore.createAccount(getString(R.string.default_password));
                Log.d("INFO", "Generated account: " + account.getAddress().getHex().toString());
            } catch (Exception e) {
                Log.d("ERROR", "Error generating wallet: " + e.toString());
            }
            return null;
        }
    }

    private class ExportAccountTask extends AsyncTask<String, Void, Void> {
        protected Void doInBackground(String... params) {
            try {
                Account account = etherStore.createAccount(getString(R.string.default_password));
                String accountJson = etherStore.exportAccount(account, getString(R.string.default_password));
                Log.d("INFO", accountJson);
            } catch (Exception e) {
                Log.d("ERROR", e.toString());
            }
            return null;
        }
    }

    private class SendTransactionTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            try {
                Web3j web3j = Web3jFactory.build(new InfuraHttpService(getString(R.string.default_network)));

                String storeJson = "{\"address\":\"aa3cc54d7f10fa3a1737e4997ba27c34f330ce16\",\"crypto\":{\"cipher\":\"aes-128-ctr\",\"ciphertext\":\"94119190a98a3e6fd0512c1e170d2a632907192a54d4a355768dec5eb0818db7\",\"cipherparams\":{\"iv\":\"4e5fea1dbb06694c6809d379f736c2e2\"},\"kdf\":\"scrypt\",\"kdfparams\":{\"dklen\":32,\"n\":4096,\"p\":6,\"r\":8,\"salt\":\"0b92da3c8548156453b2a5960f16cdef9f365c49e44c3f3f9a9ee3544a0ef16b\"},\"mac\":\"08700b32aad5ca0b0ffd55001db36606ff52ee3d94f762176bb1269d27074bb9\"},\"id\":\"1e7a1a79-9ce9-47c9-b764-fed548766c65\",\"version\":3}";
                Account fromAccount = etherStore.importKeyStore(storeJson, getString(R.string.default_password));
                Log.d("INFO", "Imported account: " + fromAccount.getAddress().getHex());

                String password = getString(R.string.default_password);
                Account toAccount = etherStore.createAccount(password);
                Log.d("INFO", "Created account: " + toAccount.getAddress().getHex().toString());

                EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                        toAccount.getAddress().getHex().toString(), DefaultBlockParameterName.LATEST).sendAsync().get();
                BigInteger nonce = ethGetTransactionCount.getTransactionCount();
                Log.d("INFO", "New account nonce:" + new Long(nonce.longValue()).toString());

                byte[] signedMessage = etherStore.signTransaction(fromAccount, password, toAccount.getAddress().getHex().toString(), nonce.longValue());
                String hexValue = Numeric.toHexString(signedMessage);

                Log.d("INFO", "Sent transaction: " + hexValue);

                EthSendTransaction raw = web3j
                        .ethSendRawTransaction(hexValue)
                        .sendAsync()
                        .get();
                String result = raw.getTransactionHash();

                if (raw.hasError()) {
                    Log.d("INFO", "Transaction error message: " + raw.getError().getMessage());
                    Log.d("INFO", "Transaction error data: " + raw.getError().getData());
                }
                Log.d("INFO", "Transaction JSON-RPC"+ raw.getJsonrpc());
                Log.d("INFO", "Transaction result: " + raw.getResult());
                Log.d("INFO", "Transaction hash: " + raw.getTransactionHash());
            } catch (Exception e) {
                Log.d("ERROR", e.toString());
            }
            return null;
        }
    }

    public void testEtherscanService() throws Exception {

        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(getString(R.string.etherscan_url))
                .build();

        EtherscanService service = retrofit.create(EtherscanService.class);

        Call<ESTransactionListResponse> call =
                service.getTransactionList(
                        "account",
                        "txlist",
                        getString(R.string.default_address),
                        "4021100",
                        "4021122",
                        "asc",
                        getString(R.string.etherscan_api_key)
                    );


        Log.d("INFO", "Request query:" + call.request().url().query());
        call.enqueue(new Callback<ESTransactionListResponse>() {

            @Override

            public void onResponse(Call<ESTransactionListResponse> call, Response<ESTransactionListResponse> response) {

                List<ESTransaction> transactions = response.body().getTransactionList();

                //recyclerView.setAdapter(new MoviesAdapter(movies, R.layout.list_item_movie, getApplicationContext()));

                Log.d("INFO", "Number of transactions: " + transactions.size());
                Log.d("INFO", "Last transaction block number: " + transactions.get(0).getBlockNumber());
                Log.d("INFO", "First transaction block number: " + transactions.get(transactions.size() - 1).getBlockNumber());

            }

            @Override
            public void onFailure(Call<ESTransactionListResponse> call, Throwable t) {
                Log.e("ERROR", t.toString());
            }
        });
    }

    private class GetTransactionsTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            try {
                testEtherscanService();
            } catch (Exception e) {
                e.printStackTrace();
            }
            /*try {
                Web3j web3j = Web3jFactory.build(new InfuraHttpService(getString(R.string.default_network)));

                EthBlock ethBlock = web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false)
                        .send();

                BigInteger latestBlockNumber = ethBlock.getBlock().getNumber();
                Log.d("INFO", "Block number: " + latestBlockNumber.toString());
                Observable observable = web3j.transactionObservable();
                Log.d("INFO", "Created observable: " + observable.toString());

                Subscriber<Object> subscriber = new Subscriber<Object>() {
                    @Override
                    public void onCompleted() {
                        Log.d("INFO", "Completed");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d("INFO", "MyObserver error: " + e.toString());
                    }

                    @Override
                    public void onNext(Object o) {

                        Log.d("INFO", "onNext: " + o.toString());
                    }
                };
                Log.d("INFO", "Created observer");

                Subscription subscription = observable.subscribe(subscriber);

            } catch (Exception e) {
                Log.d("ERROR", e.toString());
            }*/
            return null;
        }
    }

    private class GetAccountsTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            try {
                Web3j web3 = Web3jFactory.build(new InfuraHttpService(getString(R.string.default_network)));
                EthAccounts ethAccounts = web3
                        .ethAccounts().send();
                List<String> accounts = ethAccounts.getAccounts();
                Log.d("INFO", "Get accounts: " + accounts.size());
            } catch (Exception e) {
                Log.d("ERROR", e.toString());
            }
            return null;
        }
    }

    private class GetWeb3ClientVersionTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            try {
                Web3j web3 = Web3jFactory.build(new InfuraHttpService(getString(R.string.default_network)));
                Web3ClientVersion web3ClientVersion = web3.web3ClientVersion().sendAsync().get();
                String clientVersion = web3ClientVersion.getWeb3ClientVersion();
                Log.d("INFO", "web3 client version: " + clientVersion);
            } catch (Exception e) {
                Log.d("ERROR", e.toString());
            }
            return null;
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);*/

        // TODO: remove
        etherStore = new EtherStore(this.getFilesDir().toString());

        keystoreBaseDir = this.getFilesDir() + "/keystore1";
        new GetWeb3ClientVersionTask().execute();

        Log.d("INFO", "MainActivity.onCreate");
    }

}
