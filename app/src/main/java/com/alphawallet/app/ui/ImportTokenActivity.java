package com.alphawallet.app.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.token.entity.MagicLinkInfo;
import com.alphawallet.token.tools.Convert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import javax.inject.Inject;
import dagger.android.AndroidInjection;
import com.alphawallet.token.entity.MagicLinkData;
import com.alphawallet.token.entity.TicketRange;
import com.alphawallet.token.tools.ParseMagicLink;
import com.alphawallet.app.R;

import com.alphawallet.app.entity.CryptoFunctions;
import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.PinAuthenticationCallbackInterface;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.Ticket;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.router.HomeRouter;
import com.alphawallet.app.viewmodel.ImportTokenViewModel;
import com.alphawallet.app.viewmodel.ImportTokenViewModelFactory;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.AWalletConfirmationDialog;
import com.alphawallet.app.widget.CertifiedToolbarView;
import com.alphawallet.app.widget.SignTransactionDialog;
import com.alphawallet.app.widget.SystemView;
import static com.alphawallet.token.tools.Convert.getEthString;
import static com.alphawallet.token.tools.ParseMagicLink.currencyLink;
import static com.alphawallet.token.tools.ParseMagicLink.spawnable;
import static com.alphawallet.app.C.IMPORT_STRING;
import static com.alphawallet.app.entity.Operation.SIGN_DATA;
import static org.web3j.crypto.WalletUtils.isValidAddress;

/**
 * Created by James on 9/03/2018.
 */

public class ImportTokenActivity extends BaseActivity implements View.OnClickListener, SignAuthenticationCallback
{

    @Inject
    protected ImportTokenViewModelFactory importTokenViewModelFactory;
    private ImportTokenViewModel viewModel;
    private SystemView systemView;

    private TicketRange ticketRange;
    private String importString;
    private AWalletAlertDialog aDialog;
    private AWalletConfirmationDialog cDialog;
    private CertifiedToolbarView toolbarView;

    private TextView priceETH;
    private TextView priceUSD;
    private TextView priceUSDLabel;
    private TextView importTxt;
    private TextView importHeader;

    private LinearLayout costLayout;
    private int chainId = 0;
    private boolean usingFeeMaster = false;
    private String paymasterUrlPrefix = "https://paymaster.stormbird.sg/api";
    private final String TAG = "ITA";
    private PinAuthenticationCallbackInterface authInterface;

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
        priceUSDLabel = findViewById(R.id.fiat_price_txt);
        importHeader = findViewById(R.id.import_header);
        toolbarView = findViewById(R.id.toolbar);
        priceETH.setVisibility(View.GONE);
        priceUSD.setVisibility(View.GONE);
        priceUSDLabel.setVisibility(View.GONE);

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
        viewModel.invalidTime().observe(this, this::invalidTime);
        viewModel.newTransaction().observe(this, this::onTransaction);
        viewModel.error().observe(this, this::onError);
        viewModel.txError().observe(this, this::onTxError);
        viewModel.invalidLink().observe(this, this::onBadLink);
        viewModel.network().observe(this, this::onNetwork);
        viewModel.checkContractNetwork().observe(this, this::checkContractNetwork);
        viewModel.ticketNotValid().observe(this, this::onInvalidTicket);
        viewModel.feemasterAvailable().observe(this, this::onFeemasterAvailable);
        viewModel.sig().observe(this, toolbarView::onSigData);

        ticketRange = null;

        Ticket.blankTicketHolder(R.string.loading,this);
    }

    private void onError(ErrorEnvelope errorEnvelope)
    {
        Log.d(TAG, errorEnvelope.message);
    }

    private void checkContractNetwork(String contractAddress)
    {
        //check for currency link - currently only xDAI
        MagicLinkData data = viewModel.getSalesOrder();
        if (data.chainId > 0)
        {
            viewModel.switchNetwork(data.chainId);
            viewModel.loadToken();
        }
        else
        {
            //Legacy support
            switch (data.contractType)
            {
                case currencyLink:
                    //for currency drop link, check xDai first, then other networks
                    viewModel.switchNetwork(EthereumNetworkRepository.XDAI_ID);
                    viewModel.checkTokenNetwork(contractAddress, "requiredPrefix");
                    break;
                default:
                    viewModel.checkTokenNetwork(contractAddress, "name");
                    break;
            }
        }
    }

    private void onNetwork(NetworkInfo networkInfo)
    {
        chainId = networkInfo.chainId;
        String domain = EthereumNetworkRepository.getMagicLinkDomainFromNetworkId(chainId);
        paymasterUrlPrefix = MagicLinkInfo.formPaymasterURLPrefixFromDomain(domain);
        TextView networkText = findViewById(R.id.textNetworkName);
        networkText.setText(networkInfo.name);
    }

    private void onInvalidTicket(Boolean aBoolean)
    {
        TextView tv = findViewById(R.id.text_ticket_range);
        tv.setVisibility(View.GONE);
        importTxt.setVisibility(View.GONE);
        setTicket(false, false, true);
        //bad link
        hideDialog();
        aDialog = new AWalletAlertDialog(this);
        aDialog.setIcon(AWalletAlertDialog.ERROR);
        aDialog.setTitle(R.string.ticket_not_valid);
        aDialog.setMessage(R.string.ticket_not_valid_body);
        aDialog.setButtonText(R.string.action_cancel);
        aDialog.setButtonListener(v -> aDialog.dismiss());
        aDialog.show();
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
    protected void onPause()
    {
        super.onPause();
        hideDialog();
        viewModel.resetSignDialog();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //16908332
        switch (item.getItemId()) {
            case android.R.id.home: {
                new HomeRouter().open(this, true);
                finish();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void setTicket(boolean ticket, boolean progress, boolean invalid)
    {
        LinearLayout progress_ticket = findViewById(R.id.layout_select_overlay);
        RelativeLayout valid_ticket = findViewById(R.id.layout_select_ticket);
        LinearLayout invalid_ticket = findViewById(R.id.layout_select_invalid);
        if (ticket)
        {
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
    protected void onResume()
    {
        super.onResume();
        viewModel.prepare(importString);
    }

    private void onImportRange(TicketRange importTokens)
    {
        setTicket(true, false, false);
        usingFeeMaster = false;

        //now update the import token
        ticketRange = importTokens;
        MagicLinkData order = viewModel.getSalesOrder();

        //get current symbol
        String networkSymbol = viewModel.network().getValue().symbol;
        String ethPrice = getEthString(order.price) + " " + networkSymbol;
        String priceUsd = "$" + getUsdString(viewModel.getUSDPrice() * order.price);

        updateImportPageForFunction();

        if (order.price == 0)
        {
            if (paymasterUrlPrefix != null)
            {
                viewModel.checkFeemaster(paymasterUrlPrefix);
                priceETH.setText(R.string.check_feemaster);
                return;
            }
            else
            {
                priceETH.setText(R.string.free_import_with_gas);
                displayImportAction();
            }

            priceETH.setVisibility(View.VISIBLE);
            priceUSD.setVisibility(View.GONE);
            priceUSDLabel.setVisibility(View.GONE);
        }
        else
        {
            priceETH.setText(ethPrice);
            priceUSD.setText(priceUsd);
            priceETH.setVisibility(View.VISIBLE);
            priceUSD.setVisibility(View.VISIBLE);
            priceUSDLabel.setVisibility(View.VISIBLE);
            Button importTickets = findViewById(R.id.import_ticket);
            importTickets.setText(R.string.action_purchase);
            displayImportAction();
        }
    }

    private void updateImportPageForFunction()
    {
        MagicLinkData data = viewModel.getSalesOrder();
        switch (data.contractType)
        {
            case spawnable:
                importTxt.setText(R.string.token_spawn_valid);
                importHeader.setText(R.string.import_spawnable);
                break;
            case currencyLink:
                importTxt.setText(R.string.currency_drop);
                importHeader.setText(R.string.currency_import);
                setTicket(false, false, false); //switch off all ticket info
                //show currency to import
                LinearLayout currencyCard = findViewById(R.id.layout_currency_import);
                currencyCard.setVisibility(View.VISIBLE);
                TextView currency = findViewById(R.id.text_currency_message);
                BigDecimal ethValue = Convert.fromWei(Convert.toWei(new BigDecimal(data.amount), Convert.Unit.SZABO), Convert.Unit.ETHER);
                String networkSymbol = viewModel.network().getValue().symbol;
                String message = getString(R.string.you_will_receive) + " " + ethValue.toPlainString() + " " + networkSymbol;
                currency.setText(message);
                findViewById(R.id.text_total_cost).setVisibility(View.GONE);
                break;
            default:
                importTxt.setText(R.string.ticket_import_valid);
                break;
        }

        viewModel.checkTokenScriptSignature(data.chainId, data.contractAddress);
    }

    private void displayImportAction()
    {
        Token token = viewModel.getImportToken();
        Button importTickets = findViewById(R.id.import_ticket);
        importTickets.setVisibility(View.VISIBLE);
        importTickets.setAlpha(1.0f);

        MagicLinkData data = viewModel.getSalesOrder();
        //Customise button text
        View baseView = findViewById(android.R.id.content);

        switch (data.contractType)
        {
            case spawnable:
                importTickets.setText(R.string.spawn);
                token.displayTicketHolder(ticketRange, baseView, viewModel.getAssetDefinitionService(), getBaseContext());
                break;
            case currencyLink:
                importTickets.setText(R.string.action_import);
                break;
            default:
                importTxt.setText(R.string.ticket_import_valid);
                token.displayTicketHolder(ticketRange, baseView, viewModel.getAssetDefinitionService(), getBaseContext());
                break;
        }
    }

    private void onFeemasterAvailable(Boolean available)
    {
        priceETH.setVisibility(View.VISIBLE);
        usingFeeMaster = available;
        if (available)
        {
            priceETH.setText(R.string.free_import);
            displayImportAction();
        }
        else
        {
            switch (viewModel.getSalesOrder().contractType)
            {
                case currencyLink:
                    priceUSD.setVisibility(View.GONE);
                    priceUSDLabel.setVisibility(View.GONE);
                    importTxt.setVisibility(View.GONE);
                    findViewById(R.id.text_total_cost).setVisibility(View.GONE);
                    priceETH.setText(R.string.feemaster_service_not_available);
                    break;
                default:
                    priceETH.setText(R.string.free_import_with_gas);
                    priceUSD.setVisibility(View.GONE);
                    priceUSDLabel.setVisibility(View.GONE);
                    displayImportAction();
            }
        }
    }

    private void invalidTime(Integer integer)
    {
        MagicLinkData order = viewModel.getSalesOrder();
        importTxt.setText(R.string.ticket_range_expired);

        setTicket(false, false, true);
        Token t = viewModel.getImportToken();
        TextView tv = findViewById(R.id.text_ticket_range);
        String importText = String.valueOf(order.ticketCount) + "x ";
        importText += t.getTokenName(viewModel.getAssetDefinitionService(), order.ticketCount);

        tv.setText(importText);
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
        Token t = viewModel.getImportToken();
        TextView tv = findViewById(R.id.text_ticket_range);
        String importText = String.valueOf(order.ticketCount) + "x ";
        importText += t.getTokenName(viewModel.getAssetDefinitionService(), order.ticketCount);
        tv.setText(importText);
        //Note: it's actually not possible to pull the event or anything like that since we can't get the tokenID if it's been imported.
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
        String currencySymbol = viewModel.getNetwork().symbol;
        MagicLinkData order = viewModel.getSalesOrder();
        cDialog = new AWalletConfirmationDialog(this);
        cDialog.setTitle(R.string.confirm_purchase);
        String ticketLabel = order.ticketCount > 1 ? getString(R.string.tickets) : getString(R.string.ticket);
        cDialog.setSmallText(getString(R.string.total_cost_for_x_tickets, order.ticketCount, ticketLabel));
        cDialog.setMediumText(getString(R.string.total_cost, getEthString(order.price), currencySymbol));
        cDialog.setPrimaryButtonText(R.string.confirm_purchase_button_text);
        cDialog.setSecondaryButtonText(R.string.dialog_cancel_back);
        cDialog.setPrimaryButtonListener(v -> {
            viewModel.getAuthorisation(this, this);
            cDialog.dismiss();
        });
        cDialog.setSecondaryButtonText(R.string.dialog_cancel_back);
        cDialog.setSecondaryButtonListener(v -> cDialog.dismiss());
        cDialog.show();
    }

    private void onTransaction(String hash) {
        onProgress(false);
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

    private void onTxError(ErrorEnvelope error) {
        hideDialog();
        aDialog = new AWalletAlertDialog(this);
        aDialog.setIcon(AWalletAlertDialog.ERROR);
        aDialog.setTitle(R.string.error_transaction_failed);
        aDialog.setMessage(error.message);
        aDialog.setCancelable(true);
        aDialog.setButtonText(R.string.button_ok);
        aDialog.setButtonListener(v -> {
            aDialog.dismiss();
        });
        aDialog.show();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        viewModel.stopGasPriceChecker();
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
                    else
                    {
                        onProgress(true);
                        completeImport();
                    }
                }
                else if (viewModel.getSalesOrder().contractType == currencyLink)
                {
                    onProgress(true);
                    completeCurrencyImport();
                }
                break;
            case R.id.cancel_button:
                //go to main screen
                new HomeRouter().open(this, true);
                finish();
                break;
        }
    }

    private void completeImport()
    {
        if (paymasterUrlPrefix != null && usingFeeMaster)
        {
            viewModel.importThroughFeemaster(paymasterUrlPrefix);
        }
        else
        {
            //needs gas, so require auth first
            SignAuthenticationCallback cb = new SignAuthenticationCallback()
            {
                @Override
                public void GotAuthorisation(boolean gotAuth)
                {
                    viewModel.performImport();
                }

                @Override
                public void setupAuthenticationCallback(PinAuthenticationCallbackInterface authCallback)
                {
                    authInterface = authCallback;
                }
            };
            viewModel.getAuthorisation(this, cb);
        }
    }

    private void completeCurrencyImport()
    {
        //attempt to import through the server
        if (usingFeeMaster)
        {
            viewModel.importThroughFeemaster(paymasterUrlPrefix);
        }
        else
        {
            viewModel.performImport();
        }
    }

    public static String getUsdString(double usdPrice)
    {
        DecimalFormat df = new DecimalFormat("#,###.00");
        df.setRoundingMode(RoundingMode.CEILING);
        return df.format(usdPrice);
    }

    public String getMagiclinkFromClipboard(Context ctx)
    {
        String magicLink = null;
        try
        {
            //try clipboard data
            ClipboardManager clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.getPrimaryClip() != null)
            {
                CharSequence text = clipboard.getPrimaryClip().getItemAt(0).getText();
                //see if text is a magic link
                if (text != null && text.length() > 60 && text.length() < 300)
                {
                    //could be magicLink
                    CryptoFunctions cryptoFunctions = new CryptoFunctions();
                    ParseMagicLink parser = new ParseMagicLink(cryptoFunctions, EthereumNetworkRepository.extraChains());
                    MagicLinkData order = parser.parseUniversalLink(text.toString());
                    if (isValidAddress(order.contractAddress) && order.indices.length > 0)
                    {
                        magicLink = text.toString();
                        //now clear the clipboard - we only ever do this if it's definitely a magicLink in the clipboard
                        ClipData clipData = ClipData.newPlainText("", "");
                        clipboard.setPrimaryClip(clipData);
                    }
                }
            }
        }
        catch (Exception e)
        {
            //not a magicLink
            magicLink = null;
        }

        return magicLink;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode,resultCode,intent);

        if (requestCode >= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS && requestCode <= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS + 10)
        {
            GotAuthorisation(resultCode == RESULT_OK);
        }
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }

    /**
     * Return from key authorisation either through activity result (for PIN) or directly through the callback from fingerprint unlock
     * @param gotAuth authorisation was successful
     */
    @Override
    public void GotAuthorisation(boolean gotAuth)
    {
        if (gotAuth && authInterface != null) authInterface.CompleteAuthentication(SIGN_DATA);
        else if (!gotAuth && authInterface != null) authInterface.FailedAuthentication(SIGN_DATA);
        viewModel.performImport();
        onProgress(true);
    }

    @Override
    public void setupAuthenticationCallback(PinAuthenticationCallbackInterface authCallback)
    {
        authInterface = authCallback;
    }
}
