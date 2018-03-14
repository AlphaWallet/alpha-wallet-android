package com.wallet.crypto.alphawallet.ui;

import android.app.Dialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.wallet.crypto.alphawallet.R;
import com.wallet.crypto.alphawallet.entity.ErrorEnvelope;
import com.wallet.crypto.alphawallet.entity.SalesOrder;
import com.wallet.crypto.alphawallet.entity.Ticket;
import com.wallet.crypto.alphawallet.entity.TicketDecode;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.router.HomeRouter;
import com.wallet.crypto.alphawallet.ui.widget.entity.TicketRange;
import com.wallet.crypto.alphawallet.viewmodel.ImportTokenViewModel;
import com.wallet.crypto.alphawallet.viewmodel.ImportTokenViewModelFactory;
import com.wallet.crypto.alphawallet.widget.DepositView;
import com.wallet.crypto.alphawallet.widget.SystemView;

import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Locale;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.wallet.crypto.alphawallet.C.IMPORT_STRING;
import static com.wallet.crypto.alphawallet.C.Key.WALLET;

/**
 * Created by James on 9/03/2018.
 */

public class ImportTokenActivity extends BaseActivity implements View.OnClickListener {

    @Inject
    protected ImportTokenViewModelFactory importTokenViewModelFactory;
    private ImportTokenViewModel viewModel;
    private SystemView systemView;

    private TicketRange ticketRange;
    private String importString;
    private Dialog dialog;

    private TextView priceETH;
    private TextView priceUSD;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_import_token);
        toolbar();

        setTitle(getString(R.string.empty));

        importString = getIntent().getStringExtra(IMPORT_STRING);
        systemView = findViewById(R.id.system_view);
        priceETH = findViewById(R.id.textImportPrice);
        priceUSD = findViewById(R.id.textImportPriceUSD);
        priceETH.setVisibility(View.GONE);
        priceUSD.setVisibility(View.GONE);

        setTicket(false, true, false);

        Button importTickets = findViewById(R.id.import_ticket);
        importTickets.setOnClickListener(this);
        importTickets.setAlpha(0.4f);
        Button cancel = findViewById(R.id.cancel_button);
        cancel.setOnClickListener(this);

        viewModel = ViewModelProviders.of(this, importTokenViewModelFactory)
                .get(ImportTokenViewModel.class);

        viewModel.importRange().observe(this, this::onImportRange);
        viewModel.invalidRange().observe(this, this::invalidTicket);
        viewModel.newTransaction().observe(this, this::onTransaction);
        viewModel.error().observe(this, this::onError);
        viewModel.invalidLink().observe(this, this::onBadLink);

        ticketRange = null;
    }

    private void onBadLink(Boolean aBoolean)
    {
        TextView importTxt = findViewById(R.id.textImport);
        importTxt.setText("Invalid Ticket");
        setTicket(false, false, true);
        //bad link
        hideDialog();
        dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.bad_import_link)
                .setMessage(getString(R.string.bad_import_link_body))
                .setNegativeButton(R.string.cancel, (dialog1, id) -> {

                })
                .create();
        dialog.show();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private void setTicket(boolean ticket, boolean progress, boolean invalid)
    {
        LinearLayout progress_ticket = findViewById(R.id.layout_select_overlay);
        LinearLayout valid_ticket = findViewById(R.id.layout_select);
        LinearLayout invalid_ticket = findViewById(R.id.layout_select_invalid);
        if (ticket) {
            valid_ticket.setVisibility(View.VISIBLE);
        }
        else
        {
            valid_ticket.setVisibility(View.GONE);
        }

        if (progress) {
            progress_ticket.setVisibility(View.VISIBLE);
        }
        else
        {
            progress_ticket.setVisibility(View.GONE);
        }

        if (invalid) {
            invalid_ticket.setVisibility(View.VISIBLE);
        }
        else
        {
            invalid_ticket.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.prepare(importString);
    }

    private void onImportRange(TicketRange importTokens)
    {
        setTicket(true, false, false);

        //now update the import token
        ticketRange = importTokens;
        Ticket ticket = viewModel.getImportToken();
        SalesOrder order = viewModel.getSalesOrder();

        String ethPrice = getEthString(order.price) + " ETH";
        String priceUsd = "$" + getUsdString(viewModel.getUSDPrice());

        if (order.price == 0) {
            priceETH.setText("Free import");
            priceETH.setVisibility(View.VISIBLE);
            priceUSD.setVisibility(View.GONE);
        }
        else
        {
            priceETH.setText(ethPrice);
            priceUSD.setText(priceUsd);
            priceETH.setVisibility(View.VISIBLE);
            priceUSD.setVisibility(View.VISIBLE);
            Button importTickets = findViewById(R.id.import_ticket);
            importTickets.setText("PURCHASE");
        }

        Button importTickets = findViewById(R.id.import_ticket);
        importTickets.setVisibility(View.VISIBLE);
        importTickets.setAlpha(1.0f);

        TextView importTxt = findViewById(R.id.textImport);
        importTxt.setText("Ticket Valid to Import");

        TextView textAmount = findViewById(R.id.amount);
        TextView textTicketName = findViewById(R.id.name);
        TextView textVenue = findViewById(R.id.venue);
        TextView textDate = findViewById(R.id.date);
        TextView textRange = findViewById(R.id.tickettext);
        TextView textCat = findViewById(R.id.cattext);

        int numberOfTickets = ticketRange.tokenIds.size();
        if (numberOfTickets > 0) {
            Integer firstTicket = ticketRange.tokenIds.get(0);
            Integer lastTicket = ticketRange.tokenIds.get(numberOfTickets-1);

            String ticketTitle = ticket.getFullName();
            String venue = TicketDecode.getVenue(firstTicket);
            String date = TicketDecode.getDate(firstTicket);
            int rangeFirst = TicketDecode.getSeatIdInt(firstTicket);
            int rangeLast = TicketDecode.getSeatIdInt(lastTicket);
            String cat = TicketDecode.getZone(firstTicket);
            String seatCount = String.format(Locale.getDefault(), "x%d", numberOfTickets);

            textAmount.setText(seatCount);
            textTicketName.setText(ticketTitle);
            textVenue.setText(venue);
            textDate.setText(date);
            textRange.setText(rangeFirst + "-" + rangeLast);
            textCat.setText(cat);
        }
    }

    private void invalidTicket(int count)
    {
        TextView importTxt = findViewById(R.id.textImport);
        importTxt.setText("Ticket already imported");
        setTicket(false, false, true);
        Ticket t = viewModel.getImportToken();
        TextView tv = findViewById(R.id.text_ticket_range);
        String importText = String.valueOf(count) + "x ";
        importText += t.getFullName();

        tv.setText(importText);
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

    private void confirmPurchaseDialog() {
        hideDialog();
        SalesOrder order = viewModel.getSalesOrder();
        String purchase = "Confirm purchase of " + order.ticketCount + " tickets at " + getEthString(order.price) + " ETH total";
        dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_purchase)
                .setMessage(purchase)
                .setPositiveButton(R.string.action_purchase, (dialog1, id) -> {
                    viewModel.performImport();
                })
                .setNegativeButton(R.string.cancel, (dialog1, id) -> {

                })
                .create();
        dialog.show();
    }

    private void onTransaction(String hash) {
        hideDialog();
        dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.transaction_succeeded)
                .setMessage(hash)
                .setPositiveButton(R.string.button_ok, (dialog1, id) -> {
                    new HomeRouter().open(this, true);
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

    private void onError(ErrorEnvelope error) {
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.import_ticket:
                if (ticketRange != null) {
                    if (viewModel.getSalesOrder().price > 0.0)
                    {
                        confirmPurchaseDialog();
                    }
                    else {
                        onProgress(true);
                        viewModel.performImport();
                    }
                }
                break;
            case R.id.cancel_button:
                //go to main screen
                new HomeRouter().open(this, true);
                finish();
                break;
        }
    }

    public static String getUsdString(double usdPrice)
    {
        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.CEILING);
        String formatted = df.format(usdPrice);
        return formatted;
    }

    public static String getEthString(double ethPrice)
    {
        DecimalFormat df = new DecimalFormat("#.####");
        df.setRoundingMode(RoundingMode.CEILING);
        String formatted = df.format(ethPrice);
        return formatted;
    }
}
