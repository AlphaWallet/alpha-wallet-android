package com.alphawallet.app.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.FinishReceiver;
import com.alphawallet.app.entity.PinAuthenticationCallbackInterface;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.ui.widget.TokensAdapterCallback;
import com.alphawallet.app.ui.widget.adapter.NonFungibleTokenAdapter;
import com.alphawallet.app.util.KeyboardUtils;
import com.alphawallet.app.viewmodel.SellDetailViewModel;
import com.alphawallet.app.widget.AWalletConfirmationDialog;
import com.alphawallet.app.widget.SignTransactionDialog;
import com.alphawallet.ethereum.EthereumNetworkBase;

import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.MissingFormatArgumentException;

import timber.log.Timber;

import static com.alphawallet.app.C.EXTRA_PRICE;
import static com.alphawallet.app.C.EXTRA_STATE;
import static com.alphawallet.app.C.EXTRA_TOKENID_LIST;
import static com.alphawallet.app.C.Key.WALLET;
import static com.alphawallet.app.C.PRUNE_ACTIVITY;
import static com.alphawallet.app.entity.Operation.SIGN_DATA;
import static com.alphawallet.token.tools.Convert.getEthString;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Created by James on 21/02/2018.
 */
@AndroidEntryPoint
public class SellDetailActivity extends BaseActivity implements TokensAdapterCallback, Runnable, SignAuthenticationCallback
{
    private static final int SEND_INTENT_REQUEST_CODE = 2;
    public static final int SET_A_PRICE = 1;
    public static final int SET_EXPIRY = 2;
    public static final int SET_MARKET_SALE = 3;

    protected SellDetailViewModel viewModel;

    private FinishReceiver finishReceiver;

    private Token token;
    private Wallet wallet;
    private NonFungibleTokenAdapter adapter;
    private String ticketIds;
    private List<BigInteger> selection;
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
    private TextView currencyText;
    private boolean activeClick;
    private Handler handler;
    private PinAuthenticationCallbackInterface authInterface;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this)
                .get(SellDetailViewModel.class);
        setContentView(R.layout.activity_set_price);
        toolbar();
        setTitle(getString(R.string.empty));

        long chainId = getIntent().getLongExtra(C.EXTRA_CHAIN_ID, EthereumNetworkBase.MAINNET_ID);
        token = viewModel.getTokensService().getToken(chainId, getIntent().getStringExtra(C.EXTRA_ADDRESS));
        wallet = getIntent().getParcelableExtra(WALLET);
        ticketIds = getIntent().getStringExtra(EXTRA_TOKENID_LIST);
        saleStatus = getIntent().getIntExtra(EXTRA_STATE, 0);
        sellPriceValue = getIntent().getDoubleExtra(EXTRA_PRICE, 0.0);
        selection = token.stringHexToBigIntegerList(ticketIds);

        viewModel.prepare(token, wallet);
        viewModel.pushToast().observe(this, this::displayToast);
        viewModel.ethereumPrice().observe(this, this::onEthereumPrice);
        viewModel.universalLinkReady().observe(this, this::linkReady);
        viewModel.defaultWallet().observe(this, this::setupPage);

        //we should import a token and a list of chosen ids
        list = findViewById(R.id.listTickets);
        adapter = new NonFungibleTokenAdapter(this, token, selection, viewModel.getAssetDefinitionService());
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
        currencyText = findViewById(R.id.text_currency);

        finishReceiver = new FinishReceiver(this);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (finishReceiver != null)
        {
            finishReceiver.unregister();
        }
    }

    private void setupPage(Wallet wallet)
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

        String currencySymbol = viewModel.getNetwork().symbol;

        int quantity = selection.size();
        String unit = quantity > 1 ? getString(R.string.tickets) : getString(R.string.ticket);
        String totalCostStr = getString(R.string.total_cost, getEthString(quantity * sellPriceValue), currencySymbol);
        confirmQuantityText.setText(getString(R.string.tickets_selected, String.valueOf(quantity), unit));
        confirmPricePerTicketText.setText(getString(R.string.eth_per_ticket_w_value, getEthString(sellPriceValue), currencySymbol));
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
        currencyText.setText(viewModel.getNetwork().symbol);
        totalCostText.setText(getString(R.string.currency_00, viewModel.getNetwork().symbol));
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
        if (activeClick) return;
        activeClick = true;
        handler.postDelayed(this, 500);

        switch (saleStatus)
        {
            case SET_A_PRICE:
                if (isPriceAndQuantityValid())
                {
                    viewModel.openUniversalLinkSetExpiry(this, selection, sellPriceValue);
                }
                break;
            case SET_EXPIRY:
                if (isExpiryDateTimeValid())
                {
                    viewModel.getAuthorisation(this, this);
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

    private void initQuantitySelector()
    {
        RelativeLayout plusButton = findViewById(R.id.layout_quantity_add);
        plusButton.setOnClickListener(v -> {
            int quantity = Integer.parseInt(textQuantity.getText().toString());
            if ((quantity + 1) <= adapter.getTicketRangeCount()) {
                quantity++;
                textQuantity.setText(String.valueOf(quantity));
                updateSellPrice(quantity);
                selection = token.pruneIDList(ticketIds, quantity);
            }
        });

        RelativeLayout minusButton = findViewById(R.id.layout_quantity_minus);
        minusButton.setOnClickListener(v -> {
            int quantity = Integer.parseInt(textQuantity.getText().toString());
            if ((quantity - 1) > 0) {
                quantity--;
                textQuantity.setText(String.valueOf(quantity));
                updateSellPrice(quantity);
                selection = token.pruneIDList(ticketIds, quantity);
            }
        });

        textQuantity.setText("1");
        selection = token.pruneIDList(ticketIds, 1);
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
        long tomorrowStamp = System.currentTimeMillis() + 1 * DateUtils.DAY_IN_MILLIS;
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
                String fiatText = TickerService.getCurrencyString(ethPrice * ethToUsd * (double) quantity);
                usdPrice.setText(fiatText);
            }
        } catch (NumberFormatException e) {

        }
    }

    private void updateSellPrice(int quantity)
    {
        if (!sellPrice.getText().toString().isEmpty()) {
            try {
                sellPriceValue = Double.parseDouble(sellPrice.getText().toString());
                totalCostText.setText(getString(R.string.total_cost, getEthString(quantity * sellPriceValue), viewModel.getSymbol()));
                updateUSDBalance();
            } catch (NumberFormatException|MissingFormatArgumentException e) {
                //silent fail, just don't update
            }
        }
    }

    private void sellTicketLinkFinal() {
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
            Timber.d("date : %s", dateString);
            UTCTimeStamp = (date.getTime())/1000;
        } catch (ParseException e) {
            Timber.e(e);;
        }

        //1. validate price
        BigInteger price = getPriceInWei();
        //2. get quantity
        int quantity = selection.size();

        if (price.doubleValue() > 0.0 && selection != null && quantity > 0) {
            //get the specific ID's, pick from the start of the run
            BigInteger totalValue = price.multiply(BigInteger.valueOf(quantity)); //in wei
            viewModel.generateUniversalLink(token.getTransferListFormat(selection), token.getAddress(), totalValue, UTCTimeStamp);
        }

        KeyboardUtils.hideKeyboard(getCurrentFocus());
    }

    private BigInteger getPriceInWei() {
        //now convert to microWei
        long microEth = (int)(sellPriceValue * 1000000.0);
        byte[] max = Numeric.hexStringToByteArray("FFFFFFFF");
        BigInteger maxValue = new BigInteger(1, max);
        if (microEth > maxValue.longValue()) microEth = 0; //check on UI screen if amount is more than we can handle
        //now convert to Wei
        return Convert.toWei(Long.toString(microEth), Convert.Unit.SZABO).toBigInteger();
    }

    boolean isValidAmount(String eth) {
        try {
            return !getPriceInWei().equals(BigInteger.ZERO);
        } catch (Exception e) {
            return false;
        }
    }

    private void linkReady(String universalLink) {
        //how many indices are we selling?
        String currencySymbol = viewModel.getNetwork().symbol;
        int quantity = selection.size();
        String unit = quantity > 1 ? getString(R.string.tickets) : getString(R.string.ticket);
        String totalCostStr = getString(R.string.total_cost, getEthString(quantity * sellPriceValue), currencySymbol);

        String qty = quantity + " " + unit + "\n" +
                getEthString(sellPriceValue) + " " + getString(R.string.eth_per_ticket, currencySymbol) + "\n" +
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
        dialog.show();
    }

    ActivityResultLauncher<Intent> sellLinkFinalResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                sendBroadcastToPrune(); //TODO: implement prune via result codes
            });

    private void sellLinkFinal(String universalLink) {
        //create share intent
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, universalLink);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Magic Link");
        sendIntent.setType("text/plain");
        sellLinkFinalResult.launch(Intent.createChooser(sendIntent, "Share via"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler = new Handler();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        viewModel.resetSignDialog();
    }

    @Override
    public void onTokenClick(View view, Token token, List<BigInteger> ids, boolean selected) {
        Context context = view.getContext();
        //TODO: what action should be performed when clicking on a range?
    }

    @Override
    public void onLongTokenClick(View view, Token token, List<BigInteger> tokenId)
    {

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
        if (requestCode >= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS
                && requestCode <= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS + 10)
        {
            requestCode = SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS;
        }

        switch (requestCode)
        {
            case SEND_INTENT_REQUEST_CODE:
                sendBroadcastToPrune();
                break;

            case SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS:
                gotAuthorisation(resultCode == RESULT_OK);
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void sendBroadcastToPrune()
    {
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(PRUNE_ACTIVITY));
    }

    @Override
    public void run()
    {
        activeClick = false;
    }

    @Override
    public void gotAuthorisation(boolean gotAuth)
    {
        if (gotAuth)
        {
            viewModel.completeAuthentication(SIGN_DATA);
            sellTicketLinkFinal();
        }
        else viewModel.failedAuthentication(SIGN_DATA);
    }

    @Override
    public void cancelAuthentication()
    {

    }
}
