package com.alphawallet.app.ui;

import static com.alphawallet.app.repository.TokensRealmSource.databaseKey;
import static com.alphawallet.app.ui.Erc20DetailActivity.HISTORY_LENGTH;
import static com.alphawallet.app.widget.AWalletAlertDialog.WARNING;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.analytics.Analytics;
import com.alphawallet.app.entity.AnalyticsProperties;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.TransactionData;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.analytics.ActionSheetSource;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.ui.widget.adapter.ActivityAdapter;
import com.alphawallet.app.ui.widget.entity.ActionSheetCallback;
import com.alphawallet.app.viewmodel.TokenFunctionViewModel;
import com.alphawallet.app.web3.OnSetValuesListener;
import com.alphawallet.app.web3.Web3TokenView;
import com.alphawallet.app.web3.entity.PageReadyCallback;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.ActionSheetDialog;
import com.alphawallet.app.widget.ActivityHistoryList;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.ethereum.EthereumNetworkBase;
import com.alphawallet.token.entity.TSAction;
import com.alphawallet.token.entity.TicketRange;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;
import io.realm.Realm;
import io.realm.RealmResults;
import timber.log.Timber;

/**
 * Created by James on 2/04/2019.
 * Stormbird in Singapore
 */
@AndroidEntryPoint
public class TokenFunctionActivity extends BaseActivity implements StandardFunctionInterface, PageReadyCallback,
                                                                    OnSetValuesListener, ActionSheetCallback
{
    private TokenFunctionViewModel viewModel;

    private Web3TokenView tokenView;
    private Token token;
    private List<BigInteger> idList = null;
    private FunctionButtonBar functionBar;
    private final Map<String, String> args = new HashMap<>();
    private boolean reloaded;
    private AWalletAlertDialog dialog;
    private LinearLayout webWrapper;
    private ActivityHistoryList activityHistoryList = null;
    private Realm realm = null;
    private RealmResults<RealmToken> realmTokenUpdates;
    private ActionSheetDialog confirmationDialog;
    private AnalyticsProperties confirmationDialogProps;

    private void initViews(Token t) {
        token = t;
        String displayIds = getIntent().getStringExtra(C.EXTRA_TOKEN_ID);
        tokenView = findViewById(R.id.web3_tokenview);
        webWrapper = findViewById(R.id.layout_webwrapper);
        idList = token.stringHexToBigIntegerList(displayIds);
        reloaded = false;

        TicketRange data = new TicketRange(idList, token.tokenInfo.address, false);

        tokenView.displayTicketHolder(token, data, viewModel.getAssetDefinitionService(), false);
        tokenView.setOnReadyCallback(this);
        tokenView.setOnSetValuesListener(this);

        activityHistoryList = findViewById(R.id.history_list);
        activityHistoryList.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_script_view);

        viewModel = new ViewModelProvider(this)
                .get(TokenFunctionViewModel.class);
        viewModel.insufficientFunds().observe(this, this::errorInsufficientFunds);
        viewModel.invalidAddress().observe(this, this::errorInvalidAddress);
        viewModel.walletUpdate().observe(this, this::onWalletUpdate);
        viewModel.transactionError().observe(this, this::txError);
        viewModel.gasEstimateComplete().observe(this, this::checkConfirm);
        viewModel.transactionFinalised().observe(this, this::txWritten);

        functionBar = findViewById(R.id.layoutButtons);
        long chainId = getIntent().getLongExtra(C.EXTRA_CHAIN_ID, EthereumNetworkBase.MAINNET_ID);
        initViews(viewModel.getTokenService().getToken(chainId, getIntent().getStringExtra(C.EXTRA_ADDRESS)));
        toolbar();
        setTitle(getString(R.string.token_function));

        viewModel.startGasPriceUpdate(token.tokenInfo.chainId);
        viewModel.getCurrentWallet();
    }

    private void txError(Throwable throwable)
    {
        throwable.getStackTrace();
        Timber.d("ERROR: %s", throwable.getMessage());

        viewModel.trackError(Analytics.Error.TOKEN_SCRIPT, throwable.getMessage());
    }

    private void onWalletUpdate(Wallet w)
    {
        if(BuildConfig.DEBUG || viewModel.isAuthorizeToFunction())
        {
            functionBar.revealButtons();
            functionBar.setupFunctions(this, viewModel.getAssetDefinitionService(), token, null, idList);
            functionBar.setWalletType(w.type);
        }

        setupRealmListeners(w);
    }

    private void setupRealmListeners(Wallet w)
    {
        realm = viewModel.getRealmInstance(w);
        setTokenListener();
        setEventListener(w);
    }

    private void setTokenListener()
    {
        if (realmTokenUpdates != null) realmTokenUpdates.removeAllChangeListeners();
        String dbKey = databaseKey(token.tokenInfo.chainId, token.tokenInfo.address.toLowerCase());
        realmTokenUpdates = realm.where(RealmToken.class).equalTo("address", dbKey).findAllAsync();
        realmTokenUpdates.addChangeListener(realmTokens -> {
            if (realmTokens.size() == 0) return;
            RealmToken t = realmTokens.first();
            Token update = viewModel.getToken(t.getChainId(), t.getTokenAddress());
            if (update != null) initViews(update);
        });
    }

    private void setEventListener(Wallet wallet)
    {
        ActivityAdapter adapter = new ActivityAdapter(viewModel.getTokensService(), viewModel.getTransactionsInteract(),
                viewModel.getAssetDefinitionService());

        adapter.setDefaultWallet(wallet);

        activityHistoryList.setupAdapter(adapter);
        activityHistoryList.startActivityListeners(viewModel.getRealmInstance(wallet), wallet,
                token, viewModel.getTokensService(), HISTORY_LENGTH);
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        viewModel.stopGasSettingsFetch();
        if (activityHistoryList != null) activityHistoryList.onDestroy();
        if (realmTokenUpdates != null) realmTokenUpdates.removeAllChangeListeners();
        if (realm != null) realm.close();
    }

    @Override
    public void onPageLoaded(WebView view)
    {
        tokenView.callToJS("refresh()");
    }

    @Override
    public void onPageRendered(WebView view)
    {
        webWrapper.setVisibility(View.VISIBLE);
        if (!reloaded) tokenView.reload(); //issue a single reload
        reloaded = true;
    }

    @Override
    public void selectRedeemTokens(List<BigInteger> selection)
    {
        viewModel.selectRedeemToken(this, token, selection);
    }

    @Override
    public void sellTicketRouter(List<BigInteger> selection)
    {
        viewModel.openUniversalLink(this, token, selection);
    }

    @Override
    public void showTransferToken(List<BigInteger> selection)
    {
        viewModel.showTransferToken(this, token, selection);
    }

    @Override
    public void showSend()
    {

    }

    @Override
    public void showReceive()
    {

    }

    @Override
    public void displayTokenSelectionError(TSAction action)
    {

    }

    @Override
    public void handleFunctionDenied(String denialMessage)
    {
        if (dialog == null) dialog = new AWalletAlertDialog(this);
        dialog.setIcon(AWalletAlertDialog.ERROR);
        dialog.setTitle(R.string.token_selection);
        dialog.setMessage(denialMessage);
        dialog.setButtonText(R.string.dialog_ok);
        dialog.setButtonListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public void handleTokenScriptFunction(String function, List<BigInteger> selection)
    {
        Map<String, TSAction> functions = viewModel.getAssetDefinitionService().getTokenFunctionMap(token.tokenInfo.chainId, token.getAddress());
        TSAction action = functions.get(function);
        if (action != null && action.view == null && action.function != null)
        {
            //open action sheet after we determine the gas limit
            Web3Transaction web3Tx = viewModel.handleFunction(action, selection.get(0), token, this);
            if (web3Tx.gasLimit.equals(BigInteger.ZERO))
            {
                calculateEstimateDialog();
                //get gas estimate
                viewModel.estimateGasLimit(web3Tx, token.tokenInfo.chainId);
            }
            else
            {
                //go straight to confirmation
                checkConfirm(web3Tx);
            }
        }
        else
        {
            viewModel.showFunction(this, token, function, idList);
        }
    }

    /**
     * Open the action sheet with the function call request
     * @param w3tx
     */
    private void checkConfirm(Web3Transaction w3tx)
    {
        if (w3tx.gasLimit.equals(BigInteger.ZERO))
        {
            estimateError(w3tx);
        }
        else
        {
            if (dialog != null && dialog.isShowing()) dialog.dismiss();
            confirmationDialog = new ActionSheetDialog(this, w3tx, token, "", //TODO: Reverse resolve ENS address
                    w3tx.recipient.toString(), viewModel.getTokenService(), this);
            confirmationDialog.setURL("TokenScript");
            confirmationDialog.setCanceledOnTouchOutside(false);
            confirmationDialog.show();

            confirmationDialogProps = new AnalyticsProperties();
            confirmationDialogProps.put(Analytics.PROPS_ACTION_SHEET_SOURCE, ActionSheetSource.TOKENSCRIPT.getValue());
            viewModel.track(Analytics.Navigation.ACTION_SHEET_FOR_TRANSACTION_CONFIRMATION, confirmationDialogProps);
        }
    }

    /**
     * Final return path
     * @param transactionData
     */
    private void txWritten(TransactionData transactionData)
    {
        confirmationDialog.transactionWritten(transactionData.txHash); //display hash and success in ActionSheet, start 1 second timer to dismiss.
        viewModel.track(Analytics.Navigation.ACTION_SHEET_FOR_TRANSACTION_CONFIRMATION_SUCCESSFUL, confirmationDialogProps);
    }

    private void calculateEstimateDialog()
    {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        dialog = new AWalletAlertDialog(this);
        dialog.setTitle(getString(R.string.calc_gas_limit));
        dialog.setIcon(AWalletAlertDialog.NONE);
        dialog.setProgressMode();
        dialog.setCancelable(false);
        dialog.show();
    }

    private void errorInsufficientFunds(Token currency)
    {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        dialog = new AWalletAlertDialog(this);
        dialog.setIcon(AWalletAlertDialog.ERROR);
        dialog.setTitle(R.string.error_insufficient_funds);
        dialog.setMessage(getString(R.string.current_funds, currency.getCorrectedBalance(currency.tokenInfo.decimals), currency.getSymbol()));
        dialog.setButtonText(R.string.button_ok);
        dialog.setButtonListener(v -> dialog.dismiss());
        dialog.show();

        viewModel.trackError(Analytics.Error.TOKEN_SCRIPT, getString(R.string.error_insufficient_funds));
    }

    private void estimateError(final Web3Transaction w3tx)
    {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        dialog = new AWalletAlertDialog(this);
        dialog.setIcon(WARNING);
        dialog.setTitle(R.string.confirm_transaction);
        dialog.setMessage(R.string.error_transaction_may_fail);
        dialog.setButtonText(R.string.button_ok);
        dialog.setSecondaryButtonText(R.string.action_cancel);
        dialog.setButtonListener(v -> {
            BigInteger gasEstimate = GasService.getDefaultGasLimit(token, w3tx);
            checkConfirm(new Web3Transaction(w3tx.recipient, w3tx.contract, w3tx.value, w3tx.gasPrice, gasEstimate, w3tx.nonce, w3tx.payload, w3tx.description));
        });

        dialog.setSecondaryButtonListener(v -> {
            dialog.dismiss();
        });

        dialog.show();
    }

    private void errorInvalidAddress(String address)
    {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        dialog = new AWalletAlertDialog(this);
        dialog.setIcon(AWalletAlertDialog.ERROR);
        dialog.setTitle(R.string.error_invalid_address);
        dialog.setMessage(getString(R.string.invalid_address_explain, address));
        dialog.setButtonText(R.string.button_ok);
        dialog.setButtonListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public void setValues(Map<String, String> updates)
    {
        boolean newValues = false;
        TicketRange data = new TicketRange(idList, token.tokenInfo.address, false);

        //called when values update
        for (String key : updates.keySet())
        {
            String value = updates.get(key);
            String old = args.put(key, updates.get(key));
            if (!value.equals(old)) newValues = true;
        }

        if (newValues)
        {
            viewModel.getAssetDefinitionService().addLocalRefs(args);
            //rebuild the view
            tokenView.displayTicketHolder(token, data, viewModel.getAssetDefinitionService(), false);
        }
    }

    private void showTransactionError()
    {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        dialog = new AWalletAlertDialog(this);
        dialog.setIcon(AWalletAlertDialog.ERROR);
        dialog.setTitle(R.string.tokenscript_error);
        dialog.setMessage(getString(R.string.invalid_parameters));
        dialog.setButtonText(R.string.button_ok);
        dialog.setButtonListener(v ->dialog.dismiss());
        dialog.show();
    }

    /*
        ActionSheet methods
     */

    @Override
    public void getAuthorisation(SignAuthenticationCallback callback)
    {
        viewModel.getAuthentication(this, callback);
    }

    @Override
    public void sendTransaction(Web3Transaction finalTx)
    {
        viewModel.sendTransaction(finalTx, token.tokenInfo.chainId, ""); //return point is txWritten
    }

    @Override
    public void dismissed(String txHash, long callbackId, boolean actionCompleted)
    {
        if (actionCompleted)
        {
            Intent intent = new Intent();
            intent.putExtra(C.EXTRA_TXHASH, txHash);
            setResult(RESULT_OK, intent);
            finish();
        }
        else
        {
            viewModel.track(Analytics.Action.ACTION_SHEET_CANCELLED, confirmationDialogProps);
        }
    }

    @Override
    public void notifyConfirm(String mode)
    {
        viewModel.actionSheetConfirm(mode);

        confirmationDialogProps.put(Analytics.PROPS_ACTION_SHEET_MODE, mode);
        viewModel.track(Analytics.Action.ACTION_SHEET_COMPLETED, confirmationDialogProps);
    }

    ActivityResultLauncher<Intent> getGasSettings = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> confirmationDialog.setCurrentGasIndex(result));

    @Override
    public ActivityResultLauncher<Intent> gasSelectLauncher()
    {
        return getGasSettings;
    }
}
