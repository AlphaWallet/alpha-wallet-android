package com.alphawallet.app;

import androidx.test.runner.AndroidJUnit4;

import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WalletRepoTest {

//	static final String STORE_1 = "{\"address\":\"eb1a948c6cc57fedf9271626404fc04a74ddd1e6\",\"crypto\":{\"cipher\":\"aes-128-ctr\",\"ciphertext\":\"6f6ba0b047f191f01df175255d0ef1eaf687905b3c22f9975d4cdec76f266d1e\",\"cipherparams\":{\"iv\":\"289195567cced5b5e6c8b18158c5f2ec\"},\"kdf\":\"scrypt\",\"kdfparams\":{\"dklen\":32,\"n\":4096,\"p\":6,\"r\":8,\"salt\":\"243df82bdd2569ecf5da25fd9db21cf5857be99ed64c7e664432f5ebef626ebe\"},\"mac\":\"87313234721b61a2c58b0d89f44847ea01df52c96fd5e3c8855efa0ecfd7ee06\"},\"id\":\"3cb467fc-7f98-435f-98e3-7f660e0368cc\",\"version\":3}";
//	static final String PASS_1 = "1234";
//	static final String ADDRESS_1 = "0xeb1a948c6cc57fedf9271626404fc04a74ddd1e6";
//
//	protected WalletRepositoryType accountRepository;
//
//	@Before
//	public void setUp() {
//		Context context = InstrumentationRegistry.getTargetContext();
//		PreferenceRepositoryType preferenceRepositoryType = new SharedPreferenceRepository(context);
//		AccountKeystoreService accountKeystoreService = new KeystoreAccountService(new File(context.getFilesDir(), "store"));
//		EthereumNetworkRepositoryType networkRepository = new EthereumNetworkRepository(preferenceRepositoryType);
//		accountRepository = new WalletRepository(preferenceRepositoryType, accountKeystoreService, networkRepository);
//	}
//
//	@Test
//	public void testCreateAccount() {
//		TestObserver<Wallet> subscription = accountRepository
//				.createWallet(PASS_1)
//				.test();
//		subscription.awaitTerminalEvent();
//		subscription.assertComplete();
//		assertEquals(subscription.valueCount(), 1);
//		deleteAccount(subscription.values().get(0).address, PASS_1);
//	}
//
//	@Test
//	public void testImportAccount() {
//		TestObserver<Wallet> subscriber = accountRepository
//				.importKeystoreToWallet(STORE_1, PASS_1)
//				.toObservable()
//				.test();
//		subscriber.awaitTerminalEvent();
//		subscriber.assertComplete();
//		subscriber.assertNoErrors();
//
//		Assert.assertEquals(subscriber.valueCount(), 1);
//		Assert.assertEquals(subscriber.values().get(0).address, ADDRESS_1);
//		Assert.assertTrue(subscriber.values().get(0).sameAddress(ADDRESS_1));
//		deleteAccount(ADDRESS_1, PASS_1);
//	}
//
//	@Test
//	public void testDeleteAccount() {
//		importAccount(STORE_1, PASS_1);
//		TestObserver<Void> subscriber = accountRepository
//				.deleteWallet(ADDRESS_1, PASS_1)
//				.test();
//		subscriber.awaitTerminalEvent();
//		subscriber.assertComplete();
//		TestObserver<Wallet[]> accountListSubscriber = accountList();
//		accountListSubscriber.awaitTerminalEvent();
//		accountListSubscriber.assertComplete();
//		Assert.assertEquals(accountListSubscriber.valueCount(), 1);
//		Assert.assertEquals(accountListSubscriber.values().get(0).length, 0);
//	}
//
//	@Test
//	public void testExportAccountStore() {
//		importAccount(STORE_1, PASS_1);
//		TestObserver<String> subscriber = accountRepository
//				.exportWallet(new Wallet(ADDRESS_1), PASS_1, PASS_1)
//				.test();
//		subscriber.awaitTerminalEvent();
//		subscriber.assertComplete();
//		Assert.assertEquals(subscriber.valueCount(), 1);
//		Log.d("EXPORT_ACC", "Val: " + subscriber.values().get(0));
//		String val = subscriber.values().get(0);
//		try {
//			JSONObject json = new JSONObject(val);
//			Assert.assertTrue(("0x" + json.getString("address")).equalsIgnoreCase(ADDRESS_1));
//		} catch (Exception ex) {
//			ex.printStackTrace();
//		}
//		deleteAccount(ADDRESS_1, PASS_1);
//	}
//
//	@Test
//	public void testFetchAccounts() {
//		List<Wallet> createdWallets = new ArrayList<>();
//		for (int i = 0; i < 100; i++) {
//			createdWallets.add(createAccount());
//		}
//		TestObserver<Wallet[]> subscriber = accountRepository
//				.fetchWallets()
//				.test();
//		subscriber.awaitTerminalEvent();
//		subscriber.assertComplete();
//		Assert.assertEquals(subscriber.valueCount(), 1);
//		Assert.assertEquals(subscriber.values().get(0).length, 100);
//
//		Wallet[] wallets = subscriber.values().get(0);
//
//		for (int i = 0; i < 100; i++) {
//			Assert.assertTrue(createdWallets.get(i).sameAddress(wallets[i].address));
//		}
//		for (Wallet wallet : createdWallets) {
//			deleteAccount(wallet.address, PASS_1);
//		}
//	}
//
//	@Test
//	public void testFindAccount() {
//		importAccount(STORE_1, PASS_1);
//		TestObserver<Wallet> subscribe = accountRepository
//				.findWallet(ADDRESS_1)
//				.test();
//		subscribe.awaitTerminalEvent();
//		subscribe.assertComplete();
//		assertEquals(subscribe.valueCount(), 1);
//		assertTrue(subscribe.values().get(0).sameAddress(ADDRESS_1));
//		deleteAccount(ADDRESS_1, PASS_1);
//	}
//
//	@Test
//	public void testSetDefaultAccount() {
//		Wallet wallet = createAccount();
//		TestObserver<Void> subscriber = accountRepository
//				.setDefaultWallet(wallet)
//				.test();
//		subscriber.awaitTerminalEvent();
//		subscriber.assertComplete();
//
//		TestObserver<Wallet> defaultSubscriber = accountRepository.getDefaultWallet()
//				.test();
//		defaultSubscriber.awaitTerminalEvent();
//		defaultSubscriber.assertComplete();
//		assertEquals(defaultSubscriber.valueCount(), 1);
//		assertTrue(defaultSubscriber.values().get(0).sameAddress(wallet.address));
//		deleteAccount(ADDRESS_1, PASS_1);
//	}
//
//	@Test
//	public void testGetBalance() {
//		importAccount(STORE_1, PASS_1);
//		TestObserver<BigInteger> subscriber = accountRepository
//				.balanceInWei(new Wallet(ADDRESS_1))
//				.test();
//		subscriber.awaitTerminalEvent();
//		subscriber.assertComplete();
//		Log.d("BALANCE", subscriber.values().get(0).toString());
//		deleteAccount(ADDRESS_1, PASS_1);
//	}
//
//	private void importAccount(String store, String password) {
//		TestObserver<Wallet> subscriber = accountRepository
//				.importKeystoreToWallet(store, password)
//				.toObservable()
//				.test();
//		subscriber.awaitTerminalEvent();
//		subscriber.assertComplete();
//	}
//
//	private void deleteAccount(String address, String pass) {
//		TestObserver<Void> subscription = accountRepository.deleteWallet(address, pass)
//				.test();
//		subscription.awaitTerminalEvent();
//		subscription.assertComplete();
//	}
//
//	private TestObserver<Wallet[]> accountList() {
//		return accountRepository
//				.fetchWallets()
//				.test();
//	}
//
//	private Wallet createAccount() {
//		TestObserver<Wallet> subscriber = new TestObserver<>();
//		accountRepository
//				.createWallet("1234")
//				.toObservable()
//				.subscribe(subscriber);
//		subscriber.awaitTerminalEvent();
//		subscriber.assertComplete();
//		Assert.assertEquals(subscriber.valueCount(), 1);
//		return subscriber.values().get(0);
//	}
}
