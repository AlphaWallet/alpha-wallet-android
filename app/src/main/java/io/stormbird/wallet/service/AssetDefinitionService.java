package io.stormbird.wallet.service;


import android.content.Context;
import android.os.Environment;

import org.xml.sax.SAXException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.token.entity.NonFungibleToken;
import io.stormbird.token.tools.TokenDefinition;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.NetworkInfo;
import okhttp3.OkHttpClient;
import okhttp3.Request;


/**
 * AssetDefinitionService is the only place where we interface with XML files.
 * This is to avoid duplicating resources
 * and also provide a consistent way to get XML values
 */

public class AssetDefinitionService
{
    private static final String XML_DIR = "Alpha Wallet";
    private static final String XML_EXT = "xml";
    private TokenDefinition assetDefinition;
    private NetworkInfo currentNetworkInfo;
    private final Context context;
    private final OkHttpClient okHttpClient;
    private Map<String, TokenDefinition> assetDefinitions;
    private Map<Integer, List<String>> networkMappings;

    public AssetDefinitionService(OkHttpClient client, Context ctx)
    {
        context = ctx;
        okHttpClient = client;
        networkMappings = new HashMap<>();
        init(ctx);
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
        return new NonFungibleToken(v, definition);
    }

    /**
     * Called at startup once we know we've got folder write permission.
     * Note - Android 6.0 and above needs user to verify folder access permission
     * TODO: if user doesn't give permission then use the app private folder and tell user they can't
     *  load contracts themselves
     */
    public void gotPermission()
    {
        //create XML repository directory
        File directory = new File(
                Environment.getExternalStorageDirectory()
                        + File.separator + XML_DIR);

        if (!directory.exists())
        {
            directory.mkdir();
        }
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
            File xmlFile = checkXMLDirectory(correctedAddress);

            //try web
            if (xmlFile == null)
            {
                loadScriptFromServer(correctedAddress); //this will complete asynchronously, and display will be updated
                assetDef = assetDefinition;
            }
            else
            {
                assetDef = loadTokenDefinition(correctedAddress);
            }

            assetDefinitions.put(address.toLowerCase(), assetDef);
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
        List<String> contractList = networkMappings.get(networkId);
        return contractList;
    }

    /**
     * Get the issuer name given the contract address
     * Note: this
     * @param contractAddress
     * @return
     */
    public String getIssuerName(String contractAddress)
    {
        //only specify the issuer name if we're on mainnet, otherwise default to 'Ethereum'
        //TODO: Remove the main-net stipulation once we do multi-XML handling
        //Note we check that the contract is actually specified in the XML - if we're just using the XML
        //as a default then we will just get default 'ethereum' issuer.
        if (isMainNet() && assetDefinition.getNetworkFromContract(contractAddress) > 0)
        {
            return assetDefinition.getKeyName();
        }
        else
        {
            return context.getString(R.string.ethereum);
        }
    }

    private void init(Context ctx)
    {
        assetDefinition = null;
        assetDefinitions = new HashMap<>();
        try
        {
            //Load background XML as fall-back
            //TODO: fetch this from the server
            assetDefinition = new TokenDefinition(
                    ctx.getResources().getAssets().open("TicketingContract.xml"),
                    ctx.getResources().getConfiguration().locale);

            assignNetworks(assetDefinition);

            //parse all contracts to populate the network map (some contract addresses are within the XML, not at the filename)
            loadAllContracts();
        }
        catch (IOException | SAXException e)
        {
            e.printStackTrace();
        }
    }

    private void loadScriptFromServer(String correctedAddress)
    {
        Disposable d = fetchXMLFromServer(correctedAddress)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleFile, this::onError);
    }

    private void onError(Throwable throwable)
    {
        throwable.printStackTrace();
    }

    private TokenDefinition loadTokenDefinition(String address)
    {
        TokenDefinition definition = null;
        File xmlFile = checkXMLDirectory(address.toLowerCase());
        if (xmlFile != null && xmlFile.exists())
        {
            definition = parseFile(xmlFile);
        }

        if (definition == null) definition = assetDefinition;

        return definition;
    }

    private TokenDefinition parseFile(File xmlFile)
    {
        TokenDefinition definition = null;
        try
        {
            FileInputStream is = new FileInputStream(xmlFile);
            definition = new TokenDefinition(
                    is,
                    context.getResources().getConfiguration().locale);

            //now assign the networks
            assignNetworks(definition);
        }
        catch (IOException | SAXException e)
        {
            e.printStackTrace();
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
                Integer networkId = definition.addresses.get(contractAddress);
                contractAddress = contractAddress.toLowerCase();
                assetDefinitions.put(contractAddress, definition);
                List<String> addresses = networkMappings.get(networkId);
                if (addresses == null) addresses = new ArrayList<>();
                if (!addresses.contains(contractAddress)) addresses.add(contractAddress);
                networkMappings.put(networkId, addresses);
            }
        }
    }

    private void handleFile(String address)
    {
        //this is file stored on the phone, notify to the main app to reload (use receiver)
        TokenDefinition assetDefinition = loadTokenDefinition(address);
        if (assetDefinition != null)
        {
            assetDefinitions.put(address.toLowerCase(), assetDefinition);
        }
    }

    private Observable<String> fetchXMLFromServer(String address)
    {
        return Observable.fromCallable(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("https://repo.awallet.io/");
            sb.append(address);
            String result = null;

            try
            {
                Request request = new Request.Builder()
                        .url(sb.toString())
                        .get()
                        .build();

                okhttp3.Response response = okHttpClient.newCall(request).execute();

                String xmlBody = response.body().string();
                if (xmlBody != null && xmlBody.length() > 10)
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

            return result;
        });
    }

    private File checkXMLDirectory(String contractAddress)
    {
        File directory = new File(
                Environment.getExternalStorageDirectory()
                        + File.separator + XML_DIR);

        if (directory.exists())
        {
            return findXMLFile(directory, contractAddress);
        }
        else
        {
            return null;
        }
    }

    private void loadAllContracts()
    {
        File directory = new File(
                Environment.getExternalStorageDirectory()
                        + File.separator + XML_DIR);

        File[] files = directory.listFiles();
        for (File f : files)
        {
            String extension = f.getName().substring(f.getName().lastIndexOf('.') + 1).toLowerCase();
            if (extension.equals("xml"))
            {
                TokenDefinition definition = parseFile(f);
            }
        }
    }

    private File findXMLFile(File directory, String contractAddress)
    {
        //build filename
        String fileName = contractAddress.toLowerCase() + "." + XML_EXT;

        //check
        File check = new File(directory, fileName);
        if (check.exists())
        {
            return check;
        }
        else
        {
            return null;
        }
    }

    private File storeFile(String address, String result) throws IOException
    {
        File directory = new File(
                Environment.getExternalStorageDirectory()
                        + File.separator + XML_DIR);

        String fName = address + ".xml";

        File file = new File(directory, fName);

        FileOutputStream fos = new FileOutputStream(file);
        OutputStream os = new BufferedOutputStream(fos);
        os.write(result.getBytes(), 0, result.length());
        os.close();
        fos.close();
        return file;
    }

    //TODO: these won't be needed when we have multi-XML handling
    public void setCurrentNetwork(NetworkInfo networkInfo)
    {
        currentNetworkInfo = networkInfo;
    }
    private boolean isMainNet()
    {
        return currentNetworkInfo.isMainNetwork;
    }

}
