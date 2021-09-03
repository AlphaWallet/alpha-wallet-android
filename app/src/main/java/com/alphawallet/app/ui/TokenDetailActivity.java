package com.alphawallet.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.gridlayout.widget.GridLayout;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.ui.widget.entity.NFTAttributeLayout;
import com.alphawallet.app.util.KittyUtils;
import com.alphawallet.app.viewmodel.TokenFunctionViewModel;
import com.alphawallet.app.viewmodel.TokenFunctionViewModelFactory;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.app.widget.NFTImageView;
import com.alphawallet.token.entity.TSAction;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.alphawallet.app.C.EXTRA_STATE;
import static com.alphawallet.app.C.EXTRA_TOKENID_LIST;
import static com.alphawallet.app.C.Key.TICKET;
import static com.alphawallet.app.C.Key.WALLET;
import static com.alphawallet.app.entity.DisplayState.TRANSFER_TO_ADDRESS;

public class TokenDetailActivity extends BaseActivity implements StandardFunctionInterface
{
    @Inject
    protected TokenFunctionViewModelFactory tokenFunctionViewModelFactory;
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
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_token_detail);
        viewModel = new ViewModelProvider(this, tokenFunctionViewModelFactory)
                .get(TokenFunctionViewModel.class);

        if (getIntent() != null && getIntent().getExtras() != null) {
            asset = getIntent().getExtras().getParcelable("asset");
            token = getIntent().getExtras().getParcelable("token");
            tokenId = new BigInteger(getIntent().getExtras().getString("tokenId"));
            initViews();
            toolbar();
            setTitle(token.getFullName());
            setupPage();
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
            viewModel.handleFunction(action, selection.get(0), token, this);
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
                Intent intent = new Intent(TokenDetailActivity.this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra("url", asset.getExternalLink());
                startActivity(intent);
            });
        } else {
            openExternal.setVisibility(View.GONE);
        }
    }

    private void setNameAndDesc(NFTAsset asset) {
        name.setText(asset.getName());
        desc.setText(asset.getDescription());
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
        intent.putExtra(TICKET, token);
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
}
