package io.stormbird.wallet.util;

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
     * io.stormbird.token.tools.TokenScriptTrustAddress class, which
     * is where the AWS lambda code resides, placed in a different
     * module (lib) because we don't want to upload an entire apk
     * (35MB) to the Amazon Lambda only to execute that bit.
     */
    @LambdaFunction
    io.stormbird.token.tools.TrustAddressGenerator.Response DeriveTrustAddress(io.stormbird.token.tools.TrustAddressGenerator.Request request);

}
