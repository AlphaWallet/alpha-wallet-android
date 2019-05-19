package io.stormbird.wallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import dagger.android.AndroidInjection;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.token.entity.NonFungibleToken;
import io.stormbird.token.entity.TSAction;
import io.stormbird.token.entity.TicketRange;
import io.stormbird.token.entity.TokenScriptResult;
import io.stormbird.token.util.DateTime;
import io.stormbird.token.util.DateTimeFactory;
import io.stormbird.wallet.C;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.Ticket;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.viewmodel.TokenFunctionViewModel;
import io.stormbird.wallet.viewmodel.TokenFunctionViewModelFactory;
import io.stormbird.wallet.web3.Web3TokenView;
import io.stormbird.wallet.web3.entity.Address;
import io.stormbird.wallet.web3.entity.PageReadyCallback;
import io.stormbird.wallet.widget.ProgressView;
import io.stormbird.wallet.widget.SystemView;

import javax.inject.Inject;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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

    private Token token;
    private Handler handler;
    private Web3TokenView tokenView;
    private ProgressBar waitSpinner;
    private List<BigInteger> idList = null;
    private StringBuilder attrs;

    private void initViews() {
        token = getIntent().getParcelableExtra(TICKET);
        String displayIds = getIntent().getStringExtra(C.EXTRA_TOKEN_ID);
        tokenView = findViewById(R.id.web3_tokenview);
        waitSpinner = findViewById(R.id.progress_element);
        if (token instanceof Ticket) //TODO: NFT flag
        {
            idList = token.stringHexToBigIntegerList(displayIds);
        }

        tokenView.setChainId(token.tokenInfo.chainId);
        tokenView.setWalletAddress(new Address(token.getWallet()));
        tokenView.setRpcUrl(token.tokenInfo.chainId);
        tokenView.setOnReadyCallback(this);

        try
        {
            waitSpinner.setVisibility(View.VISIBLE);
            tokenView.setVisibility(View.GONE);
            getAttrs(idList);
        }
        catch (Exception e)
        {
            fillEmpty();
        }
    }

    private void fillEmpty()
    {
        tokenView.loadData("<html><body>No Data</body></html>", "text/html", "utf-8");
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_script_view2);

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

        handler = new Handler();
    }

    private void updateView()
    {
        //systemView.hide();
    }

    private void setupFunctions()
    {
        Button[] buttons = new Button[3];
        buttons[0] = findViewById(R.id.button_use);
        buttons[1] = findViewById(R.id.button_sell);
        buttons[2] = findViewById(R.id.button_transfer);

        for (Button b : buttons) { b.setOnClickListener(this); }

        Map<String, TSAction> functions = viewModel.getAssetDefinitionService().getTokenFunctionMap(token.tokenInfo.chainId, token.getAddress());
        if (functions != null)
        {
            int index = 0;
            for (String name : functions.keySet())
            {
                buttons[index].setText(name);
            }
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
                Map<String, TSAction> functions = viewModel.getAssetDefinitionService().getTokenFunctionMap(token.tokenInfo.chainId, token.getAddress());
                //this will be the user function
                if (functions.size() == 0)
                {
                    viewModel.showRedeemToken(this, token, idList);
                }
                else
                {
                    String buttonText = ((Button) v).getText().toString();
                    viewModel.showFunction(this, token, buttonText, idList);
                }
            }
            break;
            case R.id.button_sell:
            {
                viewModel.sellTicketRouter(this, token);
            }
            break;
            case R.id.button_transfer:
            {
                viewModel.showTransferToken(this, token);
            }
            break;
        }
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

    private void getAttrs(List<BigInteger> tokenIds) throws Exception
    {
        BigInteger tokenId = tokenIds.get(0);
        attrs = viewModel.getAssetDefinitionService().getTokenAttrs(token, tokenIds.size());
        viewModel.getAssetDefinitionService().resolveAttrs(token, tokenId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onAttr, this::onError, () -> displayFunction(attrs.toString()))
                .wait();
    }

    private void displayFunction(String tokenAttrs)
    {
        waitSpinner.setVisibility(View.GONE);
        tokenView.setVisibility(View.VISIBLE);

        String view = viewModel.getAssetDefinitionService().getTokenView(token.tokenInfo.chainId, token.getAddress(), "view");
        String style = viewModel.getAssetDefinitionService().getTokenView(token.tokenInfo.chainId, token.getAddress(), "style");
        String viewData = tokenView.injectWeb3TokenInit(this, view, tokenAttrs);
        viewData = tokenView.injectStyleData(viewData, style); //style injected last so it comes first

        tokenView.loadData(viewData, "text/html", "utf-8");
    }

    private void onError(Throwable throwable)
    {
        throwable.printStackTrace();
    }

    private void onAttr(TokenScriptResult.Attribute attribute)
    {
        //add to string
        try
        {
            viewModel.getAssetDefinitionService().addPair(attrs, attribute.id, attribute.text);
        }
        catch (ParseException e)
        {
            e.printStackTrace();
        }
    }
}
