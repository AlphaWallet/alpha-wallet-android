package io.awallet.crypto.alphawallet.ui;

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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.awallet.crypto.alphawallet.R;
import io.awallet.crypto.alphawallet.entity.Ticket;
import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.ui.widget.adapter.TicketAdapter;
import io.awallet.crypto.alphawallet.ui.widget.entity.TicketRange;
import io.awallet.crypto.alphawallet.util.BalanceUtils;
import io.awallet.crypto.alphawallet.util.KeyboardUtils;
import io.awallet.crypto.alphawallet.viewmodel.SellDetailModel;
import io.awallet.crypto.alphawallet.viewmodel.SellDetailModelFactory;
import io.awallet.crypto.alphawallet.widget.AWalletConfirmationDialog;

import static io.awallet.crypto.alphawallet.C.EXTRA_PRICE;
import static io.awallet.crypto.alphawallet.C.EXTRA_TOKENID_LIST;
import static io.awallet.crypto.alphawallet.C.Key.TICKET;
import static io.awallet.crypto.alphawallet.C.Key.WALLET;
import static io.awallet.crypto.alphawallet.C.MARKET_INSTANCE;
import static io.awallet.crypto.alphawallet.C.MARKET_SALE;

/**
 * Created by James on 21/02/2018.
 */

public class SellDetailActivity extends BaseActivity {
    @Inject
    protected SellDetailModelFactory viewModelFactory;
    protected SellDetailModel viewModel;

    private Ticket ticket;
    private TicketRange ticketRange;
    private TicketAdapter adapter;
    private TextView usdPrice;
    private Button sellButton;

    private EditText sellPrice;
    private TextView textQuantity;
    private String ticketIds;
    private double ethToUsd;

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

    private boolean marketSale;

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
        marketSale = getIntent().getStringExtra(MARKET_INSTANCE).equals(MARKET_SALE);

        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(SellDetailModel.class);
        viewModel.setWallet(wallet);
        viewModel.pushToast().observe(this, this::displayToast);
        viewModel.ethereumPrice().observe(this, this::onEthereumPrice);
        viewModel.universalLinkReady().observe(this, this::linkReady);

        //we should import a token and a list of chosen ids
        list = findViewById(R.id.listTickets);
        adapter = new TicketAdapter(this::onTicketIdClick, ticket, ticketIds);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        sellButton = findViewById(R.id.button_sell);
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

        if (marketSale)
        {
            sellPrice.setVisibility(View.VISIBLE);
            textQuantity.setVisibility(View.VISIBLE);
            expiryDateEditText.setVisibility(View.GONE);
            expiryTimeEditText.setVisibility(View.GONE);
        }
        else
        {
            sellPrice.setVisibility(View.VISIBLE);
            textQuantity.setVisibility(View.VISIBLE);
            expiryDateEditText.setVisibility(View.VISIBLE);
            expiryTimeEditText.setVisibility(View.VISIBLE);
            sellButton.setText(getResources().getString(R.string.generate_sale_transfer_link));
            TextView subText = findViewById(R.id.text_eth_subtext);
            subText.setText(R.string.set_price_subtext_abr_magic);
        }

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

        initQuantitySelector();
        initDatePicker();
        initTimePicker();

        sellButton.setOnClickListener(v -> {
            if (isInputValid()) {
                if (marketSale)
                {
                    confirmPlaceMarketOrderDialog();
                }
                else
                {
                    sellTicketLink();
                }
            }
        });

        expiryDateEditText.setOnClickListener(v -> datePickerDialog.show());
        expiryTimeEditText.setOnClickListener(v -> timePickerDialog.show());
    }

    private boolean isInputValid() {
        boolean result = true;
        hideErrorMessages();

        if (Integer.parseInt(textQuantity.getText().toString()) <= 0) {
            quantityErrorText.setVisibility(View.VISIBLE);
            result = false;
        }
        if (sellPrice.getText().toString().isEmpty()) {
            priceErrorText.setVisibility(View.VISIBLE);
            result = false;
        }
        if (!sellPrice.getText().toString().isEmpty() && Double.parseDouble(sellPrice.getText().toString()) <= 0) {
            priceErrorText.setVisibility(View.VISIBLE);
            result = false;
        }
        if (!isValidAmount(sellPrice.getText().toString())) {
            priceErrorText.setVisibility(View.VISIBLE);
            result = false;
        }

        if (!marketSale)
        {
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
            }
        });

        RelativeLayout minusButton = findViewById(R.id.layout_quantity_minus);
        minusButton.setOnClickListener(v -> {
            int quantity = Integer.parseInt(textQuantity.getText().toString());
            if ((quantity - 1) >= 0) {
                quantity--;
                textQuantity.setText(String.valueOf(quantity));
                updateSellPrice(quantity);
            }
        });

        textQuantity.setText("1");
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.menu_share, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.action_share: {
//                sellTicketLink();
//            }
//            break;
//
//        }
        return super.onOptionsItemSelected(item);
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

    private void updateSellPrice(int quantity) {
        if (!sellPrice.getText().toString().isEmpty()) {
            try {
                double totalCost = quantity * Double.parseDouble(sellPrice.getText().toString());
                totalCostText.setText(getString(R.string.total_cost, String.valueOf(totalCost)));
                updateUSDBalance();
            } catch (NumberFormatException e) {
                //silent fail, just don't update
            }
        }
    }

//    private void sellTicketLink()
//    {
//        String textPrice = sellPrice.getText().toString();
//        double pricePerTicket = Double.valueOf(textPrice);
//        //need currently selected price and quantity
//        int[] indices = ticket.getTicketIndicies(ticketIds);
//        int quantity = Integer.parseInt(textQuantity.getText().toString());
//
//        if (pricePerTicket > 0.0 && indices != null && quantity > 0)
//        {
//            //get the specific ID's, pick from the start of the run
//            int[] prunedIndices = Arrays.copyOfRange(indices, 0, quantity);
//            String prunedIndicesStr = ticket.arrayToString(prunedIndices);
//        }
//    }

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
        //2. get indicies
        int[] indices = ticket.getTicketIndicies(ticketIds);
        int quantity = Integer.parseInt(textQuantity.getText().toString());

        if (price.doubleValue() > 0.0 && indices != null && quantity > 0) {
            //get the specific ID's, pick from the start of the run
            int[] prunedIndices = Arrays.copyOfRange(indices, 0, quantity);
            List<Integer> ticketIdList = ticket.parseIDListInteger(ticketIds);
            BigInteger totalValue = price.multiply(BigInteger.valueOf(quantity));
            viewModel.generateUniversalLink(prunedIndices, ticket.getAddress(), price, UTCTimeStamp);
        }

        KeyboardUtils.hideKeyboard(getCurrentFocus());
        //go back to previous screen
    }

    private void sellTicketFinal() {
        if (!isValidAmount(sellPrice.getText().toString())) {
            return;
        }

        //1. validate price
        BigInteger price = getPriceInWei();
        //2. get indicies
        int[] indices = ticket.getTicketIndicies(ticketIds);
        int quantity = Integer.parseInt(textQuantity.getText().toString());

        if (price.doubleValue() > 0.0 && indices != null && quantity > 0) {
            //get the specific ID's, pick from the start of the run
            int[] prunedIndices = Arrays.copyOfRange(indices, 0, quantity);
            List<Integer> ticketIdList = ticket.parseIDListInteger(ticketIds);
            BigInteger totalValue = price.multiply(BigInteger.valueOf(quantity));
            viewModel.generateSalesOrders(ticket.getAddress(), totalValue, prunedIndices, ticketIdList.get(0));
            finish();
        }

        KeyboardUtils.hideKeyboard(getCurrentFocus());
        //go back to previous screen
    }

    private BigInteger getPriceInWei() {
        String textPrice = sellPrice.getText().toString();

        //convert to a double value
        double value = Double.valueOf(textPrice);

        //now convert to milliWei
        int milliEth = (int) (value * 1000.0f);

        //now convert to ETH
        BigInteger weiValue = Convert.toWei(String.valueOf(milliEth), Convert.Unit.FINNEY).toBigInteger();

        return weiValue;
    }

    private double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    boolean isValidAmount(String eth) {
        try {
            String wei = BalanceUtils.EthToWei(eth);
            return wei != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void linkReady(String universalLink) {
        //how many tickets are we selling?
        String textPrice = sellPrice.getText().toString();
        TextView textQuantity = findViewById(R.id.text_quantity);
        int ticketName = (Integer.valueOf(textQuantity.getText().toString()) > 1) ? R.string.tickets : R.string.ticket;
        String qty = textQuantity.getText().toString() + " " + getResources().getString(ticketName) + " @" + textPrice + " Eth/Ticket";

        AWalletConfirmationDialog dialog = new AWalletConfirmationDialog(this);
        dialog.setTitle(R.string.confirm_sale_title);
        dialog.setSmallText(R.string.generate_sale_transfer_link);
        dialog.setBigText(qty);
        dialog.setPrimaryButtonText(R.string.send_universal_sale_link);
        dialog.setSecondaryButtonText(R.string.dialog_cancel_back);
        dialog.setPrimaryButtonListener(v1 -> sellLinkFinal(universalLink));
        dialog.setSecondaryButtonListener(v1 -> dialog.dismiss());
        dialog.show();
    }

    private void confirmPlaceMarketOrderDialog() {
        //how many tickets are we selling?
        String textPrice = sellPrice.getText().toString();
        TextView textQuantity = findViewById(R.id.text_quantity);
        int ticketName = (Integer.valueOf(textQuantity.getText().toString()) > 1) ? R.string.tickets : R.string.ticket;
        String qty = textQuantity.getText().toString() + " " + getResources().getString(ticketName) + " @" + textPrice + " Eth/Ticket";

        AWalletConfirmationDialog dialog = new AWalletConfirmationDialog(this);
        dialog.setTitle(R.string.confirm_sale_title);
        dialog.setSmallText(R.string.place_tickets_marketplace);
        dialog.setBigText(qty);
        dialog.setPrimaryButtonText(R.string.market_queue_title);
        dialog.setSecondaryButtonText(R.string.dialog_cancel_back);
        dialog.setPrimaryButtonListener(v1 -> sellTicketFinal());
        dialog.setSecondaryButtonListener(v1 -> dialog.dismiss());
        dialog.show();
    }

    private void sellLinkFinal(String universalLink) {
        //create share intent
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, universalLink);
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
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
}



