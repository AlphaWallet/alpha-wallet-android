package com.alphawallet.app.service;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Environment;
import android.os.FileObserver;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.SparseArray;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.FragmentMessenger;
import com.alphawallet.app.entity.TokenLocator;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.opensea.Asset;
import com.alphawallet.app.entity.tokens.ERC721Token;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenFactory;
import com.alphawallet.app.entity.tokenscript.EventUtils;
import com.alphawallet.app.entity.tokenscript.TokenScriptFile;
import com.alphawallet.app.entity.tokenscript.TokenscriptFunction;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.TokenLocalSource;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.app.repository.TransactionLocalSource;
import com.alphawallet.app.repository.TransactionsRealmCache;
import com.alphawallet.app.repository.entity.RealmAuxData;
import com.alphawallet.app.repository.entity.RealmCertificateData;
import com.alphawallet.app.ui.HomeActivity;
import com.alphawallet.app.ui.widget.entity.IconItem;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.HomeViewModel;
import com.alphawallet.token.entity.Attribute;
import com.alphawallet.token.entity.AttributeInterface;
import com.alphawallet.token.entity.ContractAddress;
import com.alphawallet.token.entity.ContractInfo;
import com.alphawallet.token.entity.EvaluateSelection;
import com.alphawallet.token.entity.EventDefinition;
import com.alphawallet.token.entity.FunctionDefinition;
import com.alphawallet.token.entity.MethodArg;
import com.alphawallet.token.entity.ParseResult;
import com.alphawallet.token.entity.SigReturnType;
import com.alphawallet.token.entity.TSAction;
import com.alphawallet.token.entity.TSActivityView;
import com.alphawallet.token.entity.TSSelection;
import com.alphawallet.token.entity.TokenScriptResult;
import com.alphawallet.token.entity.TokenscriptContext;
import com.alphawallet.token.entity.TokenscriptElement;
import com.alphawallet.token.entity.TransactionResult;
import com.alphawallet.token.entity.XMLDsigDescriptor;
import com.alphawallet.token.tools.Numeric;
import com.alphawallet.token.tools.TokenDefinition;

import org.jetbrains.annotations.NotNull;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.crypto.Keys;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;
import org.xml.sax.SAXException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import io.realm.exceptions.RealmPrimaryKeyConstraintException;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import static com.alphawallet.app.repository.TokenRepository.getWeb3jService;
import static com.alphawallet.app.repository.TokensRealmSource.IMAGES_DB;
import static com.alphawallet.token.tools.TokenDefinition.TOKENSCRIPT_CURRENT_SCHEMA;
import static com.alphawallet.token.tools.TokenDefinition.TOKENSCRIPT_REPO_SERVER;


/**
 * AssetDefinitionService is the only place where we interface with XML files.
 * This is to avoid duplicating resources
 * and also provide a consistent way to get XML values
 */

public class AssetDefinitionService implements ParseResult, AttributeInterface
{
    public static final String ASSET_SUMMARY_VIEW_NAME = "item-view";
    public static final String ASSET_DETAIL_VIEW_NAME = "view";
    private final String ICON_REPO_ADDRESS_TOKEN = "[TOKEN]";
    private final String CHAIN_REPO_ADDRESS_TOKEN = "[CHAIN]";
    private final String TRUST_ICON_REPO = "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/" + CHAIN_REPO_ADDRESS_TOKEN + "/assets/" + ICON_REPO_ADDRESS_TOKEN + "/logo.png";
    private final String ALPHAWALLET_ICON_REPO = "https://raw.githubusercontent.com/alphawallet/iconassets/master/" + ICON_REPO_ADDRESS_TOKEN + "/logo.png";
    private static final String CERTIFICATE_DB = "CERTIFICATE_CACHE-db.realm";
    private static final long CHECK_TX_LOGS_INTERVAL = 20;

    private final Context context;
    private final OkHttpClient okHttpClient;

    private final SparseArray<Map<String, TokenScriptFile>> assetDefinitions;
    private final Map<String, Long> assetChecked;                //Mapping of contract address to when they were last fetched from server
    private FileObserver fileObserver;                     //Observer which scans the override directory waiting for file change
    private FileObserver fileObserverQ;                    //Observer for Android Q directory
    private final NotificationService notificationService;
    private final RealmManager realmManager;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final TokensService tokensService;
    private final TokenLocalSource tokenLocalSource;
    private final AlphaWalletService alphaWalletService;
    private final TransactionLocalSource transactionsCache;
    private TokenDefinition cachedDefinition = null;
    private final SparseArray<Map<String, SparseArray<String>>> tokenTypeName;
    private final List<EventDefinition> eventList = new ArrayList<>(); //List of events built during file load
    private final Semaphore assetLoadingLock;  // used to block if someone calls getAssetDefinitionASync() while loading
    private Disposable eventListener;           // timer thread that periodically checks event logs for scripts that require events
    private final Semaphore eventConnection;
    private FragmentMessenger homeMessenger;

    private final TokenscriptFunction tokenscriptUtility;
    private final EventUtils eventUtils;

    @Nullable
    private Disposable checkEventDisposable;

    private final Map<String, Boolean> iconCheck = new ConcurrentHashMap<>();

    /* Designed with the assmuption that only a single instance of this class at any given time
    *  ^^ The "service" part of AssetDefinitionService is the keyword here.
    *  This is shorthand in the project to indicate this is a singleton that other classes inject.
    *  This is the design pattern of the app. See class RepositoriesModule for constructors which are called at App init only */
    public AssetDefinitionService(OkHttpClient client, Context ctx, NotificationService svs,
                                  RealmManager rm, EthereumNetworkRepositoryType eth, TokensService tokensService,
                                  TokenLocalSource trs, TransactionLocalSource tls,
                                  AlphaWalletService alphaService)
    {
        context = ctx;
        okHttpClient = client;
        assetChecked = new ConcurrentHashMap<>();
        tokenTypeName = new SparseArray<>();
        assetDefinitions = new SparseArray<>();
        notificationService = svs;
        realmManager = rm;
        ethereumNetworkRepository = eth;
        alphaWalletService = alphaService;
        this.tokensService = tokensService;
        this.eventUtils = new EventUtils() { }; //no overridden functions
        tokenscriptUtility = new TokenscriptFunction() { }; //no overridden functions
        tokenLocalSource = trs;
        transactionsCache = tls;
        assetLoadingLock = new Semaphore(1);
        eventConnection = new Semaphore(1);
        //quick check for old style event data
        checkLegacyEventData();
        loadAssetScripts();
    }

    /**
     * Load all TokenScripts
     *
     * This order has to be observed because it's an expected developer override order. If a script is placed in the /AlphaWallet directory
     * it is expected to override the one fetched from the repo server.
     * If a developer clicks on a script intent this script is expected to override the one fetched from the server.
     */
    private void loadAssetScripts()
    {
        try
        {
            assetLoadingLock.acquire(); // acquire the semaphore here to prevent attributes from being fetched until loading is complete
            // See flow above for details
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        loadInternalAssets();
    }

    //This loads bundled TokenScripts in the /assets directory eg xDAI bridge
    private void loadInternalAssets()
    {
        assetDefinitions.clear();

        Observable.fromIterable(getLocalTSMLFiles())
                .subscribeOn(Schedulers.io())
                .subscribe(this::addContractAssets, error -> { onError(error); parseAllFileScripts(); },
                        this::parseAllFileScripts).isDisposed();
    }

    private void parseAllFileScripts()
    {
        final File[] files = buildFileList(); //build an ordered list of files that need parsing
        //1. Signed files downloaded from server.
        //2. Files placed in the Android OS external directory (Android/data/<App Package Name>/files)
        //3. Files placed in the /AlphaWallet directory.
        //Depending on the order placed, files can be overridden. A file downloaded from the server is
        //overridden by a script for the same token placed in the /AlphaWallet directory.

        Observable.fromArray(files)
                .filter(File::isFile)
                .filter(this::allowableExtension)
                .filter(File::canRead)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .blockingForEach(file -> {  //load sequentially
                    try
                    {
                        final TokenDefinition td = parseFile(new FileInputStream(file));
                        cacheSignature(file)
                                .map(definition -> addContractAddresses(td, file))
                                .subscribe(success -> fileLoadComplete(success, file, td),
                                           error -> handleFileLoadError(error, file))
                                .isDisposed();
                    }
                    catch (Exception e)
                    {
                        handleFileLoadError(e, file);
                    }
                } );

        //executes after observable completes due to blockingForEach
        startDirectoryListeners();
        finishLoading();
    }

    private void handleFileLoadError(Throwable throwable, File file)
    {
        //TODO: parse error and add to error list for Token Management page
        System.out.println("ERROR WHILE PARSING: " + file.getName() + " : " + throwable.getMessage());
    }

    private void fileLoadComplete(List<ContractLocator> originContracts, File file, TokenDefinition td)
    {
        if (originContracts.size() == 0)
        {
            //TODO: parse error and add to error list for Token Management page
            System.out.println("File: " + file.getName() + " has no origin token");
        }
        else
        {
            //check for out-of-date script in the secure (downloaded) zone
            if (isInSecureZone(file) && !td.nameSpace.equals(TokenDefinition.TOKENSCRIPT_NAMESPACE))
            {
                //delete this file and check downloads for update
                removeFile(file.getAbsolutePath());
                loadScriptFromServer(getFileName(file));
            }
        }
    }

    //Start listening to the two script directories for files dropped in.
    //Why two directories? User may not want to allow AlphaWallet to have read file permission,
    //but we still want to allow them to be able to click on scripts in eg Telegram and install them
    //without needing to go through a permission screen
    //Using AlphaWallet directory is more convenient for developers using eg Android Studio to drop files into their phone
    private void startDirectoryListeners()
    {
        //listen for new files dropped into app external directory
        fileObserverQ = startFileListener(context.getExternalFilesDir("").getAbsolutePath());
        startAlphaWalletListener();
    }

    public void startAlphaWalletListener()
    {
        //listen for new files dropped into AlphaWallet directory, if we have permission
        if (checkReadPermission())
        {
            File alphaWalletDir = new File(
                    Environment.getExternalStorageDirectory()
                            + File.separator + HomeViewModel.ALPHAWALLET_DIR);

            if (alphaWalletDir.exists())
            {
                fileObserver = startFileListener(alphaWalletDir.getAbsolutePath());
            }
        }
    }

    public void onDestroy()
    {
        if (fileObserver != null) fileObserver.stopWatching();
        if (fileObserverQ != null) fileObserverQ.stopWatching();
        if (eventListener != null && !eventListener.isDisposed()) eventListener.dispose();
    }

    private File[] buildFileList()
    {
        List<File> fileList = new ArrayList<>();
        try
        {
            File[] files = context.getFilesDir().listFiles();
            if (files != null) fileList.addAll(Arrays.asList(files)); //first add files in app internal area - these are downloaded from the server
            files = context.getExternalFilesDir("").listFiles();
            if (files != null) fileList.addAll(Arrays.asList(files)); //now add files in the app's external directory; /Android/data/[app-name]/files. These override internal

            if (checkReadPermission())
            {
                File alphaWalletDir = new File(
                        Environment.getExternalStorageDirectory()
                                + File.separator + HomeViewModel.ALPHAWALLET_DIR);

                if (alphaWalletDir.exists())
                {
                    files = alphaWalletDir.listFiles();
                    if (files != null) fileList.addAll(Arrays.asList(files));
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        if (fileList.size() == 0) finishLoading();

        return fileList.toArray(new File[0]);
    }

    @Override
    public boolean resolveOptimisedAttr(ContractAddress contract, Attribute attr, TransactionResult transactionResult)
    {
        boolean optimised = false;
        if (attr.function == null) return false;
        Token checkToken = tokensService.getToken(contract.chainId, contract.address);
        if (checkToken == null) return false;
        boolean hasNoParams = attr.function.parameters == null || attr.function.parameters.size() == 0;

        switch (attr.function.method)
        {
            case "balanceOf":
                //ensure the arg check for this function call is checking the correct balance address
                for (MethodArg arg : attr.function.parameters)
                {
                    if (arg.parameterType.equals("address") && arg.element.ref.equals("ownerAddress"))
                    {
                        transactionResult.result = checkToken.balance.toString();
                        transactionResult.resultTime = checkToken.updateBlancaTime;
                        optimised = true;
                        break;
                    }
                }
                break;
            case "name":
                if (hasNoParams)
                {
                    transactionResult.result = checkToken.tokenInfo.name;
                    transactionResult.resultTime = checkToken.updateBlancaTime;
                    optimised = true;
                }
                break;
            case "symbol":
                if (hasNoParams)
                {
                    transactionResult.result = checkToken.tokenInfo.symbol;
                    transactionResult.resultTime = checkToken.updateBlancaTime;
                    optimised = true;
                }
                break;
            case "decimals":
                if (hasNoParams)
                {
                    transactionResult.result = String.valueOf(checkToken.tokenInfo.decimals);
                    transactionResult.resultTime = checkToken.updateBlancaTime;
                    optimised = true;
                }
                break;
        }

        return optimised;
    }

    @Override
    public String getWalletAddr()
    {
        return tokensService.getCurrentAddress();
    }

    @Override
    public long getLastTokenUpdate(int chainId, String address)
    {
        long txUpdateTime = 0;
        Token token = tokensService.getToken(chainId, address);
        if (token != null)
        {
            txUpdateTime = token.lastTxTime;
        }

        return txUpdateTime;
    };

    @Override
    public Attribute fetchAttribute(ContractInfo origin, String attributeName)
    {
        String addr = null;
        TokenDefinition td = null;
        int chainId = origin.addresses.keySet().iterator().next();
        if (origin.addresses.get(chainId).size() > 0) addr = origin.addresses.get(chainId).get(0);
        if (addr != null) td = getAssetDefinition(chainId, addr);
        if (td != null)
        {
            return td.attributes.get(attributeName);
        }
        else
        {
            return null;
        }
    }

    @Override
    public TokenScriptResult.Attribute fetchAttrResult(ContractAddress origin, Attribute attr, BigInteger tokenId)
    {
        TokenDefinition td = getAssetDefinition(origin.chainId, origin.address);
        Token originToken = tokensService.getToken(origin.chainId, origin.address);
        if (originToken == null || td == null) return null;

        //produce result
        return tokenscriptUtility.fetchAttrResult(originToken, attr, tokenId, td, this, false).blockingSingle();
    }

    public void addLocalRefs(Map<String, String> refs)
    {
        tokenscriptUtility.addLocalRefs(refs);
    }

    private Attribute getTypeFromList(String key, List<Attribute> attrList)
    {
        for (Attribute attr : attrList)
        {
            if (attr.name.equals(key)) return attr;
        }

        return null;
    }

    /**
     * Fetch attributes from local storage; not using contract lookup
     *
     * @param token
     * @param tokenId
     * @return
     */
    public TokenScriptResult getTokenScriptResult(Token token, BigInteger tokenId)
    {
        TokenScriptResult result = new TokenScriptResult();
        TokenDefinition definition = getAssetDefinition(token.tokenInfo.chainId, token.tokenInfo.address);
        if (definition != null)
        {
            for (String key : definition.attributes.keySet())
            {
                result.setAttribute(key, getTokenscriptAttr(definition, tokenId, key));
            }
        }

        return result;
    }

    private TokenScriptResult.Attribute getTokenscriptAttr(TokenDefinition td, BigInteger tokenId, String attribute)
    {
        TokenScriptResult.Attribute result = null;
        Attribute attrtype = td.attributes.get(attribute);
        try
        {
            if (attrtype == null)
            {
                return null;
            }
            else if (attrtype.event != null)
            {
                result = new TokenScriptResult.Attribute(attrtype.name, attrtype.label, tokenId, "unsupported encoding");
            }
            else if (attrtype.function != null)
            {
                //should be sourced from function
                ContractAddress cAddr = new ContractAddress(attrtype.function);
                TransactionResult tResult = getFunctionResult(cAddr, attrtype, tokenId); //t.getTokenIdResults(BigInteger.ZERO);
                result = tokenscriptUtility.parseFunctionResult(tResult, attrtype);//  attrtype.function.parseFunctionResult(tResult, attrtype);
            }
            else
            {
                BigInteger val = tokenId.and(attrtype.bitmask).shiftRight(attrtype.bitshift);
                result = new TokenScriptResult.Attribute(attrtype.name, attrtype.label, attrtype.processValue(val), attrtype.getSyntaxVal(attrtype.toString(val)));
            }
        }
        catch (Exception e)
        {
            result = new TokenScriptResult.Attribute(attrtype.name, attrtype.label, tokenId, "unsupported encoding");
        }

        return result;
    }

    public TokenScriptResult.Attribute getAttribute(Token token, BigInteger tokenId, String attribute)
    {
        TokenDefinition definition = getAssetDefinition(token.tokenInfo.chainId, token.tokenInfo.address);
        if (definition != null && definition.attributes.containsKey(attribute))
        {
            return getTokenscriptAttr(definition, tokenId, attribute);
        }
        else
        {
            return null;
        }
    }

    private boolean checkReadPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private TokenDefinition getDefinition(int chainId, String address)
    {
        TokenDefinition result = null;
        //try cache
        if (cachedDefinition != null)
        {
            //only match holding token
            ContractInfo holdingContracts = cachedDefinition.contracts.get(cachedDefinition.holdingToken);
            if (holdingContracts != null && holdingContracts.addresses.containsKey(chainId))
            {
                for (String addr : holdingContracts.addresses.get(chainId))
                {
                    if (addr.equalsIgnoreCase(address.toLowerCase())) return cachedDefinition;
                }
            }
        }

        TokenScriptFile tf = getTokenScriptFile(chainId, address);
        try
        {
            if (tf.isValidTokenScript())
            {
                cachedDefinition = parseFile(tf.getInputStream());
                result = cachedDefinition;
            }
        }
        catch (NumberFormatException e)
        {
            //no action
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public TokenScriptFile getTokenScriptFile(int chainId, String address)
    {
        if (address.equalsIgnoreCase(tokensService.getCurrentAddress()))
        {
            address = "ethereum";
        }

        if (assetDefinitions.get(chainId) != null && assetDefinitions.get(chainId).containsKey(address))
        {
            return assetDefinitions.get(chainId).get(address);
        }
        else
        {
            return new TokenScriptFile();
        }
    }


    /**
     * Get asset definition given contract address
     *
     * @param address
     * @return
     */
    public TokenDefinition getAssetDefinition(int chainId, String address)
    {
        TokenDefinition assetDef = null;
        if (address == null) return null;

        if (address.equalsIgnoreCase(tokensService.getCurrentAddress()))
        {
            address = "ethereum";
        }
        //is asset definition currently read?
        assetDef = getDefinition(chainId, address.toLowerCase());
        if (assetDef == null && !address.equals("ethereum"))
        {
            //try web
            loadScriptFromServer(address.toLowerCase()); //this will complete asynchronously and display will be updated
        }

        return assetDef; // if nothing found use default
    }

    public Single<TokenDefinition> getAssetDefinitionASync(int chainId, final String address)
    {
        if (address == null) return Single.fromCallable(TokenDefinition::new);
        String contractName = address;
        if (contractName.equalsIgnoreCase(tokensService.getCurrentAddress())) contractName = "ethereum";

        // hold until asset definitions have finished loading
        waitForAssets();

        final TokenDefinition assetDef = getDefinition(chainId, contractName.toLowerCase());
        if (assetDef != null) return Single.fromCallable(() -> assetDef);
        else if (!contractName.equals("ethereum"))
        {
            return fetchXMLFromServer(contractName.toLowerCase())
                    .map(this::handleDefinitionFile);
        }
        else return Single.fromCallable(TokenDefinition::new);
    }

    public Single<List<ContractLocator>> getAllLoadedScripts()
    {
        return Single.fromCallable(() -> {
            waitForAssets();
            return getAllOriginContracts();
        });
    }

    private void waitForAssets()
    {
        try
        {
            assetLoadingLock.acquire();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        finally
        {
            assetLoadingLock.release();
        }
    }

    private TokenDefinition handleDefinitionFile(File tokenScriptFile)
    {
        if (tokenScriptFile != null && !tokenScriptFile.getName().equals("cache") && tokenScriptFile.canRead())
        {
            try (FileInputStream input = new FileInputStream(tokenScriptFile))
            {
                TokenDefinition token = parseFile(input);
                ContractInfo holdingContracts = token.contracts.get(token.holdingToken);
                if (holdingContracts != null)
                {
                    for (int network : holdingContracts.addresses.keySet())
                    {
                        addContractsToNetwork(network,
                                              networkAddresses(holdingContracts.addresses.get(network), tokenScriptFile.getAbsolutePath()));
                    }
                    return token;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new TokenDefinition();
    }

    public String getTokenName(int chainId, String address, int count)
    {
        if (count > 2) count = 2;
        if (tokenTypeName.get(chainId) != null && tokenTypeName.get(chainId).containsKey(address)
                && tokenTypeName.get(chainId).get(address).get(count) != null)
        {
            return tokenTypeName.get(chainId).get(address).get(count);
        }
        else
        {
            TokenDefinition td = getAssetDefinition(chainId, address);
            if (td == null) return null;

            if (tokenTypeName.get(chainId) == null) tokenTypeName.put(chainId, new ConcurrentHashMap<>());
            if (!tokenTypeName.get(chainId).containsKey(address)) tokenTypeName.get(chainId).put(address, new SparseArray<>());
            tokenTypeName.get(chainId).get(address).put(count, td.getTokenName(count));
            return td.getTokenName(count);
        }
    }

    public String getTokenNameFromService(int chainId, String address)
    {
        Token token = tokensService.getToken(chainId, address);
        if (token != null) return token.getFullName();
        else return "";
    }

    public Token getTokenFromService(int chainId, String address)
    {
        return tokensService.getToken(chainId, address);
    }

    /**
     * Function returns all contracts on this network ID
     *
     * @param networkId
     * @return
     */
    public List<String> getAllContracts(int networkId)
    {
        Map<String, TokenScriptFile> networkList = assetDefinitions.get(networkId);
        if (networkList != null)
        {
            return new ArrayList<>(networkList.keySet());
        }
        else
        {
            return new ArrayList<>();
        }
    }

    /**
     * Get the issuer label given the contract address
     * Note: this is optimised so as we don't need to keep loading in definitions as the user scrolls
     *
     * @param token
     * @return
     */
    public String getIssuerName(Token token)
    {
        int chainId = token.tokenInfo.chainId;
        String address = token.tokenInfo.address;

        String issuer = token.getNetworkName();

        TokenDefinition td = getAssetDefinition(chainId, address);
        if (td == null) return issuer;

        try
        {
            TokenScriptFile tsf = getTokenScriptFile(chainId, address);
            XMLDsigDescriptor sig = getCertificateFromRealm(tsf.calcMD5());
            if (sig != null && sig.keyName != null) issuer = sig.keyName;
        }
        catch (Exception e)
        {
            System.out.println(token.getFullName());
            e.printStackTrace();
            // no action
        }

        return issuer;
    }

    private void loadScriptFromServer(String correctedAddress)
    {
        //first check the last time we tried this session
        if (assetChecked.get(correctedAddress) == null || (System.currentTimeMillis() > (assetChecked.get(correctedAddress) + 1000L*60L*60L)))
        {
            fetchXMLFromServer(correctedAddress)
                    .flatMap(this::cacheSignature)
                    .map(this::handleFileLoad)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::loadComplete, this::onError).isDisposed();
        }
    }

    private void loadComplete(String fileName)
    {
        if (BuildConfig.DEBUG) System.out.println("TS LOAD: " + fileName);
    }

    private void onError(Throwable throwable)
    {
        throwable.printStackTrace();
    }

    private TokenDefinition parseFile(InputStream xmlInputStream) throws IOException, SAXException, Exception
    {
        Locale locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = context.getResources().getConfiguration().getLocales().get(0);
        }
        else
        {
            locale = context.getResources().getConfiguration().locale;
        }

        return new TokenDefinition(
                xmlInputStream, locale, this);
    }

    private String handleFileLoad(File newFile) throws Exception
    {
        String fileLoad = "";
        if (newFile != null && !newFile.getName().equals("cache") && newFile.canRead())
        {
            List<ContractLocator> originContracts = addContractAddresses(newFile);
            //update tokensService
            for (ContractLocator cl : originContracts)
            {
                tokensService.addUnknownTokenToCheck(new ContractAddress(cl.chainId, cl.address));
            }
            fileLoad = newFile.getName();
        }

        return fileLoad;
    }

    private Single<File> fetchXMLFromServer(String address)
    {
        return Single.fromCallable(() -> {
            final File defaultReturn = new File("");
            if (address.equals("")) return defaultReturn;

            File result = getDownloadedXMLFile(address);

            //peek to see if this file exists
            long fileTime = 0;
            if (result != null && result.exists())
            {
                TokenDefinition td = getTokenDefinition(result);
                if (definitionIsOutOfDate(td))
                {
                    removeFile(result.getAbsolutePath());
                    assetChecked.put(address, 0L);
                }
                else
                {
                    fileTime = result.lastModified();
                }
            }
            else
            {
                result = defaultReturn;
            }

            if (assetChecked.get(address) != null && (System.currentTimeMillis() > (assetChecked.get(address) + 1000L*60L*60L))) return result;

            SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            String dateFormat = format.format(new Date(fileTime));

            StringBuilder sb = new StringBuilder();
            sb.append(TOKENSCRIPT_REPO_SERVER);
            sb.append(TOKENSCRIPT_CURRENT_SCHEMA);
            sb.append("/");
            sb.append(address);

            //prepare Android headers
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(
                    context.getPackageName(), 0);
            String appVersion = info.versionName;
            String OSVersion = String.valueOf(Build.VERSION.RELEASE);

            okhttp3.Response response = null;

            try
            {
                Request request = new Request.Builder()
                        .url(sb.toString())
                        .get()
                        .addHeader("Accept", "text/xml; charset=UTF-8")
                        .addHeader("X-Client-Name", "AlphaWallet")
                        .addHeader("X-Client-Version", appVersion)
                        .addHeader("X-Platform-Name", "Android")
                        .addHeader("X-Platform-Version", OSVersion)
                        .addHeader("If-Modified-Since", dateFormat)
                        .build();

                response = okHttpClient.newCall(request).execute();

                switch (response.code())
                {
                    case HttpURLConnection.HTTP_NOT_MODIFIED:
                        result = defaultReturn;
                        break;
                    case HttpURLConnection.HTTP_OK:
                        String xmlBody = response.body().string();
                        result = storeFile(address, xmlBody);
                        break;
                    default:
                        result = defaultReturn;
                        break;
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                if (response != null) response.body().close();
            }

            assetChecked.put(address, System.currentTimeMillis());

            return result;
        });
    }

    private boolean definitionIsOutOfDate(TokenDefinition td)
    {
        return td != null && !td.nameSpace.equals(TokenDefinition.TOKENSCRIPT_NAMESPACE);
    }

    private void finishLoading()
    {
        assetLoadingLock.release();
        updateEventBlockTimes();
        startEventListener();
    }

    private void addContractsToNetwork(Integer network, Map<String, File> newTokenDescriptionAddresses)
    {
        String externalDir = context.getExternalFilesDir("").getAbsolutePath();
        if (assetDefinitions.get(network) == null) assetDefinitions.put(network, new ConcurrentHashMap<>());
        for (String address : newTokenDescriptionAddresses.keySet())
        {
            String newTsFile = newTokenDescriptionAddresses.get(address).getAbsolutePath();

            if (assetDefinitions.get(network).containsKey(address))
            {
                String existingFilename = assetDefinitions.get(network).get(address).getAbsolutePath();
                boolean existingFileIsDebug = existingFilename.contains(HomeViewModel.ALPHAWALLET_DIR)
                        || existingFilename.contains(externalDir);
                boolean newFileIsDebug = newTsFile.contains(HomeViewModel.ALPHAWALLET_DIR)
                        || newTsFile.contains(externalDir);

                //remove old file if it's an active update and file is in dev area
                if (!newTsFile.equals(existingFilename) && newFileIsDebug && existingFileIsDebug)
                {
                    //delete old developer override - could be a different filename which will cause trouble later
                    removeFile(existingFilename);
                }

                if (existingFileIsDebug && !newFileIsDebug) continue;
            }

            TokenScriptFile oldTsFile = assetDefinitions.get(network).put(address, new TokenScriptFile(context, newTsFile));
            if (oldTsFile != null && !oldTsFile.getAbsolutePath().equals(newTsFile))
            {
                System.out.println("TSOverride: " + newTsFile + " Overrides " + oldTsFile.getAbsolutePath());
            }

            tokensService.addUnknownTokenToCheck(new ContractAddress(network, address));
        }
    }

    private List<String> getAllNewFiles(Map<String, File> newTokenAddresses)
    {
        List<String> newFiles = new ArrayList<>();
        for (File f : newTokenAddresses.values())
        {
            newFiles.add(f.getAbsolutePath());
        }

        return newFiles;
    }

    private void removeFile(String filename)
    {
        try
        {
            File fileToDelete = new File(filename);
            fileToDelete.delete();
        }
        catch (Exception e)
        {
            //ignore error
        }
    }

    private Map<String, File> networkAddresses(List<String> strings, String path)
    {
        Map<String, File> addrMap = new ConcurrentHashMap<>();
        for (String address : strings) addrMap.put(address, new File(path));
        return addrMap;
    }


    private boolean addContractAssets(String asset)
    {
        try (InputStream input = context.getResources().getAssets().open(asset)) {
            TokenDefinition token = parseFile(input);
            TokenScriptFile tsf = new TokenScriptFile(context, asset);
            ContractInfo holdingContracts = token.contracts.get(token.holdingToken);
            if (holdingContracts != null)
            {
                //some Android versions don't have stream()
                for (int network : holdingContracts.addresses.keySet())
                {
                    addContractsToNetwork(network, networkAddresses(holdingContracts.addresses.get(network), asset));
                    XMLDsigDescriptor AWSig = new XMLDsigDescriptor();
                    String hash = tsf.calcMD5();
                    AWSig.result = "pass";
                    AWSig.issuer = "AlphaWallet";
                    AWSig.keyName = "AlphaWallet";
                    AWSig.type = SigReturnType.SIGNATURE_PASS;
                    tsf.determineSignatureType(AWSig);
                    storeCertificateData(hash, AWSig);
                }
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public TokenDefinition getTokenDefinition(File file)
    {
        try (FileInputStream input = new FileInputStream(file)) {
            return parseFile(input);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    private List<ContractLocator> addContractAddresses(File file) throws Exception
    {
        FileInputStream input = new FileInputStream(file);
        return addContractAddresses(parseFile(input), file);
    }

    private List<ContractLocator> addContractAddresses(TokenDefinition tokenDef, File file) throws Exception
    {
        ContractInfo holdingContracts = tokenDef.contracts.get(tokenDef.holdingToken);

        if (holdingContracts != null)
        {
            addToEventList(tokenDef);
            for (int network : holdingContracts.addresses.keySet())
            {
                addContractsToNetwork(network, networkAddresses(holdingContracts.addresses.get(network), file.getAbsolutePath()));
            }

            return ContractLocator.fromContractInfo(holdingContracts);
        }

        return new ArrayList<>();
    }

    private void addToEventList(TokenDefinition tokenDef)
    {
        for (String attrName : tokenDef.attributes.keySet())
        {
            Attribute attr = tokenDef.attributes.get(attrName);
            if (attr.event != null && attr.event.contract != null)
            {
                checkAddToEventList(attr.event); //note: event definition contains link back to the contract it refers to
            }
        }

        if (tokenDef.getActivityCards().size() > 0)
        {
            for (String activityName : tokenDef.getActivityCards().keySet())
            {
                EventDefinition ev = tokenDef.getActivityEvent(activityName);
                checkAddToEventList(ev);
            }
        }
    }

    private void checkAddToEventList(EventDefinition ev)
    {
        //check we haven't already added the event
        boolean found = false;
        for (EventDefinition evCheck : eventList)
        {
            if (ev.equals(evCheck))
            {
                found = true;
                break;
            }
        }

        if (!found) eventList.add(ev);
    }

    public void stopEventListener()
    {
        if (eventListener != null && !eventListener.isDisposed()) eventListener.dispose();
        if (checkEventDisposable != null && !checkEventDisposable.isDisposed()) checkEventDisposable.dispose();
    }

    public void startEventListener()
    {
        if (assetLoadingLock.availablePermits() == 0) return;

        if (eventListener != null && !eventListener.isDisposed()) eventListener.dispose();
        eventListener =  Observable.interval(0, CHECK_TX_LOGS_INTERVAL, TimeUnit.SECONDS)
                .doOnNext(l -> {
                    checkEvents()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(() -> {}, t -> {}) //results are handled within logging function
                            .isDisposed();
                }).subscribe();
    }

    private Completable checkEvents()
    {
        //check events for corresponding tokens
        return Completable.fromAction(() -> {
            for (EventDefinition ev : eventList)
            {
                getEvent(ev);
            }
        });
    }

    private void getEvent(EventDefinition ev)
    {
        try
        {
            EthFilter filter = getEventFilter(ev);
            if (filter == null) return;
            eventConnection.acquire(); //prevent overlapping event calls
            final String walletAddress = tokensService.getCurrentAddress();
            Web3j web3j = getWeb3jService(ev.getEventChainId());

            checkEventDisposable = handleLogs(ev, filter, web3j, walletAddress)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.computation())
                    .subscribe();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private Single<String> handleLogs(EventDefinition ev, EthFilter filter, Web3j web3j, final String walletAddress)
    {
        return Single.fromCallable(() -> {
            String txHash = "";
            try
            {
                EthLog ethLogs = web3j.ethGetLogs(filter).send();
                txHash = processLogs(ev, ethLogs.getLogs(), walletAddress);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                eventConnection.release();
            }
            return txHash;
        });
        //More elegant, but requires a private node
//        return web3j.ethLogFlowable(filter)
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(log -> {
//            System.out.println("log.toString(): " +  log.toString());
//            //TODO here: callback to event service listener
//        }, this::onLogError);
    }

    private EthFilter getEventFilter(EventDefinition ev) throws Exception
    {
        int chainId = ev.getEventChainId();
        String address = ev.getEventContractAddress();

        Token originToken = tokensService.getToken(chainId, address);

        //Some events don't require a tokenId to be present, but if we don't have the contract then don't proceed. TODO: Check this assumption. Could be a contract with events with no token
        if (originToken == null) return null;

        return eventUtils.generateLogFilter(ev, originToken, this);
    }

    private String processLogs(EventDefinition ev, List<EthLog.LogResult> logs, String walletAddress)
    {
        if (logs.size() == 0) return ""; //early return
        int chainId = ev.contract.addresses.keySet().iterator().next();
        String eventAddress = ev.contract.getFirstAddress();
        Web3j web3j = getWeb3jService(chainId);

        String firstTxHash = "";

        for (EthLog.LogResult ethLog : logs)
        {
            String txHash = ((Log)ethLog.get()).getTransactionHash();
            if (TextUtils.isEmpty(firstTxHash)) firstTxHash = txHash;
            String selectVal = eventUtils.getSelectVal(ev, ethLog);
            EthBlock txBlock = eventUtils.getBlockDetails(((Log)ethLog.get()).getBlockHash(), web3j).blockingGet();
            //do we need to fetch transaction from chain or do we have it already
            com.alphawallet.app.entity.Transaction tx = transactionsCache.fetchTransaction(new Wallet(walletAddress), txHash);
            long blockTime = txBlock.getBlock().getTimestamp().longValue();
            BigInteger blockNumber = ((Log)ethLog.get()).getBlockNumber();

            if (tx == null)
            {
                //fetchTx & store, we will need the transaction info for the view data
                eventUtils.getTransactionDetails(txHash, web3j)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.computation())
                        .subscribe(ethTx -> transactionsCache.storeRawTx(new Wallet(walletAddress), ethTx, blockTime))
                        .isDisposed();
            }

            //Should store the latest event value
            storeLatestEventBlockTime(walletAddress, chainId, eventAddress, ev.type.name, ev.filter, blockNumber, blockTime);

            if (ev.parentAttribute != null)
            {
                storeEventValue(walletAddress, ev, ethLog, ev.parentAttribute, blockTime, selectVal);
            }
            else
            {
                //Activity event - store as event but first let's pass this to activity pane
                //To produce event we need:
                //all the elements in the sequence (can use a CSV string)
                //block#, timeStamp
                //TODO: normalise this in a new database to allow it to be indexable
                storeActivityValue(walletAddress, ev, ethLog, blockTime, ev.activityName);
            }
        }

        return firstTxHash;
    }

    private void storeLatestEventBlockTime(String walletAddress, int chainId, String eventAddress, String namedType, String filter, BigInteger readBlock, long timeStamp)
    {
        try (Realm realm = realmManager.getRealmInstance(walletAddress))
        {
            String databaseKey = TokensRealmSource.eventBlockKey(chainId, eventAddress, namedType, filter);
            realm.executeTransaction(r -> {
                RealmAuxData realmToken = realm.where(RealmAuxData.class)
                        .equalTo("instanceKey", databaseKey)
                        .findFirst();
                if (realmToken == null) realmToken = realm.createObject(RealmAuxData.class, databaseKey);
                realmToken.setResultTime(timeStamp);
                realmToken.setResult(readBlock.toString(16));
                realmToken.setFunctionId("");
                realmToken.setChainId(chainId);
                realmToken.setTokenAddress("");
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void storeActivityValue(String walletAddress, EventDefinition ev, EthLog.LogResult log, long blockTime, String activityName)
    {
        //split out all the event data
        String valueList = eventUtils.getAllTopics(ev, log);

        ContractAddress eventContractAddress = new ContractAddress(ev.getEventChainId(),
                ev.getEventContractAddress());

        //store this data
        String txHash = ((Log) log.get()).getTransactionHash();
        storeAuxData(walletAddress, txHash, BigInteger.ZERO, valueList, activityName, eventContractAddress, ev.type.name, blockTime); //store the event itself
    }

    private void storeAuxData(String walletAddress, String txHash, BigInteger tokenId, String eventData, String activityName, ContractAddress cAddr, String eventName, long blockTime)
    {
        try (Realm realm = realmManager.getRealmInstance(walletAddress))
        {
            String databaseKey = TokensRealmSource.eventKey(txHash, eventName);
            realm.executeTransaction(r -> {
                RealmAuxData realmToken = realm.where(RealmAuxData.class)
                        .equalTo("instanceKey", databaseKey)
                        .findFirst();
                if (realmToken == null) realmToken = realm.createObject(RealmAuxData.class, databaseKey);
                realmToken.setResultTime(blockTime);
                realmToken.setResult(eventData);
                realmToken.setFunctionId(activityName);
                realmToken.setChainId(cAddr.chainId);
                realmToken.setTokenId(tokenId.toString(16));
                realmToken.setTokenAddress(cAddr.address);
                realmToken.setResultReceivedTime(System.currentTimeMillis());
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void storeEventValue(String walletAddress, EventDefinition ev, EthLog.LogResult log, Attribute attr,
                                 long blockTime, String selectVal)
    {
        //store result
        String filterTopicValue = ev.getFilterTopicValue();
        TransactionResult txResult;
        BigInteger tokenId;

        if (filterTopicValue.equals("tokenId"))
        {
            String tokenIdStr = eventUtils.getTopicVal(ev, log);
            if (tokenIdStr.startsWith("0x"))
            {
                tokenId = Numeric.toBigInt(tokenIdStr);
            }
            else
            {
                tokenId = new BigInteger(tokenIdStr);
            }
        }
        else
        {
            tokenId = BigInteger.ZERO;
        }

        String txHash = ((Log)log.get()).getTransactionHash();

        ContractAddress eventContractAddress = new ContractAddress(ev.getEventChainId(),
                ev.getEventContractAddress());
        txResult = getFunctionResult(eventContractAddress, attr, tokenId);
        txResult.result = attr.getSyntaxVal(selectVal);

        //Updates the entry for the attribute if required
        if (txResult.resultTime == 0 || blockTime >= txResult.resultTime)
        {
            txResult.resultTime = blockTime;
            storeAuxData(walletAddress, txResult);
        }

        txResult.resultTime = blockTime;
        //Always store the event for the event log
        storeAuxData(walletAddress, txHash, tokenId, txResult.result, ev.attributeName, eventContractAddress, ev.type.name, blockTime);
    }

    private boolean allowableExtension(File file)
    {
        int index = file.getName().lastIndexOf(".");
        if (index >= 0)
        {
            String extension = file.getName().substring(index+1);
            switch (extension)
            {
                case "xml":
                case "tsml":
                    return true;
                default:
                    break;
            }
        }

        return false;
    }

    private String getFileName(File file)
    {
        String name = file.getName();
        int index = name.lastIndexOf(".");
        if (index > 0)
        {
            return name.substring(0, index);
        }
        else
        {
            return null;
        }
    }

    private boolean isAddress(File file)
    {
        String name = getFileName(file);
        if (name != null) return Utils.isAddressValid(name);
        else return false;
    }

    /**
     * This is used to retrieve the file from the secure area in order to check the date.
     * Note: it only finds files previously downloaded from the server
     * @param contractAddress
     * @return
     */
    private File getDownloadedXMLFile(String contractAddress)
    {
        //if in secure area will simply be address + XML
        String filename = contractAddress + ".xml";
        File file = new File(context.getFilesDir(), filename);
        if (file.exists() && file.canRead())
        {
            return file;
        }

        File[] files = context.getFilesDir().listFiles();
        for (File f : files)
        {
            if (f.getName().equalsIgnoreCase(filename)) return f;
        }

        return null;
    }

    private List<String> getScriptsInSecureZone()
    {
        List<String> checkScripts = new ArrayList<>();
        File[] files = context.getFilesDir().listFiles();
        Observable.fromArray(files)
                .filter(File::isFile)
                .filter(this::allowableExtension)
                .filter(File::canRead)
                .filter(this::isAddress)
                .forEach(file -> checkScripts.add(getFileName(file)) ).isDisposed();

        return checkScripts;
    }

    private boolean isInSecureZone(File file)
    {
        return file.getPath().contains(context.getFilesDir().getPath());
    }

    /**
     * check the repo server for updates to downloaded TokenScripts
     */
    private void checkDownloadedFiles()
    {
        Observable.fromIterable(getScriptsInSecureZone())
                .concatMap(addr -> fetchXMLFromServer(addr).toObservable())
                .map(this::handleFileLoad)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::loadComplete, this::onError).isDisposed();
    }

    /* Add cached signature if uncached files found. */
    private Single<File> cacheSignature(File file)
    {
        // note that outdated cache is never deleted - we don't have that level of finesse:
        // Note from developer to commenter above: outdated certificate is simply replaced in the realm - there's no history.
        //      However there is an issue here - if the tokenscript is removed then this entry will be orphaned.
        //      Once we cache the tokenscript contracts we will know if the script has been removed and can remove this file too.
        return Single.fromCallable(() -> {
            if (file.canRead())
            {
                TokenScriptFile tsf = new TokenScriptFile(context, file.getAbsolutePath());
                String hash = tsf.calcMD5();

                //pull data from realm
                XMLDsigDescriptor sig = getCertificateFromRealm(hash);
                if (sig == null || sig.keyName == null)
                {
                    //fetch signature and store in realm
                    sig = alphaWalletService.checkTokenScriptSignature(tsf);
                    tsf.determineSignatureType(sig);
                    storeCertificateData(hash, sig);
                }
            }

            return file;
        });
    }

    private void storeCertificateData(String hash, XMLDsigDescriptor sig)
    {
        try (Realm realm = realmManager.getRealmInstance(CERTIFICATE_DB))
        {
            //if signature present, then just update
            RealmCertificateData realmData = realm.where(RealmCertificateData.class)
                    .equalTo("instanceKey", hash)
                    .findFirst();

            TransactionsRealmCache.addRealm();
            realm.beginTransaction();
            if (realmData == null) realmData = realm.createObject(RealmCertificateData.class, hash);
            realmData.setFromSig(sig);
            realm.commitTransaction();
            realm.close();
            TransactionsRealmCache.subRealm();
        }
        catch (Exception e)
        {
            TransactionsRealmCache.subRealm();
            e.printStackTrace();
        }
    }

    private XMLDsigDescriptor getCertificateFromRealm(String hash)
    {
        XMLDsigDescriptor sig = null;
        try (Realm realm = realmManager.getRealmInstance(CERTIFICATE_DB))
        {
            RealmCertificateData realmCert = realm.where(RealmCertificateData.class)
                    .equalTo("instanceKey", hash)
                    .findFirst();

            if (realmCert != null)
            {
                sig = new XMLDsigDescriptor();
                sig.issuer = realmCert.getIssuer();
                sig.certificateName = realmCert.getCertificateName();
                sig.keyName = realmCert.getKeyName();
                sig.keyType = realmCert.getKeyType();
                sig.result = realmCert.getResult();
                sig.subject = realmCert.getSubject();
                sig.type = realmCert.getType();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return sig;
    }

    /**
     * Use internal directory to store contracts fetched from the server
     * @param address
     * @param result
     * @return
     * @throws
     */
    private File storeFile(String address, String result) throws IOException
    {
        if (result == null || result.length() < 10) return null;

        String fName = address + ".xml";

        //Store received files in the internal storage area - no need to ask for permissions
        File file = new File(context.getFilesDir(), fName);

        FileOutputStream fos = new FileOutputStream(file);
        OutputStream os = new BufferedOutputStream(fos);
        os.write(result.getBytes());
        fos.flush();
        os.close();
        fos.close();
        return file;
    }

    public boolean hasDefinition(int chainId, String address)
    {
        if (address.equalsIgnoreCase(tokensService.getCurrentAddress()))
        {
            address = "ethereum";
        }
        return assetDefinitions.get(chainId) != null && assetDefinitions.get(chainId).containsKey(address);
    }

    //when user reloads the tokens we should also check XML for any files
    public void clearCheckTimes()
    {
        assetChecked.clear();
    }

    public boolean hasTokenView(int chainId, String contractAddr, String type)
    {
        TokenDefinition td = getAssetDefinition(chainId, contractAddr);
        return td != null && td.hasTokenView();
    }

    public String getTokenView(int chainId, String contractAddr, String type)
    {
        String viewHTML = "";
        TokenDefinition td = getAssetDefinition(chainId, contractAddr);
        if (td != null)
        {
            viewHTML = td.getTokenView(type);
        }

        return viewHTML;
    }

    public String getTokenViewStyle(int chainId, String contractAddr, String type)
    {
        String styleData = "";
        TokenDefinition td = getAssetDefinition(chainId, contractAddr);
        if (td != null)
        {
            styleData = td.getTokenViewStyle(type);
        }

        return styleData;
    }

    public List<Attribute> getTokenViewLocalAttributes(int chainId, String contractAddr)
    {
        TokenDefinition td = getAssetDefinition(chainId, contractAddr);
        List<Attribute> results = new ArrayList<>();
        if (td != null)
        {
            Map<String, Attribute> attrMap = td.getTokenViewLocalAttributes();
            results.addAll(attrMap.values());
        }

        return results;
    }

    public Map<String, TSAction> getTokenFunctionMap(int chainId, String contractAddr)
    {
        TokenDefinition td = getAssetDefinition(chainId, contractAddr);
        if (td != null)
        {
            return td.getActions();
        }
        else
        {
            return null;
        }
    }

    /**
     * Build a map of all available tokenIds to a list of available functions for that tokenId
     *
     * @param token
     * @return map of unique tokenIds to lists of allowed functions for that ID - note that we allow the function to be displayed if it has a denial message
     */
    public Single<Map<BigInteger, List<String>>> fetchFunctionMap(Token token)
    {
        return Single.fromCallable(() -> {
            Map<BigInteger, List<String>> validActions = new HashMap<>();
            TokenDefinition td = getAssetDefinition(token.tokenInfo.chainId, token.getAddress());
            if (td != null)
            {
                List<BigInteger> tokenIds = token.getUniqueTokenIds();
                Map<String, TSAction> actions = td.getActions();
                //first gather all attrs required - do this so if there's multiple actions using the same attribute for a tokenId we aren't fetching the value repeatedly
                List<String> requiredAttrNames = getRequiredAttributeNames(actions, td);
                Map<BigInteger, Map<String, TokenScriptResult.Attribute>> attrResults   // Map of attribute results vs tokenId
                        = getRequiredAttributeResults(requiredAttrNames, tokenIds, td, token); // Map of all required attribute values vs all the tokenIds

                for (BigInteger tokenId : tokenIds)
                {
                    for (String actionName : actions.keySet())
                    {
                        TSAction action = actions.get(actionName);
                        TSSelection selection = action.exclude != null ? td.getSelection(action.exclude) : null;
                        if (selection == null)
                        {
                            if (!validActions.containsKey(tokenId)) validActions.put(tokenId, new ArrayList<>());
                            validActions.get(tokenId).add(actionName);
                        }
                        else
                        {
                            //get required Attribute Results for this tokenId & selection
                            List<String> requiredAttributeNames = selection.getRequiredAttrs();
                            Map<String, TokenScriptResult.Attribute> idAttrResults = getAttributeResultsForTokenIds(attrResults, requiredAttributeNames, tokenId);
                            addIntrinsicAttributes(idAttrResults, token, tokenId); //adding intrinsic attributes eg ownerAddress, tokenId, contractAddress

                            //Now evaluate the selection
                            boolean exclude = EvaluateSelection.evaluate(selection.head, idAttrResults);
                            if (!exclude || selection.denialMessage != null)
                            {
                                if (!validActions.containsKey(tokenId)) validActions.put(tokenId, new ArrayList<>());
                                validActions.get(tokenId).add(actionName);
                            }
                        }
                    }
                }
            }

            return validActions;
        });
    }

    private void addIntrinsicAttributes(Map<String, TokenScriptResult.Attribute> attrs, Token token, BigInteger tokenId)
    {
        //add tokenId, ownerAddress & contractAddress
        attrs.put("tokenId", new TokenScriptResult.Attribute("tokenId", "tokenId", tokenId, tokenId.toString(10)));
        attrs.put("ownerAddress", new TokenScriptResult.Attribute("ownerAddress", "ownerAddress", BigInteger.ZERO, token.getWallet()));
        attrs.put("contractAddress", new TokenScriptResult.Attribute("contractAddress", "contractAddress", BigInteger.ZERO, token.getAddress()));
    }

    public String checkFunctionDenied(Token token, String actionName, List<BigInteger> tokenIds)
    {
        String denialMessage = null;
        TokenDefinition td = getAssetDefinition(token.tokenInfo.chainId, token.getAddress());
        if (td != null)
        {
            BigInteger tokenId = tokenIds != null ? tokenIds.get(0) : BigInteger.ZERO;
            TSAction action = td.actions.get(actionName);
            TSSelection selection = action.exclude != null ? td.getSelection(action.exclude) : null;
            if (selection != null)
            {
                //gather list of attribute results
                List<String> requiredAttrs = selection.getRequiredAttrs();
                //resolve all these attrs
                Map<String, TokenScriptResult.Attribute> attrs = new HashMap<>();
                //get results
                for (String attrId : requiredAttrs)
                {
                    Attribute attr = td.attributes.get(attrId);
                    if (attr == null) continue;
                    TokenScriptResult.Attribute attrResult = tokenscriptUtility.fetchAttrResult(token, attr, tokenId, td, this, false).blockingSingle();
                    if (attrResult != null) attrs.put(attrId, attrResult);
                }

                addIntrinsicAttributes(attrs, token, tokenId);

                boolean exclude = EvaluateSelection.evaluate(selection.head, attrs);
                if (exclude && !TextUtils.isEmpty(selection.denialMessage))
                {
                    denialMessage = selection.denialMessage;
                }
            }
        }

        return denialMessage;
    }

    private Map<String, TokenScriptResult.Attribute> getAttributeResultsForTokenIds(Map<BigInteger, Map<String, TokenScriptResult.Attribute>> attrResults, List<String> requiredAttributeNames, BigInteger tokenId)
    {
        Map<String, TokenScriptResult.Attribute> results = new HashMap<>();
        if (!attrResults.containsKey(tokenId)) return results; //check values

        for (String attributeName : requiredAttributeNames)
        {
            results.put(attributeName, attrResults.get(tokenId).get(attributeName));
        }

        return results;
    }

    private Map<BigInteger, Map<String, TokenScriptResult.Attribute>> getRequiredAttributeResults(List<String> requiredAttrNames, List<BigInteger> tokenIds, TokenDefinition td, Token token)
    {
        Map<BigInteger, Map<String, TokenScriptResult.Attribute>> resultSet = new HashMap<>();
        for (BigInteger tokenId : tokenIds)
        {
            for (String attrName : requiredAttrNames)
            {
                Attribute attr = td.attributes.get(attrName);
                if (attr == null) continue;
                TokenScriptResult.Attribute attrResult = tokenscriptUtility.fetchAttrResult(token, attr, tokenId, td, this, false).blockingSingle();
                if (attrResult != null)
                {
                    Map<String, TokenScriptResult.Attribute> tokenIdMap = resultSet.get(tokenId);
                    if (tokenIdMap == null)
                    {
                        tokenIdMap = new HashMap<>();
                        resultSet.put(tokenId, tokenIdMap);
                    }
                    tokenIdMap.put(attrName, attrResult);
                }
            }
        }

        return resultSet;
    }

    private List<String> getRequiredAttributeNames(Map<String, TSAction> actions, TokenDefinition td)
    {
        List<String> requiredAttrs = new ArrayList<>();
        for (String actionName : actions.keySet())
        {
            TSAction action = actions.get(actionName);
            TSSelection selection = action.exclude != null ? td.getSelection(action.exclude) : null;
            if (selection != null)
            {
                List<String> attrNames = selection.getRequiredAttrs();
                for (String attrName : attrNames)
                {
                    if (!requiredAttrs.contains(attrName))
                        requiredAttrs.add(attrName);
                }
            }
        }

        return requiredAttrs;
    }

    @Override
    public void parseMessage(ParseResultId parseResult)
    {
        switch (parseResult)
        {
            case PARSER_OUT_OF_DATE:
                HomeActivity.setUpdatePrompt();
                break;
            case XML_OUT_OF_DATE:
                break;
            case OK:
                break;
        }
    }

    private FileObserver startFileListener(String path)
    {
        FileObserver observer = new FileObserver(path)
        {
            private final String listenerPath = path;
            @Override
            public void onEvent(int event, @Nullable String file)
            {
                //watch for new files and file change
                switch (event)
                {
                    case CREATE:
                        //if this file already exists then wait for the modify
                        File checkFile = new File(listenerPath, file);
                        if (checkFile.exists() && checkFile.canRead())
                        {
                            break;
                        }
                    case MODIFY:
                        try
                        {
                            if (file.contains(".xml") || file.contains(".tsml"))
                            {
                                //form filename
                                TokenScriptFile newTSFile = new TokenScriptFile(context, listenerPath, file);
                                List<ContractLocator> originContracts = addContractAddresses(newTSFile);

                                if (originContracts.size() > 0)
                                {
                                    notificationService.DisplayNotification("Definition Updated", file, NotificationCompat.PRIORITY_MAX);
                                    cachedDefinition = null;
                                    cacheSignature(newTSFile) //update signature data if necessary
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe().isDisposed();

                                    //update tokensService
                                    for (ContractLocator cl : originContracts)
                                    {
                                        tokensService.addUnknownTokenToCheck(new ContractAddress(cl.chainId, cl.address));
                                    }
                                }
                            }
                        }
                        catch (Exception e)
                        {
                            if (homeMessenger != null) homeMessenger.tokenScriptError(e.getMessage());
                        }
                        break;
                    default:
                        break;
                }
            }
        };

        observer.startWatching();

        return observer;
    }

    public void checkTokenscriptEnabledTokens(TokensService tokensService)
    {
        for (int i = 0; i < assetDefinitions.size(); i++)
        {
            int networkId = assetDefinitions.keyAt(i);
            Map<String, TokenScriptFile> defMap = assetDefinitions.valueAt(i);
            for (String address : defMap.keySet())
            {
                Token token = tokensService.getToken(networkId, address);
                if (token != null)
                {
                    TokenScriptFile tokenDef = defMap.get(address);
                    token.hasTokenScript = true;
                    TokenDefinition td = getAssetDefinition(networkId, address);
                    if (td != null && td.contracts != null)
                    {
                        ContractInfo cInfo = td.contracts.get(td.holdingToken);
                        if (cInfo != null) checkCorrectInterface(token, cInfo.contractInterface);
                    }
                }
            }
        }
    }

    public Single<XMLDsigDescriptor> getSignatureData(int chainId, String contractAddress)
    {
        return Single.fromCallable(() -> {
            XMLDsigDescriptor sigDescriptor = new XMLDsigDescriptor();
            sigDescriptor.result = "fail";
            sigDescriptor.type = SigReturnType.NO_TOKENSCRIPT;

            TokenScriptFile tsf = getTokenScriptFile(chainId, contractAddress);
            if (tsf != null && tsf.isValidTokenScript())
            {
                String hash = tsf.calcMD5();
                XMLDsigDescriptor sig = getCertificateFromRealm(hash);
                if (sig == null)
                {
                    sig = alphaWalletService.checkTokenScriptSignature(tsf);
                    tsf.determineSignatureType(sig);
                    storeCertificateData(hash, sig);
                }
                sigDescriptor = sig;
            }

            return sigDescriptor;
        });
    }

    private void checkCorrectInterface(Token token, String contractInterface)
    {
        ContractType cType;
        switch (contractInterface.toLowerCase())
        {
            case "erc875":
                cType = ContractType.ERC875;
                if (token.isERC875()) return;
                break;
            case "erc20":
                cType = ContractType.ERC20;
                break;
            // note: ERC721 and ERC721Ticket are contracts with different interfaces which are handled in different ways but we describe them
            // as the same within the tokenscript.
            case "erc721":
                if (token.isERC721() || token.isERC721Ticket()) return;
                cType = ContractType.ERC721;
                break;
            case "erc721ticket":
                if (token.isERC721() || token.isERC721Ticket()) return;
                cType = ContractType.ERC721_TICKET;
                break;
            case "ethereum":
                cType = ContractType.ETHEREUM;
                break;
            default:
                cType = ContractType.OTHER;
                break;
        }

        if (cType == ContractType.OTHER) return;
        if (cType == token.getInterfaceSpec()) return;

        //contract mismatch, re-assign
        //first delete from database
        tokenLocalSource.deleteRealmToken(token.tokenInfo.chainId, new Wallet(token.getWallet()), token.tokenInfo.address);

        //now store into database
        //TODO: if erc20 refresh all values
        TokenFactory tf = new TokenFactory();

        Token newToken = tf.createToken(token.tokenInfo, BigDecimal.ZERO, null, 0, cType, token.getNetworkName(), 0);
        newToken.setTokenWallet(token.getWallet());
        newToken.walletUIUpdateRequired = true;
        newToken.updateBlancaTime = 0;

        tokenLocalSource.saveToken(new Wallet(token.getWallet()), newToken)
                .subscribeOn(Schedulers.io())
                .subscribe()
                .isDisposed();
    }


    //Database functions
    private String functionKey(ContractAddress cAddr, BigInteger tokenId, String attrId)
    {
        //produce a unique key for this. token address, token Id, chainId
        return cAddr.address + "-" + tokenId.toString(Character.MAX_RADIX) + "-" + cAddr.chainId + "-" + attrId;
    }

    private String eventKey(TransactionResult tResult)
    {
        return tResult.contractAddress + "-" + tResult.tokenId.toString(Character.MAX_RADIX) + "-" + tResult.contractChainId + "-" + tResult.attrId + tResult.resultTime + "-log";
    }

    @Override
    public TransactionResult getFunctionResult(ContractAddress contract, Attribute attr, BigInteger tokenId)
    {
        TransactionResult tr = new TransactionResult(contract.chainId, contract.address, tokenId, attr);
        String dataBaseKey = functionKey(contract, tokenId, attr.name);
        try (Realm realm = realmManager.getRealmInstance(tokensService.getCurrentAddress()))
        {
            RealmAuxData realmToken = realm.where(RealmAuxData.class)
                    .equalTo("instanceKey", dataBaseKey)
                    .equalTo("chainId", contract.chainId)
                    .findFirst();

            if (realmToken != null)
            {
                tr.resultTime = realmToken.getResultTime();
                tr.result = realmToken.getResult();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return tr;
    }

    @Override
    public TransactionResult storeAuxData(String walletAddress, TransactionResult tResult)
    {
        if (tokensService.getCurrentAddress() == null || !WalletUtils.isValidAddress(tokensService.getCurrentAddress())) return tResult;
        if (tResult.result == null || tResult.resultTime < 0) return tResult;
        try (Realm realm = realmManager.getRealmInstance(walletAddress))
        {
            ContractAddress cAddr = new ContractAddress(tResult.contractChainId, tResult.contractAddress);
            String databaseKey = functionKey(cAddr, tResult.tokenId, tResult.attrId);
            RealmAuxData realmToken = realm.where(RealmAuxData.class)
                    .equalTo("instanceKey", databaseKey)
                    .equalTo("chainId", tResult.contractChainId)
                    .findFirst();

            realm.executeTransaction(r -> {
                if (realmToken == null)
                {
                    createAuxData(realm, tResult, databaseKey);
                }
                else if (tResult.result != null)
                {
                    realmToken.setResult(tResult.result);
                    realmToken.setResultTime(tResult.resultTime);
                    realmToken.setResultReceivedTime(System.currentTimeMillis());
                }
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return tResult;
    }

    private void updateEventBlockTimes()
    {
        try (Realm realm = realmManager.getRealmInstance(tokensService.getCurrentAddress()))
        {
            RealmResults<RealmAuxData> realmEvents = realm.where(RealmAuxData.class)
                    .endsWith("instanceKey", "-eventBlock")
                    .sort("resultTime", Sort.ASCENDING)
                    .findAll();

            for (RealmAuxData eventData : realmEvents)
            {
                updateEventList(eventData);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void checkLegacyEventData()
    {
        try (Realm realm = realmManager.getRealmInstance(tokensService.getCurrentAddress()))
        {
            RealmResults<RealmAuxData> realmEvents = realm.where(RealmAuxData.class)
                    .endsWith("instanceKey", "-eventName")
                    .not().beginsWith("tokenAddress", "0x")
                    .findAll();

            if (realmEvents.size() > 0)
            {
                RealmResults<RealmAuxData> realmBlockTimes = realm.where(RealmAuxData.class)
                        .endsWith("instanceKey", "-eventBlock")
                        .sort("resultTime", Sort.ASCENDING)
                        .findAll();
                //re-do all these events and reset block times
                realm.beginTransaction();
                realmEvents.deleteAllFromRealm();
                realmBlockTimes.deleteAllFromRealm();
                realm.commitTransaction();
            }
        }
        catch (Exception e)
        {
            //
        }
    }

    //TODO: Can this be optimised? Maybe a 2 dimensional map using the namedType name as the first key, then filter as the second key
    //TODO: This would replace the event list
    private void updateEventList(RealmAuxData eventData)
    {
        String[] contractDetails = eventData.getInstanceKey().split("-");
        if (contractDetails.length != 5) return;
        String eventAddress = contractDetails[0];
        int chainId = Integer.parseInt(contractDetails[1]);
        String namedType = contractDetails[2];
        String filter = contractDetails[3];

        for (EventDefinition ev : eventList)
        {
            String evContract = ev.contract.getFirstAddress();
            int evChainId =  ev.contract.getfirstChainId();

            if (evChainId == chainId && evContract.equalsIgnoreCase(eventAddress) && ev.type.name.equals(namedType) && ev.filter.equals(filter))
            {
                ev.readBlock = new BigInteger(eventData.getResult(), 16).add(BigInteger.ONE); // add one so we don't pick up the same event again
            }
        }
    }

    private void createAuxData(Realm realm, TransactionResult tResult, String dataBaseKey)
    {
        try
        {
            //ContractAddress cAddr = new ContractAddress(tResult.contractChainId, tResult.contractAddress);
            RealmAuxData realmData = realm.createObject(RealmAuxData.class, dataBaseKey);
            realmData.setResultTime(tResult.resultTime);
            realmData.setResult(tResult.result);
            realmData.setChainId(tResult.contractChainId);
            realmData.setFunctionId(tResult.method);
            realmData.setTokenId(tResult.tokenId.toString(Character.MAX_RADIX));
            realmData.setResultReceivedTime(System.currentTimeMillis());
        }
        catch (RealmPrimaryKeyConstraintException e)
        {
            //in theory we should never see this
            e.printStackTrace();
        }
    }


    //private Token

    private void addOpenSeaAttributes(StringBuilder attrs, Token erc721Token, BigInteger tokenId)
    {
        Asset tokenAsset = erc721Token.getAssetForToken(tokenId.toString());
        if(tokenAsset == null) return;

        try
        {
            if (tokenAsset.getBackgroundColor() != null) TokenScriptResult.addPair(attrs, "background_colour", URLEncoder.encode(tokenAsset.getBackgroundColor(), "utf-8"));
            if (tokenAsset.getImagePreviewUrl() != null) TokenScriptResult.addPair(attrs, "image_preview_url", URLEncoder.encode(tokenAsset.getImagePreviewUrl(), "utf-8"));
            if (tokenAsset.getDescription() != null) TokenScriptResult.addPair(attrs, "description", URLEncoder.encode(tokenAsset.getDescription(), "utf-8"));
            if (tokenAsset.getExternalLink() != null) TokenScriptResult.addPair(attrs, "external_link", URLEncoder.encode(tokenAsset.getExternalLink(), "utf-8"));
            if (tokenAsset.getTraits() != null) TokenScriptResult.addPair(attrs, "traits", tokenAsset.getTraits());
            if (tokenAsset.getName() != null) TokenScriptResult.addPair(attrs, "name", URLEncoder.encode(tokenAsset.getName(), "utf-8"));
        }
        catch (UnsupportedEncodingException e)
        {
            //
        }
    }

    public StringBuilder getTokenAttrs(Token token, BigInteger tokenId, int count)
    {
        StringBuilder attrs = new StringBuilder();

        TokenDefinition definition = getAssetDefinition(token.tokenInfo.chainId, token.tokenInfo.address);
        String name = token.getTokenTitle();
        if (definition != null && definition.getTokenName(1) != null)
        {
            name = definition.getTokenName(1);
        }
        TokenScriptResult.addPair(attrs, "name", name);
        TokenScriptResult.addPair(attrs, "label", name);
        TokenScriptResult.addPair(attrs, "symbol", token.getSymbol());
        TokenScriptResult.addPair(attrs, "_count", String.valueOf(count));
        TokenScriptResult.addPair(attrs, "contractAddress", token.tokenInfo.address);
        TokenScriptResult.addPair(attrs, "chainId", String.valueOf(token.tokenInfo.chainId));
        TokenScriptResult.addPair(attrs, "tokenId", tokenId);
        TokenScriptResult.addPair(attrs, "ownerAddress", token.getWallet());

        if(token instanceof ERC721Token)
        {
            addOpenSeaAttributes(attrs, token, tokenId);
        }

        if (token.isEthereum())
        {
            TokenScriptResult.addPair(attrs, "balance", token.balance.toString());
        }

        return attrs;
    }

    /**
     * Get all the magic values - eg native crypto balances for all chains
     * @return
     */
    public String getMagicValuesForInjection(int chainId) throws Exception
    {
        String walletBalance = "walletBalance";
        String prefix = "web3.eth";
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n");
        Token nativeCurrency = tokensService.getToken(chainId, tokensService.getCurrentAddress());
        sb.append(prefix).append(" = {\n").append(walletBalance).append(": ").append(nativeCurrency.balance.toString()).append("\n}\n");

        List<Token> nativeCurrencies = tokensService.getAllAtAddress(tokensService.getCurrentAddress());
        for (Token currency : nativeCurrencies)
        {
            sb.append(prefix).append("_").append(currency.tokenInfo.chainId).append(" = {\n").append(walletBalance).append(": ").append(currency.balance.toString()).append("\n}\n");
        }

        sb.append("\n\n");

        return sb.toString();
    }

    public void clearResultMap()
    {
        tokenscriptUtility.clearParseMaps();
    }

    public Observable<TokenScriptResult.Attribute> resolveAttrs(Token token, BigInteger tokenId, List<Attribute> extraAttrs, boolean itemView)
    {
        TokenDefinition definition = getAssetDefinition(token.tokenInfo.chainId, token.tokenInfo.address);
        ContractAddress cAddr = new ContractAddress(token.tokenInfo.chainId, token.tokenInfo.address);
        if (definition == null) return Observable.fromCallable(() -> new TokenScriptResult.Attribute("RAttrs", "", BigInteger.ZERO, ""));

        definition.context = new TokenscriptContext();
        definition.context.cAddr = cAddr;
        definition.context.attrInterface = this;

        List<Attribute> attrList = new ArrayList<>(definition.attributes.values());
        if (extraAttrs != null) attrList.addAll(extraAttrs);

        tokenscriptUtility.buildAttrMap(attrList);

        return Observable.fromIterable(attrList)
                .flatMap(attr -> tokenscriptUtility.fetchAttrResult(token, attr, tokenId,
                                                                    definition, this, itemView));
    }

    public Observable<TokenScriptResult.Attribute> resolveAttrs(Token token, List<BigInteger> tokenIds, List<Attribute> extraAttrs)
    {
        TokenDefinition definition = getAssetDefinition(token.tokenInfo.chainId, token.tokenInfo.address);
        //pre-fill tokenIds
        for (Attribute attrType : definition.attributes.values())
        {
            resolveTokenIds(attrType, tokenIds);
        }

        //TODO: store transaction fetch time for multiple tokenIds

        return resolveAttrs(token, tokenIds.get(0), extraAttrs, false);
    }

    private void resolveTokenIds(Attribute attrType, List<BigInteger> tokenIds)
    {
        if (attrType.function == null) return;

        for (MethodArg arg : attrType.function.parameters)
        {
            int index = arg.getTokenIndex();
            if (arg.isTokenId() && index >= 0 && index < tokenIds.size())
            {
                arg.element.value = tokenIds.get(index).toString();
            }
        }
    }

    private List<String> getLocalTSMLFiles()
    {
        List<String> localTSMLFilesStr = new ArrayList<>();
        AssetManager mgr = context.getResources().getAssets();
        try
        {
            String[] filelist = mgr.list("");
            for (String file : filelist)
            {
                if (file.contains("tsml"))
                {
                    localTSMLFilesStr.add(file);
                }
            }

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return localTSMLFilesStr;
    }

    public String generateTransactionPayload(Token token, BigInteger tokenId, FunctionDefinition def)
    {
        TokenDefinition td = getAssetDefinition(token.tokenInfo.chainId, token.tokenInfo.address);
        if (td == null) return "";
        Function function = tokenscriptUtility.generateTransactionFunction(token, tokenId, td, def, this);
        if (function.getInputParameters() == null)
        {
            return null;
        }
        else
        {
            return FunctionEncoder.encode(function);
        }
    }

    /**
     * Clear the currently cached definition. This forces the service to reload the definition so it's clean for the next usage.
     */
    public void clearCache()
    {
        cachedDefinition = null;
    }

    public boolean viewsEqual(Token token)
    {
        String view = getTokenView(token.tokenInfo.chainId, token.tokenInfo.address, ASSET_DETAIL_VIEW_NAME);
        String iconifiedView = getTokenView(token.tokenInfo.chainId, token.tokenInfo.address, ASSET_SUMMARY_VIEW_NAME);
        if (view == null || iconifiedView == null) return false;
        else return view.equals(iconifiedView);
    }

    public List<ContractLocator> getHoldingContracts(TokenDefinition td)
    {
        List<ContractLocator> holdingContracts = new ArrayList<>();
        ContractInfo holdingContractInfo = td.contracts.get(td.holdingToken);
        if (holdingContractInfo == null || holdingContractInfo.addresses.size() == 0) return null;
        for (int chainId : holdingContractInfo.addresses.keySet())
        {
            for (String address : holdingContractInfo.addresses.get(chainId))
            {
                holdingContracts.add(new ContractLocator(address, chainId));
            }
        }

        return holdingContracts;
    }

    public ContractLocator getHoldingContract(String importFileName)
    {
        ContractLocator cr = null;
        for (int i = 0; i < assetDefinitions.size(); i++)
        {
            int chainId = assetDefinitions.keyAt(i);
            for (String address : assetDefinitions.get(chainId).keySet())
            {
                TokenScriptFile f = assetDefinitions.get(chainId).get(address);
                String path = f.getAbsoluteFile().toString();
                if (path.contains(importFileName))
                {
                    cr = new ContractLocator(address, chainId);
                    break;
                }
            }
            if (cr != null) break;
        }

        return cr;
    }

    public String convertInputValue(Attribute attr, String valueFromInput)
    {
        return tokenscriptUtility.convertInputValue(attr, valueFromInput);
    }

    public String resolveReference(@NotNull Token token, TSAction action, TokenscriptElement arg, BigInteger tokenId)
    {
        TokenDefinition td = getAssetDefinition(token.tokenInfo.chainId, token.getAddress());
        return tokenscriptUtility.resolveReference(token, arg, tokenId, td, this);
    }

    public void setErrorCallback(FragmentMessenger callback)
    {
        homeMessenger = callback;
    }

    /**
     * Using a file search method rather than the pre-parsed method.
     * This lets us catch bad tokenscripts and report on errors.
     *
     * @return List of Tokenscripts with details
     */
    public Single<List<TokenLocator>> getAllTokenDefinitions(boolean refresh)
    {
        return Single.fromCallable(() -> {
            if (refresh)
            {
                loadAssetScripts();
            }
            waitForAssets();
            final File[] files = buildFileList();
            List<TokenLocator> tokenLocators = new ArrayList<>();
            Observable.fromArray(files)
                    .filter(File::isFile)
                    .filter(this::allowableExtension)
                    .filter(File::canRead)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .blockingForEach(file -> {
                        try
                        {
                            FileInputStream input = new FileInputStream(file);
                            TokenDefinition tokenDef = parseFile(input);
                            ContractInfo origins = tokenDef.contracts.get(tokenDef.holdingToken);
                            if (origins.addresses.size() > 0)
                            {
                                TokenScriptFile tsf = new TokenScriptFile(context, file.getAbsolutePath());
                                tokenLocators.add(new TokenLocator(tokenDef.getTokenName(1), origins, tsf));
                            }
                        } // TODO: Catch specific tokenscript parse errors to report tokenscript errors.
                        catch (Exception e)
                        {
                            TokenScriptFile tsf = new TokenScriptFile(context, file.getAbsolutePath());
                            ContractInfo contractInfo = new ContractInfo("Contract Type",new HashMap<>());
                            StringWriter stackTrace = new StringWriter();
                            e.printStackTrace(new PrintWriter(stackTrace));

                            tokenLocators.add(new TokenLocator(file.getName(), contractInfo, tsf, true, stackTrace.toString()));
                        }
                    });

            return tokenLocators;
        });
    }

    private List<ContractLocator> getAllOriginContracts()
    {
        List<ContractLocator> originContracts = new ArrayList<>();
        for (int i = 0; i < assetDefinitions.size(); i++)
        {
            int chainId = assetDefinitions.keyAt(i);
            for (String address : assetDefinitions.get(chainId).keySet())
            {
                if (address.equals("ethereum"))
                    continue;
                if (tokensService.getToken(chainId, address) == null)
                {
                    originContracts.add(new ContractLocator(address, chainId));
                }
            }
        }

        return originContracts;
    }

    public Single<String> checkServerForScript(int chainId, String address)
    {
        TokenScriptFile tf = getTokenScriptFile(chainId, address);
        if (tf != null && !isInSecureZone(tf)) return Single.fromCallable(() -> { return ""; }); //early return for debug script check

        //now try the server
        return fetchXMLFromServer(address.toLowerCase())
                .flatMap(this::cacheSignature)
                .map(this::handleFileLoad)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Disposable storeTokenViewHeight(int chainId, String address, int listViewHeight)
    {
        return Completable.complete()
                .subscribeWith(new DisposableCompletableObserver()
                {
                    Realm realm;
                    @Override
                    public void onStart()
                    {
                        TransactionsRealmCache.addRealm();
                        realm = realmManager.getRealmInstance(tokensService.getCurrentAddress());
                        //determine hash
                        TokenScriptFile tsf = getTokenScriptFile(chainId, address);
                        if (tsf == null || !tsf.exists()) return;
                        String hash = tsf.calcMD5();
                        String databaseKey = tokenSizeDBKey(chainId, address);

                        RealmAuxData realmToken = realm.where(RealmAuxData.class)
                                .equalTo("instanceKey", databaseKey)
                                .equalTo("chainId", chainId)
                                .findFirst();

                        realm.beginTransaction();

                        if (realmToken == null)
                        {
                            realmToken = realm.createObject(RealmAuxData.class, databaseKey);
                        }
                        realmToken.setChainId(chainId);
                        realmToken.setResult(hash);
                        realmToken.setResultTime(listViewHeight);
                    }

                    @Override
                    public void onComplete()
                    {
                        if (realm.isInTransaction()) realm.commitTransaction();
                        TransactionsRealmCache.subRealm();
                        realm.close();
                    }

                    @Override
                    public void onError(Throwable e)
                    {
                        TransactionsRealmCache.subRealm();
                        if (realm != null && !realm.isClosed())
                        {
                            realm.close();
                        }
                    }
                });
    }

    public String getTokenImageUrl(int networkId, String address)
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
            ex.printStackTrace();
        }

        return url;
    }

    public IconItem fetchIconForToken(Token token)
    {
        String correctedAddr = Keys.toChecksumAddress(token.getAddress());

        String tURL = getTokenImageUrl(token.tokenInfo.chainId, token.getAddress());
        if (TextUtils.isEmpty(tURL))
        {
            tURL = TRUST_ICON_REPO;
            String repoChain;
            switch (token.tokenInfo.chainId)
            {
                case EthereumNetworkRepository.CLASSIC_ID:
                    repoChain = "classic";
                    break;
                case EthereumNetworkRepository.XDAI_ID:
                    repoChain = "xdai";
                    break;
                case EthereumNetworkRepository.POA_ID:
                    repoChain = "poa";
                    break;
                case EthereumNetworkBase.KOVAN_ID:
                case EthereumNetworkBase.RINKEBY_ID:
                case EthereumNetworkBase.SOKOL_ID:
                case EthereumNetworkBase.ROPSTEN_ID:
                    tURL = ALPHAWALLET_ICON_REPO;
                    repoChain = "";
                    break;
                default:
                    repoChain = "ethereum";
                    break;
            }
            tURL = tURL.replace(ICON_REPO_ADDRESS_TOKEN, correctedAddr).replace(CHAIN_REPO_ADDRESS_TOKEN, repoChain);
        }

        boolean onlyTryCache = iconCheck.containsKey(correctedAddr);
        iconCheck.put(correctedAddr, true);

        return new IconItem(tURL, onlyTryCache, correctedAddr, token.tokenInfo.chainId);
    }

    public Single<Integer> fetchViewHeight(int chainId, String address)
    {
        return Single.fromCallable(() -> {
            try (Realm realm = realmManager.getRealmInstance(tokensService.getCurrentAddress()))
            {
                //determine hash
                TokenScriptFile tsf = getTokenScriptFile(chainId, address);
                if (tsf == null || !tsf.exists()) return 0;
                String hash = tsf.calcMD5();
                String databaseKey = tokenSizeDBKey(chainId, address);

                RealmAuxData realmToken = realm.where(RealmAuxData.class)
                        .equalTo("instanceKey", databaseKey)
                        .equalTo("chainId", chainId)
                        .findFirst();

                if (realmToken == null)
                {
                    return 0;
                }

                if (hash.equals(realmToken.getResult()))
                {
                    //can use this height
                    return (int)realmToken.getResultTime();
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            return 0;
        });
    }

    private String tokenSizeDBKey(int chainId, String address)
    {
        return "szkey-" + chainId + "-" + address.toLowerCase();
    }
}