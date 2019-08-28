package io.stormbird.wallet.util;

import com.amazonaws.mobileconnectors.lambdainvoker.LambdaFunction;

public interface AppleAssociationConfig {
    @LambdaFunction
    io.stormbird.token.tools.AppleAssociationConfig.Response AppleConfig(io.stormbird.token.tools.AppleAssociationConfig.Request request);
}
