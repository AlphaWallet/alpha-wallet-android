package io.stormbird.wallet.interact;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.ArraySet;
import com.google.gson.Gson;
import io.stormbird.wallet.entity.CreateWalletCallbackInterface;
import io.stormbird.wallet.entity.DApp;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.interact.rx.operator.Operators;
import io.stormbird.wallet.repository.PasswordStore;
import io.stormbird.wallet.repository.WalletRepositoryType;

import io.reactivex.Single;
import io.stormbird.wallet.service.HDKeyService;
import wallet.core.jni.HDWallet;

import java.util.List;
import java.util.Set;

import static io.stormbird.wallet.interact.rx.operator.Operators.completableErrorProxy;

public class CreateWalletInteract {

	private final WalletRepositoryType walletRepository;
	private final PasswordStore passwordStore;

	public CreateWalletInteract(WalletRepositoryType walletRepository, PasswordStore passwordStore) {
		this.walletRepository = walletRepository;
		this.passwordStore = passwordStore;
	}

	//TODO: handle errors, don't return until key is created
	public void create(Activity ctx, CreateWalletCallbackInterface callback) {
		HDKeyService hdService = new HDKeyService(ctx);
		String addr = hdService.createNewHDKey(callback);
		Wallet wallet = new Wallet(addr);
		wallet.setWalletType(Wallet.WalletType.HDKEY);
//		return passwordStore.generatePassword()
//				.flatMap(masterPassword -> walletRepository
//						.createWallet(masterPassword)
//						.compose(Operators.savePassword(passwordStore, walletRepository, masterPassword))
//                        .flatMap(wallet -> passwordVerification(wallet, masterPassword)));
	}

	private Single<Wallet> passwordVerification(Wallet wallet, String masterPassword) {
        return passwordStore
                .getPassword(wallet)
                .flatMap(password -> walletRepository
                        .exportWallet(wallet, password, password)
                        .flatMap(keyStore -> walletRepository.findWallet(wallet.address)))
                .onErrorResumeNext(throwable -> walletRepository
                        .deleteWallet(wallet.address, masterPassword)
                        .lift(completableErrorProxy(throwable))
                        .toSingle(() -> wallet));
    }
}
