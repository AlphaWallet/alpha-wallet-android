package com.wallet.crypto.alphawallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.wallet.crypto.alphawallet.R;
import com.wallet.crypto.alphawallet.entity.Ticket;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.ui.barcode.BarcodeCaptureActivity;
import com.wallet.crypto.alphawallet.ui.widget.adapter.TicketAdapter;
import com.wallet.crypto.alphawallet.ui.widget.entity.TicketRange;
import com.wallet.crypto.alphawallet.util.BalanceUtils;
import com.wallet.crypto.alphawallet.util.KeyboardUtils;
import com.wallet.crypto.alphawallet.util.QRURLParser;
import com.wallet.crypto.alphawallet.viewmodel.TransferTicketDetailViewModel;
import com.wallet.crypto.alphawallet.viewmodel.TransferTicketDetailViewModelFactory;
import com.wallet.crypto.alphawallet.widget.AWalletConfirmationDialog;
import com.wallet.crypto.alphawallet.widget.ProgressView;
import com.wallet.crypto.alphawallet.widget.SystemView;

import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.wallet.crypto.alphawallet.C.EXTRA_TOKENID_LIST;
import static com.wallet.crypto.alphawallet.C.Key.TICKET;
import static com.wallet.crypto.alphawallet.C.Key.WALLET;

/**
 * Created by James on 21/02/2018.
 */

public class TransferTicketDetailActivity extends BaseActivity {
    private static final int BARCODE_READER_REQUEST_CODE = 1;

    @Inject
    protected TransferTicketDetailViewModelFactory viewModelFactory;
    protected TransferTicketDetailViewModel viewModel;
    private SystemView systemView;
    private ProgressView progressView;

    private Ticket ticket;
    private TicketRange ticketRange;
    private TicketAdapter adapter;

    private String ticketIds;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_detail);

        ticket = getIntent().getParcelableExtra(TICKET);
        Wallet wallet = getIntent().getParcelableExtra(WALLET);
        ticketIds = getIntent().getStringExtra(EXTRA_TOKENID_LIST);

        //we should import a token and a list of chosen ids
        RecyclerView list = findViewById(R.id.listTickets);
        adapter = new TicketAdapter(this::onTicketIdClick, ticket, ticketIds);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        toolbar();
        setTitle(getString(R.string.empty));
        systemView = findViewById(R.id.system_view);
        systemView.hide();
        progressView = findViewById(R.id.progress_view);
        progressView.hide();

        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(TransferTicketDetailViewModel.class);
        viewModel.setWallet(wallet);
        viewModel.progress().observe(this, systemView::showProgress);
        viewModel.queueProgress().observe(this, progressView::updateProgress);
        viewModel.pushToast().observe(this, this::displayToast);

        TextView textQuantity = findViewById(R.id.text_quantity);

        RelativeLayout plusButton = findViewById(R.id.layout_quantity_add);
        plusButton.setOnClickListener(v -> {
            int quantity = Integer.parseInt(textQuantity.getText().toString());
            if ((quantity + 1) <= adapter.getTicketRangeCount()) {
                quantity++;
                textQuantity.setText(String.valueOf(quantity));
            }
        });

        RelativeLayout minusButton = findViewById(R.id.layout_quantity_minus);
        minusButton.setOnClickListener(v -> {
            int quantity = Integer.parseInt(textQuantity.getText().toString());
            if ((quantity - 1) >= 0) {
                quantity--;
                textQuantity.setText(String.valueOf(quantity));
            }
        });

        Button btnTransfer = findViewById(R.id.button_transfer);
        btnTransfer.setOnClickListener((View v) -> {
            if (Integer.parseInt(textQuantity.getText().toString()) > 0) {
                AWalletConfirmationDialog dialog = new AWalletConfirmationDialog(this);
                dialog.setTitle(R.string.confirm_transfer_title);
//                dialog.setSmallText(R.string.confirm_sale_small_text);
//                dialog.setBigText(totalCostText.getText().toString());
                dialog.setPrimaryButtonText(R.string.action_transfer);
                dialog.setSecondaryButtonText(R.string.dialog_cancel_back);
                dialog.setPrimaryButtonListener(v1 -> transferTicketFinal());
                dialog.setSecondaryButtonListener(v1 -> dialog.dismiss());
                dialog.show();
            }
        });
    }

    private void transferTicketFinal() {
//        boolean inputValid = true;
//        final String to = toAddressText.getText().toString();
//        if (!isAddressValid(to)) {
//            toInputLayout.setError(getString(R.string.error_invalid_address));
//            inputValid = false;
//        }
//        final String amount = idsText.getText().toString();
//        List<Integer> idSendList = viewModel.ticket().getValue().parseIndexList(amount);
//
//        if (idSendList == null || idSendList.isEmpty())
//        {
//            amountInputLayout.setError(getString(R.string.error_invalid_amount));
//            inputValid = false;
//        }
//
//        if (!inputValid) {
//            return;
//        }
//
//        String indexList = viewModel.ticket().getValue().populateIDs(idSendList, true);
//        toInputLayout.setErrorEnabled(false);
//        viewModel.openConfirmation(this, to, indexList, amount);
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.prepare(ticket);
    }

    private void onTicketIdClick(View view, TicketRange range) {
        Context context = view.getContext();
        //TODO: what action should be performed when clicking on a range?
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (requestCode == BARCODE_READER_REQUEST_CODE) {
//            if (resultCode == CommonStatusCodes.SUCCESS) {
//                if (data != null) {
//                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
//
//                    QRURLParser parser = QRURLParser.getInstance();
//                    String extracted_address = parser.extractAddressFromQrString(barcode.displayValue);
//                    if (extracted_address == null) {
//                        Toast.makeText(this, R.string.toast_qr_code_no_address, Toast.LENGTH_SHORT).show();
//                        return;
//                    }
//                    Point[] p = barcode.cornerPoints;
//                    toAddressText.setText(extracted_address);
//                }
//            } else {
//                Log.e("SEND", String.format(getString(R.string.barcode_error_format),
//                        CommonStatusCodes.getStatusCodeString(resultCode)));
//            }
//        } else {
//            super.onActivityResult(requestCode, resultCode, data);
//        }
    }
}

