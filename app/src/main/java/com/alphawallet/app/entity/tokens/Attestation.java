package com.alphawallet.app.entity.tokens;

import static com.alphawallet.app.entity.tokenscript.TokenscriptFunction.ZERO_ADDRESS;
import static com.alphawallet.app.repository.TokensRealmSource.attestationDatabaseKey;

import android.content.Context;
import android.text.TextUtils;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.EasAttestation;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.repository.entity.RealmAttestation;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.web3j.StructuredDataEncoder;
import com.alphawallet.token.entity.AttestationDefinition;
import com.alphawallet.token.entity.AttestationValidation;
import com.alphawallet.token.entity.AttestationValidationStatus;
import com.alphawallet.token.entity.TokenScriptResult;
import com.alphawallet.token.tools.Numeric;
import com.alphawallet.token.tools.TokenDefinition;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.reactivex.Single;
import timber.log.Timber;
import wallet.core.jni.Hash;

/**
 * Created by JB on 23/02/2023.
 */
public class Attestation extends Token
{
    private final byte[] attestation;
    private String attestationSubject;
    private String issuerKey;
    private long validFrom;
    private long validUntil;
    private final Map<String, MemberData> additionalMembers = new HashMap<>();
    private boolean isValid;
    private ContractType baseTokenType = ContractType.ERC721; // default to ERC721
    private static final String VALID_FROM = "time";
    private static final String VALID_TO = "expirationTime";
    private static final String TICKET_ID = "TicketId";
    private static final String SCRIPT_URI = "scriptURI";
    private static final String EVENT_IDS = "orgId,eventId,devconId"; //TODO: Remove once we use TokenScript
    private static final String SECONDARY_IDS = "version";
    private static final String SCHEMA_DATA_PREFIX = "data.";
    public static final String ATTESTATION_SUFFIX = "-att";
    public static final String EAS_ATTESTATION_TEXT = "EAS Attestation";
    public static final String EAS_ATTESTATION_SYMBOL = "ATTN";
    private static final String SMART_LAYER = "SMARTLAYER"; //TODO: Remove once we use TokenScript

    //TODO: Supplemental data
    public Attestation(TokenInfo tokenInfo, String networkName, byte[] attestation)
    {
        super(tokenInfo, BigDecimal.ONE, System.currentTimeMillis(), networkName, ContractType.ATTESTATION);
        this.attestation = attestation;
        setAttributeResult(BigInteger.ONE, new TokenScriptResult.Attribute("attestation", "attestation", BigInteger.ONE, Numeric.toHexString(attestation)));
    }

    public static TokenInfo getDefaultAttestationInfo(long chainId, String collectionHash)
    {
        return new TokenInfo(collectionHash, EAS_ATTESTATION_TEXT, EAS_ATTESTATION_SYMBOL, 0, true, chainId);
    }

    public byte[] getAttestation()
    {
        return attestation;
    }

    public void handleValidation(AttestationValidation attValidation)
    {
        if (attValidation == null)
        {
            return;
        }

        attestationSubject = attValidation._subjectAddress;
        isValid = attValidation._isValid;
        issuerKey = attValidation._issuerKey;

        for (Map.Entry<String, Type<?>> t : attValidation.additionalMembers.entrySet())
        {
            addToMemberData(t.getKey(), t.getValue());
        }

        MemberData ticketId = new MemberData(SCHEMA_DATA_PREFIX + TICKET_ID, attValidation._attestationId.longValue());
        additionalMembers.put(SCHEMA_DATA_PREFIX + TICKET_ID, ticketId);
    }

    public void handleEASAttestation(EasAttestation attn, List<String> names, List<Type> values, String issuer)
    {
        //add members
        for (int index = 0; index < names.size(); index++)
        {
            String name = SCHEMA_DATA_PREFIX + names.get(index);
            Type<?> type = values.get(index);
            addToMemberData(name, type);
        }

        issuerKey = issuer;
        attestationSubject = attn.recipient;
        validFrom = attn.time;
        validUntil = attn.expirationTime;
        long currentTime = System.currentTimeMillis() / 1000L;
        isValid = currentTime > validFrom && (validUntil == 0 || currentTime < validUntil);

        additionalMembers.put(VALID_FROM, new MemberData(VALID_FROM, attn.time).setIsTime());
        if (attn.expirationTime > 0)
        {
            additionalMembers.put(VALID_TO, new MemberData(VALID_TO, attn.expirationTime).setIsTime());
        }
    }

    public AttestationValidationStatus isValid()
    {
        //Check has expired
        if (!isValid)
        {
            return AttestationValidationStatus.Expired;
        }

        //Check attestation is being collected by the correct wallet (TODO: if not correct wallet, and wallet is present in user's wallets offer to switch wallet)
        if (TextUtils.isEmpty(attestationSubject) || (!attestationSubject.equalsIgnoreCase(getWallet()) && !attestationSubject.equals("0") && !attestationSubject.equals(ZERO_ADDRESS)))
        {
            return AttestationValidationStatus.Incorrect_Subject;
        }

        return AttestationValidationStatus.Pass;
    }

    public String getAttestationUID()
    {
        StringBuilder identifier = new StringBuilder();
        for (Map.Entry<String, MemberData> m : additionalMembers.entrySet())
        {
            if (m.getValue().isURI() || !m.getValue().isSchemaValue() || m.getValue().isBytes()) //this may change between same attestations.
            {
                continue;
            }

            if (identifier.length() > 0)
            {
                identifier.append("-");
            }

            identifier.append(m.getValue().getString());
        }

        return Numeric.toHexStringNoPrefix(Hash.keccak256(identifier.toString().getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public String getAttestationCollectionId()
    {
        // TODO: Pull these from the TokenScript
        String eventId = getCollectionId(EVENT_IDS);
        String eventPostFix = getCollectionId(SECONDARY_IDS);

        EasAttestation easAttestation = getEasAttestation();

        byte[] collectionBytes;
        if (easAttestation == null)
        {
            collectionBytes = "No Collection".getBytes(StandardCharsets.UTF_8);
        }
        else
        {
            //issuer public key
            //calculate hash from attestation
            String hexStr = Numeric.cleanHexPrefix(easAttestation.getSchema()).toLowerCase(Locale.ROOT) + Keys.getAddress(recoverPublicKey(easAttestation)).toLowerCase(Locale.ROOT)
                    + eventId + eventPostFix;
            //now convert this into ASCII hex bytes
            collectionBytes = hexStr.getBytes(StandardCharsets.UTF_8);
        }

        //get Hash
        byte[] hash = Hash.keccak256(collectionBytes);

        return Numeric.toHexString(hash);
    }

    private String getCollectionId(String eventIds)
    {
        String collectionId = "";
        String[] candidates = eventIds.split(",");
        for (String candidate : candidates)
        {
            MemberData memberData = additionalMembers.get(SCHEMA_DATA_PREFIX + candidate);
            if (memberData != null)
            {
                collectionId = memberData.getString();
                break;
            }
        }

        return collectionId;
    }

    @Override
    public String getTSKey()
    {
        return getAttestationCollectionId();
    }

    public String getAttestationDescription(TokenDefinition td)
    {
        AttestationDefinition att = td != null ? td.getAttestation() : null;
        if (att != null && att.attributes != null && att.attributes.size() > 0)
        {
            return displayTokenScriptAttrs(att);
        }
        else
        {
            return displayIntrinsicAttrs();
        }
    }

    private String displayTokenScriptAttrs(AttestationDefinition att)
    {
        StringBuilder identifier = new StringBuilder();
        for (Map.Entry<String, String> attr : att.attributes.entrySet())
        {
            String typeName = attr.getKey();
            String attrTitle = attr.getValue();
            MemberData attrVal = additionalMembers.get(typeName);
            if (attrVal == null || !attrVal.isSchemaValue())
            {
                continue;
            }

            String attrValue = getAttrValue(typeName);

            if (!TextUtils.isEmpty(attrValue))
            {
                if (identifier.length() > 0)
                {
                    identifier.append(" ");
                }

                identifier.append(attrTitle).append(": ").append(attrValue);
            }
        }

        return identifier.toString();
    }

    private String displayIntrinsicAttrs()
    {
        StringBuilder identifier = new StringBuilder();
        for (Map.Entry<String, MemberData> m : additionalMembers.entrySet())
        {
            if (m.getValue().isSchemaValue() && !m.getValue().isURI() && !m.getValue().isBytes())
            {
                if (identifier.length() > 0)
                {
                    identifier.append(" ");
                }

                identifier.append(m.getValue().getCleanKey()).append(": ").append(m.getValue().getString());
            }
        }

        return identifier.toString();
    }

    public String getIssuer()
    {
        return issuerKey;
    }

    public void loadAttestationData(RealmAttestation rAtt, String recoveredIssuer)
    {
        additionalMembers.putAll(getMembersFromJSON(rAtt.getSubTitle()));
        isValid = rAtt.isValid();
        patchLegacyAttestation(rAtt);

        MemberData validFromData = additionalMembers.get(VALID_FROM);
        MemberData validToData = additionalMembers.get(VALID_TO);

        validFrom = validFromData != null ? validFromData.getValue().longValue() : 0;
        validUntil = validToData != null ? validToData.getValue().longValue() : 0;

        issuerKey = recoveredIssuer;
    }

    public boolean isEAS()
    {
        //Can we reconstruct EASAttestation?
        EasAttestation easAttestation = getEasAttestation();
        return (easAttestation != null && easAttestation.getSignatureBytes().length == 65 && !TextUtils.isEmpty(easAttestation.version));
    }

    @Override
    public void addAssetElements(NFTAsset asset, Context ctx)
    {
        //add all the attestation members
        for (Map.Entry<String, MemberData> m : additionalMembers.entrySet())
        {
            if (!m.getValue().isSchemaValue() || m.getKey().contains(SCRIPT_URI) || m.getValue().isBytes())
            {
                continue;
            }

            String key = m.getKey();
            if (key.startsWith(SCHEMA_DATA_PREFIX))
            {
                key = key.substring(SCHEMA_DATA_PREFIX.length());
            }

            asset.addAttribute(key, m.getValue().getString());
        }

        //now add expiry, issuer key and valid from
        MemberData validFrom = additionalMembers.get(VALID_FROM);
        MemberData validTo = additionalMembers.get(VALID_TO);

        addDateToAttributes(asset, validFrom, R.string.valid_from, ctx);
        addDateToAttributes(asset, validTo, R.string.valid_until, ctx);
    }

    @Override
    public String getAttrValue(String typeName)
    {
        //pull out the value
        MemberData attrVal = additionalMembers.get(typeName);
        if (attrVal != null)
        {
            return attrVal.getString();
        }
        else
        {
            return "";
        }
    }

    private void addDateToAttributes(NFTAsset asset, MemberData validFrom, int resource, Context ctx)
    {
        if (validFrom != null && validFrom.getValue().compareTo(BigInteger.ZERO) > 0)
        {
            asset.addAttribute(ctx.getString(resource), validFrom.getString());
        }
    }

    private void patchLegacyAttestation(RealmAttestation rAtt)
    {
        if (additionalMembers.isEmpty())
        {
            BigInteger id = recoverId(rAtt);
            MemberData tId = new MemberData(SCHEMA_DATA_PREFIX + TICKET_ID, id.longValue());
            additionalMembers.put(SCHEMA_DATA_PREFIX + TICKET_ID, tId);
        }
    }

    // For legacy attestation support
    private BigInteger recoverId(RealmAttestation rAtt)
    {
        BigInteger id;
        try
        {
            int index = rAtt.getAttestationKey().lastIndexOf("-");
            id = new BigInteger(rAtt.getAttestationKey().substring(index + 1));
        }
        catch (Exception e)
        {
            //
            id = BigInteger.ONE; //not really important
        }

        return id;
    }

    public void populateRealmAttestation(RealmAttestation rAtt)
    {
        rAtt.setSubTitle(generateMembersJSON());
        //rAtt.setAttestation(attestation);
        rAtt.setChain(tokenInfo.chainId);
        rAtt.setName(tokenInfo.name);
    }

    private String generateMembersJSON()
    {
        JSONArray members = new JSONArray();
        for (Map.Entry<String, MemberData> t : additionalMembers.entrySet())
        {
            members.put(t.getValue().element);
        }

        return members.toString();
    }

    public String getDatabaseKey()
    {
        //pull IDs from the members
        return attestationDatabaseKey(tokenInfo.chainId, tokenInfo.address, getAttestationUID());
    }

    public void setBaseTokenType(ContractType baseType)
    {
        baseTokenType = baseType;
    }

    public ContractType getBaseTokenType()
    {
        return baseTokenType;
    }

    private void addToMemberData(String name, Type<?> type)
    {
        additionalMembers.put(name, new MemberData(name, type));
    }

    private static Map<String, MemberData> getMembersFromJSON(String jsonData)
    {
        Map<String, MemberData> members = new HashMap<>();
        try
        {
            JSONArray elements = new JSONArray(jsonData);
            int index;
            for (index = 0; index < elements.length(); index++)
            {
                JSONObject element = elements.getJSONObject(index);
                members.put(element.getString("name"), new MemberData(element));
            }
        }
        catch (Exception e)
        {
            Timber.e(e);
        }

        return members;
    }

    public EasAttestation getEasAttestation()
    {
        try
        {
            String rawAttestation = new String(attestation, StandardCharsets.UTF_8);
            String taglessAttestation = Utils.parseEASAttestation(rawAttestation);
            return new Gson().fromJson(Utils.toAttestationJson(taglessAttestation), EasAttestation.class);
        }
        catch (Exception e) //Expected
        {
            return null;
        }
    }

    public String getAttestationName(TokenDefinition td)
    {
        NFTAsset nftAsset = new NFTAsset();
        nftAsset.setupScriptElements(td);
        String name = nftAsset.getName();
        if (!TextUtils.isEmpty(name))
        {
            return name;
        }
        else
        {
            return tokenInfo.name;
        }
    }

    public boolean knownIssuerKey()
    {
        return getKnownRootIssuers(tokenInfo.chainId).contains(issuerKey);
    }

    public boolean isSmartPass()
    {
        //TODO: This should use TokenScript
        return knownIssuerKey() && orgIsSmartLayer();
    }

    private boolean orgIsSmartLayer()
    {
        return getCollectionId(EVENT_IDS).equalsIgnoreCase(SMART_LAYER);
    }

    private static class MemberData
    {
        JSONObject element;

        public MemberData(String name, Type<?> type)
        {
            try
            {
                element = new JSONObject();
                element.put("name", name);
                element.put("type", type.getTypeAsString());
                if (type.getTypeAsString().equals("bytes"))
                {
                    byte[] ddo = (byte[])type.getValue();
                    element.put("value", Numeric.toHexString((byte[])type.getValue()));
                }
                else
                {
                    element.put("value", type.getValue());
                }
            }
            catch (Exception e)
            {
                Timber.e(e);
            }
        }

        public MemberData(String name, long value)
        {
            try
            {
                element = new JSONObject();
                element.put("name", name);
                element.put("type", "uint");
                element.put("value", value);
            }
            catch (Exception e)
            {
                Timber.e(e);
            }
        }

        public MemberData(String name, String value)
        {
            try
            {
                element = new JSONObject();
                element.put("name", name);
                element.put("type", "string");
                element.put("value", value);
            }
            catch (Exception e)
            {
                Timber.e(e);
            }
        }

        public MemberData(String name, boolean value)
        {
            try
            {
                element = new JSONObject();
                element.put("name", name);
                element.put("type", "boolean");
                element.put("value", value);
            }
            catch (Exception e)
            {
                Timber.e(e);
            }
        }

        public MemberData(JSONObject jsonObject)
        {
            element = jsonObject;
        }

        public String getEncoding()
        {
            return element.toString();
        }

        public String getCleanKey()
        {
            try
            {
                String name = element.get("name").toString();
                if (name.startsWith(SCHEMA_DATA_PREFIX))
                {
                    name = name.substring(SCHEMA_DATA_PREFIX.length());
                }
                return name;
            }
            catch (JSONException e)
            {
                return "";
            }
        }

        public BigInteger getValue()
        {
            try
            {
                String type = element.getString("type");
                if (type.startsWith("uint") || type.startsWith("int") || type.startsWith("time"))
                {
                    return BigInteger.valueOf(element.getLong("value"));
                }
            }
            catch (Exception e)
            {
                Timber.e(e);
            }

            return BigInteger.ZERO;
        }

        public String getString()
        {
            try
            {
                String type = element.getString("type");
                if (type.equals("time"))
                {
                    return formatDate(Long.parseLong(element.getString("value")));
                }
                else
                {
                    return element.getString("value");
                }
            }
            catch (Exception e)
            {
                Timber.e(e);
            }

            return "";
        }

        public boolean isTrue()
        {
            try
            {
                return element.getBoolean("value");
            }
            catch (Exception e)
            {
                Timber.e(e);
            }

            return false;
        }

        public boolean isSchemaValue()
        {
            try
            {
                String name = element.getString("name");
                return name.startsWith(SCHEMA_DATA_PREFIX);
            }
            catch (Exception e)
            {
                Timber.e(e);
            }

            return false;
        }

        public boolean isURI()
        {
            try
            {
                String name = element.getString("name");
                return name.endsWith(SCRIPT_URI);
            }
            catch (Exception e)
            {
                Timber.e(e);
            }

            return false;
        }

        public MemberData setIsTime()
        {
            try
            {
                element.put("type", "time");
            }
            catch (Exception e)
            {
                //
            }
            return this;
        }

        private String formatDate(long time)
        {
            DateFormat f = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault());
            return f.format(time*1000);
        }

        public boolean isBytes()
        {
            try
            {
                String name = element.getString("type");
                return name.equals("bytes");
            }
            catch (Exception e)
            {
                Timber.e(e);
            }

            return false;
        }
    }

    @Override
    public Single<String> getScriptURI()
    {
        MemberData memberData = additionalMembers.get(SCHEMA_DATA_PREFIX + SCRIPT_URI);
        if (memberData != null && !TextUtils.isEmpty(memberData.getString()))
        {
            return Single.fromCallable(memberData::getString);
        }
        else
        {
            return contractInteract.getScriptFileURI();
        }
    }

    @Override
    public Type<?> getIntrinsicType(String name)
    {
        EasAttestation easAttestation;
        switch (name)
        {
            case "attestation":
                easAttestation = getEasAttestation();
                if (easAttestation != null)
                {
                    return easAttestation.getAttestationCore();
                }
                break;
            case "attestationSig":
                easAttestation = getEasAttestation();
                if (easAttestation != null)
                {
                    return new DynamicBytes(easAttestation.getSignatureBytes());
                }
                break;
            default:
                break;
        }

        return null;
    }

    private static String recoverPublicKey(EasAttestation attestation)
    {
        String recoveredKey = "";

        try
        {
            StructuredDataEncoder dataEncoder = new StructuredDataEncoder(attestation.getEIP712Attestation());
            byte[] hash = dataEncoder.hashStructuredData();
            byte[] r = Numeric.hexStringToByteArray(attestation.getR());
            byte[] s = Numeric.hexStringToByteArray(attestation.getS());
            byte v = (byte)(attestation.getV() & 0xFF);

            Sign.SignatureData sig = new Sign.SignatureData(v, r, s);

            BigInteger key = Sign.signedMessageHashToKey(hash, sig);
            recoveredKey = Numeric.toHexString(Numeric.toBytesPadded(key, 64));
            Timber.i("Public Key: %s", recoveredKey);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return recoveredKey;
    }

    public BigInteger getUUID()
    {
        if (isEAS())
        {
            byte[] hash = Hash.keccak256(Numeric.hexStringToByteArray(getEasAttestation().data));
            return Numeric.toBigInt(hash);
        }
        else
        {
            //TODO: Support ASN.1 Attestations
            return BigInteger.ONE;
        }
    }

    public static List<String> getKnownRootIssuers(long chainId)
    {
        List<String> knownIssuers = new ArrayList<>();
        // Add production keys - these should work on any issued attestation
        knownIssuers.add("0x715e50699db0a553119a4eb1cd13808eedc2910d"); //production key
        knownIssuers.add("0xA20efc4B9537d27acfD052003e311f762620642D".toLowerCase(Locale.ROOT)); //Testkey for resolve

        // Testnet keys should only work on testnet chains
        if (!EthereumNetworkBase.hasRealValue(chainId))
        {
            knownIssuers.add("0x4461110869a5d65df76b85e2cd8bbfdda2ca6e4d");
        }

        return knownIssuers;
    }
}
