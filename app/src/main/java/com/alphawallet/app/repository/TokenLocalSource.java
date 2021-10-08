package com.alphawallet.app.repository;

import android.util.Pair;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.nftassets.NFTAsset;
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
    void updateTokenBalance(NetworkInfo network, Wallet wallet, Token token);
    boolean updateTokenBalance(Wallet wallet, Token token, BigDecimal balance, List<BigInteger> balanceArray);
    Token fetchToken(int chainId, Wallet wallet, String address);
    void setEnable(Wallet wallet, Token token, boolean isEnabled);
    void createBaseNetworkTokens(String walletAddress);
    String getTokenImageUrl(int networkId, String address);

    Single<Token[]> saveERC20Tokens(Wallet wallet, Token[] tokens);
    void deleteRealmToken(int chainId, Wallet wallet, String address);

    Token updateTokenType(Token token, Wallet wallet, ContractType type);
    void storeTokenUrl(int networkId, String address, String imageUrl);
    Token[] initNFTAssets(Wallet wallet, Token[] tokens);

    Single<TokenCardMeta[]> fetchTokenMetas(Wallet wallet, List<Integer> networkFilters,
                                            AssetDefinitionService svs);

    Single<TokenCardMeta[]> fetchAllTokenMetas(Wallet wallet, List<Integer> networkFilters,
                                             String seachTerm);

    TokenCardMeta[] fetchTokenMetasForUpdate(Wallet wallet, List<Integer> networkFilters);

    Single<Token[]> fetchAllTokensWithNameIssue(String walletAddress, List<Integer> networkFilters);
    Single<ContractAddress[]> fetchAllTokensWithBlankName(String walletAddress, List<Integer> networkFilters);

    Single<Integer> fixFullNames(Wallet wallet, AssetDefinitionService svs);

    void updateEthTickers(Map<Integer, TokenTicker> ethTickers);
    void updateERC20Tickers(int chainId, Map<String, TokenTicker> erc20Tickers);
    void removeOutdatedTickers();

    Realm getRealmInstance(Wallet wallet);
    Realm getTickerRealmInstance();

    TokenTicker getCurrentTicker(Token token);

    void setVisibilityChanged(Wallet wallet, Token token);

    boolean hasVisibilityBeenChanged(Token token);
    boolean getEnabled(Token token);

    void updateNFTAssets(String wallet, Token erc721Token, List<BigInteger> additions, List<BigInteger> removals);
    void storeAsset(String wallet, Token token, BigInteger tokenId, NFTAsset asset);

    void storeATokenAddresses(List<String> addresses);
    Single<List<String>> getATokenAddresses();

    Single<Pair<Double, Double>> getTotalValue(String currentAddress, List<Integer> networkFilters);
}
