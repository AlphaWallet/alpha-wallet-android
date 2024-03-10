package com.alphawallet.app.viewmodel;

import static com.alphawallet.app.repository.TokensRealmSource.IMAGES_DB;
import static com.alphawallet.app.util.Utils.ALPHAWALLET_REPO_NAME;
import static com.alphawallet.app.util.Utils.isValidUrl;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.entity.RealmAuxData;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.RealmManager;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.util.Utils;

import org.json.JSONObject;
import org.web3j.crypto.Keys;

import java.util.Iterator;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Single;
import io.realm.Realm;
import timber.log.Timber;

@HiltViewModel
public class TokenIconViewModel extends BaseViewModel
{
    private final RealmManager realmManager;
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;

    @Inject
    public TokenIconViewModel(
            RealmManager realmManager,
            AssetDefinitionService assetDefinitionService,
            TokensService tokensService)
    {
        this.realmManager = realmManager;
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
    }

    public String getTokenName(Token token)
    {
        return token.getName(assetDefinitionService, token.getTokenCount());
    }

    public Single<String> getIconFallback(final Token token, boolean useContractURI)
    {
        if (useContractURI)
        {
            //attempt to pull contract URI
            return token.getContractURI()
                    .map(uri -> handleURIResult(token, uri));
        }
        else //use TW as last resort
        {
            return Single.fromCallable(() -> getFallbackUrlForToken(token));
        }
    }

    private String handleURIResult(Token token, String uriResult)
    {
        String imageUrl = imageFromMetadata(uriResult);

        if (TextUtils.isEmpty(imageUrl))
        {
            imageUrl = token.getFirstImageUrl(); //check for first NFT image

            if (TextUtils.isEmpty(imageUrl))
            {
                imageUrl = getFallbackUrlForToken(token);
            }
        }

        return Utils.parseIPFS(imageUrl);
    }

    @NonNull
    private String getFallbackUrlForToken(@NonNull Token token)
    {
        String correctedAddr = Keys.toChecksumAddress(token.getAddress());
        return Utils.getTWTokenImageUrl(token.tokenInfo.chainId, correctedAddr);
    }

    //Only store if it's not the AW iconassets url
    public void storeImageUrl(long chainId, String address, String imageUrl)
    {
        if (!TextUtils.isEmpty(imageUrl) && isValidUrl(imageUrl) && !imageUrl.startsWith(ALPHAWALLET_REPO_NAME) && TextUtils.isEmpty(getTokenImageUrl(chainId, address)))
        {
            tokensService.addTokenImageUrl(chainId, address, imageUrl);
        }
    }

    private String getTokenImageUrl(long networkId, String address)
    {
        String url = "";
        String instanceKey = address.toLowerCase() + "-" + networkId;
        try (Realm realm = realmManager.getRealmInstance(IMAGES_DB))
        {
            RealmAuxData instance = realm.where(RealmAuxData.class)
                    .equalTo("instanceKey", instanceKey)
                    .findFirst();

            if (instance != null)
            {
                url = instance.getResult();
            }
        }
        catch (Exception ex)
        {
            Timber.e(ex);
        }

        return url;
    }

    public String getTokenIcon(Token token)
    {
        //return getPrimaryIconURL(token);
        //see if there's a stored icon
        String icon = getTokenImageUrl(token.tokenInfo.chainId, token.tokenInfo.address);
        if (TextUtils.isEmpty(icon))
        {
            //attempt usual fetch
            icon = getPrimaryIconURL(token);
        }
        else
        {
            System.out.println("FROM DB: " + icon);
        }

        return icon;
    }

    private String getPrimaryIconURL(Token token)
    {
        String correctedAddr = Keys.toChecksumAddress(token.getAddress());
        return Utils.getTokenImageUrl(correctedAddr);
    }

    private String imageFromMetadata(String metaData)
    {
        try
        {
            JSONObject jsonData = new JSONObject(metaData);
            Iterator<String> keys = jsonData.keys();
            while (keys.hasNext())
            {
                String key = keys.next();
                String value = jsonData.getString(key);

                if (key.startsWith("image"))
                {
                    return value;
                }
            }
        }
        catch (Exception e)
        {
            //
        }

        return "";
    }
}
