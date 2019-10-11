package com.alphawallet.app.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import com.alphawallet.app.entity.DAppFunction;
import com.alphawallet.app.entity.PinAuthenticationCallbackInterface;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.tokenscript.TokenScriptRenderCallback;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.util.KeyboardUtils;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.web3.OnSignPersonalMessageListener;
import com.alphawallet.app.web3.Web3TokenView;
import com.alphawallet.app.web3.entity.Address;
import com.alphawallet.app.web3.entity.FunctionCallback;
import com.alphawallet.app.web3.entity.Message;
import com.alphawallet.app.web3.entity.PageReadyCallback;
import dagger.android.AndroidInjection;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.token.entity.*;
import com.alphawallet.token.tools.Numeric;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.viewmodel.TokenFunctionViewModel;
import com.alphawallet.app.viewmodel.TokenFunctionViewModelFactory;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.ProgressView;
import com.alphawallet.app.widget.SignMessageDialog;
import com.alphawallet.app.widget.SignTransactionDialog;
import com.alphawallet.app.widget.SystemView;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static com.alphawallet.app.C.Key.TICKET;
import static com.alphawallet.app.entity.CryptoFunctions.sigFromByteArray;
import static com.alphawallet.app.entity.Operation.SIGN_DATA;

/**
 * Created by James on 4/04/2019.
 * Stormbird in Singapore
 */
public class FunctionActivity extends BaseActivity implements FunctionCallback,
        PageReadyCallback, OnSignPersonalMessageListener, SignAuthenticationCallback, StandardFunctionInterface, TokenScriptRenderCallback
{
    @Inject
    protected TokenFunctionViewModelFactory viewModelFactory;
    private TokenFunctionViewModel viewModel;

    private Token token;
    private List<BigInteger> tokenIds;
    private BigInteger tokenId;
    private String actionMethod;
    private SystemView systemView;
    private Web3TokenView tokenView;
    private ProgressBar waitSpinner;
    private SignMessageDialog dialog;
    private String functionEffect;
    private Map<String, String> args = new HashMap<>();
    private StringBuilder attrs;
    private AWalletAlertDialog alertDialog;
    private Message<String> messageToSign;
    private PinAuthenticationCallbackInterface authInterface;
    private FunctionButtonBar functionBar;
    private Handler handler;
    private boolean isClosing;

    private void initViews() {
        token = getIntent().getParcelableExtra(TICKET);
        actionMethod = getIntent().getStringExtra(C.EXTRA_STATE);
        String tokenIdStr = getIntent().getStringExtra(C.EXTRA_TOKEN_ID);
        if (tokenIdStr == null || tokenIdStr.length() == 0) tokenIdStr = "0";
        tokenIds = token.stringHexToBigIntegerList(tokenIdStr);
        tokenId = tokenIds.get(0);

        tokenView = findViewById(R.id.web3_tokenview);
        waitSpinner = findViewById(R.id.progress_element);

        tokenView.setChainId(token.tokenInfo.chainId);
        tokenView.setWalletAddress(new Address(token.getWallet()));
        tokenView.setupWindowCallback(this);
        tokenView.setRpcUrl(token.tokenInfo.chainId);
        tokenView.setOnReadyCallback(this);
        tokenView.setOnSignPersonalMessageListener(this);
        tokenView.setVisibility(View.GONE);
        waitSpinner.setVisibility(View.VISIBLE);
        viewModel.startGasPriceUpdate(token.tokenInfo.chainId);
        viewModel.getCurrentWallet();

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

            String base64 = Base64.encodeToString(injectedView.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
            tokenView.loadData(base64, "text/html; charset=utf-8", "base64");
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
            attrs = viewModel.getAssetDefinitionService().getTokenAttrs(token, tokenId, 1);
            //add extra tokenIds if required
            addMultipleTokenIds(attrs);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        viewModel.getAssetDefinitionService().resolveAttrs(token, tokenIds)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onAttr, this::onError, () -> displayFunction(attrs.toString()))
                .isDisposed();
    }

    private void addMultipleTokenIds(StringBuilder sb)
    {
        Map<String, TSAction> functions = viewModel.getAssetDefinitionService().getTokenFunctionMap(token.tokenInfo.chainId, token.getAddress());
        TSAction action = functions.get(actionMethod);
        boolean hasTokenIds = false;

        if (action != null && action.function != null)
        {
            for (MethodArg arg : action.function.parameters)
            {
                int index = arg.getTokenIndex();
                if (arg.isTokenId() && index >= 0 && index < tokenIds.size())
                {
                    if (!hasTokenIds)
                    {
                        sb.append("tokenIds: [");
                    }
                    else
                    {
                        sb.append(", ");
                    }
                    sb.append("\"");
                    sb.append(tokenIds.get(index));
                    sb.append("\"");
                    hasTokenIds = true;
                }
            }

            if (hasTokenIds)
            {
                sb.append("],\n");
            }
        }
    }

    private void onError(Throwable throwable)
    {
        throwable.printStackTrace();
        displayFunction(attrs.toString());
    }

    private void onAttr(TokenScriptResult.Attribute attribute)
    {
        TokenScriptResult.addPair(attrs, attribute.id, attribute.text);
    }

    private void fillEmpty()
    {
        tokenView.loadData("<html><body>No Data</body></html>", "text/html", "utf-8");
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_script_view);

        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(TokenFunctionViewModel.class);
        systemView = findViewById(R.id.system_view);
        ProgressView progressView = findViewById(R.id.progress_view);
        systemView.hide();
        progressView.hide();
        isClosing = false;

        //expose the webview and remove the token 'card' background
        findViewById(R.id.layout_webwrapper).setBackgroundResource(R.drawable.background_card);
        findViewById(R.id.layout_webwrapper).setVisibility(View.VISIBLE);
        findViewById(R.id.layout_legacy).setVisibility(View.GONE);

        initViews();
        toolbar();
        setTitle(getString(R.string.token_function));
        setupFunctions();
    }

    private void setupFunctions()
    {
        functionBar = findViewById(R.id.layoutButtons);
        List<String> funcList = new ArrayList<>();

        funcList.add(actionMethod);

        functionBar.setupFunctionList(this, funcList);
        functionBar.revealButtons();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    private void completeTokenscriptFunction(String function)
    {
        Map<String, TSAction> functions = viewModel.getAssetDefinitionService().getTokenFunctionMap(token.tokenInfo.chainId, token.getAddress());
        TSAction action = functions.get(function);

        if (action.function != null)
        {
            //check params for function.
            //if there's input params, resolve them
            boolean resolved = checkNativeTransaction(action, true);
            resolved = checkFunctionArgs(action, resolved);
            resolveTokenIds(action);

            if (resolved)
            {
                handleFunction(action);
            }
        }
    }

    private void resolveTokenIds(TSAction action)
    {
        for (MethodArg arg : action.function.parameters)
        {
            int index = arg.getTokenIndex();
            if (arg.isTokenId() && index >= 0 && index < tokenIds.size())
            {
                arg.element.value = tokenIds.get(index).toString();
            }
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

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        viewModel.stopGasSettingsFetch();
    }

    private boolean checkTokenScriptElement(TSAction action, TokenscriptElement e, boolean resolved)
    {
        if (e.ref != null && e.ref.length() > 0 && action.attributeTypes != null)
        {
            AttributeType attr = action.attributeTypes.get(e.ref);
            if (attr == null) return true;
            if (attr.userInput)
            {
                return getUserInput(attr, e, resolved);
            }
        }

        return resolved;
    }

    private boolean getUserInput(AttributeType attr, TokenscriptElement e, boolean resolved)
    {
        String valueFromInput = args.get(e.ref);
        if (valueFromInput == null)
        {
            //fetch mapping
            args.put(e.ref, "__searching"); // indicate search TokenScript rendered page for user input
            getInput(e.ref);
            resolved = false;
        }
        else if (valueFromInput.equals("__searching")) //second pass through, still searching - error.
        {
            //display error, basic script error reporting
            String attrDetails = e.ref + " (" + attr.name + ")";
            String details = getString(R.string.tokenscript_element_not_present, attrDetails);
            tokenscriptError(details);
            resolved = false;
        }
        else
        {
            try
            {
                switch (attr.as)
                {
                    //UTF8, Unsigned, Signed, Mapping, Boolean, UnsignedInput, TokenId
                    case Unsigned:
                    case Signed:
                    case UnsignedInput:
                        BigDecimal unsignedValue = new BigDecimal(valueFromInput);
                        args.put(e.ref, unsignedValue.toString());
                        e.value = unsignedValue.toString();
                        break;
                    case UTF8:
                        e.value = valueFromInput;
                        break;
                    case Bytes:
                        //apply bitmask to user entry and shift it because bytes is the other way round
                        BigInteger userEntry;
                        if(valueFromInput.matches("-?[0-9a-fA-F]+")) {
                            userEntry = new BigInteger(valueFromInput, 16);
                        } else {
                            userEntry = new BigInteger(valueFromInput);
                        }
                        BigInteger val = userEntry.and(attr.bitmask).shiftRight(attr.bitshift);
                        e.value = val.toString(16);
                        args.put(e.ref, e.value);
                        break;
                    case e18:
                        e.value = BalanceUtils.EthToWei(valueFromInput);
                        args.put(e.ref, e.value);
                        break;
                    case e8:
                        e.value = new BigInteger(valueFromInput).multiply(new BigInteger("8")).toString();
                        args.put(e.ref, e.value);
                        break;
                    case e4:
                        e.value = new BigInteger(valueFromInput).multiply(new BigInteger("4")).toString();
                        args.put(e.ref, e.value);
                        break;
                    case e2:
                        e.value = new BigInteger(valueFromInput).multiply(new BigInteger("2")).toString();
                        args.put(e.ref, e.value);
                        break;
                    case Mapping:
                        //makes no sense as input
                        break;
                    case Boolean:
                        //attempt to decode
                        if (valueFromInput.equalsIgnoreCase("true") || valueFromInput.equals("1"))
                        {
                            e.value = "TRUE";
                        }
                        else
                        {
                            e.value = "FALSE";
                        }
                        args.put(e.ref, e.value);
                        break;
                    case TokenId:
                        break;
                }
            }
            catch (Exception excp)
            {
                excp.printStackTrace();
                resolved = false;
            }
        }

        return resolved;
    }

    private void handleFunction(TSAction action)
    {
        if (isClosing) return;
        if (action.function.tx != null && (action.function.method == null || action.function.method.length() == 0)
                && (action.function.parameters == null || action.function.parameters.size() == 0))
        {
            //no params, this is a native style transaction
            NativeSend(action);
        }
        else
        {
            //TODO: try to match the transaction with known token functions and check if this function will pass (insufficient funds or gas etc).
            String functionData = viewModel.getTransactionBytes(token, tokenId, action.function);
            //confirm the transaction
            ContractAddress cAddr = new ContractAddress(action.function, token.tokenInfo.chainId, token.tokenInfo.address); //viewModel.getAssetDefinitionService().getContractAddress(action.function, token);

            if (functionEffect == null)
            {
                functionEffect = actionMethod;
            }
            else
            {
                functionEffect = functionEffect + " " + token.tokenInfo.symbol + " to " + actionMethod;
                //functionEffect = functionEffect + " to " + actionMethod;
            }

            //function call may include some value
            String value = "0";
            if (action.function.tx != null && action.function.tx.args.containsKey("value"))
            {
                //this is very specific but 'value' is a specifically handled param
                value = action.function.tx.args.get("value").value;
                BigDecimal valCorrected = getCorrectedBalance(value, 18);
                Token currency = viewModel.getCurrency(token.tokenInfo.chainId);
                functionEffect = valCorrected.toString() + " " + currency.tokenInfo.symbol + " to " + actionMethod;
            }

            //finished resolving attributes, blank definition cache so definition is re-loaded when next needed
            viewModel.getAssetDefinitionService().clearCache();

            viewModel.confirmTransaction(this, cAddr.chainId, functionData, null, cAddr.address, actionMethod, functionEffect, value);
        }
    }

    private void NativeSend(TSAction action)
    {
        boolean isValid = true;
        KeyboardUtils.hideKeyboard(getCurrentFocus());
        FunctionDefinition function = action.function;

        //check we have a 'to' and a 'value'
        if (!(function.tx.args.containsKey("value")))
        {
            isValid = false;
            //TODO: Display error
        }

        //calculate native amount
        BigDecimal value = new BigDecimal(function.tx.args.get("value").value);
        //this is a native send, so check the native currency
        Token currency = viewModel.getCurrency(token.tokenInfo.chainId);

        if (currency.balance.subtract(value).compareTo(BigDecimal.ZERO) < 0)
        {
            //flash up dialog box insufficent funds
            errorInsufficientFunds(currency);
            isValid = false;
        }

        //is 'to' overridden?
        String to = null;
        if (function.tx.args.get("to") != null)
        {
            to = function.tx.args.get("to").value;
        }
        else if (function.contract.addresses.get(token.tokenInfo.chainId) != null)
        {
            to = function.contract.addresses.get(token.tokenInfo.chainId).get(0);
        }

        if (to == null || !Utils.isAddressValid(to))
        {
            errorInvalidAddress(to);
            isValid = false;
        }

        //eg Send 2(*1) ETH(*2) to Alex's Amazing Coffee House(*3) (0xdeadacec0ffee(*4))
        String extraInfo = String.format(getString(R.string.tokenscript_send_native), functionEffect, token.tokenInfo.symbol, actionMethod, to);

        //Clear the cache to refresh any resolved values
        viewModel.getAssetDefinitionService().clearCache();

        if (isValid) {
            viewModel.confirmNativeTransaction(this, to, value, token, extraInfo);
        }
    }

    private void errorInvalidAddress(String address)
    {
        if (alertDialog != null && alertDialog.isShowing()) alertDialog.dismiss();
        alertDialog = new AWalletAlertDialog(this);
        alertDialog.setIcon(AWalletAlertDialog.ERROR);
        alertDialog.setTitle(R.string.error_invalid_address);
        alertDialog.setMessage(getString(R.string.invalid_address_explain, address));
        alertDialog.setButtonText(R.string.button_ok);
        alertDialog.setButtonListener(v ->alertDialog.dismiss());
        alertDialog.show();
    }

    private void errorInsufficientFunds(Token currency)
    {
        if (alertDialog != null && alertDialog.isShowing()) alertDialog.dismiss();
        alertDialog = new AWalletAlertDialog(this);
        alertDialog.setIcon(AWalletAlertDialog.ERROR);
        alertDialog.setTitle(R.string.error_insufficient_funds);
        alertDialog.setMessage(getString(R.string.current_funds, currency.getCorrectedBalance(currency.tokenInfo.decimals), currency.tokenInfo.symbol));
        alertDialog.setButtonText(R.string.button_ok);
        alertDialog.setButtonListener(v ->alertDialog.dismiss());
        alertDialog.show();
    }

    private void tokenscriptError(String elementName)
    {
        if (alertDialog != null && alertDialog.isShowing()) alertDialog.dismiss();
        alertDialog = new AWalletAlertDialog(this);
        alertDialog.setIcon(AWalletAlertDialog.ERROR);
        alertDialog.setTitle(R.string.tokenscript_error);
        alertDialog.setMessage(getString(R.string.tokenscript_error_detail, elementName));
        alertDialog.setButtonText(R.string.button_ok);
        alertDialog.setButtonListener(v ->alertDialog.dismiss());
        alertDialog.show();
    }

    private void getInput(String value)
    {
        tokenView.evaluateJavascript(
                "(function() { var x = document.getElementById(\"" + value + "\");\n" +
                        "            return x.value; })();",
                html -> {
                    StringBuilder sb = new StringBuilder();
                    for (char ch : html.toCharArray()) if (ch!='\"') sb.append(ch);
                    if (!html.equals("null")) args.put(value, sb.toString());
                    completeTokenscriptFunction(actionMethod);
                }
        );
    }

    @Override
    public void signMessage(byte[] sign, DAppFunction dAppFunction, Message<String> message)
    {
        viewModel.signMessage(sign, dAppFunction, message, token.tokenInfo.chainId);
    }

    @Override
    public void functionSuccess()
    {
        isClosing = true;
        if (handler == null) handler = new Handler();
        LinearLayout successOverlay = findViewById(R.id.layout_success_overlay);
        if (successOverlay != null) successOverlay.setVisibility(View.VISIBLE);
        handler.postDelayed(closer, 1000);
    }

    private Runnable closer = new Runnable()
    {
        @Override
        public void run()
        {
            finish();
        }
    };

    @Override
    public void functionFailed()
    {
        System.out.println("FAIL");
    }

    @Override
    public void onPageLoaded()
    {

    }

    @Override
    public void onPause()
    {
        super.onPause();
        viewModel.resetSignDialog();
    }

    @Override
    public void onSignPersonalMessage(Message<String> message)
    {
        dialog = new SignMessageDialog(this, message);
        dialog.setAddress(token.getAddress());
        dialog.setMessage(message.value);
        dialog.setOnApproveListener(v -> {
            messageToSign = message;
            viewModel.getAuthorisation(this, this);
        });
        dialog.setOnRejectListener(v -> {
            tokenView.onSignCancel(message);
            dialog.dismiss();
        });
        dialog.show();
    }

    public void testRecoverAddressFromSignature(String message, String sig)
    {
        String prefix = DappBrowserFragment.PERSONAL_MESSAGE_PREFIX + message.length();
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
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public BigDecimal getCorrectedBalance(String value, int scale)
    {
        BigDecimal val = BigDecimal.ZERO;
        try
        {
            val = new BigDecimal(value);
            BigDecimal decimalDivisor = new BigDecimal(Math.pow(10, scale));
            val = val.divide(decimalDivisor);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return val.setScale(scale, RoundingMode.HALF_DOWN).stripTrailingZeros();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode,resultCode,intent);

        if (requestCode >= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS && requestCode <= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS + 10)
        {
            GotAuthorisation(resultCode == RESULT_OK);
        }
    }

    @Override
    public void GotAuthorisation(boolean gotAuth)
    {
        if (gotAuth && authInterface != null) authInterface.CompleteAuthentication(SIGN_DATA);
        else if (!gotAuth && authInterface != null) authInterface.FailedAuthentication(SIGN_DATA);

        if (gotAuth)
        {
            DAppFunction dAppFunction = new DAppFunction()
            {
                @Override
                public void DAppError(Throwable error, Message<String> message)
                {
                    tokenView.onSignCancel(message);
                    dialog.dismiss();
                    functionFailed();
                }

                @Override
                public void DAppReturn(byte[] data, Message<String> message)
                {
                    String signHex = Numeric.toHexString(data);
                    signHex = Numeric.cleanHexPrefix(signHex);
                    tokenView.onSignPersonalMessageSuccessful(message, signHex);
                    testRecoverAddressFromSignature(message.value, signHex);
                    dialog.dismiss();
                }
            };

            String convertedMessage = messageToSign.value;
            String signMessage = DappBrowserFragment.PERSONAL_MESSAGE_PREFIX
                    + convertedMessage.length()
                    + convertedMessage;
            signMessage(signMessage.getBytes(), dAppFunction, messageToSign);
        }
    }

    @Override
    public void setupAuthenticationCallback(PinAuthenticationCallbackInterface authCallback)
    {
        authInterface = authCallback;
    }

    @Override
    public void handleTokenScriptFunction(String function, List<BigInteger> selection)
    {
        isClosing = false;
        args.clear();
        //run the onConfirm JS and await callback
        tokenView.TScallToJS(function, "onConfirm" + "('sig')", this);
    }

    @Override
    public void callToJSComplete(String function, String result)
    {
        completeTokenscriptFunction(function);
    }
}
