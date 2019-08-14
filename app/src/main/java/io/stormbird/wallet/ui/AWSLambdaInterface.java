package io.stormbird.wallet.ui;

import com.amazonaws.mobileconnectors.lambdainvoker.LambdaFunction;
import io.stormbird.token.tools.TokenScriptTrustAddressRequest;

public interface AWSLambdaInterface {

    /**
     * Invoke the Lambda function "AndroidBackendLambdaFunction".
     * The function name is the method name.
     */
    @LambdaFunction
    String TokenScriptTrustAddress(TokenScriptTrustAddressRequest request);

}