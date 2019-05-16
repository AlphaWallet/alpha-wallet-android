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
import dagger.android.AndroidInjection;
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
    private String actionMethod;
    private SystemView systemView;
    private Web3TokenView tokenView;
    private Handler handler;
    private SignMessageDialog dialog;
    private Map<String, String> args = new HashMap<>();

    private void initViews() {
        token = getIntent().getParcelableExtra(TICKET);
        actionMethod = getIntent().getStringExtra(C.EXTRA_STATE);

        tokenView = findViewById(R.id.web3_tokenview);

        tokenView.setChainId(token.tokenInfo.chainId);
        tokenView.setWalletAddress(new Address(token.getWallet()));
        tokenView.setRpcUrl(token.tokenInfo.chainId);
        tokenView.setOnReadyCallback(this);
        tokenView.setOnSignPersonalMessageListener(this);

        try
        {
            String tokenAttrs = buildTokenAttrs(null);
            Map<String, TSAction> functions = viewModel.getAssetDefinitionService().getTokenFunctionMap(token.tokenInfo.chainId, token.getAddress());
            TSAction action = functions.get(actionMethod);
            String injectedView = tokenView.injectWeb3TokenInit(this, action.view, tokenAttrs);
            if (action.style != null) injectedView = tokenView.injectStyleData(injectedView, action.style);

            tokenView.loadData(injectedView, "text/html", "utf-8");
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

    private void updateView()
    {
        systemView.hide();
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
            boolean resolved = true;
            for (MethodArg arg : action.function.parameters)
            {
                if (arg.ref != null && arg.ref.length() > 0)
                {
                    AttributeType attr = action.attributeTypes.get(arg.ref);
                    if (attr == null) continue;
                    switch (attr.as)
                    {
                        case UnsignedInput:
                            //do we have a mapping?
                            String valueFromInput = args.get(arg.ref);
                            if (valueFromInput == null)
                            {
                                //fetch mapping
                                resolved = false;
                                args.put(arg.ref, "__searching");
                                getInput(arg.ref);
                                return;
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
                                    unsignedValue = unsignedValue.movePointRight(attr.bitshift);
                                    args.put(arg.ref, unsignedValue.toString());
                                }
                                arg.value = unsignedValue.toString();
                            }
                            break;
                        default:
                            resolved = false;
                            break;
                    }
                }
            }

            if (resolved)
            {
                handleFunction(action.function);
            }

            //String inputVal = getInput("amount");
//            tokenView.evaluateJavascript(
//                    "(function() { var x = document.getElementById(\"amount\");\n" +
//                            "            return x.elements[0].value; })();",
//                    html -> {
//                        StringBuilder sb = new StringBuilder();
//                        for (char ch : html.toCharArray()) if (ch!='\"') sb.append(ch);
//                        BigDecimal bd = new BigDecimal(sb.toString());
//                        System.out.println(bd.toString());
//
//                        //now process the transaction
//                        String method = action.function.method;
//                        MethodArg param1 = action.function.parameters.get(0);
//                        MethodArg param2 = action.function.parameters.get(1);
//
//                        //how to resolve a reference?
//                        if (param2.ref != null && param2.ref.length() > 0)
//                        {
//                            AttributeType attr = action.attributeTypes.get(param2.ref);
//                            System.out.println(attr.name);
//                        }
//
//                        System.out.println(method + " " + param1.ref + " " + param2.ref);
//                    }
//            );
        }
        else
        {
            tokenView.callToJS("onConfirm" + "('sig')");
        }
    }

    private void handleFunction(FunctionDefinition function)
    {
        System.out.println("Handling");
        //should have all the info we need to push the transaction
        //build transaction
        //need to regulate args
        String functionData = viewModel.getTransactionBytes(token, BigInteger.ZERO, function);
        System.out.println(functionData);
    }

    private void getInput(String value)
    {
        tokenView.evaluateJavascript(
                "(function() { var x = document.getElementById(\"" + value + "\");\n" +
                        "            return x.elements[0].value; })();",
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

    private String buildTokenAttrs(List<BigInteger> tokenIds) throws Exception
    {
        BigInteger tokenId = (tokenIds == null) ? BigInteger.ZERO : tokenIds.get(0);
        //NonFungibleToken nft = viewModel.getAssetDefinitionService().getNonFungibleToken(token, token.getAddress(), tokenId);
        TokenScriptResult tsr = viewModel.getTokenScriptResult(token, tokenId);
        StringBuilder attrs = new StringBuilder();
        addPair(attrs, "name", token.getTokenTitle());
        addPair(attrs, "symbol", token.tokenInfo.symbol);
        if (!tokenId.equals(BigInteger.ZERO)) addPair(attrs, "_count", String.valueOf(tokenIds.size()));

        for (String attrKey : tsr.getAttributes().keySet())
        {
            TokenScriptResult.Attribute attr = tsr.getAttribute(attrKey);
            addPair(attrs, attrKey, attr.text);
        }

        return attrs.toString();
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

    private void addPair(StringBuilder attrs, String name, String value) throws ParseException
    {
        attrs.append(name);
        attrs.append(": ");

        if (name.equals("time"))
        {
            DateTime dt = DateTimeFactory.getDateTime(value);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat simpleTimeFormat = new SimpleDateFormat("hh:mm:ssZ");
            String JSDate = dt.format(simpleDateFormat) + "T" + dt.format(simpleTimeFormat);

            value = "{ generalizedTime: \"" + value + "\", date: new Date(\"" + JSDate + "\") }";// ((DateTime) dt).toString();
            attrs.append(value);
        }
        else
        {
            attrs.append("\"");
            attrs.append(value);
            attrs.append("\"");
        }

        attrs.append(",\n");
    }
}
