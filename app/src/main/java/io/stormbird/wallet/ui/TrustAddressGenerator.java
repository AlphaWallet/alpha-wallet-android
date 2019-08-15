package io.stormbird.wallet.ui;

import com.amazonaws.mobileconnectors.lambdainvoker.LambdaFunction;

public interface TrustAddressGenerator {

    /**
     * Invoke the Lambda function "TokenScriptTrustAddress".
     * The function name is the method name.
     * Note that this Lambda function actually is the io.stormbird.token.tools.TokenScriptTrustAddress class
     * I named the Lambda function after the class name to make it appear less confusing
     * -- Weiwu
     */
    @LambdaFunction
    io.stormbird.token.tools.TrustAddressGenerator.Response DeriveTrustAddress(io.stormbird.token.tools.TrustAddressGenerator.Request request);

}
