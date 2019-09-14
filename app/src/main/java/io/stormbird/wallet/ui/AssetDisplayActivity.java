package io.stormbird.wallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.*;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import javax.inject.Inject;

import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import dagger.android.AndroidInjection;
import io.stormbird.token.entity.FunctionDefinition;
import io.stormbird.token.entity.TSAction;
import io.stormbird.token.entity.XMLDsigDescriptor;
import io.stormbird.token.tools.TokenDefinition;
import io.stormbird.wallet.C;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.ERC721Token;
import io.stormbird.wallet.entity.FinishReceiver;
import io.stormbird.wallet.entity.Ticket;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.TokenInfo;
import io.stormbird.wallet.ui.widget.OnTokenClickListener;
import io.stormbird.wallet.ui.widget.adapter.NonFungibleTokenAdapter;
import io.stormbird.wallet.viewmodel.AssetDisplayViewModel;
import io.stormbird.wallet.viewmodel.AssetDisplayViewModelFactory;
import io.stormbird.wallet.widget.AWalletAlertDialog;
import io.stormbird.wallet.widget.CertifiedToolbarView;
import io.stormbird.wallet.widget.ProgressView;
import io.stormbird.wallet.widget.SystemView;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static android.os.VibrationEffect.DEFAULT_AMPLITUDE;
import static io.stormbird.wallet.C.Key.TICKET;
import static io.stormbird.wallet.C.Key.WALLET;

/**
 * Created by James on 22/01/2018.
 */

/**
 *
 */
public class AssetDisplayActivity extends BaseActivity implements OnTokenClickListener, View.OnClickListener, Runnable
{
    @Inject
    protected AssetDisplayViewModelFactory assetDisplayViewModelFactory;
    private AssetDisplayViewModel viewModel;
    private SystemView systemView;
    private ProgressView progressView;
    private RecyclerView list;
    private FinishReceiver finishReceiver;
    private CertifiedToolbarView toolbarView;
    private Token token;
    private NonFungibleTokenAdapter adapter;
    private String balance = null;
    private List<BigInteger> selection = new ArrayList<>();
    private Handler handler;
    private boolean activeClick;
    private AWalletAlertDialog dialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        AndroidInjection.inject(this);

        token = getIntent().getParcelableExtra(TICKET);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_asset_display);
        toolbar();

        setTitle(getString(R.string.empty));
        systemView = findViewById(R.id.system_view);
        systemView.hide();
        progressView = findViewById(R.id.progress_view);
        progressView.hide();
        SwipeRefreshLayout refreshLayout = findViewById(R.id.refresh_layout);
        systemView.attachSwipeRefreshLayout(refreshLayout);
        refreshLayout.setOnRefreshListener(this::refreshAssets);
        
        list = findViewById(R.id.listTickets);
        toolbarView = findViewById(R.id.toolbar);

        viewModel = ViewModelProviders.of(this, assetDisplayViewModelFactory)
                .get(AssetDisplayViewModel.class);

        viewModel.queueProgress().observe(this, progressView::updateProgress);
        viewModel.pushToast().observe(this, this::displayToast);
        viewModel.ticket().observe(this, this::onTokenUpdate);
        viewModel.sig().observe(this, this::onSigData);

        adapter = new NonFungibleTokenAdapter(this, token, viewModel.getAssetDefinitionService(), viewModel.getOpenseaService());
        if (token instanceof ERC721Token)
        {
            findViewById(R.id.button_use).setVisibility(View.GONE);
            findViewById(R.id.button_sell).setVisibility(View.GONE);
        }

        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        list.setHapticFeedbackEnabled(true);

        finishReceiver = new FinishReceiver(this);
        checkForTokenScript();
        findViewById(R.id.certificate_spinner).setVisibility(View.VISIBLE);
        viewModel.checkTokenScriptValidity(token);
    }

    /**
     * Received Signature data either cached from AssetDefinitionService or from the API call
     * @param sigData
     */
    private void onSigData(XMLDsigDescriptor sigData)
    {
        toolbarView.onSigData(sigData);
        adapter.notifyItemChanged(0); //notify issuer update
    }

    /**
     * Hide the button bar, and check if it's not ERC875: if not then hide the 'use' button.
     * TODO: need to populate button bar as per ERC20 button bar
     */
    private void checkForTokenScript()
    {
        findViewById(R.id.layoutButtons).setVisibility(View.GONE);

        LinearLayout auxButtons = findViewById(R.id.buttons_aux);

        final Button[] buttons = new Button[6];
        buttons[0] = findViewById(R.id.button_use);
        buttons[4] = findViewById(R.id.button_sell);
        buttons[1] = findViewById(R.id.button_action1);
        buttons[2] = findViewById(R.id.button_action2);
        buttons[3] = findViewById(R.id.button_action3);
        buttons[5] = findViewById(R.id.button_transfer);

        for (Button b : buttons) b.setOnClickListener(this);

        Map<String, TSAction> functions = viewModel.getAssetDefinitionService().getTokenFunctionMap(token.tokenInfo.chainId, token.getAddress());
        if (functions != null && functions.size() > 0)
        {
            int index = 0;
            for (String function : functions.keySet())
            {
                buttons[index].setVisibility(View.VISIBLE);
                buttons[index].setText(function);
                if (index == 1) auxButtons.setVisibility(View.VISIBLE);
                if (index == 4) break;
                index++;
            }
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        viewModel.prepare(token);
        handler = new Handler();
        activeClick = false;
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(finishReceiver);
    }

    private void onTokenUpdate(Token t)
    {
        if (t instanceof Ticket)
        {
            token = t;
            if (!t.getFullBalance().equals(balance))
            {
                adapter.setToken(token);
                RecyclerView list = findViewById(R.id.listTickets);
                list.setAdapter(null);
                list.setAdapter(adapter);
                balance = token.getFullBalance();
            }
        }
    }

    /**
     * Useful for volatile assets, this will refresh any volatile data in the token eg dynamic content or images
     */
    private void refreshAssets()
    {
        adapter.reloadAssets(this);
        systemView.hide();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_qr, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_qr) {
            viewModel.showContractInfo(this, token);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v)
    {
        if (!activeClick)
        {
            activeClick = true;
            handler.postDelayed(this, 500);

            Map<String, TSAction> functions = viewModel.getAssetDefinitionService().getTokenFunctionMap(token.tokenInfo.chainId, token.getAddress());
            //this will be the user function
            String buttonText = ((Button) v).getText().toString();
            if (functions.containsKey(buttonText))
            {
                handleUseClick(buttonText, v.getId());
            }
            else
            {
                switch (v.getId())
                {
                    case R.id.button_use:
                        viewModel.selectRedeemTokens(this, token, selection);
                        break;
                    case R.id.button_sell:
                        viewModel.sellTicketRouter(this, token, token.intArrayToString(selection, false));
                        break;
                    case R.id.button_transfer:
                        viewModel.showTransferToken(this, token, selection);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private boolean hasCorrectTokens(TSAction action)
    {
        //get selected tokens
        List<BigInteger> selected = adapter.getSelectedTokenIds(selection);
        int groupings = adapter.getSelectedGroups();
        if (action.function != null)
        {
            int requiredCount = action.function.getTokenRequirement();
            if (requiredCount == 1 && selected.size() > 1 && groupings == 1)
            {
                BigInteger first = selected.get(0);
                selected.clear();
                selected.add(first);
            }

            return selected.size() == requiredCount;
        }
        return true;
    }

    private void handleUseClick(String function, int useId)
    {
        //first see if this is override function
        Map<String, TSAction> functions = viewModel.getAssetDefinitionService().getTokenFunctionMap(token.tokenInfo.chainId, token.getAddress());
        if (functions != null && functions.containsKey(function))
        {
            TSAction action = functions.get(function);
            //ensure we have sufficient tokens for selection
            if (!hasCorrectTokens(action))
            {
                if (dialog == null) dialog = new AWalletAlertDialog(this);
                dialog.setIcon(AWalletAlertDialog.ERROR);
                dialog.setTitle(R.string.token_selection);
                dialog.setMessage(getString(R.string.token_requirement, String.valueOf(action.function.getTokenRequirement())));
                dialog.setButtonText(R.string.dialog_ok);
                dialog.setButtonListener(v -> dialog.dismiss());
                dialog.show();
            }
            else
            {
                //handle TS function
                Intent intent = new Intent(this, FunctionActivity.class);
                intent.putExtra(TICKET, token);
                intent.putExtra(WALLET, viewModel.defaultWallet().getValue());
                intent.putExtra(C.EXTRA_STATE, function);
                intent.putExtra(C.EXTRA_TOKEN_ID, token.intArrayToString(adapter.getSelectedTokenIds(selection), true));
                intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                startActivity(intent);
            }
        }
        else if (token.isERC875())
        {
            switch (useId)
            {
                case R.id.button_sell:
                    viewModel.sellTicketRouter(this, token, token.intArrayToString(selection, false));
                    break;
                case R.id.button_use:
                    viewModel.selectRedeemTokens(this, token, selection);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onTokenClick(View v, Token token, List<BigInteger> tokenIds, boolean selected)
    {
        Map<String, TSAction> functions = viewModel.getAssetDefinitionService().getTokenFunctionMap(token.tokenInfo.chainId, token.getAddress());
        int maxSelect = 1;

        if (!selected && tokenIds.containsAll(selection))
        {
            selection = new ArrayList<>();
        }

        if (!selected) return;

        if (functions != null)
        {
            for (TSAction action : functions.values())
            {
                if (action.function != null && action.function.getTokenRequirement() > maxSelect)
                {
                    maxSelect = action.function.getTokenRequirement();
                }
            }
        }

        if (maxSelect <= 1)
        {
            selection = tokenIds;
            adapter.setRadioButtons(true);
        }
    }

    @Override
    public void onLongTokenClick(View view, Token token, List<BigInteger> tokenIds)
    {
        //show radio buttons of all token groups
        adapter.setRadioButtons(true);

        selection = tokenIds;
        Vibrator vb = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vb != null && vb.hasVibrator())
        {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                VibrationEffect vibe = VibrationEffect.createOneShot(200, DEFAULT_AMPLITUDE);
                vb.vibrate(vibe);
            }
            else
            {
                //noinspection deprecation
                vb.vibrate(200);
            }
        }

        if (findViewById(R.id.layoutButtons).getVisibility() != View.VISIBLE)
        {
            findViewById(R.id.layoutButtons).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void run()
    {
        activeClick = false;
    }
}
