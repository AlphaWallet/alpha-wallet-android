package com.alphawallet.app.service;

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
import android.util.SparseArray;
import com.alphawallet.app.C;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.tokens.ERC721Token;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenFactory;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.opensea.Asset;
import com.alphawallet.app.entity.tokenscript.TokenScriptFile;
import com.alphawallet.app.entity.tokenscript.TokenScriptFileData;
import com.alphawallet.app.entity.tokenscript.TokenscriptFunction;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.TokenLocalSource;
import com.alphawallet.app.repository.TransactionsRealmCache;
import com.alphawallet.app.repository.entity.RealmAuxData;
import com.alphawallet.app.ui.HomeActivity;
import com.alphawallet.app.viewmodel.HomeViewModel;
import com.alphawallet.token.entity.AttributeInterface;
import com.alphawallet.token.entity.AttributeType;
import com.alphawallet.token.entity.ContractAddress;
import com.alphawallet.token.entity.ContractInfo;
import com.alphawallet.token.entity.FunctionDefinition;
import com.alphawallet.token.entity.MethodArg;
import com.alphawallet.token.entity.NonFungibleToken;
import com.alphawallet.token.entity.ParseResult;
import com.alphawallet.token.entity.SigReturnType;
import com.alphawallet.token.entity.TSAction;
import com.alphawallet.token.entity.TokenScriptResult;
import com.alphawallet.token.entity.TransactionResult;
import com.alphawallet.token.entity.XMLDsigDescriptor;
import com.alphawallet.token.tools.TokenDefinition;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.crypto.WalletUtils;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.exceptions.RealmPrimaryKeyConstraintException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import static android.os.FileObserver.ALL_EVENTS;
import static com.alphawallet.token.tools.TokenDefinition.TOKENSCRIPT_CURRENT_SCHEMA;
import static com.alphawallet.token.tools.TokenDefinition.TOKENSCRIPT_REPO_SERVER;

/**
 * AssetDefinitionService is the only place where we interface with XML files.
 * This is to avoid duplicating resources
 * and also provide a consistent way to get XML values
 */

public class AssetDefinitionService implements ParseResult, AttributeInterface
{
    private final Context context;
    private final OkHttpClient okHttpClient;

    private SparseArray<Map<String, TokenScriptFile>> assetDefinitions;
    private Map<String, Long> assetChecked;                //Mapping of contract address to when they were last fetched from server
    private FileObserver fileObserver;                     //Observer which scans the override directory waiting for file change
    private Map<String, TokenScriptFileData> fileHashes;                //Mapping of files and hashes.

    private final NotificationService notificationService;
    private final RealmManager realmManager;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final TokensService tokensService;
    private final TokenLocalSource tokenLocalSource;
    private final AlphaWalletService alphaWalletService;
    private TokenDefinition cachedDefinition = null;
    private SparseArray<Map<String, SparseArray<String>>> tokenTypeName;

    private final TokenscriptFunction tokenscriptUtility;

    public AssetDefinitionService(OkHttpClient client, Context ctx, NotificationService svs,
                                  RealmManager rm, EthereumNetworkRepositoryType eth, TokensService tokensService,
                                  TokenLocalSource trs, AlphaWalletService alphaService)
    {
        context = ctx;
        okHttpClient = client;
        assetChecked = new HashMap<>();
        tokenTypeName = new SparseArray<>();
        fileHashes = new ConcurrentHashMap<>();
        notificationService = svs;
        realmManager = rm;
        ethereumNetworkRepository = eth;
        alphaWalletService = alphaService;
        this.tokensService = tokensService;
        tokenscriptUtility = new TokenscriptFunction() { }; //no overriden functions
        tokenLocalSource = trs;

        loadLocalContracts();
    }

    private void loadLocalContracts()
    {
        assetDefinitions = new SparseArray<>();

        try
        {
            loadContracts(context.getFilesDir());
            checkDownloadedFiles();
        }
        catch (IOException| SAXException e)
        {
            e.printStackTrace();
        }
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
            if (attrtype.function != null)
            {
                ContractAddress cAddr = new ContractAddress(attrtype.function, token.tokenInfo.chainId, token.tokenInfo.address);
                TransactionResult tResult = getFunctionResult(cAddr, attrtype, tokenId); //t.getTokenIdResults(BigInteger.ZERO);
                result = tokenscriptUtility.parseFunctionResult(tResult, attrtype);//  attrtype.function.parseFunctionResult(tResult, attrtype);
            }
            else
            {
                BigInteger val = tokenId.and(attrtype.bitmask).shiftRight(attrtype.bitshift);
                result = new TokenScriptResult.Attribute(attrtype.id, attrtype.name, val, attrtype.getSyntaxVal(attrtype.toString(val)));
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

    /**
     * Called at startup once we know we've got folder write permission.
     * Note - Android 6.0 and above needs user to verify folder access permission
     * TODO: if user doesn't give permission then use the app private folder and tell user they can't
     *  load contracts themselves
     */
    public void checkExternalDirectoryAndLoad()
    {
        //Check the external files directory for Android Q+
        loadExternalContracts(context.getExternalFilesDir(""));

        //Now check the legacy /AlphaWallet directory
        File directory = new File(
                Environment.getExternalStorageDirectory()
                        + File.separator + HomeViewModel.ALPHAWALLET_DIR);

        if (!directory.exists())
        {
            directory.mkdir(); //does this throw if we haven't given permission?
        }

        loadExternalContracts(directory);
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
        reloadAssets(); //if OS scaveneged the lookup map then rebuild.
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

    public Single<TokenDefinition> getAssetdefinitionASync(int chainId, final String address)
    {
        if (address == null) return Single.fromCallable(TokenDefinition::new);
        String contractName = address;
        if (contractName.equalsIgnoreCase(tokensService.getCurrentAddress())) contractName = "ethereum";

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

            if (tokenTypeName.get(chainId) == null) tokenTypeName.put(chainId, new HashMap<>());
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
            TokenScriptFileData fd = fileHashes.get(tsf.getAbsolutePath());
            if (tsf.fileUnchanged(fd))
            {
                issuer = fd.sigDescriptor.keyName;
            }
        }
        catch (Exception e)
        {
            // no action
        }

        return issuer;
    }

    private void loadScriptFromServer(String correctedAddress)
    {
        //first check the last time we tried this session
        if (assetChecked.get(correctedAddress) == null || (System.currentTimeMillis() - assetChecked.get(correctedAddress)) > 1000*60*60)
        {
            fetchXMLFromServer(correctedAddress)
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
            context.sendBroadcast(new Intent(C.ADDED_TOKEN)); //inform walletview there is a new token

            //TODO: check interface spec
        }
    }

    private Single<File> fetchXMLFromServer(String address)
    {
        return Single.fromCallable(() -> {
            File result = context.getCacheDir();
            if (address.equals("")) return result;

            //peek to see if this file exists
            File existingFile = getXMLFile(address);
            long fileTime = 0;
            if (existingFile != null && existingFile.exists())
            {
                fileTime = existingFile.lastModified();
            }

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

    private void loadExternalContracts(File directory)
    {
        try
        {
            loadContracts(directory);

            Observable.fromIterable(getCanonicalizedAssets())
                    .forEach(this::addContractAssets).isDisposed();

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q)
            {
                String externalDir = context.getExternalFilesDir("").getAbsolutePath();
                if (directory.getAbsolutePath().contains(externalDir)) startFileListener(directory);
            }
            else if (directory.getAbsolutePath().contains(HomeViewModel.ALPHAWALLET_DIR))
            {
                startFileListener(directory);
            }
        }
        catch (IOException|SAXException e)
        {
            e.printStackTrace();
        }
    }

    private void addContractsToNetwork(Integer network, Map<String, File> newTokenDescriptionAddresses)
    {
        String externalDir = context.getExternalFilesDir("").getAbsolutePath();
        if (assetDefinitions.get(network) == null) assetDefinitions.put(network, new HashMap<>());
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
        Map<String, File> addrMap = new HashMap<>();
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
                    TokenScriptFileData fd = new TokenScriptFileData();
                    fd.hash = tsf.calcMD5();
                    fd.sigDescriptor = new XMLDsigDescriptor();
                    fd.sigDescriptor.result = "pass";
                    fileHashes.put(asset, fd);
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
            TokenDefinition token = parseFile(input);
            ContractInfo holdingContracts = token.contracts.get(token.holdingToken);
            if (holdingContracts != null)
            {
                for (int network : holdingContracts.addresses.keySet())
                {
                    addContractsToNetwork(network, networkAddresses(holdingContracts.addresses.get(network), file.getAbsolutePath()));
                }
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
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

    private void loadContracts(File directory) throws IOException, SAXException
    {
        File[] files = directory.listFiles();
        if (files == null || files.length == 0) return;

        Observable.fromArray(files)
                .filter(File::isFile)
                .filter(this::allowableExtension)
                .filter(File::canRead)
                .forEach(this::addContractAddresses).isDisposed();
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
     * check the downloaded XML files for updates when wallet restarts.
     */
    private void checkDownloadedFiles()
    {
        //check all definitions in the download zone
        Disposable d = Observable.fromIterable(getScriptsInSecureZone())
                .concatMap(addr -> fetchXMLFromServer(addr).toObservable())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleFileLoad, this::onError);
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
        reloadAssets();
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
        reloadAssets();
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

    private void startFileListener(File path)
    {
        fileObserver = new FileObserver(path.getPath(), ALL_EVENTS)
        {
            @Override
            public void onEvent(int i, @Nullable String s)
            {
                //watch for new files and file change
                switch (i)
                {
                    case CREATE:
                    case MODIFY:
                        try
                        {
                            if (s.contains(".xml") || s.contains(".tsml"))
                            {
                                //form filename
                                TokenScriptFile newTSFile = new TokenScriptFile(context,
                                        Environment.getExternalStorageDirectory()
                                                + File.separator + HomeViewModel.ALPHAWALLET_DIR, s);

                                String hash = newTSFile.calcMD5();
                                TokenScriptFileData fData = fileHashes.get(newTSFile.getAbsolutePath());
                                if (fData != null)
                                {
                                    if (fData.hash.equals(hash)) break;
                                    else
                                    {
                                        fData.hash = hash;
                                        fData.sigDescriptor = null;
                                    }
                                }
                                else
                                {
                                    fData = new TokenScriptFileData();
                                    fileHashes.put(newTSFile.getAbsolutePath(), fData);
                                }
                                fData.hash = hash;
                                if (addContractAddresses(newTSFile))
                                {
                                    notificationService.DisplayNotification("Definition Updated", s, NotificationCompat.PRIORITY_MAX);
                                    cachedDefinition = null;
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

        fileObserver.startWatching();
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
                    if (tokenDef != null && tokenDef.isDebug()) token.hasDebugTokenscript = true;
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
            if (tsf.isValidTokenScript())
            {
                TokenScriptFileData fd = fileHashes.get(tsf.getAbsolutePath());
                if (tsf.fileUnchanged(fd))
                {
                    return fd.sigDescriptor;
                }
                else
                {
                    if (fd == null)
                    {
                        fd = new TokenScriptFileData();
                        fileHashes.put(tsf.getAbsolutePath(), fd);
                    }
                    fd.sigDescriptor = alphaWalletService.checkTokenScriptSignature(tsf);
                    tsf.determineSignatureType(fd);
                }
                fd.hash = tsf.calcMD5();
                sigDescriptor = fd.sigDescriptor;
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
                                        .concatMap(result -> tokenscriptUtility.fetchAttrResult(token.getWallet(), attr.id, result.tokenId, cAddr, td, this, token.lastTxTime))
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
                                tokenscriptUtility.fetchAttrResult(token.getWallet(), attr.id, tr.tokenId, cAddr, td, this, token.lastTxTime)
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

    @Override
    public TransactionResult getFunctionResult(ContractAddress contract, AttributeType attr, BigInteger tokenId)
    {
        TransactionResult tr = new TransactionResult(contract.chainId, contract.address, tokenId, attr);
        try (Realm realm = realmManager.getAuxRealmInstance(tokensService.getCurrentAddress()))
        {
            RealmAuxData realmToken = realm.where(RealmAuxData.class)
                    .equalTo("instanceKey", functionKey(contract, tokenId, attr.id))
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
                        if (tResult.result == null) tResult.result = "";
                        if (!WalletUtils.isValidAddress(tokensService.getCurrentAddress())) return;
                        realm = realmManager.getAuxRealmInstance(tokensService.getCurrentAddress());
                        ContractAddress cAddr = new ContractAddress(tResult.contractChainId, tResult.contractAddress);
                        RealmAuxData realmToken = realm.where(RealmAuxData.class)
                                .equalTo("instanceKey", functionKey(cAddr, tResult.tokenId, tResult.attrId))
                                .equalTo("chainId", tResult.contractChainId)
                                .findFirst();

                        if (realmToken == null)
                        {
                            TransactionsRealmCache.addRealm();
                            realm.beginTransaction();
                            createAuxData(realm, tResult);
                        }
                        else if (realmToken.getResultTime() != tResult.resultTime)
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

    private void createAuxData(Realm realm, TransactionResult tResult)
    {
        try
        {
            ContractAddress cAddr = new ContractAddress(tResult.contractChainId, tResult.contractAddress);
            RealmAuxData realmData = realm.createObject(RealmAuxData.class, functionKey(cAddr, tResult.tokenId, tResult.attrId));
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
        TokenScriptResult.addPair(attrs, "symbol", token.tokenInfo.symbol);
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

    public Observable<TokenScriptResult.Attribute> resolveAttrs(Token token, BigInteger tokenId)
    {
        TokenDefinition definition = getAssetDefinition(token.tokenInfo.chainId, token.tokenInfo.address);
        ContractAddress cAddr = new ContractAddress(token.tokenInfo.chainId, token.tokenInfo.address);
        //return definition.resolveAttributes(tokenId, this, cAddr);
        //resolveAttributes(BigInteger tokenId, AttributeInterface attrIf, ContractAddress cAddr, TokenDefinition td)
        return tokenscriptUtility.resolveAttributes(token.getWallet(), tokenId, this, cAddr, definition, token.lastTxTime);
    }

    public Observable<TokenScriptResult.Attribute> resolveAttrs(Token token, List<BigInteger> tokenIds)
    {
        TokenDefinition definition = getAssetDefinition(token.tokenInfo.chainId, token.tokenInfo.address);
        //pre-fill tokenIds
        for (AttributeType attrType : definition.attributeTypes.values())
        {
            resolveTokenIds(attrType, tokenIds);
        }

        //TODO: store transaction fetch time for multiple tokenIds

        return resolveAttrs(token, tokenIds.get(0));
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
        String iconifiedView = getTokenView(token.tokenInfo.chainId, token.tokenInfo.address, "view-iconified");
        return view.equals(iconifiedView);
    }

    /**
     * If the OS has scavenged the TokenScript contract lookup map then re-init
     */
    private void reloadAssets()
    {
        if (assetDefinitions == null || assetDefinitions.size() == 0)
        {
            loadLocalContracts();
        }
    }
}
