package com.alphawallet.app.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.TransactionData;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.ui.widget.entity.ActionSheetCallback;
import com.alphawallet.app.ui.widget.entity.NFTAttributeLayout;
import com.alphawallet.app.util.KittyUtils;
import com.alphawallet.app.viewmodel.TokenFunctionViewModel;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.ActionSheetDialog;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.app.widget.NFTImageView;
import com.alphawallet.ethereum.EthereumNetworkBase;
import com.alphawallet.token.entity.TSAction;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;


import static com.alphawallet.app.C.EXTRA_STATE;
import static com.alphawallet.app.C.EXTRA_TOKENID_LIST;
import static com.alphawallet.app.C.Key.WALLET;
import static com.alphawallet.app.entity.DisplayState.TRANSFER_TO_ADDRESS;
import static com.alphawallet.app.widget.AWalletAlertDialog.WARNING;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TokenDetailActivity extends BaseActivity implements StandardFunctionInterface, ActionSheetCallback
{
    private TokenFunctionViewModel viewModel;

    private NFTImageView assetImage;
    private TextView title;
    private TextView name;
    private TextView desc;
    private TextView id;
    private TextView generation;
    private TextView cooldown;
    private TextView openExternal;
    private NFTAttributeLayout attributeLayout;
    private FunctionButtonBar functionBar;
    private ActionSheetDialog confirmationDialog;
    private AWalletAlertDialog dialog;
    private Token token;
    private NFTAsset asset;
    private BigInteger tokenId;

    private void initViews() {
        title = findViewById(R.id.title);
        assetImage = findViewById(R.id.layout_image);
        name = findViewById(R.id.name);
        desc = findViewById(R.id.description);
        id = findViewById(R.id.id);
        generation = findViewById(R.id.generation);
        cooldown = findViewById(R.id.cooldown);
        openExternal = findViewById(R.id.open_external);
        attributeLayout = findViewById(R.id.attributes);
        functionBar = findViewById(R.id.layoutButtons);
        if (functionBar != null)
        {
            List<BigInteger> selection = new ArrayList<>();
            selection.add(tokenId);
            functionBar.setupFunctions(this, viewModel.getAssetDefinitionService(), token, null, selection);
            functionBar.revealButtons();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_token_detail);
        viewModel = new ViewModelProvider(this)
                .get(TokenFunctionViewModel.class);
        viewModel.gasEstimateComplete().observe(this, this::checkConfirm);
        viewModel.transactionFinalised().observe(this, this::txWritten);

        if (getIntent() != null && getIntent().getExtras() != null) {
            asset = getIntent().getExtras().getParcelable(C.EXTRA_NFTASSET);
            String address = getIntent().getStringExtra(C.EXTRA_ADDRESS);
            long chainId = getIntent().getLongExtra(C.EXTRA_CHAIN_ID, EthereumNetworkBase.MAINNET_ID);
            token = viewModel.getToken(chainId, address);
            tokenId = new BigInteger(getIntent().getExtras().getString(C.EXTRA_TOKEN_ID));
            initViews();
            toolbar();
            setTitle(token.getFullName());
            setupPage();
            viewModel.prepare();
        } else {
            finish();
        }
    }

    private void setupPage() {
        assetImage.setupTokenImage(asset);
        setDetails(asset);
        setNameAndDesc(asset);
        setExternalLink(asset);
        attributeLayout.bind(token, asset);
    }

    @Override
    public void showTransferToken(List<BigInteger> selection)
    {
        openTransferDirectDialog();
    }

    @Override
    public void handleTokenScriptFunction(String function, List<BigInteger> selection)
    {
        //does the function have a view? If it's transaction only then handle here
        Map<String, TSAction> functions = viewModel.getAssetDefinitionService().getTokenFunctionMap(token.tokenInfo.chainId, token.getAddress());
        TSAction action = functions.get(function);

        //handle TS function
        if (action != null && action.view == null && action.function != null)
        {
            //go straight to function call
            Web3Transaction w3Tx = viewModel.handleFunction(action, selection.get(0), token, this);
            calculateEstimateDialog();
            //get gas estimate
            viewModel.estimateGasLimit(w3Tx, token.tokenInfo.chainId);
        }
        else
        {
            viewModel.showFunction(this, token, function, selection);
        }
    }

    private void setExternalLink(NFTAsset asset) {
        if (asset.getExternalLink() != null && !asset.getExternalLink().equals("null")) {
            openExternal.setText(getString(R.string.open_on_external_link,
                    token.getFullName()));

            openExternal.setOnClickListener(v -> {
                Intent launchBrowser = new Intent(Intent.ACTION_VIEW, Uri.parse(asset.getExternalLink()));
                startActivity(launchBrowser);
            });
        } else {
            openExternal.setVisibility(View.GONE);
        }
    }

    private void setNameAndDesc(NFTAsset asset) {
        name.setText(asset.getName());
        desc.setText(asset.getDescription());
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

    private void setDetails(NFTAsset asset) {
        id.setText(tokenId.toString());
        if (asset.getAttributeValue("generation") != null) {
            generation.setText(String.format("Gen %s",
                    asset.getAttributeValue("generation")));
        } else if (asset.getAttributeValue("gen") != null) {
            generation.setText(String.format("Gen %s",
                    asset.getAttributeValue("gen")));
        } else {
            generation.setVisibility(View.GONE);
        }

        if (asset.getAttributeValue("cooldown_index") != null) {
            cooldown.setText(String.format("%s Cooldown",
                    KittyUtils.parseCooldownIndex(
                            asset.getAttributeValue("cooldown_index"))));
        } else if (asset.getAttributeValue("cooldown") != null) { // Non-CK
            cooldown.setText(String.format("%s Cooldown",
                    asset.getAttributeValue("cooldown")));
        } else {
            cooldown.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    ActivityResultLauncher<Intent> transferDirectDialogResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null)
                {
                    Intent i = new Intent();
                    i.putExtra(C.EXTRA_TXHASH, result.getData().getStringExtra(C.EXTRA_TXHASH));
                    setResult(RESULT_OK, new Intent());
                    finish();
                }
            });

    private void openTransferDirectDialog()
    {
        Intent intent = new Intent(this, TransferTicketDetailActivity.class);
        intent.putExtra(WALLET, new Wallet(token.getWallet()));
        intent.putExtra(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);
        intent.putExtra(C.EXTRA_ADDRESS, token.getAddress());
        intent.putExtra(EXTRA_TOKENID_LIST, tokenId.toString(16));
        intent.putExtra(EXTRA_STATE, TRANSFER_TO_ADDRESS.ordinal());
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        transferDirectDialogResult.launch(intent);
    }

    @Override
    public void displayTokenSelectionError(TSAction action)
    {
        AWalletAlertDialog dialog = new AWalletAlertDialog(this);
        dialog.setIcon(AWalletAlertDialog.ERROR);
        dialog.setTitle(R.string.token_selection);
        dialog.setMessage(getString(R.string.token_requirement, String.valueOf(action.function.getTokenRequirement())));
        dialog.setButtonText(R.string.dialog_ok);
        dialog.setButtonListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void checkConfirm(Web3Transaction w3tx)
    {
        if (w3tx.gasLimit.equals(BigInteger.ZERO))
        {
            estimateError(w3tx);
        }
        else
        {
            if (dialog != null && dialog.isShowing()) dialog.dismiss();
            confirmationDialog = new ActionSheetDialog(this, w3tx, token, "", //TODO: Reverse resolve address
                    w3tx.recipient.toString(), viewModel.getTokenService(), this);
            confirmationDialog.setURL("TokenScript");
            confirmationDialog.setCanceledOnTouchOutside(false);
            confirmationDialog.show();
        }
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

    /**
     * Final return path
     * @param transactionData
     */
    private void txWritten(TransactionData transactionData)
    {
        confirmationDialog.transactionWritten(transactionData.txHash); //display hash and success in ActionSheet, start 1 second timer to dismiss.
    }

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
        //no action
    }

    @Override
    public void notifyConfirm(String mode)
    {
        viewModel.actionSheetConfirm(mode);
    }
}
