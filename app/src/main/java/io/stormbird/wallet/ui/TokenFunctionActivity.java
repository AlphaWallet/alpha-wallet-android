package io.stormbird.wallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import dagger.android.AndroidInjection;
import io.stormbird.token.entity.TSAction;
import io.stormbird.token.entity.TicketRange;
import io.stormbird.wallet.C;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.viewmodel.TokenFunctionViewModel;
import io.stormbird.wallet.viewmodel.TokenFunctionViewModelFactory;
import io.stormbird.wallet.web3.Web3TokenView;
import io.stormbird.wallet.web3.entity.PageReadyCallback;
import io.stormbird.wallet.widget.ProgressView;
import io.stormbird.wallet.widget.SystemView;

import javax.inject.Inject;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static io.stormbird.wallet.C.Key.TICKET;

/**
 * Created by James on 2/04/2019.
 * Stormbird in Singapore
 */
public class TokenFunctionActivity extends BaseActivity implements View.OnClickListener, Runnable, PageReadyCallback
{
    @Inject
    protected TokenFunctionViewModelFactory tokenFunctionViewModelFactory;
    private TokenFunctionViewModel viewModel;

    private Web3TokenView tokenView;
    private Token token;
    private Handler handler;
    private List<BigInteger> idList = null;
    private StringBuilder attrs;

    private void initViews() {
        token = getIntent().getParcelableExtra(TICKET);
        String displayIds = getIntent().getStringExtra(C.EXTRA_TOKEN_ID);
        RelativeLayout frameLayout = findViewById(R.id.layout_select_ticket);
        tokenView = findViewById(R.id.web3_tokenview);
        idList = token.stringHexToBigIntegerList(displayIds);

        TicketRange data = new TicketRange(idList, token.tokenInfo.address, false);

        token.displayTicketHolder(data, frameLayout, viewModel.getAssetDefinitionService(), this, false);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_script_view);

        viewModel = ViewModelProviders.of(this, tokenFunctionViewModelFactory)
                .get(TokenFunctionViewModel.class);
        SystemView systemView = findViewById(R.id.system_view);
        ProgressView progressView = findViewById(R.id.progress_view);
        systemView.hide();
        progressView.hide();

        initViews();
        toolbar();
        setTitle(getString(R.string.token_function));
        setupFunctions();
        viewModel.startGasPriceUpdate(token.tokenInfo.chainId);

        handler = new Handler();
    }

    private void setupFunctions()
    {
        Button[] buttons = new Button[3];
        buttons[1] = findViewById(R.id.button_use);
        buttons[0] = findViewById(R.id.button_sell);
        buttons[2] = findViewById(R.id.button_transfer);

        for (Button b : buttons) { b.setOnClickListener(this); }

        Map<String, TSAction> functions = viewModel.getAssetDefinitionService().getTokenFunctionMap(token.tokenInfo.chainId, token.getAddress());
        if (functions != null)
        {
            int index = 0;
            for (String name : functions.keySet())
            {
                buttons[index++].setText(name);
            }
        }

        if (!token.isERC875())
        {
            buttons[1].setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        handler.postDelayed(this, 300);
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
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.button_use:
            {
                viewModel.selectRedeemToken(this, token, idList);
            }
            break;
            case R.id.button_sell:
            {
                Map<String, TSAction> functions = viewModel.getAssetDefinitionService().getTokenFunctionMap(token.tokenInfo.chainId, token.getAddress());
                //this will be the user function
                if (functions == null || functions.size() == 0)
                {
                    viewModel.openUniversalLink(this, token, token.intArrayToString(idList, false));
                }
                else
                {
                    String buttonText = ((Button) v).getText().toString();
                    viewModel.showFunction(this, token, buttonText, idList);
                }
            }
            break;
            case R.id.button_transfer:
            {
                viewModel.showTransferToken(this, token, token.intArrayToString(idList, false));
            }
            break;
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        viewModel.stopGasSettingsFetch();
    }

    @Override
    public void run()
    {
        //adapter.notifyDataSetChanged();
    }

    @Override
    public void onPageLoaded()
    {
        tokenView.callToJS("refresh()");
    }
}
