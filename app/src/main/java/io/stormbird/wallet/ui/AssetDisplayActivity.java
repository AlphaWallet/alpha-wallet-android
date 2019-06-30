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
import dagger.android.AndroidInjection;
import io.stormbird.token.entity.FunctionDefinition;
import io.stormbird.token.entity.TSAction;
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
import io.stormbird.wallet.widget.ProgressView;
import io.stormbird.wallet.widget.SystemView;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static android.os.VibrationEffect.DEFAULT_AMPLITUDE;
import static io.stormbird.wallet.C.Key.TICKET;

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

        setTitle(getString(R.string.title_show_tickets));
        systemView = findViewById(R.id.system_view);
        systemView.hide();
        progressView = findViewById(R.id.progress_view);
        progressView.hide();
        SwipeRefreshLayout refreshLayout = findViewById(R.id.refresh_layout);
        systemView.attachSwipeRefreshLayout(refreshLayout);
        refreshLayout.setOnRefreshListener(this::refreshAssets);
        
        list = findViewById(R.id.listTickets);

        viewModel = ViewModelProviders.of(this, assetDisplayViewModelFactory)
                .get(AssetDisplayViewModel.class);

        viewModel.queueProgress().observe(this, progressView::updateProgress);
        viewModel.pushToast().observe(this, this::displayToast);
        viewModel.ticket().observe(this, this::onTokenUpdate);

        adapter = new NonFungibleTokenAdapter(this, token, viewModel.getAssetDefinitionService(), viewModel.getOpenseaService());
        if (token instanceof ERC721Token)
        {
            findViewById(R.id.button_use).setVisibility(View.GONE);
            findViewById(R.id.button_sell).setVisibility(View.GONE);
        }

        findViewById(R.id.button_transfer).setOnClickListener(this);
        findViewById(R.id.button_use).setOnClickListener(this);

        list.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.button_sell).setOnClickListener(this);
        list.setAdapter(adapter);
        list.setHapticFeedbackEnabled(true);

        finishReceiver = new FinishReceiver(this);
        checkForTokenScript();
    }

    /**
     * Hide the button bar, and check if it's not ERC875: if not then hide the 'use' button.
     * TODO: need to populate button bar as per ERC20 button bar
     */
    private void checkForTokenScript()
    {
        findViewById(R.id.layoutButtons).setVisibility(View.GONE);

        final Button[] buttons = new Button[3];
        buttons[1] = findViewById(R.id.button_use);
        buttons[0] = findViewById(R.id.button_sell);

        Map<String, TSAction> functions = viewModel.getAssetDefinitionService().getTokenFunctionMap(token.tokenInfo.chainId, token.getAddress());
        if (functions != null && functions.size() > 0)
        {
            int index = 0;
            for (String function : functions.keySet())
            {
                buttons[index].setVisibility(View.VISIBLE);
                buttons[index].setText(function);
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

            switch (v.getId())
            {
                case R.id.button_transfer:
                {
                    viewModel.showTransferToken(this, token, selection);
                }
                break;

                default:
                    Button b = findViewById(v.getId());
                    handleUseClick(b.getText().toString(), v.getId());
                    break;
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
                dialog.setMessage(getString(R.string.token_requirement, action.function.getTokenRequirement()));
                dialog.setButtonText(R.string.dialog_ok);
                dialog.setButtonListener(v -> dialog.dismiss());
                dialog.show();
            }
            else
            {
                //handle TS function
                Intent intent = new Intent(this, FunctionActivity.class);
                intent.putExtra(TICKET, token);
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
            VibrationEffect vibe = VibrationEffect.createOneShot(200, DEFAULT_AMPLITUDE);
            vb.vibrate(vibe);
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
