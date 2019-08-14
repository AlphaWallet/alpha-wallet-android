package io.stormbird.wallet.ui;

import com.amazonaws.mobileconnectors.lambdainvoker.LambdaFunction;
import io.stormbird.token.tools.TokenScriptTrustAddressRequest;
import io.stormbird.token.tools.TokenScriptTrustAddress;

public interface AWSLambdaInterface {

    /**
     * Invoke the Lambda function "TokenScriptTrustAddress".
     * The function name is the method name.
     * Note that this Lambda function actually is the io.stormbird.token.tools.TokenScriptTrustAddress class
     * I named the Lambda function after the class name to make it appear less confusing
     * -- Weiwu
     */
    @LambdaFunction
    TokenScriptTrustAddress.Response TokenScriptTrustAddress(TokenScriptTrustAddressRequest request);

}