package io.awallet.crypto.alphawallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import io.awallet.crypto.alphawallet.R;
import io.awallet.crypto.alphawallet.entity.MagicLinkParcel;
import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.ui.widget.adapter.ERC875MarketAdapter;
import io.awallet.crypto.alphawallet.ui.widget.entity.TicketRange;
import io.awallet.crypto.alphawallet.util.KeyboardUtils;
import io.awallet.crypto.alphawallet.viewmodel.PurchaseTicketsViewModel;
import io.awallet.crypto.alphawallet.viewmodel.PurchaseTicketsViewModelFactory;
import io.awallet.crypto.alphawallet.widget.AWalletConfirmationDialog;
import io.awallet.crypto.alphawallet.widget.ProgressView;
import io.awallet.crypto.alphawallet.widget.SystemView;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.stormbird.token.entity.MagicLinkData;

import static io.awallet.crypto.alphawallet.C.Key.WALLET;
import static io.awallet.crypto.alphawallet.C.MARKET_INSTANCE;

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

    private MagicLinkData ticketRange;
    private ERC875MarketAdapter adapter;
    private TextView ethPrice;
    private TextView usdPrice;
    private Button purchase;

//    private TextInputLayout amountInputLayout;

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
        MagicLinkData[] singleInstance = new MagicLinkData[1];
        singleInstance[0] = ticketRange;
        adapter = new ERC875MarketAdapter(this::onOrderClick, singleInstance);

        double pricePerTicket = ticketRange.price;
        int ticketCount = ticketRange.ticketCount;

        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        //calculate total price
        double totalEthPrice = round(ticketRange.price * ticketCount, 2);
        String priceStr = String.valueOf(totalEthPrice) + " ETH";
        ethPrice.setText(priceStr);

        //TODO: Remove Ghastly hack and get latest ETH/USD price
        double totalUsdPrice = round(totalEthPrice * 1000.0, 2);
        String totalUsdStr = "$" + String.valueOf(totalUsdPrice);
        usdPrice.setText(totalUsdStr);

        toolbar();

        setTitle("");

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
            AWalletConfirmationDialog dialog = new AWalletConfirmationDialog(this);
            dialog.setTitle(R.string.confirm_purchase_title);
            dialog.setSmallText(R.string.confirm_purchase_small_text);
            dialog.setBigText(ethPrice.getText().toString());
            dialog.setPrimaryButtonText(R.string.confirm_purchase_button_text);
            dialog.setSecondaryButtonText(R.string.dialog_cancel_back);
            dialog.setPrimaryButtonListener(v1 -> purchaseTicketsFinal());
            dialog.setSecondaryButtonListener(v1 -> dialog.dismiss());
            dialog.show();
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

    private void onOrderClick(View view, MagicLinkData instance)
    {
        //do nothing
    }

    private void purchaseTicketsFinal()
    {
        MagicLinkParcel parcel = new MagicLinkParcel(ticketRange);
        viewModel.buyRange(parcel);
        KeyboardUtils.hideKeyboard(getCurrentFocus());
    }

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

