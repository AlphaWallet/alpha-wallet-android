package io.awallet.crypto.alphawallet.ui;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import io.awallet.crypto.alphawallet.R;
import io.awallet.crypto.alphawallet.entity.ErrorEnvelope;
import io.awallet.crypto.alphawallet.entity.Ticket;
import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.ui.barcode.BarcodeCaptureActivity;
import io.awallet.crypto.alphawallet.ui.widget.adapter.TicketAdapter;
import io.awallet.crypto.alphawallet.ui.widget.entity.TicketRange;
import io.awallet.crypto.alphawallet.util.KeyboardUtils;
import io.awallet.crypto.alphawallet.util.QRURLParser;
import io.awallet.crypto.alphawallet.viewmodel.TransferTicketDetailViewModel;
import io.awallet.crypto.alphawallet.viewmodel.TransferTicketDetailViewModelFactory;
import io.awallet.crypto.alphawallet.widget.AWalletConfirmationDialog;
import io.awallet.crypto.alphawallet.widget.ProgressView;
import io.awallet.crypto.alphawallet.widget.SystemView;

import org.web3j.abi.datatypes.Address;
import org.web3j.tx.Contract;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static io.awallet.crypto.alphawallet.C.EXTRA_STATE;
import static io.awallet.crypto.alphawallet.C.EXTRA_TOKENID_LIST;
import static io.awallet.crypto.alphawallet.C.Key.TICKET;
import static io.awallet.crypto.alphawallet.C.Key.WALLET;

/**
 * Created by James on 21/02/2018.
 */

public class TransferTicketDetailActivity extends BaseActivity
{
    private static final int BARCODE_READER_REQUEST_CODE = 1;
    private static final int CHOOSE_QUANTITY = 0;
    private static final int PICK_TRANSFER_METHOD = 1;
    private static final int TRANSFER_USING_LINK = 2;
    private static final int TRANSFER_TO_ADDRESS = 3;

    @Inject
    protected TransferTicketDetailViewModelFactory viewModelFactory;
    protected TransferTicketDetailViewModel viewModel;
    private SystemView systemView;
    private ProgressView progressView;
    private Dialog dialog;

    private Ticket ticket;
    private TicketAdapter adapter;

    private TextView toAddressError;
    private EditText toAddressEditText;
    private ImageButton qrImageView;
    private TextView textQuantity;

    private String ticketIds;
    private String prunedIds;
    private int transferStatus;

    private AppCompatRadioButton pickLink;
    private AppCompatRadioButton pickTransfer;

    private LinearLayout pickTransferAddress;
    private LinearLayout pickTicketQuantity;
    private LinearLayout pickTransferMethod;
    private LinearLayout pickExpiryDate;
    private LinearLayout buttonLinkPick;
    private LinearLayout buttonTransferPick;

    private EditText expiryDateEditText;
    private EditText expiryTimeEditText;
    private DatePickerDialog datePickerDialog;
    private TimePickerDialog timePickerDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_detail);

        ticket = getIntent().getParcelableExtra(TICKET);
        Wallet wallet = getIntent().getParcelableExtra(WALLET);
        ticketIds = getIntent().getStringExtra(EXTRA_TOKENID_LIST);
        transferStatus = getIntent().getIntExtra(EXTRA_STATE, 0);
        prunedIds = ticketIds;

        //we should import a token and a list of chosen ids
        RecyclerView list = findViewById(R.id.listTickets);
        adapter = new TicketAdapter(this, this::onTicketIdClick, ticket, ticketIds);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        toolbar();
        setTitle(getString(R.string.empty));
        systemView = findViewById(R.id.system_view);
        systemView.hide();
        progressView = findViewById(R.id.progress_view);
        progressView.hide();

        toAddressEditText = findViewById(R.id.edit_to_address);

        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(TransferTicketDetailViewModel.class);
        viewModel.setWallet(wallet);
        viewModel.progress().observe(this, systemView::showProgress);
        viewModel.queueProgress().observe(this, progressView::updateProgress);
        viewModel.pushToast().observe(this, this::displayToast);
        viewModel.newTransaction().observe(this, this::onTransaction);
        viewModel.error().observe(this, this::onError);
        viewModel.universalLinkReady().observe(this, this::linkReady);

        textQuantity = findViewById(R.id.text_quantity);
        toAddressError = findViewById(R.id.to_address_error);

        pickTransferAddress = findViewById(R.id.layout_addressbar);
        pickTicketQuantity = findViewById(R.id.layout_ticket_quantity);
        pickTransferMethod = findViewById(R.id.layout_choose_method);
        pickExpiryDate = findViewById(R.id.layout_date_picker);

        expiryDateEditText = findViewById(R.id.edit_expiry_date);
        expiryTimeEditText = findViewById(R.id.edit_expiry_time);

        pickLink = findViewById(R.id.radio_pickup_link);
        pickTransfer = findViewById(R.id.radio_transfer_now);

        buttonLinkPick = findViewById(R.id.layout_link_pick);
        buttonTransferPick = findViewById(R.id.layout_transfer_now);

        Button nextAction = findViewById(R.id.button_next);
        nextAction.setOnClickListener((View v) -> {
            int nextState = getNextState();
            if (nextState >= 0)
            {
                viewModel.openTransferState(this, ticket, prunedIds, nextState);
            }
        });

        qrImageView = findViewById(R.id.img_scan_qr);
        qrImageView.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), BarcodeCaptureActivity.class);
            startActivityForResult(intent, BARCODE_READER_REQUEST_CODE);
        });

        toAddressEditText.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                toAddressError.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s)
            {

            }
        });

        setupScreen();
    }

    //TODO: This is repeated code also in SellDetailActivity. Probably should be abstracted out into generic view code routine
    private void initQuantitySelector() {
        pickTicketQuantity.setVisibility(View.VISIBLE);
        RelativeLayout plusButton = findViewById(R.id.layout_quantity_add);
        plusButton.setOnClickListener(v -> {
            int quantity = Integer.parseInt(textQuantity.getText().toString());
            if ((quantity + 1) <= adapter.getTicketRangeCount()) {
                quantity++;
                textQuantity.setText(String.valueOf(quantity));
                prunedIds = ticket.pruneIDList(ticketIds, quantity);
            }
        });

        RelativeLayout minusButton = findViewById(R.id.layout_quantity_minus);
        minusButton.setOnClickListener(v -> {
            int quantity = Integer.parseInt(textQuantity.getText().toString());
            if ((quantity - 1) >= 0) {
                quantity--;
                textQuantity.setText(String.valueOf(quantity));
                prunedIds = ticket.pruneIDList(ticketIds, quantity);
            }
        });

        textQuantity.setText("1");
        prunedIds = ticket.pruneIDList(ticketIds, 1);
    }

    private void setupRadioButtons()
    {
        buttonLinkPick.setOnClickListener((View v) -> {
                                              pickLink.setChecked(true);
                                              pickTransfer.setChecked(false);
                                          }
        );

        buttonTransferPick.setOnClickListener((View v) -> {
                                                  pickLink.setChecked(false);
                                                  pickTransfer.setChecked(true);
                                              }
        );

        pickLink.setOnClickListener((View v) -> {
                                        pickLink.setChecked(true);
                                        pickTransfer.setChecked(false);
                                    }
        );

        pickTransfer.setOnClickListener((View v) -> {
                                            pickLink.setChecked(false);
                                            pickTransfer.setChecked(true);
                                        }
        );
    }

    private int getNextState()
    {
        int newState = -1;

        switch (transferStatus)
        {
            case CHOOSE_QUANTITY:
                newState = PICK_TRANSFER_METHOD;
                break;
            case PICK_TRANSFER_METHOD:
                if (pickTransfer.isChecked())
                {
                    newState = TRANSFER_TO_ADDRESS;
                }
                else
                {
                    newState = TRANSFER_USING_LINK;
                }
                break;
            case TRANSFER_USING_LINK:
                //generate link
                viewModel.generateUniversalLink(ticket.getTicketIndicies(ticketIds), ticket.getAddress(), calculateExpiryTime());
                break;
            case TRANSFER_TO_ADDRESS:
                //transfer using eth
                confirmTransfer();
                break;
        }

        return newState;
    }

    private long calculateExpiryTime()
    {
        String expiryDate = expiryDateEditText.getText().toString();
        String expiryTime = expiryTimeEditText.getText().toString();
        String tempDateString = expiryDate + " " + expiryTime;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        Date date;
        String dateString = "";
        long UTCTimeStamp = 0;
        try
        {
            date = simpleDateFormat.parse(tempDateString);
            dateString = simpleDateFormat.format(date);
            Log.d(SellDetailActivity.class.getSimpleName(), "date : " + dateString);
            UTCTimeStamp = (date.getTime()) / 1000;
        }
        catch (ParseException e)
        {
            Log.e(SellDetailActivity.class.getSimpleName(), e.getMessage(), e);
        }

        return UTCTimeStamp;
    }

    private void setupScreen()
    {
        pickTransferAddress.setVisibility(View.GONE);
        pickTicketQuantity.setVisibility(View.GONE);
        pickTransferMethod.setVisibility(View.GONE);
        pickExpiryDate.setVisibility(View.GONE);

        switch (transferStatus)
        {
            case CHOOSE_QUANTITY:
                initQuantitySelector();
                break;
            case PICK_TRANSFER_METHOD:
                setupRadioButtons();
                pickTransferMethod.setVisibility(View.VISIBLE);
                break;
            case TRANSFER_USING_LINK:
                initDatePicker();
                initTimePicker();
                expiryDateEditText.setOnClickListener(v -> datePickerDialog.show());
                expiryTimeEditText.setOnClickListener(v -> timePickerDialog.show());
                pickExpiryDate.setVisibility(View.VISIBLE);
                break;
            case TRANSFER_TO_ADDRESS:
                pickTransferAddress.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void onTransaction(String hash)
    {
        hideDialog();
        dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.transaction_succeeded)
                .setMessage(hash)
                .setPositiveButton(R.string.button_ok, (dialog1, id) -> {
                    //TODO: go back to ticket asset view page
                    finish();
                })
                .setNeutralButton(R.string.copy, (dialog1, id) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("transaction hash", hash);
                    clipboard.setPrimaryClip(clip);
                    finish();
                })
                .create();
        dialog.show();
    }

    private void hideDialog()
    {
        if (dialog != null && dialog.isShowing())
        {
            dialog.dismiss();
        }
    }

    private void onProgress(boolean shouldShowProgress)
    {
        hideDialog();
        if (shouldShowProgress)
        {
            dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.title_dialog_sending)
                    .setView(new ProgressBar(this))
                    .setCancelable(false)
                    .create();
            dialog.show();
        }
    }

    private void onError(ErrorEnvelope error)
    {
        hideDialog();
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.error_transaction_failed)
                .setMessage(error.message)
                .setPositiveButton(R.string.button_ok, (dialog1, id) -> {
                    // Do nothing
                })
                .create();
        dialog.show();
    }

    private void transferTicketFinal()
    {
        boolean isValid = true;
        //complete the transfer

        //check send address
        toAddressError.setVisibility(View.GONE);
        final String to = toAddressEditText.getText().toString();
        if (!isAddressValid(to))
        {
            toAddressError.setVisibility(View.VISIBLE);
            toAddressError.setText(getString(R.string.error_invalid_address));
            isValid = false;
        }

        onProgress(true);

        if (isValid)
        {
            viewModel.createTicketTransfer(
                    to,
                    ticket.getAddress(),
                    ticket.integerListToString(ticket.ticketIdStringToIndexList(prunedIds), false),
                    Contract.GAS_PRICE,
                    Contract.GAS_LIMIT);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        viewModel.prepare(ticket);
        KeyboardUtils.hideKeyboard(toAddressEditText);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    private void onTicketIdClick(View view, TicketRange range)
    {
        Context context = view.getContext();
        //TODO: what action should be performed when clicking on a range?
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == BARCODE_READER_REQUEST_CODE)
        {
            if (resultCode == CommonStatusCodes.SUCCESS)
            {
                if (data != null)
                {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);

                    QRURLParser parser = QRURLParser.getInstance();
                    String extracted_address = parser.extractAddressFromQrString(barcode.displayValue);
                    if (extracted_address == null)
                    {
                        Toast.makeText(this, R.string.toast_qr_code_no_address, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Point[] p = barcode.cornerPoints;
                    toAddressEditText.setText(extracted_address);
                }
            }
            else
            {
                Log.e("SEND", String.format(getString(R.string.barcode_error_format),
                                            CommonStatusCodes.getStatusCodeString(resultCode)));
            }
        }
        else
        {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void linkReady(String universalLink)
    {
        //how many tickets are we selling?
        TextView textQuantity = findViewById(R.id.text_quantity);
        int ticketName = (Integer.valueOf(textQuantity.getText().toString()) > 1) ? R.string.tickets : R.string.ticket;
        String qty = textQuantity.getText().toString() + " " +
                getResources().getString(ticketName) + "\n" +
                getString(R.string.universal_link_expiry_on) + expiryDateEditText.getText().toString() + " " + expiryTimeEditText.getText().toString();

        AWalletConfirmationDialog dialog = new AWalletConfirmationDialog(this);
        dialog.setTitle(R.string.generate_pick_up_link);
        dialog.setSmallText(R.string.generate_free_transfer_link);
        dialog.setMediumText(qty);
        dialog.setPrimaryButtonText(R.string.send_universal_link);
        dialog.setSecondaryButtonText(R.string.dialog_cancel_back);
        dialog.setPrimaryButtonListener(v1 -> transferLinkFinal(universalLink));
        dialog.setSecondaryButtonListener(v1 -> dialog.dismiss());
        dialog.showShareLink();
        dialog.show();
    }

    private void confirmTransfer()
    {
        final String to = toAddressEditText.getText().toString();
        //check address
        if (!isAddressValid(to))
        {
            toAddressError.setVisibility(View.VISIBLE);
            return;
        }

        //how many tickets are we selling?
        int quantity = ticket.stringHexToBigIntegerList(prunedIds).size();
        int ticketName = (quantity > 1) ? R.string.tickets : R.string.ticket;

        String qty = String.valueOf(quantity) + " " +
                getResources().getString(ticketName) + "\n" +
                getResources().getString(R.string.to) + " " +
                to;

        AWalletConfirmationDialog dialog = new AWalletConfirmationDialog(this);
        dialog.setTitle(R.string.title_transaction_details);
        dialog.setSmallText(R.string.confirm_transfer_details);
        dialog.setMediumText(qty);
        dialog.setPrimaryButtonText(R.string.transfer_tickets);
        dialog.setSecondaryButtonText(R.string.dialog_cancel_back);
        dialog.setPrimaryButtonListener(v1 -> transferTicketFinal());
        dialog.setSecondaryButtonListener(v1 -> dialog.dismiss());
        dialog.show();
    }

    private void transferLinkFinal(String universalLink)
    {
        //create share intent
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, universalLink);
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }

    private boolean isAddressValid(String address)
    {
        try
        {
            new Address(address);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private void initDatePicker()
    {
        String dateFormat = "dd/MM/yyyy";
        Calendar newCalendar = Calendar.getInstance();
        SimpleDateFormat dateFormatter = new SimpleDateFormat(dateFormat, Locale.ENGLISH);

        datePickerDialog = new DatePickerDialog(this, (view, year, monthOfYear, dayOfMonth) -> {
            Calendar newDate = Calendar.getInstance();
            newDate.set(year, monthOfYear, dayOfMonth);
            expiryDateEditText.setText(dateFormatter.format(newDate.getTime()));
        }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));

        //set default for tomorrow
        long tomorrowStamp = System.currentTimeMillis() + 1000 * 60 * 60 * 24;
        Date tomorrow = new Date(tomorrowStamp);
        expiryDateEditText.setText(dateFormatter.format(tomorrow.getTime()));
    }

    private void initTimePicker()
    {
        Calendar newCalendar = Calendar.getInstance();
        timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
            expiryTimeEditText.setText(time);
        }, newCalendar.get(Calendar.HOUR_OF_DAY), newCalendar.get(Calendar.MINUTE), true);

        //set for now
        String time = String.format(Locale.getDefault(), "%02d:%02d", Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                                    Calendar.getInstance().get(Calendar.MINUTE));
        expiryTimeEditText.setText(time);
    }
}

