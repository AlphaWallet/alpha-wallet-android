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
import com.alphawallet.app.entity.opensea.Asset;
import com.alphawallet.app.entity.opensea.Trait;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.util.KittyUtils;
import com.alphawallet.app.viewmodel.TokenFunctionViewModel;
import com.alphawallet.app.viewmodel.TokenFunctionViewModelFactory;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.ERC721ImageView;
import com.alphawallet.app.widget.FunctionButtonBar;
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
import static com.alphawallet.app.C.PRUNE_ACTIVITY;
import static com.alphawallet.app.entity.DisplayState.TRANSFER_TO_ADDRESS;

public class TokenDetailActivity extends BaseActivity implements StandardFunctionInterface
{
    @Inject
    protected TokenFunctionViewModelFactory tokenFunctionViewModelFactory;
    private TokenFunctionViewModel viewModel;

    private ERC721ImageView assetImage;
    private TextView title;
    private TextView name;
    private TextView desc;
    private TextView id;
    private TextView generation;
    private TextView cooldown;
    private TextView openExternal;
    private TextView labelAttributes;
    private GridLayout grid;
    private FunctionButtonBar functionBar;
    private Token token;
    private Asset asset;

    private void initViews() {
        title = findViewById(R.id.title);
        assetImage = findViewById(R.id.layout_image);
        name = findViewById(R.id.name);
        desc = findViewById(R.id.description);
        id = findViewById(R.id.id);
        generation = findViewById(R.id.generation);
        cooldown = findViewById(R.id.cooldown);
        openExternal = findViewById(R.id.open_external);
        labelAttributes = findViewById(R.id.label_attributes);
        grid = findViewById(R.id.grid);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        grid.setUseDefaultMargins(false);
        functionBar = findViewById(R.id.layoutButtons);
        if (functionBar != null)
        {
            List<BigInteger> selection = new ArrayList<>();
            selection.add(new BigInteger(asset.getTokenId(16), 16));
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
        setTraits(asset);
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

    private void setTraits(Asset asset) {
        if (asset.getTraits() != null && !asset.getTraits().isEmpty()) {
            if (asset.getAssetContract().getName().equals("CryptoKitties")) {
                labelAttributes.setText(R.string.label_cattributes);
            } else {
                labelAttributes.setText(R.string.label_attributes);
            }
            for (Trait trait : asset.getTraits()) {
                View attributeView = View.inflate(this, R.layout.item_attribute, null);
                TextView traitType = attributeView.findViewById(R.id.trait);
                TextView traitValue = attributeView.findViewById(R.id.value);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                        GridLayout.spec(GridLayout.UNDEFINED, 1f),
                        GridLayout.spec(GridLayout.UNDEFINED, 1f));
                attributeView.setLayoutParams(params);
                traitType.setText(trait.getTraitType());
                traitValue.setText(trait.getValue());
                grid.addView(attributeView);
            }
        } else {
            labelAttributes.setVisibility(View.GONE);
        }
    }

    private void setExternalLink(Asset asset) {
        if (asset.getExternalLink() != null && !asset.getExternalLink().equals("null")) {
            openExternal.setText(getString(R.string.open_on_external_link,
                    asset.getAssetContract().getName()));

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

    private void setNameAndDesc(Asset asset) {
        name.setText(asset.getName());
        desc.setText(asset.getDescription());
    }

    private void setDetails(Asset asset) {
        id.setText(asset.getTokenId());
        if (asset.getTraitFromType("generation") != null) {
            generation.setText(String.format("Gen %s",
                    asset.getTraitFromType("generation").getValue()));
        } else if (asset.getTraitFromType("gen") != null) {
            generation.setText(String.format("Gen %s",
                    asset.getTraitFromType("gen").getValue()));
        } else {
            generation.setVisibility(View.GONE);
        }

        if (asset.getTraitFromType("cooldown_index") != null) {
            cooldown.setText(String.format("%s Cooldown",
                    KittyUtils.parseCooldownIndex(
                            asset.getTraitFromType("cooldown_index").getValue())));
        } else if (asset.getTraitFromType("cooldown") != null) { // Non-CK
            cooldown.setText(String.format("%s Cooldown",
                    asset.getTraitFromType("cooldown").getValue()));
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
        intent.putExtra(EXTRA_TOKENID_LIST, asset.getTokenId(16));
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
