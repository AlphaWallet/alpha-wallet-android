package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;

import java.math.BigInteger;
import java.util.Arrays;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.token.entity.SalesOrderMalformed;
import io.stormbird.token.tools.Numeric;
import io.stormbird.token.tools.ParseMagicLink;
import io.stormbird.wallet.entity.CryptoFunctions;
import io.stormbird.wallet.entity.ERC721Token;
import io.stormbird.wallet.entity.GasSettings;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Ticket;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.entity.opensea.Asset;
import io.stormbird.wallet.interact.CreateTransactionInteract;
import io.stormbird.wallet.interact.FetchTokensInteract;
import io.stormbird.wallet.interact.FetchTransactionsInteract;
import io.stormbird.wallet.interact.FindDefaultNetworkInteract;
import io.stormbird.wallet.interact.FindDefaultWalletInteract;
import io.stormbird.wallet.repository.TokenRepository;
import io.stormbird.wallet.router.AssetDisplayRouter;
import io.stormbird.wallet.router.ConfirmationRouter;
import io.stormbird.wallet.router.TransferTicketDetailRouter;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.MarketQueueService;
import io.stormbird.wallet.service.TokensService;

import static io.stormbird.wallet.C.ENSCONTRACT;
import static io.stormbird.wallet.viewmodel.SendViewModel.hashJoin;

/**
 * Created by James on 21/02/2018.
 */
public class TransferTicketDetailViewModel extends BaseViewModel {
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<GasSettings> gasSettings = new MutableLiveData<>();
    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<String> newTransaction = new MutableLiveData<>();
    private final MutableLiveData<String> universalLinkReady = new MutableLiveData<>();
    private final MutableLiveData<String> userTransaction = new MutableLiveData<>();
    private final MutableLiveData<String> ensResolve = new MutableLiveData<>();
    private final MutableLiveData<Boolean> ensFail = new MutableLiveData<>();

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final MarketQueueService marketQueueService;
    private final CreateTransactionInteract createTransactionInteract;
    private final TransferTicketDetailRouter transferTicketDetailRouter;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final AssetDisplayRouter assetDisplayRouter;
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;
    private final ConfirmationRouter confirmationRouter;
    private final FetchTokensInteract fetchTokensInteract;

    private CryptoFunctions cryptoFunctions;
    private ParseMagicLink parser;

    private byte[] linkMessage;

    TransferTicketDetailViewModel(FindDefaultNetworkInteract findDefaultNetworkInteract,
                                  FindDefaultWalletInteract findDefaultWalletInteract,
                                  MarketQueueService marketQueueService,
                                  CreateTransactionInteract createTransactionInteract,
                                  TransferTicketDetailRouter transferTicketDetailRouter,
                                  FetchTransactionsInteract fetchTransactionsInteract,
                                  AssetDisplayRouter assetDisplayRouter,
                                  AssetDefinitionService assetDefinitionService,
                                  TokensService tokensService,
                                  ConfirmationRouter confirmationRouter,
                                  FetchTokensInteract fetchTokensInteract) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.marketQueueService = marketQueueService;
        this.createTransactionInteract = createTransactionInteract;
        this.transferTicketDetailRouter = transferTicketDetailRouter;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.assetDisplayRouter = assetDisplayRouter;
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
        this.confirmationRouter = confirmationRouter;
        this.fetchTokensInteract = fetchTokensInteract;
    }

    public LiveData<Wallet> defaultWallet() {
        return defaultWallet;
    }
    public LiveData<String> newTransaction() { return newTransaction; }
    public LiveData<String> universalLinkReady() { return universalLinkReady; }
    public LiveData<String> userTransaction() { return userTransaction; }
    public LiveData<String> ensResolve() { return ensResolve; }
    public LiveData<Boolean> ensFail() { return ensFail; }

    private void initParser()
    {
        if (parser == null)
        {
            cryptoFunctions = new CryptoFunctions();
            parser = new ParseMagicLink(cryptoFunctions);
        }
    }

    public void prepare(Token token)
    {
        disposable = findDefaultNetworkInteract
                .find()
                .subscribe(this::onDefaultNetwork, this::onError);
    }

    private void onDefaultNetwork(NetworkInfo networkInfo)
    {
        defaultNetwork.postValue(networkInfo);
        disposable = findDefaultWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    private void onDefaultWallet(Wallet wallet) {
        defaultWallet.setValue(wallet);
    }

    public void generateSalesOrders(String contractAddr, BigInteger price, int[] ticketIndicies, BigInteger firstTicketId)
    {
        marketQueueService.createSalesOrders(defaultWallet.getValue(), price, ticketIndicies, contractAddr, firstTicketId, processMessages);
    }

    public void setWallet(Wallet wallet)
    {
        defaultWallet.setValue(wallet);
    }

    private void onCreateTransaction(String transaction)
    {
        userTransaction.postValue(transaction);
    }

    public void generateUniversalLink(int[] ticketSendIndexList, String contractAddress, long expiry)
    {
        initParser();
        if (ticketSendIndexList == null || ticketSendIndexList.length == 0) return; //TODO: Display error message

        //NB tradeBytes is the exact bytes the ERC875 contract builds to check the valid order.
        //This is what we must sign.
        byte[] tradeBytes = parser.getTradeBytes(ticketSendIndexList, contractAddress, BigInteger.ZERO, expiry);
        try {
            linkMessage = parser.generateLeadingLinkBytes(ticketSendIndexList, contractAddress, BigInteger.ZERO, expiry);
        } catch (SalesOrderMalformed e) {
            //TODO: Display appropriate error to user
        }

        //sign this link
        disposable = createTransactionInteract
                .sign(defaultWallet().getValue(), tradeBytes)
                .subscribe(this::gotSignature, this::onError);
    }

    private void gotSignature(byte[] signature)
    {
        try
        {
            String universalLink = parser.completeUniversalLink(linkMessage, signature);
            //Now open the share icon
            universalLinkReady.postValue(universalLink);
        }
        catch (SalesOrderMalformed sm) {
            //TODO: Display appropriate error to user
            sm.printStackTrace();
        }
    }

    public void openTransferState(Context context, Token token, String ticketIds, int transferStatus)
    {
        transferTicketDetailRouter.openTransfer(context, token, ticketIds, defaultWallet.getValue(), transferStatus);
    }

    public void createTicketTransfer(String to, String contractAddress, String indexList, BigInteger gasPrice, BigInteger gasLimit)
    {
        Token token = tokensService.getToken(contractAddress);
        if (token.unspecifiedSpec())
        {
            //need to determine the spec
            disposable = fetchTransactionsInteract.queryInterfaceSpec(token)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(spec -> onInterfaceSpec(spec, to, contractAddress, indexList, gasPrice, gasLimit), this::onError);
        }
        else
        {
            final byte[] data = TokenRepository.createTicketTransferData(to, indexList, token);
            disposable = createTransactionInteract
                    .create(defaultWallet.getValue(), contractAddress, BigInteger.valueOf(0), gasPrice, gasLimit, data)
                    .subscribe(this::onCreateTransaction, this::onError);
        }
    }

    private void onInterfaceSpec(Integer spec, String to, String contractAddress, String indexList, BigInteger gasPrice, BigInteger gasLimit)
    {
        Token token = tokensService.getToken(contractAddress);
        token.setInterfaceSpec(spec);
        TokensService.setInterfaceSpec(token.getAddress(), spec);
        createTicketTransfer(to, contractAddress, indexList, gasPrice, gasLimit);
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return assetDefinitionService;
    }

    public void showAssets(Context ctx, Ticket ticket, boolean isClearStack)
    {
        assetDisplayRouter.open(ctx, ticket, isClearStack);
    }

    public void openConfirm(Context ctx, String to, Token token, String tokenId)
    {
        //first find the asset within the token
        Asset asset = null;
        for (Asset a : ((ERC721Token) token).tokenBalance)
        {
            if (a.getTokenId().equals(tokenId))
            {
                asset = a;
                break;
            }
        }

        if (asset != null)
        {
            confirmationRouter.openERC721Transfer(ctx, to, tokenId, token.getAddress(), token.getFullName(), asset.getName());
        }
    }

    public void checkENSAddress(String name)
    {
        if (name == null || name.length() < 2 || name.charAt(0) != '@') return;
        disposable = checkENSAddressFunc(name.substring(1))
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::gotHash, this::onError);
    }

    private void gotHash(byte[] resultHash)
    {
        disposable = fetchTokensInteract.callAddressMethod("owner", resultHash, ENSCONTRACT)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::gotAddress, this::onError);

    }

    private Single<byte[]> checkENSAddressFunc(final String name)
    {
        return Single.fromCallable(() -> {
            //split name
            String[] components = name.split("\\.");

            byte[] resultHash = new byte[32];
            Arrays.fill(resultHash, (byte)0);

            for (int i = (components.length - 1); i >= 0; i--)
            {
                String nameComponent = components[i];
                resultHash = hashJoin(resultHash, nameComponent.getBytes());
            }

            return resultHash;
        });
    }

    private void gotAddress(String returnedAddress)
    {
        BigInteger test = Numeric.toBigInt(returnedAddress);
        if (!test.equals(BigInteger.ZERO))
        {
            //post the response back
            ensResolve.postValue(returnedAddress);
        }
        else
        {
            ensFail.postValue(true);
        }
    }
}
