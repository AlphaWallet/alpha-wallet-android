package com.wallet.crypto.alphawallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.wallet.crypto.alphawallet.R;
import com.wallet.crypto.alphawallet.entity.Ticket;
import com.wallet.crypto.alphawallet.ui.barcode.BarcodeCaptureActivity;
import com.wallet.crypto.alphawallet.util.BalanceUtils;
import com.wallet.crypto.alphawallet.util.QRURLParser;
import com.wallet.crypto.alphawallet.viewmodel.TicketTransferViewModel;
import com.wallet.crypto.alphawallet.viewmodel.TicketTransferViewModelFactory;
import com.wallet.crypto.alphawallet.widget.SystemView;

import org.ethereum.geth.Address;

import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.wallet.crypto.alphawallet.C.Key.TICKET;

/**
 * Created by James on 28/01/2018.
 */

public class TicketTransferActivity extends BaseActivity
{
    private static final int BARCODE_READER_REQUEST_CODE = 1;

    @Inject
    protected TicketTransferViewModelFactory ticketTransferViewModelFactory;
    protected TicketTransferViewModel viewModel;
    private SystemView systemView;

    public TextView name;
    public TextView ids;

    private String address;
    private Ticket ticket;

    private EditText toAddressText;
    private EditText idsText;
    private TextInputLayout toInputLayout;
    private TextInputLayout amountInputLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_transfer_token);
        toolbar();

        ticket = getIntent().getParcelableExtra(TICKET);
        address = ticket.tokenInfo.address;

        systemView = findViewById(R.id.system_view);
        systemView.hide();

        setTitle(getString(R.string.ticket_transfer_title));

        name = findViewById(R.id.textViewName);
        ids = findViewById(R.id.textViewIDs);
        toInputLayout = findViewById(R.id.to_input_layout);
        toAddressText = findViewById(R.id.send_to_address);
        idsText = findViewById(R.id.send_ids);
        amountInputLayout = findViewById(R.id.amount_input_layout);

        name.setText(address);
        ids.setText("...");

        viewModel = ViewModelProviders.of(this, ticketTransferViewModelFactory)
                .get(TicketTransferViewModel.class);

        ImageButton scanBarcodeButton = findViewById(R.id.scan_barcode_button);
        scanBarcodeButton.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), BarcodeCaptureActivity.class);
            startActivityForResult(intent, BARCODE_READER_REQUEST_CODE);
        });

        viewModel.ticket().observe(this, this::onTicket);
    }

    private void onTicket(Ticket ticket) {
        name.setText(ticket.tokenInfo.name);
        String idStr = ticket.populateIDs(ticket.balanceArray, false);
        ids.setText(idStr);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.send_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_next: {
                onNext();
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BARCODE_READER_REQUEST_CODE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);

                    QRURLParser parser = QRURLParser.getInstance();
                    String extracted_address = parser.extractAddressFromQrString(barcode.displayValue);
                    if (extracted_address == null) {
                        Toast.makeText(this, R.string.toast_qr_code_no_address, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Point[] p = barcode.cornerPoints;
                    toAddressText.setText(extracted_address);
                }
            } else {
                Log.e("SEND", String.format(getString(R.string.barcode_error_format),
                        CommonStatusCodes.getStatusCodeString(resultCode)));
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.prepare(address);
    }

    private void onNext() {
        // Validate input fields
        boolean inputValid = true;
        final String to = toAddressText.getText().toString();
        if (!isAddressValid(to)) {
            toInputLayout.setError(getString(R.string.error_invalid_address));
            inputValid = false;
        }
        final String amount = idsText.getText().toString();
        List<Integer> idSendList = viewModel.ticket().getValue().parseIndexList(amount);

        if (idSendList == null || idSendList.isEmpty())
        {
            amountInputLayout.setError(getString(R.string.error_invalid_amount));
            inputValid = false;
        }

        if (!inputValid) {
            return;
        }

        String indexList = viewModel.ticket().getValue().populateIDs(idSendList, true);
        toInputLayout.setErrorEnabled(false);
        viewModel.openConfirmation(this, to, indexList, amount);
    }

    boolean isAddressValid(String address) {
        try {
            new Address(address);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    boolean isValidAmount(String eth) {
        try {
            String wei = BalanceUtils.EthToWei(eth);
            return wei != null;
        } catch (Exception e) {
            return false;
        }
    }
}
