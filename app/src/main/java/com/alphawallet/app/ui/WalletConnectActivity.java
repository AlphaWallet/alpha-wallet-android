package com.alphawallet.app.ui;

import android.app.AlertDialog;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.DAppFunction;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.viewmodel.WalletConnectViewModel;
import com.alphawallet.app.viewmodel.WalletConnectViewModelFactory;
import com.alphawallet.app.web3.entity.Message;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.app.walletconnect.WCClient;
import com.alphawallet.app.walletconnect.entity.WCPeerMeta;
import com.alphawallet.app.walletconnect.entity.WCEthereumSignMessage;
import com.alphawallet.app.walletconnect.entity.WCEthereumTransaction;
import com.alphawallet.app.walletconnect.WCSession;
import com.bumptech.glide.Glide;
import com.google.gson.GsonBuilder;

import org.web3j.utils.Numeric;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import kotlin.Unit;
import okhttp3.OkHttpClient;

public class WalletConnectActivity extends BaseActivity {
    private static final String TAG = WalletConnectActivity.class.getSimpleName();
    private static final String MESSAGE_PREFIX = "\u0019Ethereum Signed Message:\n";

    @Inject
    WalletConnectViewModelFactory viewModelFactory;
    WalletConnectViewModel viewModel;

    private WCClient client;
    private WCSession session;
    private WCPeerMeta peerMeta;

    private OkHttpClient httpClient;

    private ImageView icon;
    private TextView peerName;
    private TextView peerUrl;
    private TextView address;
    private ProgressBar progressBar;
    private LinearLayout infoLayout;
    private FunctionButtonBar functionBar;

    private Wallet wallet;
    private String qrCode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AndroidInjection.inject(this);

        setContentView(R.layout.activity_wallet_connect);

        toolbar();

        setTitle(getString(R.string.title_wallet_connect));

        initViews();

        initViewModel();

        retrieveQrCode();

        viewModel.prepare();
    }

    private void initViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(WalletConnectViewModel.class);

        viewModel.defaultWallet().observe(this, this::onDefaultWallet);
    }

    private void initViews() {
        progressBar = findViewById(R.id.progress);
        infoLayout = findViewById(R.id.layout_info);
        icon = findViewById(R.id.icon);
        peerName = findViewById(R.id.peer_name);
        peerUrl = findViewById(R.id.peer_url);
        address = findViewById(R.id.address);

        functionBar = findViewById(R.id.layoutButtons);
        functionBar.setPrimaryButtonText(R.string.action_end_session);
        functionBar.setPrimaryButtonClickListener(v -> {
            onBackPressed();
        });
    }

    private void onDefaultWallet(Wallet wallet) {
        this.wallet = wallet;
        address.setText(wallet.address);
        if (!wallet.address.isEmpty()) {
            initWalletConnectPeerMeta();
            initWalletConnectClient();
            initWalletConnectSession();
        }
    }

    private void initWalletConnectSession() {
        session = WCSession.Companion.from(qrCode);
        if (session != null) {
            client.connect(session, peerMeta, UUID.randomUUID().toString(), UUID.randomUUID().toString());
        } else {
            Toast.makeText(this, "Invalid WalletConnect QR data", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initWalletConnectPeerMeta() {
        String name = getString(R.string.app_name);
        String url = "https://www.alphawallet.com";
        String description = wallet.address;
        String[] icons = {"https://alphawallet.com/wp-content/uploads/2020/03/favicon.png"};

        peerMeta = new WCPeerMeta(
                name,
                url,
                description,
                Arrays.asList(icons)
        );
    }

    private void retrieveQrCode() {
        Bundle data = getIntent().getExtras();
        if (data != null) {
            this.qrCode = data.getString("qrCode");
        } else {
            Toast.makeText(this, "Error retrieving QR code", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initWalletConnectClient() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .build();
        client = new WCClient(new GsonBuilder(), httpClient);

        client.setOnSessionRequest((id, peer) -> {
            runOnUiThread(() -> {
                onSessionRequest(id, peer);
            });
            return Unit.INSTANCE;
        });

        client.setOnFailure(throwable -> {
            runOnUiThread(() -> {
                onFailure(throwable);
            });
            return Unit.INSTANCE;
        });

        client.setOnEthSign((id, message) -> {
            runOnUiThread(() -> {
                onEthSign(id, message);
            });
            return Unit.INSTANCE;
        });

        client.setOnEthSignTransaction((id, transaction) -> {
            runOnUiThread(() -> {
                onEthSignTransaction(id, transaction);
            });
            return Unit.INSTANCE;
        });

        client.setOnEthSendTransaction((id, transaction) -> {
            runOnUiThread(() -> {
                onEthSendTransaction(id, transaction);
            });
            return Unit.INSTANCE;
        });

        client.setOnDisconnect((code, reason) -> {
            finish();
            return Unit.INSTANCE;
        });
    }

    private void onSessionRequest(Long id, WCPeerMeta peer) {
        String[] accounts = {wallet.address};

        Glide.with(this)
                .load(peer.getIcons().get(0))
                .into(icon);
        peerName.setText(peer.getName());
        peerUrl.setText(peer.getUrl());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog = builder
                .setIcon(icon.getDrawable())
                .setTitle(peer.getName())
                .setMessage(peer.getUrl())
                .setPositiveButton(R.string.dialog_approve, (d, w) -> {
                    client.approveSession(Arrays.asList(accounts), 1);
                    progressBar.setVisibility(View.GONE);
                    functionBar.setVisibility(View.VISIBLE);
                    infoLayout.setVisibility(View.VISIBLE);
                })
                .setNegativeButton(R.string.dialog_reject, (d, w) -> {
                    client.rejectSession(getString(R.string.message_reject_request));
                    finish();
                })
                .setCancelable(false)
                .create();
        dialog.show();
    }

    private void onEthSign(Long id, WCEthereumSignMessage message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog = builder.setTitle(R.string.dialog_title_sign_message)
                .setMessage(message.getData())
                .setPositiveButton(R.string.dialog_ok, (d, w) -> {
                    signMessage(id, message);
                })
                .setNegativeButton(R.string.action_cancel, (d, w) -> {
                    client.rejectRequest(id, getString(R.string.message_reject_request));
                })
                .setCancelable(false)
                .create();
        dialog.show();
    }

    private void onEthSignTransaction(Long id, WCEthereumTransaction transaction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog = builder.setTitle(R.string.dialog_title_sign_transaction)
                .setMessage(transaction.getData())
                .setPositiveButton(R.string.dialog_ok, (d, w) -> {
                    signTransaction(id, transaction);
                })
                .setNegativeButton(R.string.action_cancel, (d, w) -> {
                    client.rejectRequest(id, getString(R.string.message_reject_request));
                })
                .setCancelable(false)
                .create();
        dialog.show();
    }

    private void onEthSendTransaction(Long id, WCEthereumTransaction transaction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog = builder.setTitle(R.string.dialog_send_eth_transaction)
                .setMessage(transaction.getData())
                .setPositiveButton(R.string.dialog_ok, (d, w) -> {
                    sendTransaction(id, transaction);
                })
                .setNegativeButton(R.string.action_cancel, (d, w) -> {
                    client.rejectRequest(id, getString(R.string.message_reject_request));
                })
                .setCancelable(false)
                .create();
        dialog.show();
    }

    private void onFailure(Throwable throwable) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog = builder.setTitle(R.string.title_dialog_error)
                .setMessage(throwable.getMessage())
                .setNeutralButton(R.string.try_again, (d, w) -> {
                    finish();
                })
                .setCancelable(false)
                .create();
        dialog.show();
    }

    private void doSignMessage(Long id, String data) {
        viewModel.getAuthenticationForSignature(wallet, this, new SignAuthenticationCallback() {
            @Override
            public void gotAuthorisation(boolean gotAuth) {
                if (gotAuth) {
                    viewModel.signMessage(
                            getEthereumMessage(Numeric.hexStringToByteArray(data)),
                            new DAppFunction() {
                                @Override
                                public void DAppError(Throwable error, Message<String> message) {
                                    showErrorDialog(error.getMessage());
                                }

                                @Override
                                public void DAppReturn(byte[] data, Message<String> message) {
                                    client.approveRequest(id, Numeric.toHexString(data));
                                }
                            },
                            new Message<>(data, peerMeta.getUrl(), 1)
                    );
                } else {
                    showErrorDialog(getString(R.string.message_authentication_failed));
                    client.rejectRequest(id, getString(R.string.message_authentication_failed));
                }
            }

            @Override
            public void cancelAuthentication() {
                showErrorDialog(getString(R.string.message_authentication_failed));
                client.rejectRequest(id, getString(R.string.message_authentication_failed));
            }
        });
    }

    private void sendTransaction(Long id, WCEthereumTransaction transaction) {
        viewModel.getAuthenticationForSignature(wallet, this, new SignAuthenticationCallback() {
            @Override
            public void gotAuthorisation(boolean gotAuth) {
                if (gotAuth) {
                    // TODO: Send transaction Implementation
                    // String signature = signed(transaction)
                    // client.approveRequest(id, signature);
                } else {
                    showErrorDialog(getString(R.string.message_authentication_failed));
                    client.rejectRequest(id, getString(R.string.message_authentication_failed));
                }
            }

            @Override
            public void cancelAuthentication() {
                showErrorDialog(getString(R.string.message_authentication_failed));
                client.rejectRequest(id, getString(R.string.message_authentication_failed));
            }
        });
    }

    private void signTransaction(Long id, WCEthereumTransaction transaction) {
        viewModel.getAuthenticationForSignature(wallet, this, new SignAuthenticationCallback() {
            @Override
            public void gotAuthorisation(boolean gotAuth) {
                if (gotAuth) {
                    // TODO: Sign transaction Implementation
                    // String signature = signed(transaction)
                    // client.approveRequest(id, signature);
                } else {
                    showErrorDialog(getString(R.string.message_authentication_failed));
                    client.rejectRequest(id, getString(R.string.message_authentication_failed));
                }
            }

            @Override
            public void cancelAuthentication() {
                showErrorDialog(getString(R.string.message_authentication_failed));
                client.rejectRequest(id, getString(R.string.message_authentication_failed));
            }
        });
    }

    private void signMessage(Long id, WCEthereumSignMessage message) {
        doSignMessage(id, message.getData());
    }

    private byte[] getEthereumMessage(byte[] message) {
        byte[] prefix = getEthereumMessagePrefix(message.length);
        byte[] result = new byte[prefix.length + message.length];
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        System.arraycopy(message, 0, result, prefix.length, message.length);
        return result;
    }

    private byte[] getEthereumMessagePrefix(int messageLength) {
        return MESSAGE_PREFIX.concat(String.valueOf(messageLength)).getBytes();
    }

    private void killSession() {
        if (client != null) {
            client.killSession();
        }
    }

    private void showErrorDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog = builder.setTitle(R.string.title_dialog_error)
                .setMessage(message)
                .setPositiveButton(R.string.try_again, (d, w) -> {
                    initWalletConnectSession();
                })
                .setNegativeButton(R.string.action_cancel, (d, w) -> {
                    d.dismiss();
                })
                .setCancelable(false)
                .create();
        dialog.show();
    }

    @Override
    public void onBackPressed() {
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            AlertDialog dialog = builder.setTitle(R.string.dialog_title_disconnect_session)
                    .setPositiveButton(R.string.dialog_ok, (d, w) -> {
                        killSession();
                    })
                    .setNegativeButton(R.string.action_cancel, (d, w) -> {
                        d.dismiss();
                    })
                    .create();
            dialog.show();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return false;
    }
}
