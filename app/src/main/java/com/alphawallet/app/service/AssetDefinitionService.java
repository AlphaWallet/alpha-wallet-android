package com.alphawallet.app.service;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
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

import com.alphawallet.app.C;
import com.alphawallet.app.entity.ActionEventCallback;
import com.alphawallet.app.entity.ContractResult;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.Event;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.opensea.Asset;
import com.alphawallet.app.entity.tokens.ERC721Token;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenFactory;
import com.alphawallet.app.entity.tokenscript.EventUtils;
import com.alphawallet.app.entity.tokenscript.TokenScriptFile;
import com.alphawallet.app.entity.tokenscript.TokenscriptFunction;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.TokenLocalSource;
import com.alphawallet.app.repository.TransactionsRealmCache;
import com.alphawallet.app.repository.entity.RealmAuxData;
import com.alphawallet.app.repository.entity.RealmCertificateData;
import com.alphawallet.app.ui.HomeActivity;
import com.alphawallet.app.viewmodel.HomeViewModel;
import com.alphawallet.token.entity.AttributeInterface;
import com.alphawallet.token.entity.AttributeType;
import com.alphawallet.token.entity.ContractAddress;
import com.alphawallet.token.entity.ContractInfo;
import com.alphawallet.token.entity.EventDefinition;
import com.alphawallet.token.entity.FunctionDefinition;
import com.alphawallet.token.entity.MethodArg;
import com.alphawallet.token.entity.ParseResult;
import com.alphawallet.token.entity.SigReturnType;
import com.alphawallet.token.entity.TSAction;
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
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

import static com.alphawallet.app.C.ADDED_TOKEN;
import static com.alphawallet.app.repository.TokenRepository.getWeb3jService;
import static com.alphawallet.token.tools.TokenDefinition.TOKENSCRIPT_CURRENT_SCHEMA;
import static com.alphawallet.token.tools.TokenDefinition.TOKENSCRIPT_REPO_SERVER;


/**
 * AssetDefinitionService is the only place where we interface with XML files.
 * This is to avoid duplicating resources
 * and also provide a consistent way to get XML values
 */

public class AssetDefinitionService implements ParseResult, AttributeInterface
{
    private static final String CERTIFICATE_DB = "CERTIFICATE_CACHE-db.realm";
    private static final long CHECK_TX_LOGS_INTERVAL = 15;

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
    private TokenDefinition cachedDefinition = null;
    private final SparseArray<Map<String, SparseArray<String>>> tokenTypeName;
    private final List<EventDefinition> eventList = new ArrayList<>(); //List of events built during file load
    private final Semaphore assetLoadingLock;  // used to block if someone calls getAssetDefinitionASync() while loading
    private Disposable eventListener;           // timer thread that periodically checks event logs for scripts that require events
    private ActionEventCallback eventCallback;
    private boolean requireEventSend = false;
    private final Semaphore eventConnection;

    private final TokenscriptFunction tokenscriptUtility;
    private final EventUtils eventUtils;

    /* Designed with the assmuption that only a single instance of this class at any given time
    *  ^^ The "service" part of AssetDefinitionService is the keyword here.
    *  This is shorthand in the project to indicate this is a singleton that other classes inject.
    *  This is the design pattern of the app. See class RepositoriesModule for constructors which are called at App init only */
    public AssetDefinitionService(OkHttpClient client, Context ctx, NotificationService svs,
                                  RealmManager rm, EthereumNetworkRepositoryType eth, TokensService tokensService,
                                  TokenLocalSource trs, AlphaWalletService alphaService)
    {
        context = ctx;
        okHttpClient = client;
        assetChecked = new ConcurrentHashMap<>();
        tokenTypeName = new SparseArray<>();
        notificationService = svs;
        realmManager = rm;
        ethereumNetworkRepository = eth;
        alphaWalletService = alphaService;
        this.tokensService = tokensService;
        this.eventUtils = new EventUtils() { }; //no overridden functions
        tokenscriptUtility = new TokenscriptFunction() { }; //no overridden functions
        assetDefinitions = new SparseArray<>();
        tokenLocalSource = trs;
        assetLoadingLock = new Semaphore(1);
        eventConnection = new Semaphore(1);
        loadAssetScripts();
    }

    /**
     * Load all TokenScripts
     *
     * 1. Acquire the loading lock to prevent use of scripts until fully loaded.
     * 2. Load scripts in the private data area which are only sourced from downloading from official repo
     * 3. Load scripts bundled with the App (loadInternalAssets)
     * 4. Load scripts that were installed via AlphaWallet script install intent (checkAndroidExternal)
     * 5. Load scripts manually copied to /AlphaWallet directory, for Android 9 and less (checkLegacyDirectory)
     * 6. Release semaphore
     *
     * This order has to be observed because it's an expected developer override order. If a script is placed in the /AlphaWallet directory
     * it is expected to override the one fetched from the repo server.
     * If a developer clicks on a script intent this script is expected to override the one fetched from the server.
     * TODO: This also requires a script management page where overrides can be removed.
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

        loadContracts(context.getFilesDir())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(this::addContractAddresses,
                           error -> { onError(error); loadInternalAssets(); },
                           this::loadInternalAssets).isDisposed();

        checkDownloadedFiles();
    }

    @Override
    public boolean resolveOptimisedAttr(ContractAddress contract, AttributeType attr, TransactionResult transactionResult)
    {
        boolean optimised = false;
        if (attr.function == null) return false;
        Token checkToken = tokensService.getToken(contract.chainId, contract.address);
        if (attr.function.method.equals("balanceOf") && checkToken != null)
        {
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
        }

        return optimised;
    }

    @Override
    public String getWalletAddr()
    {
        return tokensService.getCurrentAddress();
    }

    /**
     * Fetch attributes from local storage; not using contract lookup
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
            for (String key : definition.attributeTypes.keySet())
            {
                result.setAttribute(key, getTokenscriptAttr(definition, token, tokenId, key));
            }
        }

        return result;
    }

    private TokenScriptResult.Attribute getTokenscriptAttr(TokenDefinition td, Token token, BigInteger tokenId, String attribute)
    {
        TokenScriptResult.Attribute result;
        AttributeType attrtype = td.attributeTypes.get(attribute);
        try
        {
            if (attrtype.event != null)
            {
                result = new TokenScriptResult.Attribute(attrtype.id, attrtype.name, tokenId, "unsupported encoding");
            }
            else if (attrtype.function != null)
            {
                ContractAddress cAddr = new ContractAddress(attrtype.function, token.tokenInfo.chainId, token.tokenInfo.address);
                TransactionResult tResult = getFunctionResult(cAddr, attrtype, tokenId); //t.getTokenIdResults(BigInteger.ZERO);
                result = tokenscriptUtility.parseFunctionResult(tResult, attrtype);//  attrtype.function.parseFunctionResult(tResult, attrtype);
            }
            else
            {
                BigInteger val = tokenId.and(attrtype.bitmask).shiftRight(attrtype.bitshift);
                result = new TokenScriptResult.Attribute(attrtype.id, attrtype.name, attrtype.processValue(val), attrtype.getSyntaxVal(attrtype.toString(val)));
            }
        }
        catch (Exception e)
        {
            result = new TokenScriptResult.Attribute(attrtype.id, attrtype.name, tokenId, "unsupported encoding");
        }

        return result;
    }

    public TokenScriptResult.Attribute getAttribute(Token token, BigInteger tokenId, String attribute)
    {
        TokenDefinition definition = getAssetDefinition(token.tokenInfo.chainId, token.tokenInfo.address);
        if (definition != null && definition.attributeTypes.containsKey(attribute))
        {
            return getTokenscriptAttr(definition, token, tokenId, attribute);
        }
        else
        {
            return null;
        }
    }

    private boolean checkWritePermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
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

    private TokenScriptFile getTokenScriptFile(int chainId, String address)
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

        final TokenDefinition assetDef = getDefinition(chainId, contractName.toLowerCase());
        if (assetDef != null) return Single.fromCallable(() -> assetDef);
        else if (!contractName.equals("ethereum"))
        {
            return fetchXMLFromServer(contractName.toLowerCase())
                    .map(this::handleDefinitionFile);
        }
        else return Single.fromCallable(TokenDefinition::new);
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
                        addContractsToNetwork(network, networkAddresses(holdingContracts.addresses.get(network), tokenScriptFile.getAbsolutePath()));
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
     * Get the issuer name given the contract address
     * Note: this is optimised so as we don't need to keep loading in definitions as the user scrolls
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
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleFileLoad, this::onError).isDisposed();
        }
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

    private void handleFileLoad(File newFile)
    {
        if (newFile != null && !newFile.getName().equals("cache") && newFile.canRead())
        {
            addContractAddresses(newFile);
            TokenDefinition td = getTokenDefinition(newFile);
            List<ContractResult> tokenContracts = getHoldingContracts(td);
            if (tokenContracts != null && tokenContracts.size() > 0)
            {
                String[] addrs = new String[tokenContracts.size()];
                int[] chainIds = new int[tokenContracts.size()];
                int index = 0;
                for (ContractResult cr : tokenContracts)
                {
                    addrs[index] = cr.name;
                    chainIds[index] = cr.chainId;
                }

                Intent intent = new Intent(ADDED_TOKEN);
                intent.putExtra(C.EXTRA_TOKENID_LIST, addrs);
                intent.putExtra(C.EXTRA_CHAIN_ID, chainIds);
                context.sendBroadcast(intent);
            }
        }
    }

    private Single<File> fetchXMLFromServer(String address)
    {
        return Single.fromCallable(() -> {
            final File defaultReturn = new File("");
            if (address.equals("")) return defaultReturn;

            //peek to see if this file exists
            File result = getXMLFile(address);
            long fileTime = 0;
            if (result != null && result.exists())
            {
                fileTime = result.lastModified();
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
                        break;
                    case HttpURLConnection.HTTP_OK:
                        String xmlBody = response.body().string();
                        result = storeFile(address, xmlBody);
                        break;
                    default:
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

    //This loads bundled Tokenscripts in the /assets directory eg xdaicanonicalized
    private void loadInternalAssets()
    {
        Observable.fromIterable(getCanonicalizedAssets())
                .subscribeOn(Schedulers.io())
                .subscribe(this::addContractAssets, error -> { onError(error); checkAndroidExternal(); },
                           this::checkAndroidExternal).isDisposed();
    }

    //Check the external directory - Android/data/(applicationId)
    private void checkAndroidExternal()
    {
        loadContracts(context.getExternalFilesDir(""))
                .subscribeOn(Schedulers.io())
                .subscribe(this::addContractAddresses, error -> { onError(error); checkLegacyDirectory(); },
                           this::checkLegacyDirectory).isDisposed();
    }

    private void checkLegacyDirectory()
    {
        startFileListener(context.getExternalFilesDir("").getAbsolutePath(), false);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && checkWritePermission())
        {
            //Now check the legacy AlphaWallet directory
            File directory = new File(
                    Environment.getExternalStorageDirectory()
                            + File.separator + HomeViewModel.ALPHAWALLET_DIR);

            if (!directory.exists())
            {
                directory.mkdir();
            }

            loadContracts(directory)
                    .subscribeOn(Schedulers.io())
                    .subscribe(this::addContractAddresses, error -> { onError(error); finishLoading(); },
                               this::finishLoading).isDisposed();

            startFileListener(directory.getAbsolutePath(), true);
        }
        else
        {
            finishLoading();
        }
    }

    private void finishLoading()
    {
        assetLoadingLock.release();
        if (eventCallback != null)
        {
            generateAndSendEvents();
        }
        else
        {
            requireEventSend = true;
        }
        startEventListeners();
    }

    private void addContractsToNetwork(Integer network, Map<String, File> newTokenDescriptionAddresses)
    {
        String externalDir = context.getExternalFilesDir("").getAbsolutePath();
        if (assetDefinitions.get(network) == null) assetDefinitions.put(network, new ConcurrentHashMap<>());
        for (String address : newTokenDescriptionAddresses.keySet())
        {
            if (assetDefinitions.get(network).containsKey(address))
            {
                String filename = assetDefinitions.get(network).get(address).getAbsolutePath();
                if (filename.contains(HomeViewModel.ALPHAWALLET_DIR)
                    || filename.contains(externalDir)) continue; //don't override if this is a developer added script
            }
            assetDefinitions.get(network).put(address, new TokenScriptFile(context, newTokenDescriptionAddresses.get(address).getAbsolutePath()));
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

    private boolean addContractAddresses(File file)
    {
        try (FileInputStream input = new FileInputStream(file)) {
            if (file.getAbsolutePath().contains("ENS"))
            {
                System.out.println("YOLESS");
            }
            TokenDefinition tokenDef = parseFile(input);
            ContractInfo holdingContracts = tokenDef.contracts.get(tokenDef.holdingToken);
            if (holdingContracts != null)
            {
                addToEventList(tokenDef, holdingContracts);
                for (int network : holdingContracts.addresses.keySet())
                {
                    addContractsToNetwork(network, networkAddresses(holdingContracts.addresses.get(network), file.getAbsolutePath()));
                }
                return true;
            }
        } catch (Exception e) {
            System.out.println("ERROR While parsing: " + file.getAbsolutePath());
            e.printStackTrace();
        }
        return false;
    }

    private void addToEventList(TokenDefinition tokenDef, ContractInfo cInfo)
    {
        for (String attrName : tokenDef.attributeTypes.keySet())
        {
            AttributeType attr = tokenDef.attributeTypes.get(attrName);
            if (attr.event != null)
            {
                attr.event.originContract = cInfo;
                eventList.add(attr.event); //note: event definition contains link back to the contract it refers to
            }
        }
    }

    private void stopEventListener()
    {
        if (eventListener != null) eventListener.dispose();
        //blank all events
        for (EventDefinition ev : eventList)
        {
            ev.readBlock = BigInteger.ZERO;
        }
    }

    public void startEventListeners()
    {
        stopEventListener();
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
                ContractInfo originContracts = ev.originContract;
                for (int chainId : originContracts.addresses.keySet())
                {
                    for (String addr : originContracts.addresses.get(chainId))
                    {
                        //have corresponding token?
                        Token originToken = tokensService.getToken(chainId, addr);
                        if (originToken != null && originToken.hasPositiveBalance())
                        {
                            //initiate listener
                            getEvents(ev, originToken);
                        }
                    }
                }
            }
        });
    }

    private void getEvents(EventDefinition ev, Token originToken)
    {
        Web3j web3j = getWeb3jService(originToken.tokenInfo.chainId);
        final EthFilter filter = eventUtils.generateLogFilter(ev, originToken);

        try
        {
            eventConnection.acquire(); //prevent overlapping event calls
            EthLog ethLogs = web3j.ethGetLogs(filter).send();
            processLogs(ev, ethLogs.getLogs(), originToken);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            eventConnection.release();
        }

        //More elegant, but requires a private node
//        return web3j.ethLogFlowable(filter)
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(log -> {
//            System.out.println("log.toString(): " +  log.toString());
//            //TODO here: callback to event service listener
//        }, this::onLogError);
    }

    private void processLogs(EventDefinition ev, List<EthLog.LogResult> logs, Token originToken)
    {
        if (logs.size() == 0) return; //early return

        Web3j web3j = getWeb3jService(originToken.tokenInfo.chainId);
        TokenDefinition td = getAssetDefinition(originToken.tokenInfo.chainId, originToken.getAddress());
        AttributeType attrType = td.attributeTypes.get(ev.attributeId);

        for (EthLog.LogResult ethLog : logs)
        {
            String selectVal = eventUtils.getSelectVal(ev, ethLog);
            EthBlock txBlock = eventUtils.getTransactionDetails(((Log)ethLog.get()).getBlockHash(), web3j).blockingGet();

            long blockTime = txBlock.getBlock().getTimestamp().longValue();
            eventCallback.receivedEvent(ev.attributeId, attrType.getSyntaxVal(selectVal), blockTime, originToken.tokenInfo.chainId);
            storeEventValue(ev, ethLog, attrType, originToken, blockTime, selectVal);
        }
    }

    private void storeEventValue(EventDefinition ev, EthLog.LogResult log, AttributeType attr,
                                 Token originToken, long blockTime, String selectVal)
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
            tokenId = originToken.getArrayBalance().get(0);
        }

        ContractAddress eventContractAddress = new ContractAddress(attr.event.eventModule.contractInfo.addresses.keySet().iterator().next(),
                                                                   attr.event.eventModule.contractInfo.addresses.values().iterator().next().get(0));
        txResult = getFunctionResult(eventContractAddress, attr, tokenId);

        txResult.result = selectVal;

        if (txResult.resultTime == 0 || blockTime > txResult.resultTime)
        {
            //store
            txResult.resultTime = blockTime;
            storeAuxData(txResult); // updates the entry for the attribute
            ev.hasNewEvent = true;
        }

        txResult.resultTime = blockTime;
        txResult.result = attr.getSyntaxVal(selectVal) + "," + ev.readBlock.toString(16); //store block time as well as block number
        storeAuxData(txResult); //store the event itself
    }

    public boolean checkTokenForNewEvent(Token token)
    {
        boolean hasEvent = false;
        for (EventDefinition ev : eventList)
        {
            ContractInfo originContracts = ev.eventModule.contractInfo;
            if (originContracts.addresses.containsKey(token.tokenInfo.chainId))
            {
                if (originContracts.addresses.get(token.tokenInfo.chainId).contains(token.getAddress().toLowerCase()))
                {
                    hasEvent = ev.hasNewEvent;
                    ev.hasNewEvent = false;
                    break;
                }
            }
        }

        return hasEvent;
    }

    private void onLogError(Throwable throwable)
    {
        System.out.println("Log error: " + throwable.getMessage());
        throwable.printStackTrace();
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

    private Observable<File> loadContracts(File directory)
    {
        File[] files = directory.listFiles();
        if (files == null || files.length == 0) return Observable.fromCallable(() -> new File(""));

        return Observable.fromArray(files)
                .filter(File::isFile)
                .filter(this::allowableExtension)
                .filter(File::canRead)
                .flatMap(file -> cacheSignature(file).toObservable()) //check we have signature when initialising
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io());
    }

    /**
     * Given contract address, find the corresponding File.
     * We have to search in the internal area and the external storage area
     * The reason we need two areas is prevent the need for normal users to have to give
     * permission to access external storage.
     * @param contractAddress
     * @return
     */
    private File getXMLFile(String contractAddress)
    {
        for (int i = 0; i < assetDefinitions.size(); i++)
        {
            if (assetDefinitions.valueAt(i).containsKey(contractAddress.toLowerCase()))
            {
                return assetDefinitions.valueAt(i).get(contractAddress.toLowerCase());
            }
        }

        return null;
    }

    private List<String> getScriptsInSecureZone()
    {
        List<String> checkScripts = new ArrayList<>();
        for (int i = 0; i < assetDefinitions.size(); i++)
        {
            for (String address : assetDefinitions.valueAt(i).keySet())
            {
                if (isInSecureZone(assetDefinitions.valueAt(i).get(address)))
                {
                    if (!checkScripts.contains(address)) checkScripts.add(address);
                }
            }
        }

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
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleFileLoad, this::onError).isDisposed();
    }

    /* Add cached signature if uncached files found. */
    private Single<File> cacheSignature(File file)
    {
        // note that outdated cache is never deleted - we don't have that level of finesse
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
        if (td != null && td.attributeSets.containsKey("cards"))
        {
            String view = td.attributeSets.get("cards").get(type);
            // 8 characters is about minimum for a view
            return view != null && view.length() > 8;
        }
        return false;
    }

    public String getTokenView(int chainId, String contractAddr, String type)
    {
        String viewHTML = "";
        TokenDefinition td = getAssetDefinition(chainId, contractAddr);
        if (td != null && td.attributeSets.containsKey("cards"))
        {
            viewHTML = td.getCardData(type);
        }

        return viewHTML;
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

    public boolean hasAction(int chainId, String contractAddr)
    {
        TokenDefinition td = getAssetDefinition(chainId, contractAddr);
        return td != null && td.actions != null && td.actions.size() > 0;
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

    private void startFileListener(String path, boolean isLegacyDir)
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
                    case MODIFY:
                        try
                        {
                            if (file.contains(".xml") || file.contains(".tsml"))
                            {
                                System.out.println("FILE: " + file);
                                //form filename
                                TokenScriptFile newTSFile = new TokenScriptFile(context, listenerPath, file);

                                if (addContractAddresses(newTSFile))
                                {
                                    notificationService.DisplayNotification("Definition Updated", file, NotificationCompat.PRIORITY_MAX);
                                    cachedDefinition = null;
                                    cacheSignature(newTSFile) //update signature data if necessary
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe().isDisposed();
                                }
                            }
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                        break;
                    default:
                        break;
                }
            }
        };

        observer.startWatching();

        if (isLegacyDir)
        {
            fileObserver = observer;
        }
        else
        {
            fileObserverQ = observer;
        }
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

    /**
     * When a Non Fungible Token contract which has a Tokenscript definition has new transactions
     * We need to update the cached values as they could have changed
     * TODO: Once we support event listening this is triggered from specific events
     * @param token
     * @return
     */
    public Single<Token> updateEthereumResults(Token token)
    {
        return Single.fromCallable(() -> {
            TokenDefinition td = getDefinition(token.tokenInfo.chainId, token.tokenInfo.address);
            if (td == null) return token;

            List<AttributeType> attrs = new ArrayList<>(td.attributeTypes.values());
            for (AttributeType attr : attrs)
            {
                if (attr.function == null) continue;
                FunctionDefinition fd = attr.function;
                ContractInfo cInfo = fd.contract;
                if (cInfo == null || cInfo.addresses == null) continue;
                List<String> addresses = cInfo.addresses.get(token.tokenInfo.chainId);

                if (addresses != null)
                {
                    for (String address : addresses)
                    {
                        ContractAddress cAddr = new ContractAddress(token.tokenInfo.chainId, address);
                        if (cInfo.contractInterface != null)
                        {
                            checkCorrectInterface(token, cInfo.contractInterface);
                                Observable.fromIterable(token.getNonZeroArrayBalance())
                                        .map(tokenId -> getFunctionResult(cAddr, attr, tokenId))
                                        .filter(txResult -> txResult.needsUpdating(token.lastTxTime))
                                        .concatMap(result -> tokenscriptUtility.fetchAttrResult(token.getWallet(), td.attributeTypes.get(attr.id), result.tokenId, cAddr, td, this, token.lastTxTime))
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe();
                        }
                        else
                        {
                            //doesn't have a contract interface, so just fetch the function
                            TransactionResult tr = getFunctionResult(cAddr, attr, BigInteger.ZERO);
                            if (tr.needsUpdating(token.lastTxTime))
                            {
                                tokenscriptUtility.fetchAttrResult(token.getWallet(), td.attributeTypes.get(attr.id), tr.tokenId, cAddr, td, this, token.lastTxTime)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe();
                            }
                        }
                    }
                }
            }

            return token;
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
        newToken.transferPreviousData(token);

        tokenLocalSource.saveToken(new Wallet(token.getWallet()), newToken)
                .subscribeOn(Schedulers.io())
                .subscribe(tokensService::addToken).isDisposed();
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
    public TransactionResult getFunctionResult(ContractAddress contract, AttributeType attr, BigInteger tokenId)
    {
        TransactionResult tr = new TransactionResult(contract.chainId, contract.address, tokenId, attr);
        String dataBaseKey = functionKey(contract, tokenId, attr.id);
        try (Realm realm = realmManager.getAuxRealmInstance(tokensService.getCurrentAddress()))
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
    public TransactionResult storeAuxData(TransactionResult tResult)
    {
        Completable.complete()
                .subscribeWith(new DisposableCompletableObserver()
                {
                    Realm realm = null;

                    @Override
                    public void onStart()
                    {
                        if (tokensService.getCurrentAddress() == null || !WalletUtils.isValidAddress(tokensService.getCurrentAddress())) return;
                        if (tResult.result == null) return;
                        realm = realmManager.getAuxRealmInstance(tokensService.getCurrentAddress());
                        ContractAddress cAddr = new ContractAddress(tResult.contractChainId, tResult.contractAddress);
                        String databaseKey = functionKey(cAddr, tResult.tokenId, tResult.attrId);
                        if (tResult.result.contains(","))
                        {
                            databaseKey = eventKey(tResult);
                        }
                        RealmAuxData realmToken = realm.where(RealmAuxData.class)
                                .equalTo("instanceKey", databaseKey)
                                .equalTo("chainId", tResult.contractChainId)
                                .findFirst();

                        if (realmToken == null)
                        {
                            TransactionsRealmCache.addRealm();
                            realm.beginTransaction();
                            createAuxData(realm, tResult, databaseKey);
                        }
                        else if (tResult.result != null)
                        {
                            TransactionsRealmCache.addRealm();
                            realm.beginTransaction();
                            realmToken.setResult(tResult.result);
                            realmToken.setResultTime(tResult.resultTime);
                        }
                    }

                    @Override
                    public void onComplete()
                    {
                        if (realm != null)
                        {
                            if (realm.isInTransaction())
                            {
                                realm.commitTransaction();
                                TransactionsRealmCache.subRealm();
                            }
                            if (!realm.isClosed()) realm.close();
                        }
                    }

                    @Override
                    public void onError(Throwable e)
                    {
                        if (realm != null && !realm.isClosed())
                        {
                            if (realm.isInTransaction()) TransactionsRealmCache.subRealm();
                            realm.close();
                        }
                    }
                }).isDisposed();

        return tResult;
    }

    private void generateAndSendEvents()
    {
        List<Event> storedEvents = new ArrayList<>();
        try (Realm realm = realmManager.getAuxRealmInstance(tokensService.getCurrentAddress()))
        {
            RealmResults<RealmAuxData> realmEvents = realm.where(RealmAuxData.class)
                    .endsWith("instanceKey", "-log")
                    .sort("resultTime", Sort.ASCENDING)
                    .findAll();

            for (RealmAuxData eventData : realmEvents)
            {
                String[] results = eventData.getResult().split(",");
                if (results.length != 2) continue;
                String result = results[0];
                BigInteger blockNumber = new BigInteger(results[1], 16);
                String eventText = "Event: " + eventData.getFunctionId() + " becomes " + result;
                Event ev = new Event(eventText, eventData.getResultTime(), eventData.getChainId());
                storedEvents.add(ev);

                //load event with top block value
                updateEventList(eventData, blockNumber);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        if (eventCallback != null) eventCallback.eventsLoaded(storedEvents.toArray(new Event[0]));
    }

    private void updateEventList(RealmAuxData eventData, BigInteger blockNumber)
    {
        for (EventDefinition ev : eventList)
        {
            if (ev.attributeId.equals(eventData.getFunctionId()))
            {
                //does the event module correspond to this contract?
                String[] contractDetails = eventData.getInstanceKey().split("-");
                //return tResult.contractAddress + "-" + tResult.tokenId.toString(Character.MAX_RADIX) + "-" + tResult.contractChainId + "-" + tResult.attrId + tResult.resultTime + "-log";
                String contractAddr = contractDetails[0].toLowerCase();
                if (ev.eventModule.contractInfo.hasContractTokenScript(eventData.getChainId(), contractAddr))
                {
                    ev.readBlock = blockNumber;
                }
            }
        }
    }

    private void createAuxData(Realm realm, TransactionResult tResult, String dataBaseKey)
    {
        try
        {
            ContractAddress cAddr = new ContractAddress(tResult.contractChainId, tResult.contractAddress);
            RealmAuxData realmData = realm.createObject(RealmAuxData.class, dataBaseKey);
            realmData.setResultTime(tResult.resultTime);
            realmData.setResult(tResult.result);
            realmData.setChainId(tResult.contractChainId);
            realmData.setFunctionId(tResult.method);
            realmData.setTokenId(tResult.tokenId.toString(Character.MAX_RADIX));
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
        TokenScriptResult.addPair(attrs, "background_colour", tokenAsset.getBackgroundColor());
        TokenScriptResult.addPair(attrs, "image_preview_url", tokenAsset.getImagePreviewUrl());
        TokenScriptResult.addPair(attrs, "description", tokenAsset.getDescription());
        TokenScriptResult.addPair(attrs, "external_link", tokenAsset.getExternalLink());
        TokenScriptResult.addPair(attrs, "background_colour", tokenAsset.getBackgroundColor());
        TokenScriptResult.addPair(attrs, "traits", tokenAsset.getTraits());
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

    public Observable<TokenScriptResult.Attribute> resolveAttrs(Token token, BigInteger tokenId, List<AttributeType> extraAttrs)
    {
        TokenDefinition definition = getAssetDefinition(token.tokenInfo.chainId, token.tokenInfo.address);
        ContractAddress cAddr = new ContractAddress(token.tokenInfo.chainId, token.tokenInfo.address);

        definition.context = new TokenscriptContext();
        definition.context.cAddr = cAddr;
        definition.context.attrInterface = this;

        List<AttributeType> attrList = new ArrayList<>(definition.attributeTypes.values());
        if (extraAttrs != null) attrList.addAll(extraAttrs);

        return Observable.fromIterable(attrList)
                .flatMap(attr -> tokenscriptUtility.fetchAttrResult(token.getWallet(), attr, tokenId,
                                                                    cAddr, definition, this, token.lastTxTime));
    }

    public Observable<TokenScriptResult.Attribute> resolveAttrs(Token token, List<BigInteger> tokenIds, List<AttributeType> extraAttrs)
    {
        TokenDefinition definition = getAssetDefinition(token.tokenInfo.chainId, token.tokenInfo.address);
        //pre-fill tokenIds
        for (AttributeType attrType : definition.attributeTypes.values())
        {
            resolveTokenIds(attrType, tokenIds);
        }

        //TODO: store transaction fetch time for multiple tokenIds

        return resolveAttrs(token, tokenIds.get(0), extraAttrs);
    }

    private void resolveTokenIds(AttributeType attrType, List<BigInteger> tokenIds)
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

    private List<String> getCanonicalizedAssets()
    {
        List<String> canonicalizedFilesStr = new ArrayList<>();
        AssetManager mgr = context.getResources().getAssets();
        try
        {
            String[] filelist = mgr.list("");
            for (String file : filelist)
            {
                if (file.contains("canonicalized"))
                {
                    canonicalizedFilesStr.add(file);
                }
            }

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return canonicalizedFilesStr;
    }

    public String generateTransactionPayload(Token token, BigInteger tokenId, FunctionDefinition def)
    {
        TokenDefinition td = getAssetDefinition(token.tokenInfo.chainId, token.tokenInfo.address);
        if (td == null) return "";
        Function function = tokenscriptUtility.generateTransactionFunction(token.getWallet(), tokenId, td, def, this);
        return FunctionEncoder.encode(function);
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
        String view = getTokenView(token.tokenInfo.chainId, token.tokenInfo.address, "view");
        String iconifiedView = getTokenView(token.tokenInfo.chainId, token.tokenInfo.address, "item-view");
        return view.equals(iconifiedView);
    }

    public List<ContractResult> getHoldingContracts(TokenDefinition td)
    {
        List<ContractResult> holdingContracts = new ArrayList<>();
        ContractInfo holdingContractInfo = td.contracts.get(td.holdingToken);
        if (holdingContractInfo == null || holdingContractInfo.addresses.size() == 0) return null;
        for (int chainId : holdingContractInfo.addresses.keySet())
        {
            for (String address : holdingContractInfo.addresses.get(chainId))
            {
                holdingContracts.add(new ContractResult(address, chainId));
            }
        }

        return holdingContracts;
    }

    public ContractResult getHoldingContract(String importFileName)
    {
        ContractResult cr = null;
        for (int i = 0; i < assetDefinitions.size(); i++)
        {
            int chainId = assetDefinitions.keyAt(i);
            for (String address : assetDefinitions.get(chainId).keySet())
            {
                TokenScriptFile f = assetDefinitions.get(chainId).get(address);
                String path = f.getAbsoluteFile().toString();
                if (path.contains(importFileName))
                {
                    cr = new ContractResult(address, chainId);
                    break;
                }
            }
            if (cr != null) break;
        }

        return cr;
    }

    public String convertInputValue(AttributeType attr, TokenscriptElement e, String valueFromInput)
    {
        return tokenscriptUtility.convertInputValue(attr, e, valueFromInput);
    }

    public void setEventCallback(ActionEventCallback callback)
    {
        eventCallback = callback;
        if (requireEventSend)
        {
            requireEventSend = false;
            generateAndSendEvents();
        }
    }

    public void resolveReference(@NotNull Token token, TSAction action, TokenscriptElement arg, BigInteger tokenId)
    {
        TokenDefinition td = getAssetDefinition(token.tokenInfo.chainId, token.getAddress());
        if (td == null || TextUtils.isEmpty(arg.ref)) return;
        AttributeType attributeType = (action != null && action.attributeTypes != null) ? action.attributeTypes.get(arg.ref) : null; // try locals first
        if (attributeType == null) attributeType = td.attributeTypes.get(arg.ref); // now try globals
        if (attributeType != null)
        {
            arg.value = tokenscriptUtility.fetchAttrResult(tokensService.getCurrentAddress(), attributeType, tokenId, null, td, this, 0).blockingSingle().text;
        }
    }
}
