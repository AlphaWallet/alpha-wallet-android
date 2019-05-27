package io.stormbird.wallet.service;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.FileObserver;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.stormbird.token.entity.*;
import io.stormbird.token.tools.TokenDefinition;
import io.stormbird.token.util.DateTime;
import io.stormbird.token.util.DateTimeFactory;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.repository.TransactionsRealmCache;
import io.stormbird.wallet.repository.entity.RealmAuxData;
import io.stormbird.wallet.ui.HomeActivity;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.http.HttpService;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static android.os.FileObserver.ALL_EVENTS;
import static io.stormbird.wallet.C.ADDED_TOKEN;
import static io.stormbird.wallet.viewmodel.HomeViewModel.ALPHAWALLET_DIR;
import static org.web3j.crypto.WalletUtils.isValidAddress;
import static org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction;

/**
 * AssetDefinitionService is the only place where we interface with XML files.
 * This is to avoid duplicating resources
 * and also provide a consistent way to get XML values
 */

public class AssetDefinitionService implements ParseResult, AttributeInterface
{
    private static final String XML_EXT = "xml";
    private final Context context;
    private final OkHttpClient okHttpClient;

    private Map<Integer, Map<String, TokenDefinition>> assetDefinitions; //Mapping of contract address to token definitions
    private Map<String, Long> assetChecked;                //Mapping of contract address to when they were last fetched from server
    private Map<Integer, List<String>> devOverrideContracts;             //List of contract addresses which have been overridden by definition in developer folder
    private FileObserver fileObserver;                     //Observer which scans the override directory waiting for file change
    private Map<String, String> fileHashes;                //Mapping of files and hashes.

    private final NotificationService notificationService;
    private final RealmManager realmManager;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final TokensService tokensService;
    private Web3j web3j;
    private int web3ChainId;

    public AssetDefinitionService(OkHttpClient client, Context ctx, NotificationService svs, RealmManager rm, EthereumNetworkRepositoryType eth, TokensService tokensService)
    {
        context = ctx;
        okHttpClient = client;
        assetChecked = new HashMap<>();
        devOverrideContracts = new ConcurrentHashMap<>();
        fileHashes = new ConcurrentHashMap<>();
        notificationService = svs;
        realmManager = rm;
        ethereumNetworkRepository = eth;
        web3ChainId = 0;
        this.tokensService = tokensService;

        loadLocalContracts();
    }

    private void loadLocalContracts()
    {
        assetDefinitions = new HashMap<>();

        try
        {
            assetDefinitions.clear();
            loadContracts(context.getFilesDir(), false);
            checkDownloadedFiles();
        }
        catch (IOException| SAXException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Fetches non fungible token definition given contract address and token ID
     * @param token
     * @param contractAddress
     * @param tokenId
     * @return
     */
    public NonFungibleToken getNonFungibleToken(Token token, String contractAddress, BigInteger tokenId)
    {
        TokenDefinition definition = getAssetDefinition(token.tokenInfo.chainId, contractAddress);
        if (definition != null)
        {
            TokenScriptResult tsr = getTokenScriptResult(token, tokenId);
            return new NonFungibleToken(tokenId, tsr);
        }
        else
        {
            return null;
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
                if (arg.parameterType.equals("address") && arg.ref.equals("ownerAddress"))
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
            for (String key : definition.attributeTypes.keySet()) {
                AttributeType attrtype = definition.attributeTypes.get(key);
                try
                {
                    if (attrtype.function != null)
                    {
                        ContractAddress contract = new ContractAddress(attrtype.function, token.tokenInfo.chainId, token.tokenInfo.address);// getContractAddress(attrtype.function, token);
                        if (contract == null)
                            continue;
                        TransactionResult tResult = getFunctionResult(contract, attrtype, tokenId); //t.getTokenIdResults(BigInteger.ZERO);
                        result.setAttribute(attrtype.id, attrtype.function.parseFunctionResult(tResult, attrtype));
                    }
                    else
                    {
                        BigInteger val = tokenId.and(attrtype.bitmask).shiftRight(attrtype.bitshift);
                        result.setAttribute(attrtype.id,
                                           new TokenScriptResult.Attribute(attrtype.id, attrtype.name, val, attrtype.toString(val)));
                    }
                }
                catch (Exception e)
                {
                    result.setAttribute(attrtype.id,
                                       new TokenScriptResult.Attribute(attrtype.id, attrtype.name, tokenId, "unsupported encoding"));
                }
            }
        }

        return result;
    }

    private ContractAddress getFromContracts(Map<Integer, List<String>> addresses)
    {
        for (int chainId : addresses.keySet())
        {
            for (String addr : addresses.get(chainId))
            {
                return new ContractAddress(chainId, addr);
            }
        }

        return null;
    }

    /**
     * Called at startup once we know we've got folder write permission.
     * Note - Android 6.0 and above needs user to verify folder access permission
     * TODO: if user doesn't give permission then use the app private folder and tell user they can't
     *  load contracts themselves
     */
    public void checkExternalDirectoryAndLoad()
    {
        //create XML repository directory
        File directory = new File(
                Environment.getExternalStorageDirectory()
                        + File.separator + ALPHAWALLET_DIR);

        if (!directory.exists())
        {
            directory.mkdir(); //does this throw if we haven't given permission?
        }

        loadExternalContracts(directory);
    }

    private TokenDefinition getDefinitionMapping(int chainId, String address)
    {
        Map<String, TokenDefinition> networkMap = assetDefinitions.get(chainId);
        if (networkMap != null)
        {
            return networkMap.get(address);
        }
        else
        {
            return null;
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
        String correctedAddress = address.toLowerCase(); //ensure address is in the format we want
        //is asset definition currently read?
        assetDef = getDefinitionMapping(chainId, correctedAddress);
        if (assetDef == null)
        {
            //try to load from the cache directory
            File xmlFile = getXMLFile(correctedAddress);

            //try web
            if (xmlFile == null)
            {
                loadScriptFromServer(correctedAddress); //this will complete asynchronously, and display will be updated
            }
            else
            {
                assetDef = loadTokenDefinition(correctedAddress);
                if (assetDef != null)
                {
                    assetDef.populateNetworks(assetDefinitions, devOverrideContracts);
                }
                else
                {
                    assetDef = null;
                }
            }
        }

        return assetDef; // if nothing found use default
    }

    /**
     * Function returns all contracts on this network ID
     *
     * @param networkId
     * @return
     */
    public List<String> getAllContracts(int networkId)
    {
        List<String> contractList = new ArrayList<>();
        Map<String, TokenDefinition> networkList = assetDefinitions.get(networkId);
        if (networkList != null)
        {
            for (String address : networkList.keySet())
            {
                if (!contractList.contains(address)) contractList.add(address);
            }
        }
        return contractList;
    }

    /**
     * Get the issuer name given the contract address
     * Note: this
     * @param token
     * @return
     */
    public String getIssuerName(Token token)
    {
        TokenDefinition definition = getDefinitionMapping(token.tokenInfo.chainId, token.getAddress());

        if (definition != null)
        {
            String issuer = definition.getKeyName();
            return (issuer == null || issuer.length() == 0) ? context.getString(R.string.stormbird) : issuer;
        }
        else
        {
            return token.getNetworkName();
        }
    }

    private void loadScriptFromServer(String correctedAddress)
    {
        //first check the last time we tried this session
        if (assetChecked.get(correctedAddress) == null || (System.currentTimeMillis() - assetChecked.get(correctedAddress)) > 1000*60*60)
        {
            Disposable d = fetchXMLFromServer(correctedAddress)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleFileLoad, this::onError);
        }
    }

    private void onError(Throwable throwable)
    {
        throwable.printStackTrace();
    }

    private TokenDefinition loadTokenDefinition(String address)
    {
        TokenDefinition definition = null;
        File xmlFile = getXMLFile(address.toLowerCase());
        try
        {
            if (xmlFile != null && xmlFile.exists())
            {
                FileInputStream is = new FileInputStream(xmlFile);
                definition = parseFile(is);
            }
        }
        catch (IOException|SAXException e)
        {
            e.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return definition;
    }

    @SuppressWarnings("deprecation")
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

        TokenDefinition definition = new TokenDefinition(
                xmlInputStream, locale, this);

        //now assign the networks
        if (definition.hasContracts())
        {
            assignNetworks(definition);
        }

        return definition;
    }

    private void assignNetworks(TokenDefinition definition)
    {
        if (definition != null)
        {
            //now map all contained addresses
            definition.populateNetworks(assetDefinitions, devOverrideContracts);
        }
    }

    /**
     * Add the contract addresses defined in the developer XML file to the override list.
     * Subsequent refresh of XML from server will not override these definitions.
     * @param definition interpreted definition
     */
    private void addOverrideFile(TokenDefinition definition)
    {
        if (definition != null)
        {
            definition.addToOverrides(devOverrideContracts);
        }
    }

    private void handleFileLoad(String address)
    {
        if (isValidAddress(address))
        {
            handleFile(address);
            context.sendBroadcast(new Intent(ADDED_TOKEN)); //inform walletview there is a new token
        }
    }

    /**
     * Add all contracts from this file into the assetDefinitions. Always override
     * @param address
     */
    private void handleFile(String address)
    {
        //this is file stored on the phone, notify to the main app to reload (use receiver)
        TokenDefinition tokenDefinition = loadTokenDefinition(address);
        if (tokenDefinition != null)
        {
            tokenDefinition.populateNetworks(assetDefinitions, null);
            tokenDefinition.addToOverrides(devOverrideContracts);
        }
    }

    private Observable<String> fetchXMLFromServer(String address)
    {
        return Observable.fromCallable(() -> {
            if (address.equals("")) return "0x";

            //peek to see if this file exists
            File existingFile = getXMLFile(address);
            long fileTime = 0;
            if (existingFile != null && existingFile.exists())
            {
                fileTime = existingFile.lastModified();
            }

            SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH);
            String dateFormat = format.format(new Date(fileTime));

            StringBuilder sb = new StringBuilder();
            sb.append("https://repo.aw.app/");
            sb.append(address);
            String result = null;

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

                String xmlBody = response.body().string();

                if (response.code() == HttpURLConnection.HTTP_OK && xmlBody != null && xmlBody.length() > 10)
                {
                    storeFile(address, xmlBody);
                    result = address;
                }
                else
                {
                    result = "0x";
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

    private File getExternalFile(String contractAddress)
    {
        File directory = new File(
                Environment.getExternalStorageDirectory()
                        + File.separator + ALPHAWALLET_DIR);

        if (directory.exists())
        {
            File externalFile = new File(directory, contractAddress.toLowerCase());
            if (externalFile.exists())
            {
                return externalFile;
            }
        }

        return null;
    }

    private void loadExternalContracts(File directory)
    {
        try
        {
            loadContracts(directory, true);
            startFileListener(directory);
        }
        catch (IOException|SAXException e)
        {
            e.printStackTrace();
        }
    }

    private void loadContracts(File directory, boolean external) throws IOException, SAXException
    {
        File[] files = directory.listFiles();
        if (files != null)
        {
            for (File f : files)
            {
                if (f.getName().contains(".xml") || f.getName().contains(".tsml"))
                {
                    String extension = f.getName().substring(f.getName().lastIndexOf('.') + 1).toLowerCase();
                    if (extension.equals("xml") || extension.equals("tsml"))
                    {
                        try
                        {
                            if (f.getName().contains("entry2"))
                            {
                                System.out.println("door");
                            }
                            FileInputStream stream = new FileInputStream(f);
                            TokenDefinition td = parseFile(stream);
                            if (external) addOverrideFile(td);
                        }
                        catch (SAXParseException e)
                        {
                            e.printStackTrace();
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
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
        //build filename
        String fileName = contractAddress.toLowerCase() + "." + XML_EXT;

        //check
        File check = new File(context.getFilesDir(), fileName);
        if (check.exists())
        {
            return check;
        }
        else
        {
            return getExternalFile(contractAddress);
        }
    }

    private List<File> getFileList(File directory)
    {
        File[] files = context.getFilesDir().listFiles();
        return new ArrayList<File>(Arrays.asList(files));
    }

    private boolean isValidXML(File f)
    {
        int index = f.getName().lastIndexOf('.');
        if (index > 0)
        {
            String extension = f.getName().substring(index + 1).toLowerCase();
            String name = f.getName().substring(0, index).toLowerCase();
            return extension.equals("xml") && isValidAddress(name);
        }

        return false;
    }

    private String convertToAddress(File f)
    {
        return f.getName().substring(0, f.getName().lastIndexOf('.')).toLowerCase();
    }

    /**
     * check the downloaded XML files for updates when wallet restarts.
     */
    private void checkDownloadedFiles()
    {
        Disposable d = Observable.fromIterable(getFileList(context.getFilesDir()))
                .filter(this::isValidXML)
                .map(this::convertToAddress)
                .filter(this::notOverriden)
                .concatMap(this::fetchXMLFromServer)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleFile, this::onError);
    }

    private boolean notOverriden(String address)
    {
        //check all addresses
        for (int networkId : devOverrideContracts.keySet())
        {
            for (String addr : devOverrideContracts.get(networkId))
            {
                if (addr.equalsIgnoreCase(address))
                {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean notOverriden(int networkId, String address)
    {
        return !(devOverrideContracts.containsKey(networkId) && devOverrideContracts.get(networkId).contains(address));
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

    public boolean hasDefinition(int networkId, String address)
    {
        TokenDefinition definition = getDefinitionMapping(networkId, address);
        if (definition != null)
        {
            return definition.hasNetwork(networkId);
        }
        else
        {
            return false;
        }
    }

    /**
     * For legacy
     * @param address
     * @return
     */
    public int getChainId(String address)
    {
        for (int network : assetDefinitions.keySet())
        {
            if (assetDefinitions.get(network).containsKey(address))
            {
                return network;
            }
        }

        return 0;
    }

    private Observable<String> checkFileTime(File localDefinition)
    {
        return Observable.fromCallable(() -> {
            String contractAddress = convertToAddress(localDefinition);
            URL url = new URL("https://repo.awallet.io/" + contractAddress);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setIfModifiedSince( localDefinition.lastModified() );

            switch (conn.getResponseCode())
            {
                case HttpURLConnection.HTTP_OK:
                    break;

                case HttpURLConnection.HTTP_NOT_MODIFIED:
                    contractAddress = "";
                    break;
            }

            conn.disconnect();
            return contractAddress;
        });
    }

    //when user reloads the tokens we should also check XML for any files
    public void clearCheckTimes()
    {
        assetChecked.clear();
    }

    public boolean hasTokenView(int chainId, String contractAddr)
    {
        TokenDefinition td = getAssetDefinition(chainId, contractAddr);
        return (td != null && td.attributeSets.containsKey("cards"));
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

    public String getTokenFunctionView(int chainId, String contractAddr)
    {
        TokenDefinition td = getAssetDefinition(chainId, contractAddr);
        if (td != null && td.getActions().size() > 0)
        {
            for (TSAction a : td.getActions().values())
            {
                return a.view;
            }
            return null;
        }
        else
        {
            return null;
        }
    }

    public boolean hasAction(int chainId, String contractAddr)
    {
        TokenDefinition td = getAssetDefinition(chainId, contractAddr);
        if (td != null && td.actions != null && td.actions.size() > 0) return true;
        else return false;
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

    public void startFileListener(File path)
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
                                File newTSFile = new File(
                                        Environment.getExternalStorageDirectory()
                                                + File.separator + ALPHAWALLET_DIR, s);
                                FileInputStream stream = new FileInputStream(newTSFile);
                                String hash = calcMD5(newTSFile);
                                String fileName = newTSFile.getAbsolutePath();
                                if (fileHashes.containsKey(fileName) && fileHashes.get(fileName).equals(hash))
                                {
                                    break;
                                }
                                fileHashes.put(fileName, hash);
                                TokenDefinition td = parseFile(stream);
                                addOverrideFile(td);
                                notificationService.DisplayNotification("Definition Updated", s, NotificationCompat.PRIORITY_MAX);
                            }
                        }
                        catch (SAXException e)
                        {
                            e.printStackTrace();
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
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

    private static String calcMD5(File file) throws IOException, NoSuchAlgorithmException
    {
        FileInputStream fis = new FileInputStream(file);
        MessageDigest digest = MessageDigest.getInstance("MD5");

        byte[] byteArray = new byte[1024];
        int bytesCount = 0;

        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        };

        fis.close();

        byte[] bytes = digest.digest();

        //This bytes[] has bytes in decimal format;
        //Convert it to hexadecimal format
        StringBuilder sb = new StringBuilder();
        for (byte aByte : bytes)
        {
            sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
        }

        //return complete hash
        return sb.toString();
    }

    public Single<List<Token>> checkEthereumFunctions(TokensService tokensService)
    {
        return Single.fromCallable(() -> {
            List<Token> updatedTokens = new ArrayList<>();
            String walletAddress = tokensService.getCurrentAddress();
            for (int network : assetDefinitions.keySet())
            {
                Map<String, TokenDefinition> defMap = assetDefinitions.get(network);
                for (TokenDefinition td : defMap.values())
                {
                    //fetch all attr solutions
                    List<AttributeType> attrs = new ArrayList<>(td.attributeTypes.values());
                    for (AttributeType attr : attrs)
                    {
                        if (attr.function == null) continue;
                        FunctionDefinition fd = attr.function;
                        List<String> addresses = fd.contract.addresses.get(network);

                        if (addresses != null)
                        {
                            for (String address : addresses)
                            {
                                ContractAddress cAddr = new ContractAddress(network, address);
                                if (attr.function.contract.contractInterface != null)
                                {
                                    //do we have this token?
                                    Token token = tokensService.getToken(network, address);
                                    if (token != null)
                                    {
                                        Observable.fromIterable(token.getNonZeroArrayBalance())
                                                .map(tokenId -> getFunctionResult(cAddr, attr, tokenId))
                                                .filter(TransactionResult::needsUpdating)
                                                .concatMap(result -> td.fetchAttrResult(attr.id, result.tokenId, cAddr, this))
                                                .subscribeOn(Schedulers.io())
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribe();
                                    }
                                }
                                else
                                {
                                    //doesn't have a contract interface, so just fetch the function
                                    TransactionResult tr = getFunctionResult(cAddr, attr, BigInteger.ZERO);
                                    if (tr.needsUpdating())
                                    {
                                        td.fetchAttrResult(attr.id, tr.tokenId, cAddr, this)
                                                .subscribeOn(Schedulers.io())
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribe();
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return updatedTokens;
        });
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
        try (Realm realm = realmManager.getAuxRealmInstance(tokensService.getCurrentAddress())) {
            RealmAuxData realmToken = realm.where(RealmAuxData.class)
                    .equalTo("instanceKey", functionKey(contract, tokenId, attr.id))
                    .equalTo("chainId", contract.chainId)
                    .findFirst();

            if (realmToken != null)
            {
                tr.resultTime = realmToken.getResultTime();
                tr.result = realmToken.getResult();
            }

        } catch (Exception e) {
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
                    Realm realm;

                    @Override
                    public void onStart()
                    {
                        if (tResult.result == null) return;
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
        ContractAddress cAddr = new ContractAddress(tResult.contractChainId, tResult.contractAddress);
        RealmAuxData realmData = realm.createObject(RealmAuxData.class, functionKey(cAddr, tResult.tokenId, tResult.attrId));
        realmData.setResultTime(tResult.resultTime);
        realmData.setResult(tResult.result);
        realmData.setChainId(tResult.contractChainId);
        realmData.setFunctionId(tResult.method);
        realmData.setTokenId(tResult.tokenId.toString(Character.MAX_RADIX));
    }

    public StringBuilder getTokenAttrs(Token token, int count) throws Exception
    {
        StringBuilder attrs = new StringBuilder();

        TokenScriptResult.addPair(attrs, "name", token.getTokenTitle());
        TokenScriptResult.addPair(attrs, "symbol", token.tokenInfo.symbol);
        TokenScriptResult.addPair(attrs, "_count", String.valueOf(count));

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
        return definition.resolveAttributes(tokenId, this, cAddr);
    }
}
