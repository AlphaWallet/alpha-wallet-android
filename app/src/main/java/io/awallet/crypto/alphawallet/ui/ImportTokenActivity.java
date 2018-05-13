package io.awallet.crypto.alphawallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.math.RoundingMode;
import java.text.DecimalFormat;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.awallet.crypto.alphawallet.R;
import io.awallet.crypto.alphawallet.entity.ErrorEnvelope;
import io.awallet.crypto.alphawallet.entity.NetworkInfo;
import io.awallet.crypto.alphawallet.entity.Ticket;
import io.awallet.crypto.alphawallet.router.HomeRouter;
import io.awallet.crypto.alphawallet.ui.widget.entity.TicketRange;
import io.awallet.crypto.alphawallet.viewmodel.ImportTokenViewModel;
import io.awallet.crypto.alphawallet.viewmodel.ImportTokenViewModelFactory;
import io.awallet.crypto.alphawallet.widget.AWalletAlertDialog;
import io.awallet.crypto.alphawallet.widget.AWalletConfirmationDialog;
import io.awallet.crypto.alphawallet.widget.SystemView;
import io.stormbird.token.entity.MagicLinkData;

import static io.awallet.crypto.alphawallet.C.ETH_SYMBOL;
import static io.awallet.crypto.alphawallet.C.IMPORT_STRING;

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
    private AWalletAlertDialog aDialog;
    private AWalletConfirmationDialog cDialog;

    private TextView priceETH;
    private TextView priceUSD;
    private TextView importTxt;

    private LinearLayout costLayout;
    private int networkId = 0;

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

        importTxt = findViewById(R.id.textImport);
        costLayout = findViewById(R.id.cost_layout);

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
        viewModel.network().observe(this, this::onNetwork);

        ticketRange = null;
    }

    private void onNetwork(NetworkInfo networkInfo)
    {
        networkId = networkInfo.chainId;
    }

    private void onBadLink(Boolean aBoolean)
    {
        TextView tv = findViewById(R.id.text_ticket_range);
        tv.setVisibility(View.GONE);
        importTxt.setVisibility(View.GONE);
        setTicket(false, false, true);
        //bad link
        hideDialog();
        aDialog = new AWalletAlertDialog(this);
        aDialog.setIcon(AWalletAlertDialog.ERROR);
        aDialog.setTitle(R.string.bad_import_link);
        aDialog.setMessage(R.string.bad_import_link_body);
        aDialog.setButtonText(R.string.action_cancel);
        aDialog.setButtonListener(v -> aDialog.dismiss());
        aDialog.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideDialog();
    }

    private void setTicket(boolean ticket, boolean progress, boolean invalid)
    {
        LinearLayout progress_ticket = findViewById(R.id.layout_select_overlay);
        LinearLayout valid_ticket = findViewById(R.id.layout_select);
        LinearLayout invalid_ticket = findViewById(R.id.layout_select_invalid);
        if (ticket) {
            valid_ticket.setVisibility(View.VISIBLE);
            costLayout.setVisibility(View.VISIBLE);
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
        MagicLinkData order = viewModel.getSalesOrder();

        String ethPrice = getEthString(order.price) + " " + ETH_SYMBOL;
        String priceUsd = "$" + getUsdString(viewModel.getUSDPrice());

        if (order.price == 0) {
            priceETH.setText(R.string.free_import);
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
            importTickets.setText(R.string.action_purchase);
        }

        Button importTickets = findViewById(R.id.import_ticket);
        importTickets.setVisibility(View.VISIBLE);
        importTickets.setAlpha(1.0f);

        importTxt.setText(R.string.ticket_import_valid);

        ticket.displayTicketHolder(ticketRange, this);
    }

    private void invalidTicket(int count)
    {
        MagicLinkData order = viewModel.getSalesOrder();
        if (count == 0)
        {
            importTxt.setText(R.string.ticket_already_imported);
        }
        else
        {
            importTxt.setText(R.string.ticket_range_inavlid);
        }

        setTicket(false, false, true);
        Ticket t = viewModel.getImportToken();
        TextView tv = findViewById(R.id.text_ticket_range);
        String importText = String.valueOf(order.ticketCount) + "x ";
        importText += t.getFullName();

        tv.setText(importText);
    }

    private void onProgress(boolean shouldShowProgress) {
        hideDialog();
        if (shouldShowProgress) {
            aDialog = new AWalletAlertDialog(this);
            aDialog.setTitle(R.string.title_dialog_sending);
            aDialog.setProgressMode();
            aDialog.setCancelable(false);
            aDialog.show();
        }
    }

    private void hideDialog() {
        if (aDialog != null && aDialog.isShowing()) {
            aDialog.dismiss();
        }

        if (cDialog != null && cDialog.isShowing()) {
            cDialog.dismiss();
        }
    }

    private void confirmPurchaseDialog() {
        hideDialog();
        MagicLinkData order = viewModel.getSalesOrder();
        cDialog = new AWalletConfirmationDialog(this);
        cDialog.setTitle(R.string.confirm_purchase);
        String ticketLabel = order.ticketCount > 1 ? getString(R.string.tickets) : getString(R.string.ticket);
        cDialog.setSmallText(getString(R.string.total_cost_for_x_tickets, order.ticketCount, ticketLabel));
        cDialog.setMediumText(getString(R.string.total_cost, getEthString(order.price)));
        cDialog.setPrimaryButtonText(R.string.confirm_purchase_button_text);
        cDialog.setSecondaryButtonText(R.string.dialog_cancel_back);
        cDialog.setPrimaryButtonListener(v -> {
            viewModel.performImport();
            cDialog.dismiss();
        });
        cDialog.setSecondaryButtonText(R.string.dialog_cancel_back);
        cDialog.setSecondaryButtonListener(v -> cDialog.dismiss());
        cDialog.show();
    }

    private void onTransaction(String hash) {
        hideDialog();
        aDialog = new AWalletAlertDialog(this);
        aDialog.setTitle(R.string.transaction_succeeded);
        aDialog.setMessage(hash);
        aDialog.setButtonText(R.string.copy);
        aDialog.setButtonListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("transaction hash", hash);
            clipboard.setPrimaryClip(clip);
            aDialog.dismiss();
            new HomeRouter().open(this, true);
            finish();
        });
        aDialog.setOnCancelListener(v -> {
            new HomeRouter().open(this, true);
            finish();
        });
        aDialog.show();
    }

    private void onError(ErrorEnvelope error) {
        hideDialog();
        aDialog = new AWalletAlertDialog(this);
        aDialog.setIcon(AWalletAlertDialog.ERROR);
        aDialog.setTitle(R.string.error_transaction_failed);
        aDialog.setMessage(error.message);
        aDialog.setButtonText(R.string.button_ok);
        aDialog.setButtonListener(v -> {
            aDialog.dismiss();
        });
        aDialog.show();
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
                        Ticket t = viewModel.getImportToken();
                        if (t.getXMLProperty(networkId,"address", this).equalsIgnoreCase(t.getAddress()))
                        {
                            viewModel.importThroughFeemaster(t.getXMLProperty(networkId,"feemaster", this));
                        }
                        else
                        {
                            viewModel.performImport();
                        }
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
