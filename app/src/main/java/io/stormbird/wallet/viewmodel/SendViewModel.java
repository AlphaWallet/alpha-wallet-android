package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;

import org.web3j.crypto.Hash;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.token.tools.Numeric;
import io.stormbird.wallet.entity.Ticker;
import io.stormbird.wallet.entity.Transaction;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.interact.FetchGasSettingsInteract;
import io.stormbird.wallet.interact.FetchTokensInteract;
import io.stormbird.wallet.repository.TokenRepository;
import io.stormbird.wallet.router.ConfirmationRouter;
import io.stormbird.wallet.router.MyAddressRouter;

public class SendViewModel extends BaseViewModel {
    private static final long CHECK_ETHPRICE_INTERVAL = 10;
    private static final String ENSCONTRACT = "0x314159265dD8dbb310642f98f50C066173C1259b";
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<Transaction> transaction = new MutableLiveData<>();
    private final MutableLiveData<Double> ethPrice = new MutableLiveData<>();

    private final ConfirmationRouter confirmationRouter;
    private final FetchGasSettingsInteract fetchGasSettingsInteract;
    private final MyAddressRouter myAddressRouter;
    private final FetchTokensInteract fetchTokensInteract;

    public SendViewModel(ConfirmationRouter confirmationRouter,
                         FetchGasSettingsInteract fetchGasSettingsInteract,
                         MyAddressRouter myAddressRouter,
                         FetchTokensInteract fetchTokensInteract) {
        this.confirmationRouter = confirmationRouter;
        this.fetchGasSettingsInteract = fetchGasSettingsInteract;
        this.myAddressRouter = myAddressRouter;
        this.fetchTokensInteract = fetchTokensInteract;
    }

    public LiveData<Double> ethPriceReading() { return ethPrice; }

    public void openConfirmation(Context context, String to, BigInteger amount, String contractAddress, int decimals, String symbol, boolean sendingTokens) {
        confirmationRouter.open(context, to, amount, contractAddress, decimals, symbol, sendingTokens);
    }

    public void showMyAddress(Context context, Wallet wallet) {
        myAddressRouter.open(context, wallet);
    }

    public void showContractInfo(Context ctx, String contractAddress)
    {
        myAddressRouter.open(ctx, contractAddress);
    }

    public void startEthereumTicker()
    {
        disposable = Observable.interval(0, CHECK_ETHPRICE_INTERVAL, TimeUnit.SECONDS)
                .doOnNext(l -> fetchTokensInteract
                        .getEthereumTicker()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onTicker, this::onError)).subscribe();
    }

    private void onTicker(Ticker ticker)
    {
        if (ticker != null && ticker.price_usd != null)
        {
            ethPrice.postValue(Double.valueOf(ticker.price_usd));
        }
    }

    public void checkENSAddress(String name)
    {
        if (name == null || name.length() < 2 || name.charAt(0) != '@') return;
        name = name.substring(1);
        //split name
        String[] components = name.split("\\.");

        byte[] hexZeroBytes = new byte[32];
        for (int i = 0; i < 32; i++) hexZeroBytes[i] = 0;
        byte[] resultHash = hexZeroBytes;

        for (int i = (components.length - 1); i >= 0; i--)
        {
            String nameComponent = components[i];
            resultHash = hashJoin(resultHash, nameComponent.getBytes());
        }

        System.out.println(Numeric.toHexString(resultHash));
        disposable = fetchTokensInteract.callAddressMethod("owner", resultHash, ENSCONTRACT)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::gotAddress, this::onError);
    }

    private void gotAddress(String returnedAddress)
    {
        BigInteger test = new BigInteger(returnedAddress, 16);
        if (!test.equals(BigInteger.ZERO))
        {
            //post the response back
            System.out.println("YAY GOT ADDRESS: " + returnedAddress);
        }
    }

    private byte[] hashJoin(byte[] lastHash, byte[] input)
    {
        byte[] joined = new byte[lastHash.length*2];

        byte[] inputHash = Hash.sha3(input);
        System.arraycopy(lastHash, 0, joined, 0, lastHash.length);
        System.arraycopy(inputHash, 0, joined, lastHash.length, inputHash.length);
        byte[] resultHash = Hash.sha3(joined);
        System.out.println(Numeric.toHexString(resultHash));

        return resultHash;
    }
}
