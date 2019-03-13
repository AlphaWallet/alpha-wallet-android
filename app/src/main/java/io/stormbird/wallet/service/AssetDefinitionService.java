package io.stormbird.wallet.service;


import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.token.entity.NonFungibleToken;
import io.stormbird.token.tools.TokenDefinition;
import io.stormbird.wallet.R;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

import static io.stormbird.wallet.C.ADDED_TOKEN;
import static io.stormbird.wallet.viewmodel.HomeViewModel.ALPHAWALLET_DIR;
import static org.web3j.crypto.WalletUtils.isValidAddress;


/**
 * AssetDefinitionService is the only place where we interface with XML files.
 * This is to avoid duplicating resources
 * and also provide a consistent way to get XML values
 */

public class AssetDefinitionService
{
    private static final String XML_EXT = "xml";
    private final Context context;
    private final OkHttpClient okHttpClient;
    private Map<String, TokenDefinition> assetDefinitions; //Mapping of contract address to token definitions
    private Map<String, Long> assetChecked;                //Mapping of contract address to when they were last fetched from server
    private List<String> devOverrideContracts;             //List of contract addresses which have been overridden by definition in developer folder

    public AssetDefinitionService(OkHttpClient client, Context ctx)
    {
        context = ctx;
        okHttpClient = client;
        assetChecked = new HashMap<>();
        devOverrideContracts = new ArrayList<>();

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
        catch (IOException|SAXException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Fetches non fungible token definition given contract address and token ID
     * @param contractAddress
     * @param v
     * @return
     */
    public NonFungibleToken getNonFungibleToken(String contractAddress, BigInteger v)
    {
        TokenDefinition definition = getAssetDefinition(contractAddress);
        if (definition != null)
        {
            return new NonFungibleToken(v, definition);
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
        //create XML repository directory
        File directory = new File(
                Environment.getExternalStorageDirectory()
                        + File.separator + ALPHAWALLET_DIR);

        if (!directory.exists())
        {
            directory.mkdir(); //does this throw if we haven't given permission?
        }

        loadExternalContracts();
    }

    public boolean hasDefinition(String contractAddress)
    {
        TokenDefinition d = getAssetDefinition(contractAddress.toLowerCase());
        return d != null;
    }

    /**
     * Get asset definition given contract address
     *
     * @param address
     * @return
     */
    public TokenDefinition getAssetDefinition(String address)
    {
        TokenDefinition assetDef = null;
        String correctedAddress = address.toLowerCase(); //ensure address is in the format we want
        //is asset definition currently read?
        assetDef = assetDefinitions.get(correctedAddress);
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
                if (assetDef.addresses.size() > 0)
                {
                    assetDefinitions.put(address.toLowerCase(), assetDef);
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
        for (TokenDefinition td : assetDefinitions.values())
        {
            for (String address : td.addresses.keySet())
            {
                if ((td.addresses.get(address) == 1 || td.addresses.get(address) == networkId) && !contractList.contains(address))
                {
                    contractList.add(address);
                }
            }
        }
        return contractList;
    }

    /**
     * Get the issuer name given the contract address
     * Note: this
     * @param contractAddress
     * @return
     */
    public String getIssuerName(String contractAddress, String network)
    {
        //only specify the issuer name if we're on mainnet, otherwise default to 'Ethereum'
        //TODO: Remove the main-net stipulation once we do multi-XML handling
        //Note we check that the contract is actually specified in the XML - if we're just using the XML
        //as a default then we will just get default 'ethereum' issuer.
        TokenDefinition definition = getAssetDefinition(contractAddress);

        if (definition != null && definition.addresses.containsKey(contractAddress))
        {
            String issuer = definition.getKeyName();
            return (issuer == null || issuer.length() == 0) ? context.getString(R.string.stormbird) : issuer;
        }
        else if (network != null)
        {
            return network;
        }
        else
        {
            return context.getString(R.string.ethereum);
        }
    }
    public String getIssuerName(String contractAddress)
    {
        return getIssuerName(contractAddress, null);
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

        return definition;
    }

    private TokenDefinition parseFile(InputStream xmlInputStream) throws IOException, SAXException
    {
        TokenDefinition definition = null;
        definition = new TokenDefinition(
                xmlInputStream,
                context.getResources().getConfiguration().locale);

        //now assign the networks
        if (definition.addresses.size() > 0)
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
            for (String contractAddress : definition.addresses.keySet())
            {
                contractAddress = contractAddress.toLowerCase();
                if (notOverriden(contractAddress))
                {
                    assetDefinitions.put(contractAddress, definition);
                }
            }
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
            //now map all contained addresses
            for (String contractAddress : definition.addresses.keySet())
            {
                contractAddress = contractAddress.toLowerCase();
                if (!devOverrideContracts.contains(contractAddress)) devOverrideContracts.add(contractAddress);
            }
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

    private void handleFile(String address)
    {
        //this is file stored on the phone, notify to the main app to reload (use receiver)
        TokenDefinition assetDefinition = loadTokenDefinition(address);
        if (assetDefinition != null && assetDefinition.attributeTypes.size() > 0 && assetDefinition.addresses.size() > 0 && notOverriden(address))
        {
            assetDefinitions.put(address.toLowerCase(), assetDefinition);
        }
    }

    /*SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH);
            String dateFormat = format.format(new Date(fileTime));
            conn.addRequestProperty("If-Modified-Since", dateFormat);
*/

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
            String OSVersion = String.valueOf(Build.VERSION.RELEASE) ;

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

    public void loadExternalContracts()
    {
        //TODO: Check if external contracts override the internal ones - this is the expected behaviour
        File directory = new File(
                Environment.getExternalStorageDirectory()
                        + File.separator + ALPHAWALLET_DIR);

        try
        {
            loadContracts(directory, true);
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
                if (f.getName().contains(".xml"))
                {
                    String extension = f.getName().substring(f.getName().lastIndexOf('.') + 1).toLowerCase();
                    if (extension.equals("xml"))
                    {
                        try
                        {
                            FileInputStream stream = new FileInputStream(f);
                            TokenDefinition td = parseFile(stream);
                            if (external) addOverrideFile(td);
                        }
                        catch (SAXParseException e)
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
        return !devOverrideContracts.contains(address);
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

    public int getChainId(String address)
    {
        TokenDefinition definition = getAssetDefinition(address);
        if (definition != null)
        {
            return definition.getNetworkFromContract(address);
        }
        else
        {
            return 0;
        }
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

    public String getFeemasterAPI(String address)
    {
        TokenDefinition td = getAssetDefinition(address);
        if (td != null)
        {
            return td.getFeemasterAPI();
        }
        else
        {
            return null;
        }
    }

    //when user reloads the tokens we should also check XML for any files
    public void clearCheckTimes()
    {
        assetChecked.clear();
    }

    public boolean hasIFrame(String contractAddr)
    {
        boolean hasIframe = false;
        TokenDefinition td = assetDefinitions.get(contractAddr);
        if (td != null && td.attributeSets.containsKey("appearance"))
        {
            hasIframe = true;
        }

        return hasIframe;
    }

    public String getIntroductionCode(String contractAddr)
    {
        String appearance = "";
        TokenDefinition td = assetDefinitions.get(contractAddr);
        if (td != null && td.attributeSets.containsKey("appearance"))
        {
            appearance = td.getAppearance("introduction");
        }

        return appearance;
    }

    public String getInstructionCode(String contractAddr)
    {
        String appearance = "";
        TokenDefinition td = assetDefinitions.get(contractAddr);
        if (td != null && td.attributeSets.containsKey("appearance"))
        {
            appearance = td.getAppearance("instruction");
        }

        return appearance;
    }
}
