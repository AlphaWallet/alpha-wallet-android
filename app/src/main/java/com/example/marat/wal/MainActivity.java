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

import org.ethereum.geth.Account;
import org.ethereum.geth.Address;
import org.ethereum.geth.BigInt;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.request.RawTransaction;
import org.web3j.protocol.core.methods.response.EthAccounts;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetBlockTransactionCountByHash;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.EthTransaction;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.infura.InfuraHttpService;
import org.web3j.protocol.parity.Parity;
import org.web3j.protocol.parity.ParityFactory;
import org.web3j.protocol.parity.methods.response.PersonalUnlockAccount;
import org.web3j.utils.Numeric;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.List;

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

    private class GetTransactionsTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            try {
                Web3j web3 = Web3jFactory.build(new InfuraHttpService(getString(R.string.default_network)));
                EthTransaction res = web3
                        .ethGetTransactionByHash("0x67229d5a27a6405d74fbb6ac20a57f64c6b4263f6d5c89a497aa25bcc531bcb9")
                        .sendAsync()
                        .get();
                Transaction tx = res.getTransaction();
                Log.d("INFO", "From: " + tx.getFrom());
                Log.d("INFO", "To: " + tx.getTo());
                Log.d("INFO", "Value: " + tx.getValue());
                Log.d("INFO", "Gas price: " + tx.getGasPrice());
                Log.d("INFO", "Gas: " + tx.getGas());
            } catch (Exception e) {
                Log.d("ERROR", e.toString());
            }
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
