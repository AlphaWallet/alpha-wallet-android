package com.wallet.crypto.alphawallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.wallet.crypto.alphawallet.R;
import com.wallet.crypto.alphawallet.entity.SalesOrder;
import com.wallet.crypto.alphawallet.entity.Ticket;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.ui.widget.adapter.ERC875MarketAdapter;
import com.wallet.crypto.alphawallet.ui.widget.adapter.TicketAdapter;
import com.wallet.crypto.alphawallet.ui.widget.entity.TicketRange;
import com.wallet.crypto.alphawallet.util.KeyboardUtils;
import com.wallet.crypto.alphawallet.viewmodel.PurchaseTicketsViewModel;
import com.wallet.crypto.alphawallet.viewmodel.PurchaseTicketsViewModelFactory;
import com.wallet.crypto.alphawallet.widget.ProgressView;
import com.wallet.crypto.alphawallet.widget.SystemView;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.wallet.crypto.alphawallet.C.Key.TICKET;
import static com.wallet.crypto.alphawallet.C.Key.WALLET;
import static com.wallet.crypto.alphawallet.C.MARKET_INSTANCE;

/**
 * Created by James on 23/02/2018.
 */
public class PurchaseTicketsActivity extends BaseActivity
{
    AlertDialog dialog;

    @Inject
    protected PurchaseTicketsViewModelFactory viewModelFactory;
    protected PurchaseTicketsViewModel viewModel;
    private SystemView systemView;
    private ProgressView progressView;

    private SalesOrder ticketRange;
    private ERC875MarketAdapter adapter;
    private TextView ethPrice;
    private TextView usdPrice;
    private Button purchase;

    private TextInputLayout amountInputLayout;

    private EditText purchaseQuantity;
    private String ticketIds;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        Wallet wallet = getIntent().getParcelableExtra(WALLET);
        ticketRange = getIntent().getParcelableExtra(MARKET_INSTANCE);

        setContentView(R.layout.activity_purchase_ticket); //use token just provides a simple list view.

        //we should import a token and a list of chosen ids
        purchase = findViewById(R.id.button_purchase);
        ethPrice = findViewById(R.id.eth_price);
        usdPrice = findViewById(R.id.fiat_price);

        RecyclerView list = findViewById(R.id.listTickets);
        SalesOrder[] singleInstance = new SalesOrder[1];
        singleInstance[0] = ticketRange;
        adapter = new ERC875MarketAdapter(this::onOrderClick, singleInstance);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        //calculate total price
        double totalEthPrice = round(ticketRange.price * ticketRange.tickets.length, 2);
        String priceStr = String.valueOf(totalEthPrice) + " ETH";
        ethPrice.setText(priceStr);

        //TODO: Remove Ghastly hack and get latest ETH/USD price
        double totalUsdPrice = round(totalEthPrice * 1000.0, 2);
        String totalUsdStr = "$" + String.valueOf(totalUsdPrice);
        usdPrice.setText(totalUsdStr);

        toolbar();

        setTitle(getString(R.string.title_market_purchase));
        amountInputLayout = findViewById(R.id.symbol_input_layout);

        systemView = findViewById(R.id.system_view);
        systemView.hide();

        progressView = findViewById(R.id.progress_view);
        progressView.hide();

        ethPrice = findViewById(R.id.eth_price);
        usdPrice = findViewById(R.id.fiat_price);

        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(PurchaseTicketsViewModel.class);

        viewModel.setWallet(wallet);

        viewModel.progress().observe(this, systemView::showProgress);
        viewModel.queueProgress().observe(this, progressView::updateProgress);
        viewModel.pushToast().observe(this, this::displayToast);
        viewModel.sendTransaction().observe(this, this::onTransaction);
        viewModel.progress().observe(this, this::onProgress);

        purchase.setOnClickListener((View v) -> {
            purchaseTicketsFinal();
        });
    }

    private void onProgress(boolean shouldShowProgress) {
        hideDialog();
        if (shouldShowProgress) {
            dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.title_dialog_sending)
                    .setView(new ProgressBar(this))
                    .setCancelable(false)
                    .create();
            dialog.show();
        }
    }

    private void hideDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private void onTransaction(String hash) {
        hideDialog();
        dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.transaction_succeeded)
                .setMessage(hash)
                .setPositiveButton(R.string.button_ok, (dialog1, id) -> {
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

    private void onOrderClick(View view, SalesOrder instance)
    {
        //do nothing
    }

    private void purchaseTicketsFinal()
    {
        viewModel.buyRange(ticketRange);
//        if (!isValidAmount(sellPrice.getText().toString())) {
//            toInputLayout.setError(getString(R.string.error_invalid_address));
//            return;
//        }
//'
//        //1. validate price
//        BigInteger price = getPriceInWei();
//        //2. get indicies
//        short[] indicies = ticket.getTicketIndicies(ticketIds);
//
//        //TODO: use the count value from the 'count' EditText - see the invision UX plan
//
//        if (price.doubleValue() > 0.0 && indicies != null)
//        {
//            List<Integer> ticketIdList = ticket.parseIDListInteger(ticketIds);
//            BigInteger totalValue = price.multiply(BigInteger.valueOf(ticketIdList.size()));
//            viewModel.generateMarketOrders(ticket.getAddress(), totalValue, indicies, ticketIdList.get(0));
//            finish();
//        }

        KeyboardUtils.hideKeyboard(getCurrentFocus());
        //go back to previous screen
    }

//    private BigInteger getPriceInWei()
//    {
//        String textPrice = sellPrice.getText().toString();
//
//        //convert to a double value
//        double value = Double.valueOf(textPrice);
//
//        //now convert to milliWei
//        int milliEth = (int)(value*1000.0f);
//
//        //now convert to ETH
//        BigInteger weiValue = Convert.toWei(String.valueOf(milliEth), Convert.Unit.FINNEY).toBigInteger();
//
//        return weiValue;
//    }

    private double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.prepare();
    }

    private void onTicketIdClick(View view, TicketRange range) {
        Context context = view.getContext();
        //TODO: what action should be performed when clicking on a range?
    }
}

