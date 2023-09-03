package com.alphawallet.app.entity.attestation;

import static com.alphawallet.app.entity.tokenscript.TokenscriptFunction.ZERO_ADDRESS;
import static com.alphawallet.ethereum.EthereumNetworkBase.ARBITRUM_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.SEPOLIA_TESTNET_ID;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.EasAttestation;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.QRResult;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokendata.TokenGroup;
import com.alphawallet.app.entity.tokens.Attestation;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.repository.KeyProvider;
import com.alphawallet.app.repository.KeyProviderFactory;
import com.alphawallet.app.repository.entity.RealmAttestation;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.RealmManager;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.web3j.StructuredDataEncoder;
import com.alphawallet.token.entity.AttestationDefinition;
import com.alphawallet.token.entity.AttestationValidationStatus;
import com.alphawallet.token.entity.FunctionDefinition;
import com.alphawallet.token.tools.Numeric;
import com.alphawallet.token.tools.TokenDefinition;
import com.bumptech.glide.RequestBuilder;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import timber.log.Timber;

public class AttestationImport
{
    private final AssetDefinitionService assetDefinitionService;
    private final AttestationImportInterface callback;
    private final TokensService tokensService;
    private final Wallet wallet;
    private final RealmManager realmManager;
    private final OkHttpClient client;
    private final KeyProvider keyProvider = KeyProviderFactory.get();

    public static final String SMART_LAYER_DOMAIN = "https://www.smartlayer.network/pass";
    public static final String SMART_LAYER_DOMAIN_DEV = "https://smart-layer.vercel.app/pass";
    public static final String SMART_PASS_URL = "https://aw.app/openurl?url=";

    private static final String SMART_PASS_API = "https://backend.smartlayer.network/passes/pass-installed-in-aw";
    private static final String SMART_PASS_API_DEV = "https://d2a5tt41o5qmyt.cloudfront.net/passes/pass-installed-in-aw";

    public AttestationImport(AssetDefinitionService assetService,
                             TokensService tokensService,
                             AttestationImportInterface assetInterface,
                             Wallet wallet,
                             RealmManager realm,
                             OkHttpClient client)
    {
        this.assetDefinitionService = assetService;
        this.tokensService = tokensService;
        this.callback = assetInterface;
        this.wallet = wallet;
        this.realmManager = realm;
        this.client = client;
    }

    public void importAttestation(QRResult attestation)
    {
        switch (attestation.type)
        {
            case ATTESTATION:
                importLegacyAttestation(attestation);
                break;
            case EAS_ATTESTATION:
                importEASAttestation(attestation);
                break;
            default:
                break;
        }
    }

    private void importLegacyAttestation(QRResult attestation)
    {
        //Get token information - assume attestation is based on NFT
        //TODO: First validate Attestation
        tokensService.update(attestation.getAddress(), attestation.chainId, ContractType.ERC721)
                .flatMap(tInfo -> tokensService.storeTokenInfoDirect(wallet, tInfo, ContractType.ERC721))
                .flatMap(tInfo -> storeAttestation(attestation, tInfo))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(attn -> completeImport(attestation, attn), err -> callback.importError(err.getMessage()))
                .isDisposed();
    }

    private void completeImport(QRResult attestation, Attestation tokenAttn)
    {
        if (tokenAttn.isValid() == AttestationValidationStatus.Pass)
        {
            TokenCardMeta tcmAttestation = new TokenCardMeta(attestation.chainId, attestation.getAddress(), "1", System.currentTimeMillis(),
                    assetDefinitionService, tokenAttn.tokenInfo.name, tokenAttn.tokenInfo.symbol, tokenAttn.getBaseTokenType(), TokenGroup.ATTESTATION, tokenAttn.getAttestationUID());
            tcmAttestation.isEnabled = true;
            callback.attestationImported(tcmAttestation);
        }
        else
        {
            callback.importError(tokenAttn.isValid().getValue());
        }
    }

    @SuppressWarnings("checkstyle:MissingSwitchDefault")
    private Single<Attestation> storeAttestation(QRResult attestation, TokenInfo tInfo)
    {
        Attestation attn = validateAttestation(attestation.getAttestation(), tInfo);
        switch (attn.isValid())
        {
            case Pass:
                return storeAttestationInternal(attestation, tInfo, attn);
            case Expired:
            case Issuer_Not_Valid:
            case Incorrect_Subject:
                callback.importError(attn.isValid().getValue());
                break;
        }

        return Single.fromCallable(() -> attn);
    }

    private Single<Attestation> storeAttestationInternal(QRResult attestation, TokenInfo tInfo, Attestation attn)
    {
        //complete the import
        //write to realm
        return Single.fromCallable(() -> {
            try (Realm realm = realmManager.getRealmInstance(wallet))
            {
                realm.executeTransaction(r -> {

//                            RealmResults<RealmAttestation> realmAssets = realm.where(RealmAttestation.class)
//                                    .findAll();
//
//                            realmAssets.deleteAllFromRealm();

                    String key = attn.getDatabaseKey();
                    RealmAttestation realmAttn = r.where(RealmAttestation.class)
                            .equalTo("address", key)
                            .findFirst();

                    if (realmAttn == null)
                    {
                        realmAttn = r.createObject(RealmAttestation.class, key);
                    }

                    attn.populateRealmAttestation(realmAttn);
                    realmAttn.setAttestation(attn.getAttestation());
                });
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            return attn;
        }).flatMap(generatedAttestation -> {
            if (tokensService.getToken(tInfo.chainId, tInfo.address) == null)
            {
                return tokensService.storeTokenInfo(wallet, tInfo, ContractType.ERC721);
            }
            else
            {
                return Single.fromCallable(() -> tInfo);
            }
        }).map(info -> setBaseType(attn, info));
    }

    private Attestation setBaseType(Attestation attn, TokenInfo info)
    {
        Token baseToken = tokensService.getToken(info.chainId, info.address);
        if (baseToken != null)
        {
            attn.setBaseTokenType(baseToken.getInterfaceSpec());
        }

        return attn;
    }

    private void importEASAttestation(QRResult qrAttn)
    {
        //validate attestation
        //get chain and address
        EasAttestation easAttn = new Gson().fromJson(qrAttn.functionDetail, EasAttestation.class);

        //validation UID:
        storeAttestation(easAttn, qrAttn.functionDetail, qrAttn.getAddress())
                .flatMap(attn -> callSmartPassLog(attn, qrAttn.getAddress()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::checkTokenScript, err -> callback.importError(err.getMessage()))
                .isDisposed();
    }

    @SuppressWarnings("checkstyle:MissingSwitchDefault")
    private Single<Attestation> storeAttestation(EasAttestation attestation, String importedAttestation, String originLink)
    {
        //Use Default key unless specified
        return Single.fromCallable(() -> {
            Attestation attn = loadAttestation(attestation, originLink);
            switch (attn.isValid())
            {
                case Pass:
                    return storeAttestationInternal(originLink, attn);
                case Expired:
                case Issuer_Not_Valid:
                case Incorrect_Subject:
                    callback.importError(attn.isValid().getValue());
                    break;
            }

            return attn;
        });
    }

    private Attestation storeAttestationInternal(String originLink, Attestation attn)
    {
        try (Realm realm = realmManager.getRealmInstance(wallet))
        {
            realm.executeTransaction(r -> {
                String key = attn.getDatabaseKey();
                RealmAttestation realmAttn = r.where(RealmAttestation.class)
                        .equalTo("address", key)
                        .findFirst();

                if (realmAttn == null)
                {
                    realmAttn = r.createObject(RealmAttestation.class, key);
                }

                realmAttn.setAttestationLink(originLink);
                attn.populateRealmAttestation(realmAttn);
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return attn;
    }

    private void completeImport(Token token)
    {
        if (token instanceof Attestation tokenAttn && ((Attestation)token).isValid() == AttestationValidationStatus.Pass)
        {
            TokenCardMeta tcmAttestation = new TokenCardMeta(tokenAttn.tokenInfo.chainId, tokenAttn.getAddress(), "1", System.currentTimeMillis(),
                    assetDefinitionService, tokenAttn.tokenInfo.name, tokenAttn.tokenInfo.symbol, tokenAttn.getBaseTokenType(),
                    TokenGroup.ATTESTATION, tokenAttn.getAttestationUID());
            tcmAttestation.isEnabled = true;
            callback.attestationImported(tcmAttestation);
        }
    }

    private void checkTokenScript(Token token)
    {
        //check server for a TokenScript
        assetDefinitionService.checkServerForScript(token, null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(td -> completeImport(token), Timber::w)
                .isDisposed();
    }

    //Handling
    public Attestation validateAttestation(String attestation, TokenInfo tInfo)
    {
        TokenDefinition td = assetDefinitionService.getDefinition(getTSDataKeyTemp(tInfo.chainId, tInfo.address));//getDefinition(tInfo.chainId, tInfo.address);
        Attestation att = null;

        if (td != null)
        {
            NetworkInfo networkInfo = EthereumNetworkBase.getNetwork(tInfo.chainId);
            att = new Attestation(tInfo, networkInfo.name, Numeric.hexStringToByteArray(attestation));
            att.setTokenWallet(tokensService.getCurrentAddress());

            //call validation function and get details
            AttestationDefinition definitionAtt = td.getAttestation();
            //can we get the details?

            if (definitionAtt != null && definitionAtt.function != null)
            {
                //pull return type
                FunctionDefinition fd = definitionAtt.function;
                //add attestation to attr map
                //call function
                org.web3j.abi.datatypes.Function transaction = assetDefinitionService.generateTransactionFunction(att, BigInteger.ZERO, td, fd);
                transaction = new Function(fd.method, transaction.getInputParameters(), td.getAttestationReturnTypes()); //set return types

                //call and handle result
                String result = assetDefinitionService.callSmartContract(tInfo.chainId, tInfo.address, transaction);

                //break down result
                List<Type> values = FunctionReturnDecoder.decode(result, transaction.getOutputParameters());

                //interpret these values
                att.handleValidation(td.getValidation(values));
            }
        }

        return att;
    }

    public Attestation loadAttestation(EasAttestation attestation, String originLink)
    {
        String recoverAttestationSigner = recoverSigner(attestation);

        //1. Validate signer via key attestation service (using UID).
        boolean issuerOnKeyChain = checkAttestationSigner(attestation, recoverAttestationSigner);

        //2. Decode the ABI encoded payload to pull out the info. ABI Decode the schema bytes
        //initially we need a hardcoded schema - this should be fetched from the schema record EAS contract
        //fetch the schema of the attestation
        SchemaRecord attestationSchema = fetchSchemaRecord(attestation.getChainId(), attestation.getSchema());
        //convert into functionDecode
        List<String> names = new ArrayList<>();
        List<Type> values = decodeAttestationData(attestation.data, attestationSchema.schema, names);

        NetworkInfo networkInfo = EthereumNetworkBase.getNetwork(attestation.getChainId());

        TokenInfo tInfo = Attestation.getDefaultAttestationInfo(attestation.getChainId(), getEASContract(attestation.chainId));
        Attestation localAttestation = new Attestation(tInfo, networkInfo.name, originLink.getBytes(StandardCharsets.UTF_8));
        localAttestation.handleEASAttestation(attestation, names, values, recoverAttestationSigner);

        String collectionHash = localAttestation.getAttestationCollectionId();

        //is it a smartpass?
        tInfo = Attestation.getDefaultAttestationInfo(attestation.getChainId(), collectionHash);

        //Now regenerate with the correct collectionId
        localAttestation = new Attestation(tInfo, networkInfo.name, originLink.getBytes(StandardCharsets.UTF_8));
        localAttestation.handleEASAttestation(attestation, names, values, recoverAttestationSigner);
        localAttestation.setTokenWallet(tokensService.getCurrentAddress());

        return localAttestation;
    }

    private boolean checkAttestationSigner(EasAttestation attestation, String recoverAttestationSigner)
    {
        String keySchemaUID = getKeySchemaUID(attestation.getChainId());
        boolean attestationValid;
        if (Attestation.getKnownRootIssuers(attestation.chainId).contains(recoverAttestationSigner))
        {
            attestationValid = true;
        }
        else if (!TextUtils.isEmpty(keySchemaUID))
        {
            //call validate
            SchemaRecord schemaRecord = fetchSchemaRecord(attestation.getChainId(), keySchemaUID);
            attestationValid = checkAttestationIssuer(schemaRecord, attestation.getChainId(), recoverAttestationSigner);
        }
        else
        {
            attestationValid = false;
        }

        return attestationValid;
    }

    private String getDataValue(String key, List<String> names, List<Type> values)
    {
        Map<String, String> valueMap = new HashMap<>();
        for (int index = 0; index < names.size(); index++)
        {
            String name = names.get(index);
            Type<?> type = values.get(index);
            valueMap.put(name, type.toString());
        }

        return valueMap.get(key);
    }

    private List<Type> decodeAttestationData(String attestationData, @NonNull String decodeSchema, List<String> names)
    {
        List<TypeReference<?>> returnTypes = new ArrayList<TypeReference<?>>();
        if (TextUtils.isEmpty(decodeSchema))
        {
            return new ArrayList<>();
        }

        //build decoder
        String[] typeData = decodeSchema.split(",");
        for (String typeElement : typeData)
        {
            String[] data = typeElement.split(" ");
            String type = data[0];
            String name = data[1];
            if (type.startsWith("uint") || type.startsWith("int"))
            {
                type = "uint";
            }
            else if (type.startsWith("bytes") && !type.equals("bytes"))
            {
                type = "bytes32";
            }

            TypeReference<?> tRef = null;

            switch (type)
            {
                case "uint":
                    tRef = new TypeReference<Uint256>() { };
                    break;
                case "address":
                    tRef = new TypeReference<Address>() { };
                    break;
                case "bytes32":
                    tRef = new TypeReference<Bytes32>() { };
                    break;
                case "string":
                    tRef = new TypeReference<Utf8String>() { };
                    break;
                case "bytes":
                    tRef = new TypeReference<DynamicBytes>() { };
                    break;
                case "bool":
                    tRef = new TypeReference<Bool>() { };
                    break;
                default:
                    break;
            }

            if (tRef != null)
            {
                returnTypes.add(tRef);
            }
            else
            {
                Timber.e("Unhandled type!");
                returnTypes.add(new TypeReference<Uint256>() { });
            }

            names.add(name);
        }

        //decode the schema and populate the Attestation element
        return FunctionReturnDecoder.decode(attestationData, org.web3j.abi.Utils.convert(returnTypes));
    }

    private SchemaRecord fetchSchemaRecord(long chainId, String schemaUID)
    {
        SchemaRecord schemaRecord = tryCachedValues(schemaUID);

        if (schemaRecord == null)
        {
            schemaRecord = fetchSchemaRecordOnChain(chainId, schemaUID);
        }

        return schemaRecord;
    }

    private SchemaRecord fetchSchemaRecordOnChain(long chainId, String schemaUID)
    {
        //1. Resolve UID. For now, just use default: This should be on a switch for chains
        String globalResolver = getEASSchemaContract(chainId);

        //format transaction to get key resolver
        Function getKeyResolver2 = new Function("getSchema",
                Collections.singletonList(new Bytes32(Numeric.hexStringToByteArray(schemaUID))),
                Collections.singletonList(new TypeReference<SchemaRecord>() {}));

        String result = assetDefinitionService.callSmartContract(chainId, globalResolver, getKeyResolver2);
        List<Type> values = FunctionReturnDecoder.decode(result, getKeyResolver2.getOutputParameters());

        return (SchemaRecord)values.get(0);
    }

    private SchemaRecord tryCachedValues(String schemaUID) //doesn't matter about chain clash - if schemaUID matches then the schema is the same
    {
        return getCachedSchemaRecords().getOrDefault(schemaUID, null);
    }

    private boolean checkAttestationIssuer(SchemaRecord schemaRecord, long chainId, String signer)
    {
        String rootKeyUID = getDefaultRootKeyUID(chainId);
        //pull the key resolver
        Address resolverAddr = schemaRecord.resolver;
        //call the resolver to test key validity
        Function validateKey = new Function("validateSignature",
                Arrays.asList((new Bytes32(Numeric.hexStringToByteArray(rootKeyUID))),
                        new Address(signer)),
                Collections.singletonList(new TypeReference<Bool>() {}));

        String result = assetDefinitionService.callSmartContract(chainId, resolverAddr.getValue(), validateKey);
        List<Type> values = FunctionReturnDecoder.decode(result, validateKey.getOutputParameters());
        return ((Bool)values.get(0)).getValue();
    }

    public static String recoverSigner(EasAttestation attestation)
    {
        String recoveredAddress = "";

        try
        {
            StructuredDataEncoder dataEncoder = new StructuredDataEncoder(attestation.getEIP712Attestation());
            byte[] hash = dataEncoder.hashStructuredData();
            byte[] r = Numeric.hexStringToByteArray(attestation.getR());
            byte[] s = Numeric.hexStringToByteArray(attestation.getS());
            byte v = (byte)(attestation.getV() & 0xFF);

            Sign.SignatureData sig = new Sign.SignatureData(v, r, s);
            BigInteger key = Sign.signedMessageHashToKey(hash, sig);
            recoveredAddress = Numeric.prependHexPrefix(Keys.getAddress(key));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return recoveredAddress;
    }

    // NB Java 11 doesn't have support for switching on a 'long' :(
    public static String getEASContract(long chainId)
    {
        if (chainId == MAINNET_ID)
        {
            return "0xA1207F3BBa224E2c9c3c6D5aF63D0eb1582Ce587";
        }
        else if (chainId == ARBITRUM_MAIN_ID)
        {
            return "0xbD75f629A22Dc1ceD33dDA0b68c546A1c035c458";
        }
        else if (chainId == SEPOLIA_TESTNET_ID)
        {
            return "0xC2679fBD37d54388Ce493F1DB75320D236e1815e";
        }
        else
        {
            //Support Optimism Goerli (0xC2679fBD37d54388Ce493F1DB75320D236e1815e)
            return "";
        }
    }

    private String getEASSchemaContract(long chainId)
    {
        if (chainId == MAINNET_ID)
        {
            return "0xA7b39296258348C78294F95B872b282326A97BDF";
        }
        else if (chainId == ARBITRUM_MAIN_ID)
        {
            return "0xA310da9c5B885E7fb3fbA9D66E9Ba6Df512b78eB";
        }
        else if (chainId == SEPOLIA_TESTNET_ID)
        {
            return "0x0a7E2Ff54e76B8E6659aedc9103FB21c038050D0";
        }
        else
        {
            //Support Optimism Goerli (0x7b24C7f8AF365B4E308b6acb0A7dfc85d034Cb3f)
            return "";
        }
    }

    //UID of schema used for keys on each chain - the resolver is tied to this UID
    private String getKeySchemaUID(long chainId)
    {
        if (chainId == MAINNET_ID)
        {
            return "";
        }
        else if (chainId == ARBITRUM_MAIN_ID)
        {
            return "0x5f0437f7c1db1f8e575732ca52cc8ad899b3c9fe38b78b67ff4ba7c37a8bf3b4";
        }
        else if (chainId == SEPOLIA_TESTNET_ID)
        {
            return "0x4455598d3ec459c4af59335f7729fea0f50ced46cb1cd67914f5349d44142ec1";
        }
        else
        {
            //Support Optimism Goerli (0x7b24C7f8AF365B4E308b6acb0A7dfc85d034Cb3f)
            return "";
        }
    }

    // If not specified
    private String getDefaultRootKeyUID(long chainId)
    {
        if (chainId == MAINNET_ID)
        {
            return "";
        }
        else if (chainId == ARBITRUM_MAIN_ID)
        {
            return "0xe5c2bfd98a1b35573610b4e5a367bbcb5c736e42508a33fd6046bad63eaf18f9";
        }
        else if (chainId == SEPOLIA_TESTNET_ID)
        {
            return "0xee99de42f544fa9a47caaf8d4a4426c1104b6d7a9df7f661f892730f1b5b1e23";
        }
        else
        {
            //Support Optimism Goerli (0x7b24C7f8AF365B4E308b6acb0A7dfc85d034Cb3f)
            return "";
        }
    }

    private Map<String, SchemaRecord> getCachedSchemaRecords()
    {
        Map<String, SchemaRecord> recordMap = new HashMap<>();

        SchemaRecord keySchema = new SchemaRecord(Numeric.hexStringToByteArray("0x4455598d3ec459c4af59335f7729fea0f50ced46cb1cd67914f5349d44142ec1")
                , new Address("0x0ed88b8af0347ff49d7e09aa56bd5281165225b6"), true, "string KeyDescription,bytes ASN1Key,bytes PublicKey");
        SchemaRecord keySchema2 = new SchemaRecord(Numeric.hexStringToByteArray("0x5f0437f7c1db1f8e575732ca52cc8ad899b3c9fe38b78b67ff4ba7c37a8bf3b4")
                , new Address("0xF0768c269b015C0A246157c683f9377eF571dCD3"), true, "string KeyDescription,bytes ASN1Key,bytes PublicKey");
        SchemaRecord smartPass = new SchemaRecord(Numeric.hexStringToByteArray("0x7f6fb09beb1886d0b223e9f15242961198dd360021b2c9f75ac879c0f786cafd")
                , new Address(ZERO_ADDRESS), true, "string eventId,string ticketId,uint8 ticketClass,bytes commitment");
        SchemaRecord smartPass2 = new SchemaRecord(Numeric.hexStringToByteArray("0x0630f3342772bf31b669bdbc05af0e9e986cf16458f292dfd3b57564b3dc3247")
                , new Address(ZERO_ADDRESS), true, "string devconId,string ticketIdString,uint8 ticketClass,bytes commitment");
        SchemaRecord smartPassMainNetLegacy = new SchemaRecord(Numeric.hexStringToByteArray("0xba8aaaf91d1f63d998fb7da69449d9a314bef480e9555710c77d6e594e73ca7a")
                , new Address(ZERO_ADDRESS), true, "string eventId,string ticketId,uint8 ticketClass,bytes commitment,string scriptUri");
        SchemaRecord smartPassMainNet = new SchemaRecord(Numeric.hexStringToByteArray("0x44ec5251add2115c92896cf4b531eb2fcfac6d8ec8caa451df52f0a25a028545")
                , new Address(ZERO_ADDRESS), true, "uint16 version,string orgId,string memberId,string memberRole,bytes commitment,string scriptURI");

        recordMap.put(Numeric.toHexString(keySchema.uid), keySchema);
        recordMap.put(Numeric.toHexString(keySchema2.uid), keySchema2);
        recordMap.put(Numeric.toHexString(smartPass.uid), smartPass);
        recordMap.put(Numeric.toHexString(smartPass2.uid), smartPass2);
        recordMap.put(Numeric.toHexString(smartPassMainNetLegacy.uid), smartPassMainNetLegacy);
        recordMap.put(Numeric.toHexString(smartPassMainNet.uid), smartPassMainNet);

        return recordMap;
    }

    private String getTSDataKeyTemp(long chainId, String address)
    {
        if (address.equalsIgnoreCase(tokensService.getCurrentAddress()))
        {
            address = "ethereum";
        }

        return address.toLowerCase(Locale.ROOT) + "-" + chainId;
    }


    //Smart Pass handling
    private Single<Attestation> callSmartPassLog(Attestation attn, String fullPass)
    {
        return Single.fromCallable(() -> {
            //check if attestation is valid, and if it's a smartpass
            if (attn.isValid() == AttestationValidationStatus.Pass && attn.isSmartPass())
            {
                callback.smartPassValidation(callPassConfirmAPI(attn, fullPass));
            }

            return attn;
        });
    }

    //call API if required
    private SmartPassReturn callPassConfirmAPI(Attestation attn, String magicLink)
    {
        //need to send the raw attestation (not processed)
        //isolate the pass
        String rawPass = Utils.extractRawAttestation(magicLink);
        if (TextUtils.isEmpty(rawPass))
        {
            return SmartPassReturn.IMPORT_FAILED; //Should not happen if we get to this stage!
        }

        Request.Builder builder = new Request.Builder();

        if (EthereumNetworkBase.hasRealValue(attn.tokenInfo.chainId))
        {
            builder.url(SMART_PASS_API)
                    .header("Authorization", "Bearer " + keyProvider.getSmartPassKey());
        }
        else
        {
            builder.url(SMART_PASS_API_DEV)
                    .header("Authorization", "Bearer " + keyProvider.getSmartPassDevKey());
        }

        Request request = builder
                .put(buildPassBody(rawPass))
                .build();

        try (okhttp3.Response response = client.newCall(request).execute())
        {
            switch (response.code()/100)
            {
                case 2:
                    return SmartPassReturn.IMPORT_SUCCESS;
                case 4:
                    return SmartPassReturn.ALREADY_IMPORTED;
                default:
                case 5:
                    return SmartPassReturn.IMPORT_FAILED;
            }
        }
        catch (Exception e)
        {
            return SmartPassReturn.NO_CONNECTION;
        }
    }

    private RequestBody buildPassBody(String rawPass)
    {
        RequestBody body = null;
        try
        {
            JSONObject json = new JSONObject();
            json.put("signedToken", rawPass);
            json.put("installedPassedInAw", 1);
            body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
        }
        catch (JSONException e)
        {
            Timber.w(e);
        }

        return body;
    }
}
