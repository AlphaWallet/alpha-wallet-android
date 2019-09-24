package com.alphawallet.app.util;

import com.amazonaws.mobileconnectors.lambdainvoker.LambdaFunction;

public interface TrustAddressGenerator {

    /**
     * Invoke the Lambda function "TokenScriptTrustAddress".
     *
     * Change the method name in this Interface to fit the
     * function-name of deployed lambda. This class is created following the tutorial:
     * https://docs.aws.amazon.com/aws-mobile/latest/developerguide/how-to-android-lambda.html
     *
     * This Interface serve to describe to the Android runtime
     * com.stormbird.token.tools.TokenScriptTrustAddress class, which
     * is where the AWS lambda code resides, placed in a different
     * module (lib) because we don't want to upload an entire apk
     * (35MB) to the Amazon Lambda only to execute that bit.
     */
    @LambdaFunction
    com.alphawallet.token.tools.TrustAddressGenerator.Response DeriveTrustAddress(com.alphawallet.token.tools.TrustAddressGenerator.Request request);

}
