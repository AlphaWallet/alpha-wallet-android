package com.alphawallet.app.util;

import com.amazonaws.mobileconnectors.lambdainvoker.LambdaFunction;

public interface VerifyXMLDSig {
    @LambdaFunction
    com.alphawallet.token.tools.VerifyXMLDSig.Response VerifyTSMLFile(com.alphawallet.token.tools.VerifyXMLDSig.Request request);
}
