package com.alphawallet.app.ui;

import static com.alphawallet.app.widget.AWalletAlertDialog.ERROR;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.api.v1.entity.ApiV1;
import com.alphawallet.app.api.v1.entity.Method;
import com.alphawallet.app.api.v1.entity.request.ApiV1Request;
import com.alphawallet.app.api.v1.entity.request.ConnectRequest;
import com.alphawallet.app.api.v1.entity.request.SignPersonalMessageRequest;
import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.ApiV1ViewModel;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.ApiV1Dialog;
import com.alphawallet.app.widget.ConfirmationWidget;
import com.alphawallet.app.widget.SignDataWidget;
import com.alphawallet.token.entity.Signable;
import com.alphawallet.token.tools.Numeric;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ApiV1Activity extends BaseActivity
{
    private ApiV1ViewModel viewModel;
    private ApiV1Dialog apiV1Dialog;
    private ApiV1Request request;
    private AWalletAlertDialog alertDialog;
    private ConfirmationWidget confirmationWidget;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_api_v1);

        initViewModel();

        String requestUrl = getIntent().getStringExtra(C.Key.API_V1_REQUEST_URL);

        request = new ApiV1Request(requestUrl);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        viewModel.prepare();
    }

    private void initViewModel()
    {
        viewModel = new ViewModelProvider(this).get(ApiV1ViewModel.class);
        viewModel.defaultWallet().observe(this, this::onDefaultWallet);
        viewModel.signature().observe(this, this::onSignature);
        viewModel.error().observe(this, this::onError);
    }

    private void onDefaultWallet(Wallet wallet)
    {
        Method method = request.getMethod();
        if (method.getCallType().equals(ApiV1.CallType.CONNECT))
        {
            ConnectRequest connectRequest = new ConnectRequest(request.getRequestUrl());
            apiV1Dialog = setupConnectDialog(connectRequest, wallet.address);
            apiV1Dialog.show();
        }
        else if (method.getCallType().equals(ApiV1.CallType.SIGN_PERSONAL_MESSAGE))
        {
            SignPersonalMessageRequest signPersonalMessageRequest = new SignPersonalMessageRequest(request.getRequestUrl());
            if (viewModel.addressMatches(wallet.address, signPersonalMessageRequest.getAddress()))
            {
                apiV1Dialog = setupSignPersonalMessageDialog(signPersonalMessageRequest, wallet.address);
                apiV1Dialog.show();
            }
            else
            {
                cancelSignPersonalMessage();
            }
        }
    }

    private void cancelSignPersonalMessage()
    {
        Uri uri = viewModel.buildSignPersonalMessageResponse(request.getRedirectUrl(), null);
        redirect(uri);
    }

    public ApiV1Dialog setupConnectDialog(ConnectRequest req, String address)
    {
        ApiV1Dialog dialog = new ApiV1Dialog(this, req);
        dialog.addWidget(R.string.label_api_v1_note,
                getString(R.string.message_api_v1_note, Utils.formatAddress(address))
        );
        dialog.setPrimaryButtonListener(v -> {
            dialog.hideFunctionBar();
            connect(address);
        });
        dialog.setSecondaryButtonListener(v -> {
            dialog.hideFunctionBar();
            connect(null); // Reject connection
        });
        return dialog;
    }

    public ApiV1Dialog setupSignPersonalMessageDialog(SignPersonalMessageRequest req, String address)
    {
        ApiV1Dialog dialog = new ApiV1Dialog(this, req);
        dialog.addWidget(R.string.label_api_v1_note,
                getString(R.string.message_api_v1_note, Utils.formatAddress(address))
        );
        SignDataWidget signDataWidget = new SignDataWidget(this, null);
        signDataWidget.setupSignData(req.getSignable());
        dialog.addWidget(signDataWidget);
        confirmationWidget = new ConfirmationWidget(this, null);
        dialog.addWidget(confirmationWidget);
        dialog.setPrimaryButtonListener(v -> {
            dialog.hideFunctionBar();
            signPersonalMessage(req.getSignable());
        });
        dialog.setSecondaryButtonListener(v -> {
            cancelSignPersonalMessage();
            dialog.dismiss();
        });
        return dialog;
    }

    private void signPersonalMessage(Signable signable)
    {
        SignAuthenticationCallback signCallback = new SignAuthenticationCallback()
        {
            @Override
            public void gotAuthorisation(boolean gotAuth)
            {
                confirmationWidget.startProgressCycle(4);
                viewModel.signMessage(signable);
            }

            @Override
            public void cancelAuthentication()
            {

            }
        };

        viewModel.getAuthentication(this, signCallback);
    }

    private void connect(String address)
    {
        Uri uri = viewModel.buildConnectResponse(request.getRedirectUrl(), address);
        redirect(uri);
    }

    private void redirect(Uri uri)
    {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(browserIntent);
        finish();
    }

    private void onSignature(byte[] signature)
    {
        Uri uri = viewModel.buildSignPersonalMessageResponse(request.getRedirectUrl(), Numeric.toHexString(signature));
        confirmationWidget.completeProgressMessage(".", () -> redirect(uri));
    }

    private void onError(ErrorEnvelope errorEnvelope)
    {
        showErrorDialog(errorEnvelope.message);
    }

    private void showErrorDialog(String errorMessage)
    {
        alertDialog = new AWalletAlertDialog(this);
        alertDialog.setTitle(R.string.title_dialog_error);
        alertDialog.setMessage(errorMessage);
        alertDialog.setIcon(ERROR);
        alertDialog.setButtonText(R.string.dialog_ok);
        alertDialog.setButtonListener(v -> {
            alertDialog.dismiss();
            cancelSignPersonalMessage();
        });
        alertDialog.show();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if (apiV1Dialog != null && apiV1Dialog.isShowing())
        {
            apiV1Dialog.dismiss();
        }
        if (alertDialog != null && alertDialog.isShowing())
        {
            alertDialog.dismiss();
        }
    }
}
