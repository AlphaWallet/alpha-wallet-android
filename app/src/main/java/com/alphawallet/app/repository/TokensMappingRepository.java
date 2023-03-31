package com.alphawallet.app.repository;

import android.content.Context;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.TokensMapping;
import com.alphawallet.app.entity.tokendata.TokenGroup;
import com.alphawallet.app.util.Utils;
import com.alphawallet.token.entity.ContractAddress;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.Map;

public class TokensMappingRepository implements TokensMappingRepositoryType
{
    private static final String TOKENS_JSON_FILENAME = "tokens.json";
    private final Context context;
    private Map<String, TokenGroup> tokenMap;

    public TokensMappingRepository(Context context)
    {
        this.context = context;

        init();
    }

    private void init()
    {
        if (tokenMap == null)
        {
            createMap(Utils.loadJSONFromAsset(context, TOKENS_JSON_FILENAME));
        }
    }

    private void createMap(String mapping)
    {
        tokenMap = new HashMap<>();
        TokensMapping[] tokensMapping = new Gson().fromJson(mapping, new TypeToken<TokensMapping[]>()
        {
        }.getType());

        if (tokensMapping != null)
        {
            for (TokensMapping entry : tokensMapping)
            {
                for (ContractAddress address : entry.getContracts())
                {
                    tokenMap.putIfAbsent(address.getAddressKey(), entry.getGroup());
                }
            }
        }
    }

    @Override
    public TokenGroup getTokenGroup(long chainId, String address, ContractType type)
    {
        if (tokenMap == null) init();

        TokenGroup result = TokenGroup.ASSET;

        TokenGroup g = tokenMap.get(ContractAddress.toAddressKey(chainId, address));
        if (g != null)
        {
            result = g;
        }

        if (result == TokenGroup.SPAM)
        {
            return result;
        }

        switch (type)
        {
            case NOT_SET:
            case OTHER:
            case ETHEREUM:
            case CURRENCY:
            case CREATION:
            case DELETED_ACCOUNT:
            case ERC20:
            default:
                return result;

            case ERC721:
            case ERC721_ENUMERABLE:
            case ERC875_LEGACY:
            case ERC875:
            case ERC1155:
            case ERC721_LEGACY:
            case ERC721_TICKET:
            case ERC721_UNDETERMINED:
                return TokenGroup.NFT;
        }
    }
}
