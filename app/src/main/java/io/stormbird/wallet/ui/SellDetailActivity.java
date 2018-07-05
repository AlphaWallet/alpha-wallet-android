package io.stormbird.wallet.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.FinishReceiver;
import io.stormbird.wallet.entity.Ticket;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.ui.widget.adapter.TicketAdapter;
import io.stormbird.wallet.util.KeyboardUtils;
import io.stormbird.wallet.viewmodel.SellDetailModel;
import io.stormbird.wallet.viewmodel.SellDetailModelFactory;
import io.stormbird.wallet.widget.AWalletConfirmationDialog;
import io.stormbird.token.entity.TicketRange;

import static io.stormbird.wallet.C.EXTRA_PRICE;
import static io.stormbird.wallet.C.EXTRA_STATE;
import static io.stormbird.wallet.C.EXTRA_TOKENID_LIST;
import static io.stormbird.wallet.C.Key.TICKET;
import static io.stormbird.wallet.C.Key.WALLET;
import static io.stormbird.wallet.C.PRUNE_ACTIVITY;

/**
 * Created by James on 21/02/2018.
 */

public class SellDetailActivity extends BaseActivity {
    private static final int SEND_INTENT_REQUEST_CODE = 2;
    public static final int SET_A_PRICE = 1;
    public static final int SET_EXPIRY = 2;
    public static final int SET_MARKET_SALE = 3;

    @Inject
    protected SellDetailModelFactory viewModelFactory;
    protected SellDetailModel viewModel;

    private FinishReceiver finishReceiver;

    private Ticket ticket;
    private TicketRange ticketRange;
    private TicketAdapter adapter;
    private String ticketIds;
    private String prunedIds;
    private double ethToUsd;
    private int saleStatus;
    private double sellPriceValue;

    private TextView usdPrice;
    private EditText sellPrice;
    private TextView textQuantity;
    private RecyclerView list;
    private TextView totalCostText;
    private EditText expiryDateEditText;
    private EditText expiryTimeEditText;
    private DatePickerDialog datePickerDialog;
    private TimePickerDialog timePickerDialog;
    private TextView priceErrorText;
    private TextView quantityErrorText;
    private TextView expiryDateErrorText;
    private TextView expiryTimeErrorText;
    private TextView titleSetPrice;
    private LinearLayout quantityLayout;
    private LinearLayout universalLinkDetailsLayout;
    private Button nextButton;
    private TextView confirmQuantityText;
    private TextView confirmPricePerTicketText;
    private TextView confirmTotalCostText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_price);
        toolbar();
        setTitle(getString(R.string.empty));

        ticket = getIntent().getParcelableExtra(TICKET);
        Wallet wallet = getIntent().getParcelableExtra(WALLET);
        ticketIds = getIntent().getStringExtra(EXTRA_TOKENID_LIST);
        saleStatus = getIntent().getIntExtra(EXTRA_STATE, 0);
        sellPriceValue = getIntent().getDoubleExtra(EXTRA_PRICE, 0.0);
        prunedIds = ticketIds;

        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(SellDetailModel.class);
        viewModel.setWallet(wallet);
        viewModel.pushToast().observe(this, this::displayToast);
        viewModel.ethereumPrice().observe(this, this::onEthereumPrice);
        viewModel.universalLinkReady().observe(this, this::linkReady);

        //we should import a token and a list of chosen ids
        list = findViewById(R.id.listTickets);
        adapter = new TicketAdapter(this::onTicketIdClick, ticket, ticketIds, viewModel.getAssetDefinitionService());
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        usdPrice = findViewById(R.id.fiat_price);
        sellPrice = findViewById(R.id.asking_price);
        totalCostText = findViewById(R.id.eth_price);
        textQuantity = findViewById(R.id.text_quantity);
        expiryDateEditText = findViewById(R.id.edit_expiry_date);
        expiryTimeEditText = findViewById(R.id.edit_expiry_time);
        priceErrorText = findViewById(R.id.error_price);
        quantityErrorText = findViewById(R.id.error_quantity);
        expiryDateErrorText = findViewById(R.id.error_date);
        expiryTimeErrorText = findViewById(R.id.error_time);
        quantityLayout = findViewById(R.id.layout_set_quantity);
        universalLinkDetailsLayout = findViewById(R.id.layout_universal_link_details);
        nextButton = findViewById(R.id.button_next);
        nextButton.setOnClickListener(v -> onNext());
        titleSetPrice = findViewById(R.id.title_set_price);
        confirmQuantityText = findViewById(R.id.text_confirm_quantity);
        confirmPricePerTicketText = findViewById(R.id.text_confirm_price_per_ticket);
        confirmTotalCostText = findViewById(R.id.text_confirm_total_cost);

        setupPage();

        finishReceiver = new FinishReceiver(this);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(finishReceiver);
    }

    private void setupPage()
    {
        switch (saleStatus)
        {
            case SET_A_PRICE:
                showQuantityLayout();
                break;
            case SET_EXPIRY:
                showUniversalLinkDetailsLayout();
                break;
            case SET_MARKET_SALE:
                showMarketSaleLayout();
                break;
        }
    }

    private void showMarketSaleLayout()
    {
        initQuantitySelector();
        sellPrice.setVisibility(View.VISIBLE);
        textQuantity.setVisibility(View.VISIBLE);
        expiryDateEditText.setVisibility(View.GONE);
        expiryTimeEditText.setVisibility(View.GONE);
        quantityLayout.setVisibility(View.VISIBLE);
        universalLinkDetailsLayout.setVisibility(View.GONE);
        titleSetPrice.setText(R.string.set_a_price);
        addSellPriceListener();
    }

    void showUniversalLinkDetailsLayout()
    {
        initDatePicker();
        initTimePicker();
        sellPrice.setVisibility(View.GONE);
        textQuantity.setVisibility(View.GONE);
        expiryDateEditText.setVisibility(View.VISIBLE);
        expiryTimeEditText.setVisibility(View.VISIBLE);
        universalLinkDetailsLayout.setVisibility(View.VISIBLE);
        quantityLayout.setVisibility(View.GONE);
        titleSetPrice.setText(R.string.set_universal_link_expiry);

        expiryDateEditText.setOnClickListener(v -> datePickerDialog.show());
        expiryTimeEditText.setOnClickListener(v -> timePickerDialog.show());

        int quantity = ticket.ticketIdStringToIndexList(prunedIds).size();
        String unit = quantity > 1 ? getString(R.string.tickets) : getString(R.string.ticket);
        String totalCostStr = getString(R.string.total_cost, getCleanValue(quantity * sellPriceValue));
        confirmQuantityText.setText(getString(R.string.tickets_selected, String.valueOf(quantity), unit));
        confirmPricePerTicketText.setText(getString(R.string.eth_per_ticket_w_value, getCleanValue(sellPriceValue)));
        confirmTotalCostText.setText(getString(R.string.confirm_sale_total, totalCostStr));
    }

    void showQuantityLayout()
    {
        initQuantitySelector();
        sellPrice.setVisibility(View.VISIBLE);
        textQuantity.setVisibility(View.VISIBLE);
        expiryDateEditText.setVisibility(View.GONE);
        expiryTimeEditText.setVisibility(View.GONE);
        quantityLayout.setVisibility(View.VISIBLE);
        universalLinkDetailsLayout.setVisibility(View.GONE);
        titleSetPrice.setText(R.string.set_a_price);
        addSellPriceListener();
    }

    private void addSellPriceListener()
    {
        sellPrice.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    int quantity = Integer.parseInt(textQuantity.getText().toString());
                    updateSellPrice(quantity);
                } catch (NumberFormatException e) {
                    //silent fail, just don't update
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void onNext()
    {
        switch (saleStatus)
        {
            case SET_A_PRICE:
                if (isPriceAndQuantityValid())
                {
                    viewModel.openUniversalLinkSetExpiry(this, prunedIds, sellPriceValue);
                }
                break;
            case SET_EXPIRY:
                if (isExpiryDateTimeValid())
                {
                    sellTicketLink();
                }
                break;
            case SET_MARKET_SALE:
                if (isPriceAndQuantityValid())
                {
                    confirmPlaceMarketOrderDialog();
                }
                break;
        }
    }

    private boolean isPriceAndQuantityValid() {
        boolean result = true;
        hideErrorMessages();

        if (Integer.parseInt(textQuantity.getText().toString()) <= 0) {
            quantityErrorText.setVisibility(View.VISIBLE);
            result = false;
        }
        if (sellPrice.getText().toString().isEmpty() || Double.parseDouble(sellPrice.getText().toString()) <= 0) {
            priceErrorText.setVisibility(View.VISIBLE);
            result = false;
        }
        else
        {
            sellPriceValue = Double.parseDouble(sellPrice.getText().toString());
        }
        if (!isValidAmount(sellPrice.getText().toString())) {
            priceErrorText.setVisibility(View.VISIBLE);
            result = false;
        }

        return result;
    }

    private boolean isExpiryDateTimeValid()
    {
        boolean result = true;
        hideErrorMessages();

        if (expiryDateEditText.getText().toString().isEmpty())
        {
            expiryDateErrorText.setVisibility(View.VISIBLE);
            result = false;
        }
        if (expiryTimeEditText.getText().toString().isEmpty())
        {
            expiryTimeErrorText.setVisibility(View.VISIBLE);
            result = false;
        }

        return result;
    }

    private void initQuantitySelector() {
        RelativeLayout plusButton = findViewById(R.id.layout_quantity_add);
        plusButton.setOnClickListener(v -> {
            int quantity = Integer.parseInt(textQuantity.getText().toString());
            if ((quantity + 1) <= adapter.getTicketRangeCount()) {
                quantity++;
                textQuantity.setText(String.valueOf(quantity));
                updateSellPrice(quantity);
                prunedIds = ticket.pruneIDList(ticketIds, quantity);
            }
        });

        RelativeLayout minusButton = findViewById(R.id.layout_quantity_minus);
        minusButton.setOnClickListener(v -> {
            int quantity = Integer.parseInt(textQuantity.getText().toString());
            if ((quantity - 1) >= 0) {
                quantity--;
                textQuantity.setText(String.valueOf(quantity));
                updateSellPrice(quantity);
                prunedIds = ticket.pruneIDList(ticketIds, quantity);
            }
        });

        textQuantity.setText("1");
        prunedIds = ticket.pruneIDList(ticketIds, 1);
    }

    private void initDatePicker() {
        String dateFormat = "dd/MM/yyyy";
        Calendar newCalendar = Calendar.getInstance();
        SimpleDateFormat dateFormatter = new SimpleDateFormat(dateFormat, Locale.ENGLISH);

        datePickerDialog = new DatePickerDialog(this, (view, year, monthOfYear, dayOfMonth) -> {
            Calendar newDate = Calendar.getInstance();
            newDate.set(year, monthOfYear, dayOfMonth);
            expiryDateEditText.setText(dateFormatter.format(newDate.getTime()));
        }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));

        //set default for tomorrow
        long tomorrowStamp = System.currentTimeMillis() + 1000*60*60*24;
        Date tomorrow = new Date(tomorrowStamp);
        expiryDateEditText.setText(dateFormatter.format(tomorrow.getTime()));
    }

    private void initTimePicker() {
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

    private void onEthereumPrice(Double aDouble) {
        ethToUsd = aDouble;
        //see if there's a non-zero value in the eth field
        updateUSDBalance();
    }

    private void updateUSDBalance() {
        try {
            int quantity = Integer.parseInt(textQuantity.getText().toString());
            double ethPrice = Double.parseDouble(sellPrice.getText().toString());
            if (quantity > 0 && ethPrice > 0) {
                double usdValue = ethPrice * ethToUsd * (double) quantity;
                DecimalFormat df = new DecimalFormat("#.##");
                df.setRoundingMode(RoundingMode.CEILING);
                String usdText = "$" + df.format(usdValue);
                usdPrice.setText(usdText);
            }
        } catch (NumberFormatException e) {

        }
    }

    private String getCleanValue(double value)
    {
        DecimalFormat df = new DecimalFormat("#.#####");
        df.setRoundingMode(RoundingMode.UP);
        return df.format(value);
    }

    private void updateSellPrice(int quantity) {
        if (!sellPrice.getText().toString().isEmpty()) {
            try {
                sellPriceValue = Double.parseDouble(sellPrice.getText().toString());
                totalCostText.setText(getString(R.string.total_cost, getCleanValue(quantity * sellPriceValue)));
                updateUSDBalance();
            } catch (NumberFormatException e) {
                //silent fail, just don't update
            }
        }
    }

    private void sellTicketLink() {
        String expiryDate = expiryDateEditText.getText().toString();
        String expiryTime = expiryTimeEditText.getText().toString();
        String tempDateString = expiryDate + " " + expiryTime;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        Date date;
        String dateString = "";
        long UTCTimeStamp = 0;
        try {
            date = simpleDateFormat.parse(tempDateString);
            dateString = simpleDateFormat.format(date);
            Log.d(SellDetailActivity.class.getSimpleName(), "date : " + dateString);
            UTCTimeStamp = (date.getTime())/1000;
        } catch (ParseException e) {
            Log.e(SellDetailActivity.class.getSimpleName(), e.getMessage(), e);
        }

        //1. validate price
        BigInteger price = getPriceInWei();
        //2. get quantity
        int quantity = ticket.getTicketIndicies(prunedIds).length;

        if (price.doubleValue() > 0.0 && prunedIds != null && quantity > 0) {
            //get the specific ID's, pick from the start of the run
            int[] prunedIndices = ticket.getTicketIndicies(prunedIds);
            BigInteger totalValue = price.multiply(BigInteger.valueOf(quantity)); //in wei
            viewModel.generateUniversalLink(prunedIndices, ticket.getAddress(), totalValue, UTCTimeStamp);
        }

        KeyboardUtils.hideKeyboard(getCurrentFocus());
        //go back to previous screen
    }

    private void sellTicketFinal() {
        if (sellPriceValue <= 0) return;
        //1. validate price
        BigInteger price = getPriceInWei();
        //2. get indicies
        int[] prunedIndices = ticket.getTicketIndicies(prunedIds);
        int quantity = Integer.parseInt(textQuantity.getText().toString());

        if (price.doubleValue() > 0.0 && prunedIndices != null && quantity > 0) {
            //get the specific ID's, pick from the start of the run
            List<BigInteger> ticketIdList = ticket.stringHexToBigIntegerList(ticketIds);
            BigInteger totalValue = price.multiply(BigInteger.valueOf(quantity)); //in wei
            viewModel.generateSalesOrders(ticket.getAddress(), totalValue, prunedIndices, ticketIdList.get(0));
            finish();
        }

        KeyboardUtils.hideKeyboard(getCurrentFocus());
        //go back to previous screen
    }

    private BigInteger getPriceInWei() {
        //now convert to microWei
        long microEth = (int)(sellPriceValue * 1000000.0);
        byte[] max = Numeric.hexStringToByteArray("FFFFFFFF");
        BigInteger maxValue = new BigInteger(1, max);
        if (microEth > maxValue.longValue()) microEth = 0; //check on UI screen if amount is more than we can handle
        //now convert to Wei
        BigInteger weiValue = Convert.toWei(Long.toString(microEth), Convert.Unit.SZABO).toBigInteger();
        return weiValue;
    }

    boolean isValidAmount(String eth) {
        try {
            return !getPriceInWei().equals(BigInteger.ZERO);
        } catch (Exception e) {
            return false;
        }
    }

    private void linkReady(String universalLink) {
        //how many tickets are we selling?
        int quantity = ticket.ticketIdStringToIndexList(prunedIds).size();
        String unit = quantity > 1 ? getString(R.string.tickets) : getString(R.string.ticket);
        String totalCostStr = getString(R.string.total_cost, getCleanValue(quantity * sellPriceValue));

        String qty = String.valueOf(quantity) + " " + unit + "\n" +
                String.valueOf(getCleanValue(sellPriceValue)) + " " + getResources().getString(R.string.eth_per_ticket) + "\n" +
                getString(R.string.confirm_sale_total, totalCostStr) + "\n\n" +
                getString(R.string.universal_link_expiry_on) + expiryDateEditText.getText().toString() + " " + expiryTimeEditText.getText().toString();

        AWalletConfirmationDialog dialog = new AWalletConfirmationDialog(this);
        dialog.setTitle(R.string.confirm_sale_title);
        dialog.setSmallText(R.string.generate_sale_transfer_link);
        dialog.setMediumText(qty);
        dialog.setPrimaryButtonText(R.string.send_universal_link);
        dialog.setSecondaryButtonText(R.string.dialog_cancel_back);
        dialog.setPrimaryButtonListener(v1 -> sellLinkFinal(universalLink));
        dialog.setSecondaryButtonListener(v1 -> dialog.dismiss());
        dialog.showShareLink();
        dialog.show();
    }

    private void confirmPlaceMarketOrderDialog()
    {
        //how many tickets are we selling?
        int quantity = ticket.ticketIdStringToIndexList(prunedIds).size();
        String unit = quantity > 1 ? getString(R.string.tickets) : getString(R.string.ticket);
        String qty = String.valueOf(quantity) + " " + unit + " @" + getCleanValue(sellPriceValue) + getString(R.string.eth_per_ticket);

        AWalletConfirmationDialog dialog = new AWalletConfirmationDialog(this);
        dialog.setTitle(R.string.confirm_sale_title);
        dialog.setSmallText(R.string.place_tickets_marketplace);
        dialog.setMediumText(qty);
        dialog.setPrimaryButtonText(R.string.create_sell_order);
        dialog.setSecondaryButtonText(R.string.dialog_cancel_back);
        dialog.setPrimaryButtonListener(v1 -> {
            sellTicketFinal();
            sendBroadcast(new Intent(PRUNE_ACTIVITY));
        });
        dialog.setSecondaryButtonListener(v1 -> dialog.dismiss());
        dialog.show();
    }

    private void sellLinkFinal(String universalLink) {
        //create share intent
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, universalLink);
        sendIntent.setType("text/plain");
        startActivityForResult(sendIntent, SEND_INTENT_REQUEST_CODE);
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

    private void hideErrorMessages() {
        expiryDateErrorText.setVisibility(View.GONE);
        expiryTimeErrorText.setVisibility(View.GONE);
        priceErrorText.setVisibility(View.GONE);
        quantityErrorText.setVisibility(View.GONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case SEND_INTENT_REQUEST_CODE:
                sendBroadcast(new Intent(PRUNE_ACTIVITY));
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
