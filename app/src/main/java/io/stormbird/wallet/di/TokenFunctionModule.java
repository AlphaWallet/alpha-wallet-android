package io.stormbird.wallet.di;

import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.viewmodel.TokenFunctionViewModelFactory;
import dagger.Module;
import dagger.Provides;
/**
 * Created by James on 2/04/2019.
 * Stormbird in Singapore
 */

@Module
public class TokenFunctionModule
{
    @Provides
    TokenFunctionViewModelFactory provideTokenFunctionViewModelFactory(
            AssetDefinitionService assetDefinitionService) {

        return new TokenFunctionViewModelFactory(
                assetDefinitionService);
    }
}
