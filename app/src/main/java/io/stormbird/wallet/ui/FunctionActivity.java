package io.stormbird.wallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.ValueCallback;
import android.widget.Button;
import android.widget.ProgressBar;
import dagger.android.AndroidInjection;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.token.entity.*;
import io.stormbird.token.tools.Numeric;
import io.stormbird.token.tools.TokenDefinition;
import io.stormbird.token.util.DateTime;
import io.stormbird.token.util.DateTimeFactory;
import io.stormbird.wallet.C;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.DAppFunction;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.ui.widget.adapter.NonFungibleTokenAdapter;
import io.stormbird.wallet.util.BalanceUtils;
import io.stormbird.wallet.util.KeyboardUtils;
import io.stormbird.wallet.util.Utils;
import io.stormbird.wallet.viewmodel.TokenFunctionViewModel;
import io.stormbird.wallet.viewmodel.TokenFunctionViewModelFactory;
import io.stormbird.wallet.web3.OnSignPersonalMessageListener;
import io.stormbird.wallet.web3.Web3TokenView;
import io.stormbird.wallet.web3.entity.Address;
import io.stormbird.wallet.web3.entity.FunctionCallback;
import io.stormbird.wallet.web3.entity.Message;
import io.stormbird.wallet.web3.entity.PageReadyCallback;
import io.stormbird.wallet.widget.ProgressView;
import io.stormbird.wallet.widget.SignMessageDialog;
import io.stormbird.wallet.widget.SystemView;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;

import javax.inject.Inject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SignatureException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.stormbird.wallet.C.Key.TICKET;
import static io.stormbird.wallet.entity.CryptoFunctions.sigFromByteArray;
import static io.stormbird.wallet.ui.DappBrowserFragment.PERSONAL_MESSAGE_PREFIX;

/**
 * Created by James on 4/04/2019.
 * Stormbird in Singapore
 */
public class FunctionActivity extends BaseActivity implements View.OnClickListener, FunctionCallback,
        Runnable, PageReadyCallback, OnSignPersonalMessageListener
{
    @Inject
    protected TokenFunctionViewModelFactory viewModelFactory;
    private TokenFunctionViewModel viewModel;

    private Token token;
    private BigInteger tokenId;
    private String actionMethod;
    private SystemView systemView;
    private Web3TokenView tokenView;
    private ProgressBar waitSpinner;
    private Handler handler;
    private SignMessageDialog dialog;
    private String functionEffect;
    private Map<String, String> args = new HashMap<>();
    private StringBuilder attrs;

    private void initViews() {
        token = getIntent().getParcelableExtra(TICKET);
        actionMethod = getIntent().getStringExtra(C.EXTRA_STATE);
        String tokenIdStr = getIntent().getStringExtra(C.EXTRA_TOKEN_ID);
        if (tokenIdStr == null || tokenIdStr.length() == 0) tokenIdStr = "0";
        tokenId = new BigInteger(tokenIdStr, Character.MAX_RADIX);

        tokenView = findViewById(R.id.web3_tokenview);
        waitSpinner = findViewById(R.id.progress_element);

        tokenView.setChainId(token.tokenInfo.chainId);
        tokenView.setWalletAddress(new Address(token.getWallet()));
        tokenView.setRpcUrl(token.tokenInfo.chainId);
        tokenView.setOnReadyCallback(this);
        tokenView.setOnSignPersonalMessageListener(this);
        tokenView.setVisibility(View.GONE);
        waitSpinner.setVisibility(View.VISIBLE);

        getAttrs();
    }

    private void displayFunction(String tokenAttrs)
    {
        try
        {
            waitSpinner.setVisibility(View.GONE);
            tokenView.setVisibility(View.VISIBLE);
            Map<String, TSAction> functions = viewModel.getAssetDefinitionService().getTokenFunctionMap(token.tokenInfo.chainId, token.getAddress());
            TSAction action = functions.get(actionMethod);
            String magicValues = viewModel.getAssetDefinitionService().getMagicValuesForInjection(token.tokenInfo.chainId);
            String injectedView = tokenView.injectWeb3TokenInit(this, action.view, tokenAttrs);
            injectedView = tokenView.injectJSAtEnd(injectedView, magicValues);
            if (action.style != null) injectedView = tokenView.injectStyleData(injectedView, action.style);

            tokenView.loadData(injectedView, "text/html", "utf-8");
        }
        catch (Exception e)
        {
            fillEmpty();
        }
    }

    private void getAttrs()
    {
        try
        {
            attrs = viewModel.getAssetDefinitionService().getTokenAttrs(token, 1);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        viewModel.getAssetDefinitionService().resolveAttrs(token, tokenId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onAttr, this::onError, () -> displayFunction(attrs.toString()))
                .isDisposed();
    }

    private void onError(Throwable throwable)
    {
        throwable.printStackTrace();
        displayFunction(attrs.toString());
    }

    private void onAttr(TokenScriptResult.Attribute attribute)
    {
        try
        {
            TokenScriptResult.addPair(attrs, attribute.id, attribute.text);
        }
        catch (ParseException e)
        {
            e.printStackTrace();
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

        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(TokenFunctionViewModel.class);
        systemView = findViewById(R.id.system_view);
        ProgressView progressView = findViewById(R.id.progress_view);
        handler = new Handler();
        systemView.hide();
        progressView.hide();

        findViewById(R.id.layout_webwrapper).setBackgroundResource(R.drawable.background_card);

        initViews();
        toolbar();
        setTitle(getString(R.string.token_function));
        setupFunctions();
    }

    private void setupFunctions()
    {
        Button[] buttons = new Button[3];
        buttons[0] = findViewById(R.id.button_use);
        buttons[1] = findViewById(R.id.button_sell);
        buttons[2] = findViewById(R.id.button_transfer);

        for (Button b : buttons)
        {
            b.setVisibility(View.GONE);
            b.setOnClickListener(this);
        }

        buttons[0].setVisibility(View.VISIBLE);
        buttons[0].setText("Confirm");
    }

    @Override
    public void onResume() {
        super.onResume();
        handler.postDelayed(this, 1000);
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
            //get challenge from the source
            case R.id.button_use:
                //Sign is default, if there's a transaction then push this
                handleConfirmClick();
                break;
            default:
                break;
        }
    }

    private void handleConfirmClick()
    {
        Map<String, TSAction> functions = viewModel.getAssetDefinitionService().getTokenFunctionMap(token.tokenInfo.chainId, token.getAddress());
        TSAction action = functions.get(actionMethod);

        if (action.function != null)
        {
            //check params for function.
            //if there's input params, resolve them
            boolean resolved = checkNativeTransaction(action, true);
            resolved = checkFunctionArgs(action, resolved);

            if (resolved)
            {
                handleFunction(action);
            }
        }
        else
        {
            tokenView.callToJS("onConfirm" + "('sig')");
        }
    }

    private boolean checkNativeTransaction(TSAction action, boolean resolved)
    {
        FunctionDefinition function = action.function;
        if (function.tx == null) return true;
        for (TokenscriptElement e : function.tx.args.values())
        {
            resolved = checkTokenScriptElement(action, e, resolved);
        }

        return resolved;
    }

    private boolean checkFunctionArgs(TSAction action, boolean resolved)
    {
        for (MethodArg arg : action.function.parameters)
        {
            resolved = checkTokenScriptElement(action, arg.element, resolved);
        }

        return resolved;
    }

    private boolean checkTokenScriptElement(TSAction action, TokenscriptElement e, boolean resolved)
    {
        if (e.ref != null && e.ref.length() > 0)
        {
            AttributeType attr = action.attributeTypes.get(e.ref);
            if (attr == null) return true;
            switch (attr.as)
            {
                case UnsignedInput:
                    //do we have a mapping?
                    String valueFromInput = args.get(e.ref);
                    if (valueFromInput == null)
                    {
                        //fetch mapping
                        args.put(e.ref, "__searching");
                        getInput(e.ref);
                        return false;
                    }
                    else if (valueFromInput.equals("__searching"))
                    {
                        //display error
                        System.out.println("ERROR!!!");
                        resolved = false;
                    }
                    else
                    {
                        BigDecimal unsignedValue = new BigDecimal(valueFromInput);
                        //handle value
                        if (attr.bitshift > 0)
                        {
                            functionEffect = unsignedValue.toString();
                            unsignedValue = unsignedValue.movePointRight(attr.bitshift);
                            args.put(e.ref, unsignedValue.toString());
                        }
                        e.value = unsignedValue.toString();
                    }
                    break;
                default:
                    resolved = false;
                    break;
            }
        }

        return resolved;
    }

    private void handleFunction(TSAction action)
    {
        if (action.function.tx != null)
        {
            //this is a native style transaction
            NativeSend(action);
        }
        else
        {
            String functionData = viewModel.getTransactionBytes(token, tokenId, action.function);
            //confirm the transaction
            ContractAddress cAddr = new ContractAddress(action.function, token.tokenInfo.chainId, token.tokenInfo.address); //viewModel.getAssetDefinitionService().getContractAddress(action.function, token);

            functionEffect = functionEffect + " to " + actionMethod;

            viewModel.confirmTransaction(this, cAddr.chainId, functionData, null, cAddr.address, actionMethod, functionEffect);
        }
    }

    private void NativeSend(TSAction action)
    {
        boolean isValid = true;
        KeyboardUtils.hideKeyboard(getCurrentFocus());
        FunctionDefinition function = action.function;

        //check we have a 'to' and a 'value'
        if (!(function.tx.args.containsKey("to") && function.tx.args.containsKey("value")))
        {
            isValid = false;
            //TODO: Display error
        }

        //calculate native amount
        BigDecimal value = new BigDecimal(function.tx.args.get("value").value);

        if (token.balance.subtract(value).compareTo(BigDecimal.ZERO) < 0)
        {
            //amountInput.setError(R.string.error_insufficient_funds);
            isValid = false;
        }

        String to = function.tx.args.get("to").value;
        if (to == null || !Utils.isAddressValid(to))
        {
            isValid = false;
            return;
        }

        //eg Send 2(*1) ETH(*2) to Alex's Amazing Coffee House(*3) (0xdeadacec0ffee(*4))
        String extraInfo = String.format(getString(R.string.tokenscript_send_native), functionEffect, token.tokenInfo.symbol, actionMethod, to);

        if (isValid) {
            viewModel.confirmNativeTransaction(this, to, value, token, extraInfo);
        }
    }

    private void getInput(String value)
    {
        tokenView.evaluateJavascript(
                "(function() { var x = document.getElementById(\"" + value + "\");\n" +
                        "            return x.value; })();",
                html -> {
                    StringBuilder sb = new StringBuilder();
                    for (char ch : html.toCharArray()) if (ch!='\"') sb.append(ch);
                    args.put(value, sb.toString());
                    handleConfirmClick();
                }
        );
    }

    @Override
    public void signMessage(byte[] sign, DAppFunction dAppFunction, Message<String> message)
    {
        viewModel.signMessage(sign, dAppFunction, message, token.tokenInfo.chainId, token.getWallet());
    }

    @Override
    public void run()
    {

    }

    @Override
    public void onPageLoaded()
    {

    }

    @Override
    public void onSignPersonalMessage(Message<String> message)
    {
        DAppFunction dAppFunction = new DAppFunction() {
            @Override
            public void DAppError(Throwable error, Message<String> message) {
                tokenView.onSignCancel(message);
                dialog.dismiss();
            }

            @Override
            public void DAppReturn(byte[] data, Message<String> message) {
                String signHex = Numeric.toHexString(data);
                signHex = Numeric.cleanHexPrefix(signHex);
                tokenView.onSignPersonalMessageSuccessful(message, signHex);
                testRecoverAddressFromSignature(message.value, signHex);
                dialog.dismiss();
            }
        };

        dialog = new SignMessageDialog(this, message);
        dialog.setAddress(token.getAddress());
        dialog.setMessage(message.value);
        dialog.setOnApproveListener(v -> {
            String convertedMessage = message.value;
            String signMessage = PERSONAL_MESSAGE_PREFIX
                    + convertedMessage.length()
                    + convertedMessage;
            signMessage(signMessage.getBytes(), dAppFunction, message);
        });
        dialog.setOnRejectListener(v -> {
            tokenView.onSignCancel(message);
            dialog.dismiss();
        });
        dialog.show();
    }

    public void testRecoverAddressFromSignature(String message, String sig)
    {
        String prefix = PERSONAL_MESSAGE_PREFIX + message.length();
        byte[] msgHash = (prefix + message).getBytes();
        String msgBytes = Numeric.toHexString(msgHash);
        System.out.println(msgBytes);

        byte[] equivHash = Hash.sha3(msgHash);
        String hashBytes = Numeric.toHexString(equivHash);
        System.out.println(hashBytes);

        byte[] signatureBytes = Numeric.hexStringToByteArray(sig);
        Sign.SignatureData sd = sigFromByteArray(signatureBytes);
        String addressRecovered;

        try
        {
            BigInteger recoveredKey = Sign.signedMessageToKey(msgHash, sd);
            addressRecovered = "0x" + Keys.getAddress(recoveredKey);
            System.out.println("Recovered: " + addressRecovered);
        }
        catch (SignatureException e)
        {
            e.printStackTrace();
        }
    }
}
