package io.stormbird.wallet.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.web3j.abi.datatypes.Address;
import org.web3j.tx.Contract;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.ErrorEnvelope;
import io.stormbird.wallet.entity.Ticket;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.router.HomeRouter;
import io.stormbird.wallet.ui.widget.adapter.TicketAdapter;
import io.stormbird.wallet.ui.zxing.FullScannerFragment;
import io.stormbird.wallet.ui.zxing.QRScanningActivity;
import io.stormbird.wallet.util.KeyboardUtils;
import io.stormbird.wallet.util.QRURLParser;
import io.stormbird.wallet.viewmodel.TransferTicketDetailViewModel;
import io.stormbird.wallet.viewmodel.TransferTicketDetailViewModelFactory;
import io.stormbird.wallet.widget.AWalletAlertDialog;
import io.stormbird.wallet.widget.AWalletConfirmationDialog;
import io.stormbird.wallet.widget.ProgressView;
import io.stormbird.wallet.widget.SystemView;
import io.stormbird.wallet.entity.FinishReceiver;
import io.stormbird.token.entity.TicketRange;

import static io.stormbird.wallet.C.EXTRA_STATE;
import static io.stormbird.wallet.C.EXTRA_TOKENID_LIST;
import static io.stormbird.wallet.C.Key.TICKET;
import static io.stormbird.wallet.C.Key.WALLET;
import static io.stormbird.wallet.C.PRUNE_ACTIVITY;

/**
 * Created by James on 21/02/2018.
 */

public class TransferTicketDetailActivity extends BaseActivity
{
    private static final int BARCODE_READER_REQUEST_CODE = 1;
    private static final int SEND_INTENT_REQUEST_CODE = 2;
    private static final int CHOOSE_QUANTITY = 0;
    private static final int PICK_TRANSFER_METHOD = 1;
    private static final int TRANSFER_USING_LINK = 2;
    private static final int TRANSFER_TO_ADDRESS = 3;

    @Inject
    protected TransferTicketDetailViewModelFactory viewModelFactory;
    protected TransferTicketDetailViewModel viewModel;
    private SystemView systemView;
    private ProgressView progressView;
    private AWalletAlertDialog dialog;

    private FinishReceiver finishReceiver;

    private Ticket ticket;
    private TicketAdapter adapter;

    private TextView titleText;
    private TextView toAddressError;
    private TextView validUntil;
    private EditText toAddressEditText;
    private ImageButton qrImageView;
    private TextView textQuantity;

    private String ticketIds;
    private String prunedIds;
    private int transferStatus;

    private AWalletConfirmationDialog confirmationDialog;

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
        viewModel.userTransaction().observe(this, this::onUserTransaction);

        //we should import a token and a list of chosen ids
        RecyclerView list = findViewById(R.id.listTickets);
        adapter = new TicketAdapter(this::onTicketIdClick, ticket, ticketIds, viewModel.getAssetDefinitionService());
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        textQuantity = findViewById(R.id.text_quantity);
        titleText = findViewById(R.id.title_transfer);
        validUntil = findViewById(R.id.text_valid_until);
        toAddressError = findViewById(R.id.to_address_error);

        pickTransferAddress = findViewById(R.id.layout_addressbar);
        pickTicketQuantity = findViewById(R.id.layout_ticket_quantity);
        pickTransferMethod = findViewById(R.id.layout_choose_method);
        pickExpiryDate = findViewById(R.id.layout_date_picker);

        expiryDateEditText = findViewById(R.id.edit_expiry_date);
        expiryDateEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validUntil.setText(getString(R.string.link_valid_until, s.toString(), expiryTimeEditText.getText().toString()));
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        expiryTimeEditText = findViewById(R.id.edit_expiry_time);
        expiryTimeEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validUntil.setText(getString(R.string.link_valid_until, expiryDateEditText.getText().toString(), s.toString()));
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

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
            Intent intent = new Intent(this, QRScanningActivity.class);
            startActivityForResult(intent, BARCODE_READER_REQUEST_CODE);
//            Intent intent = new Intent(getApplicationContext(), BarcodeCaptureActivity.class);
//            startActivityForResult(intent, BARCODE_READER_REQUEST_CODE);
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

        finishReceiver = new FinishReceiver(this);
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
        buttonLinkPick.setSelected(true);
        buttonLinkPick.setOnClickListener((View v) -> {
            pickLink.setChecked(true);
            pickTransfer.setChecked(false);
            buttonLinkPick.setSelected(true);
            buttonTransferPick.setSelected(false);
        });

        buttonTransferPick.setOnClickListener((View v) -> {
            pickLink.setChecked(false);
            pickTransfer.setChecked(true);
            buttonLinkPick.setSelected(false);
            buttonTransferPick.setSelected(true);
        });

        pickLink.setOnClickListener((View v) -> {
            pickLink.setChecked(true);
            pickTransfer.setChecked(false);
            buttonLinkPick.setSelected(true);
            buttonTransferPick.setSelected(false);
        });

        pickTransfer.setOnClickListener((View v) -> {
            pickLink.setChecked(false);
            pickTransfer.setChecked(true);
            buttonLinkPick.setSelected(false);
            buttonTransferPick.setSelected(true);
        });
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
                pickTicketQuantity.setVisibility(View.VISIBLE);
                titleText.setText(R.string.title_select_ticket_quantity);
                break;
            case PICK_TRANSFER_METHOD:
                setupRadioButtons();
                pickTransferMethod.setVisibility(View.VISIBLE);
                titleText.setText(R.string.title_select_transfer_method);
                break;
            case TRANSFER_USING_LINK:
                initDatePicker();
                initTimePicker();
                expiryDateEditText.setOnClickListener(v -> datePickerDialog.show());
                expiryTimeEditText.setOnClickListener(v -> timePickerDialog.show());
                pickExpiryDate.setVisibility(View.VISIBLE);
                titleText.setText(R.string.title_set_universal_link_expiry);
                break;
            case TRANSFER_TO_ADDRESS:
                pickTransferAddress.setVisibility(View.VISIBLE);
                titleText.setText(R.string.title_input_wallet_address);
                break;
        }
    }

    private void onTransaction(String success)
    {
        hideDialog();
        dialog = new AWalletAlertDialog(this);
        dialog.setTitle(R.string.transaction_succeeded);
        dialog.setMessage(success);
        dialog.setIcon(AWalletAlertDialog.SUCCESS);
        dialog.setButtonText(R.string.button_ok);
        dialog.setButtonListener(v -> finish());

        dialog.show();
    }

    private void onUserTransaction(String hash)
    {
        hideDialog();
        dialog = new AWalletAlertDialog(this);
        dialog.setTitle(R.string.transaction_succeeded);
        dialog.setMessage(hash);
        dialog.setButtonText(R.string.copy);
        dialog.setButtonListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("transaction hash", hash);
            clipboard.setPrimaryClip(clip);
            dialog.dismiss();
            sendBroadcast(new Intent(PRUNE_ACTIVITY));
        });
        dialog.setOnDismissListener(v -> {
            dialog.dismiss();
            sendBroadcast(new Intent(PRUNE_ACTIVITY));
            new HomeRouter().open(this, true);
            finish();
        });
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
            dialog = new AWalletAlertDialog(this);
            dialog.setIcon(AWalletAlertDialog.NONE);
            dialog.setTitle(R.string.title_dialog_sending);
            dialog.setMessage(R.string.transfer);
            dialog.setProgressMode();
            dialog.setCancelable(false);
            dialog.show();
        }
    }

    private void onError(ErrorEnvelope error)
    {
        hideDialog();
        dialog = new AWalletAlertDialog(this);
        dialog.setIcon(AWalletAlertDialog.ERROR);
        dialog.setTitle(R.string.error_transaction_failed);
        dialog.setMessage(error.message);
        dialog.setCancelable(true);
        dialog.setButtonText(R.string.button_ok);
        dialog.setButtonListener(v -> dialog.dismiss());
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
                    ticket.integerListToString(ticket.ticketIdStringToIndexList(prunedIds), true),
                    Contract.GAS_PRICE,
                    Contract.GAS_LIMIT);

//            viewModel.createTicketTransfer(
//                    to,
//                    ticket.getAddress(),
//                    ticket.integerListToString(ticket.ticketIdStringToIndexList(prunedIds), true),
//                    Contract.GAS_PRICE,
//                    Contract.GAS_LIMIT);

            //select between feemaster or user-pays-gas
            //Not sure if we will ever implement this
//            String XMLContractAddress = ticket.getXMLProperty("address", this);
//            if (XMLContractAddress.equalsIgnoreCase(ticket.getAddress()))
//            {
//                String feeMasterUrl = ticket.getXMLProperty("feemaster", this);
//                viewModel.feeMasterCall(
//                        feeMasterUrl,
//                        to,
//                        ticket,
//                        ticket.integerListToString(ticket.ticketIdStringToIndexList(prunedIds), true));
//            }
//            else
//            {
//                viewModel.createTicketTransfer(
//                        to,
//                        ticket.getAddress(),
//                        ticket.integerListToString(ticket.ticketIdStringToIndexList(prunedIds), true),
//                        Contract.GAS_PRICE,
//                        Contract.GAS_LIMIT);
//            }
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

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(finishReceiver);
        if (confirmationDialog != null && confirmationDialog.isShowing())
        {
            confirmationDialog.dismiss();
            confirmationDialog = null;
        }
    }

    private void onTicketIdClick(View view, TicketRange range)
    {
        Context context = view.getContext();
        //TODO: what action should be performed when clicking on a range?
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case BARCODE_READER_REQUEST_CODE:
            if (resultCode == FullScannerFragment.SUCCESS)
            {
                if (data != null)
                {
                    String barcode = data.getParcelableExtra(FullScannerFragment.BarcodeObject);
                    if (barcode == null) barcode = data.getStringExtra(FullScannerFragment.BarcodeObject);

                    //if barcode is still null, ensure we don't GPF
                    if (barcode == null)
                    {
                        Toast.makeText(this, R.string.toast_qr_code_no_address, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    QRURLParser parser = QRURLParser.getInstance();
                    String extracted_address = parser.extractAddressFromQrString(barcode);
                    if (extracted_address == null)
                    {
                        Toast.makeText(this, R.string.toast_qr_code_no_address, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    toAddressEditText.setText(extracted_address);
                }
            }
            else
            {
                Log.e("SEND", String.format(getString(R.string.barcode_error_format),
                        "Code: " + String.valueOf(resultCode)
                        ));
            }
            break;

            case SEND_INTENT_REQUEST_CODE:
                sendBroadcast(new Intent(PRUNE_ACTIVITY));
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void linkReady(String universalLink)
    {
        int quantity = ticket.ticketIdStringToIndexList(prunedIds).size();
        int ticketName = (quantity > 1) ? R.string.tickets : R.string.ticket;
        String qty = String.valueOf(quantity) + " " +
                getResources().getString(ticketName) + "\n" +
                getString(R.string.universal_link_expiry_on) + expiryDateEditText.getText().toString() + " " + expiryTimeEditText.getText().toString();

        confirmationDialog = new AWalletConfirmationDialog(this);
        confirmationDialog.setTitle(R.string.generate_pick_up_link);
        confirmationDialog.setSmallText(R.string.generate_free_transfer_link);
        confirmationDialog.setMediumText(qty);
        confirmationDialog.setPrimaryButtonText(R.string.send_universal_link);
        confirmationDialog.setSecondaryButtonText(R.string.dialog_cancel_back);
        confirmationDialog.setPrimaryButtonListener(v1 -> transferLinkFinal(universalLink));
        confirmationDialog.setSecondaryButtonListener(v1 -> confirmationDialog.dismiss());
        confirmationDialog.showShareLink();
        confirmationDialog.show();
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

        confirmationDialog = new AWalletConfirmationDialog(this);
        confirmationDialog.setTitle(R.string.title_transaction_details);
        confirmationDialog.setSmallText(R.string.confirm_transfer_details);
        confirmationDialog.setMediumText(qty);
        confirmationDialog.setPrimaryButtonText(R.string.transfer_tickets);
        confirmationDialog.setSecondaryButtonText(R.string.dialog_cancel_back);
        confirmationDialog.setPrimaryButtonListener(v1 -> transferTicketFinal());
        confirmationDialog.setSecondaryButtonListener(v1 -> confirmationDialog.dismiss());
        confirmationDialog.show();
    }

    private void transferLinkFinal(String universalLink)
    {
        //create share intent
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, universalLink);
        sendIntent.setType("text/plain");
        startActivityForResult(sendIntent, SEND_INTENT_REQUEST_CODE);
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

