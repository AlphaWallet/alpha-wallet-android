package com.alphawallet.app.repository;

import android.util.Pair;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokendata.TokenGroup;
import com.alphawallet.app.entity.tokendata.TokenTicker;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.token.entity.ContractAddress;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import io.reactivex.Single;
import io.realm.Realm;

public interface TokenLocalSource {
    Single<Token> saveToken(Wallet wallet, Token token);
    Single<Token[]> saveTokens(Wallet wallet, Token[] items);
    boolean updateTokenBalance(Wallet wallet, Token token, BigDecimal balance, List<BigInteger> balanceArray);
    Token fetchToken(long chainId, Wallet wallet, String address);
    void setEnable(Wallet wallet, Token token, boolean isEnabled);
    String getTokenImageUrl(long chainId, String address);
    void deleteRealmToken(long chainId, Wallet wallet, String address);
    void storeTokenUrl(long chainId, String address, String imageUrl);
    Token[] initNFTAssets(Wallet wallet, Token[] tokens);

    Single<TokenCardMeta[]> fetchTokenMetas(Wallet wallet, List<Long> networkFilters,
                                            AssetDefinitionService svs);

    Single<TokenCardMeta[]> fetchAllTokenMetas(Wallet wallet, List<Long> networkFilters,
                                             String seachTerm);

    TokenCardMeta[] fetchTokenMetasForUpdate(Wallet wallet, List<Long> networkFilters);

    Single<Token[]> fetchAllTokensWithNameIssue(String walletAddress, List<Long> networkFilters);
    Single<ContractAddress[]> fetchAllTokensWithBlankName(String walletAddress, List<Long> networkFilters);

    Single<Integer> fixFullNames(Wallet wallet, AssetDefinitionService svs);

    void updateEthTickers(Map<Long, TokenTicker> ethTickers);
    void updateERC20Tickers(long chainId, Map<String, TokenTicker> erc20Tickers);
    void removeOutdatedTickers();

    Realm getRealmInstance(Wallet wallet);
    Realm getTickerRealmInstance();

    TokenTicker getCurrentTicker(Token token);
    TokenTicker getCurrentTicker(String key);

    void setVisibilityChanged(Wallet wallet, Token token);

    boolean hasVisibilityBeenChanged(Token token);
    boolean getEnabled(Token token);

    void updateNFTAssets(String wallet, Token erc721Token, List<BigInteger> additions, List<BigInteger> removals);
    void storeAsset(String wallet, Token token, BigInteger tokenId, NFTAsset asset);

    int storeTokensMapping(Pair<Map<String, ContractAddress>, Map<String, TokenGroup>> mappings);
    long getLastMappingsUpdate();

    Single<Pair<Double, Double>> getTotalValue(String currentAddress, List<Long> networkFilters);

    Map<String, Long> getTickerTimeMap(long chainId, List<TokenCardMeta> erc20Tokens);

    void deleteTickers();

    Single<List<String>> getTickerUpdateList(List<Long> networkFilter);

    ContractAddress getBaseToken(long chainId, String address);
    TokenGroup getTokenGroup(long chainId, String address, ContractType type);

    void updateTicker(long chainId, String address, TokenTicker ticker);
}
