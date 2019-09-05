package io.stormbird.wallet.util;

import com.amazonaws.mobileconnectors.lambdainvoker.LambdaFunction;

public interface VerifyXMLDSig {
    @LambdaFunction
    io.stormbird.token.tools.VerifyXMLDSig.Response VerifyTSMLFile(io.stormbird.token.tools.VerifyXMLDSig.Request request);
}
