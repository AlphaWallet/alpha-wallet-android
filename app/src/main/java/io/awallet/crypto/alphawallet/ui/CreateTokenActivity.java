package io.awallet.crypto.alphawallet.ui;

import android.app.Dialog;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Base64;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.awallet.crypto.alphawallet.R;
import io.awallet.crypto.alphawallet.entity.ErrorEnvelope;
import io.awallet.crypto.alphawallet.viewmodel.CreateTokenViewModel;
import io.awallet.crypto.alphawallet.viewmodel.CreateTokenViewModelFactory;
import io.awallet.crypto.alphawallet.widget.InputView;

public class CreateTokenActivity extends BaseActivity implements View.OnClickListener
{
    @Inject
    protected CreateTokenViewModelFactory createTokenViewModelFactory;
    private CreateTokenViewModel viewModel;

    public LinearLayout ticketLayout;

    //Ticket Info
    public TextView venue;
    public TextView date;
    public TextView price;

    private Dialog dialog;
    private String lastCheck;

    LinearLayout progressLayout;

    public InputView textInputview;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_create_token);

        toolbar();

        textInputview = findViewById(R.id.input_text);

        progressLayout = findViewById(R.id.layout_progress);

        venue = findViewById(R.id.textViewVenue);
        date = findViewById(R.id.textViewDate);
        price = findViewById(R.id.textViewPrice);

        ticketLayout = findViewById(R.id.layoutTicket);

        findViewById(R.id.save).setOnClickListener(this);

        viewModel = ViewModelProviders.of(this, createTokenViewModelFactory)
                .get(CreateTokenViewModel.class);
        viewModel.progress().observe(this, this::showProgress);
        viewModel.error().observe(this, this::onError);

        lastCheck = "";

        setTitle(R.string.empty);
    }

    private void showProgress(Boolean shouldShowProgress) {
        if (shouldShowProgress) {
            progressLayout.setVisibility(View.VISIBLE);
        } else {
            progressLayout.setVisibility(View.GONE);
        }
    }

    private void onSaved(boolean result) {
        if (result) {
            //viewModel.showTokens(this);
            finish();
        }
    }

    private void onError(ErrorEnvelope errorEnvelope) {
        dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.title_dialog_error)
                .setMessage(R.string.error_add_token)
                .setPositiveButton(R.string.try_again, null)
                .create();
        dialog.show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.save: {
                onSave();
            } break;
        }
    }

    private void onSave() {
        boolean isValid = true;
        String inputText = textInputview.getText().toString();

        if (inputText == null || inputText.length() < 3)
        {
            textInputview.setError(getString(R.string.error_token_text));
            isValid = false;
        }

        byte[] b64 = Base64.encode(inputText.getBytes(), Base64.URL_SAFE);
        String b64Str = new String(b64);

        if (b64Str.length() > 32)
        {
            textInputview.setError(getString(R.string.error_too_long));
            isValid = false;
        }

        if (isValid) {
            //viewModel.save(address, symbol, decimals, name, isStormbird);
        }
    }
}
