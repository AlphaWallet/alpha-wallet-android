package com.alphawallet.app.entity.tokens;

import static com.alphawallet.app.repository.TokensRealmSource.attestationDatabaseKey;

import android.content.Context;
import android.text.TextUtils;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.EasAttestation;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.repository.entity.RealmAttestation;
import com.alphawallet.token.entity.AttestationValidation;
import com.alphawallet.token.entity.AttestationValidationStatus;
import com.alphawallet.token.entity.TokenScriptResult;
import com.alphawallet.token.tools.Numeric;

import org.json.JSONArray;
import org.json.JSONObject;
import org.web3j.abi.datatypes.Type;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

/**
 * Created by JB on 23/02/2023.
 */
public class Attestation extends Token
{
    private final byte[] attestation;
    private String attestationSubject;
    private String issuerKey;
    private boolean issuerValid;
    private String issuerAddress;
    private long validFrom;
    private long validUntil;
    private final Map<String, MemberData> additionalMembers = new HashMap<>();
    private boolean isValid;
    private ContractType baseTokenType = ContractType.ERC721; // default to ERC721
    private static final String VALID_FROM = "validFrom";
    private static final String VALID_TO = "validTo";
    private static final String TICKET_ID = "TicketId";


    //TODO: Supplemental data

    public Attestation(TokenInfo tokenInfo, String networkName, byte[] attestation)
    {
        super(tokenInfo, BigDecimal.ZERO, System.currentTimeMillis(), networkName, ContractType.ATTESTATION);
        this.attestation = attestation;
        setAttributeResult(BigInteger.ZERO, new TokenScriptResult.Attribute("attestation", "attestation", BigInteger.ZERO, Numeric.toHexString(attestation)));
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
        issuerAddress = attValidation._issuerAddress;
        isValid = attValidation._isValid;
        issuerKey = attValidation._issuerKey;
        issuerValid = attValidation._issuerValid || (!TextUtils.isEmpty(issuerKey) && (TextUtils.isEmpty(issuerAddress) || !issuerKey.equalsIgnoreCase(issuerAddress)));

        for (Map.Entry<String, Type<?>> t : attValidation.additionalMembers.entrySet())
        {
            addToMemberData(t.getKey(), t.getValue());
        }

        MemberData ticketId = new MemberData(TICKET_ID, attValidation._attestationId.longValue());
        ticketId.setIsSchema();
        additionalMembers.put(TICKET_ID, ticketId);
    }

    public void handleEASAttestation(EasAttestation attn, List<String> names, List<Type> values, boolean isAttestationValid)
    {
        //add members
        for (int index = 0; index < names.size(); index++)
        {
            String name = names.get(index);
            Type<?> type = values.get(index);
            addToMemberData(name, type);
        }

        issuerAddress = attn.signer;
        issuerValid = isAttestationValid;
        attestationSubject = attn.recipient;
        validFrom = attn.time;
        validUntil = attn.expirationTime;
        long currentTime = System.currentTimeMillis() / 1000L;
        isValid = currentTime > validFrom && (validUntil == 0 || currentTime < validUntil);
        additionalMembers.put(VALID_FROM, new MemberData(VALID_FROM, attn.time));
        additionalMembers.put(VALID_TO, new MemberData(VALID_TO, attn.expirationTime));
    }

    public AttestationValidationStatus isValid()
    {
        //Check has expired
        if (!isValid)
        {
            return AttestationValidationStatus.Expired;
        }

        //Check attestation is being collected by the correct wallet (TODO: if not correct wallet, and wallet is present in user's wallets offer to switch wallet)
        if (TextUtils.isEmpty(attestationSubject) || !attestationSubject.equalsIgnoreCase(getWallet()))
        {
            return AttestationValidationStatus.Incorrect_Subject;
        }

        //Check issuer - if not valid issuer fail.
        if (!issuerValid)
        {
            return AttestationValidationStatus.Issuer_Not_Valid;
        }

        return AttestationValidationStatus.Pass;
    }

    public String getAttestationUID()
    {
        StringBuilder identifier = new StringBuilder();
        for (Map.Entry<String, MemberData> m : additionalMembers.entrySet())
        {
            if (m.getValue().isSchemaValue())
            {
                if (identifier.length() > 0)
                {
                    identifier.append("-");
                }

                identifier.append(m.getValue().getString());
            }
        }

        return identifier.toString();
    }

    public String getAttestationDescription()
    {
        StringBuilder identifier = new StringBuilder();
        for (Map.Entry<String, MemberData> m : additionalMembers.entrySet())
        {
            if (m.getValue().isSchemaValue())
            {
                if (identifier.length() > 0)
                {
                    identifier.append(" ");
                }

                identifier.append(m.getKey()).append(": ").append(m.getValue().getString());
            }
        }

        return identifier.toString();
    }

    public String getIssuer()
    {
        return issuerAddress;
    }

    public void loadAttestationData(RealmAttestation rAtt)
    {
        populateMembersFromJSON(rAtt.getSubTitle());
        isValid = rAtt.isValid();
        patchLegacyAttestation(rAtt);

        MemberData validFromData = additionalMembers.get(VALID_FROM);
        MemberData validToData = additionalMembers.get(VALID_TO);

        validFrom = validFromData != null ? validFromData.getValue().longValue() : 0;
        validUntil = validToData != null ? validToData.getValue().longValue() : 0;
    }

    @Override
    public void addAssetElements(NFTAsset asset, Context ctx)
    {
        //add all the attestation members
        for (Map.Entry<String, MemberData> m : additionalMembers.entrySet())
        {
            if (!m.getValue().isSchemaValue())
            {
                continue;
            }

            asset.addAttribute(m.getKey(), m.getValue().getString());
        }

        //now add expiry, issuer key and valid from
        MemberData validFrom = additionalMembers.get(VALID_FROM);
        MemberData validTo = additionalMembers.get(VALID_TO);

        addDateToAttributes(asset, validFrom, R.string.valid_from, ctx);
        addDateToAttributes(asset, validTo, R.string.valid_until, ctx);
    }

    private void addDateToAttributes(NFTAsset asset, MemberData validFrom, int resource, Context ctx)
    {
        if (validFrom != null)
        {
            String dateFormat = "HH:mm dd MMM yy";
            SimpleDateFormat dateFormatter = new SimpleDateFormat(dateFormat, Locale.ENGLISH);

            long validTime = validFrom.getValue().longValue() * 1000L;
            if (validTime > 0)
            {
                String date = dateFormatter.format(validTime);
                asset.addAttribute(ctx.getString(resource), date);
            }
        }
    }

    private void patchLegacyAttestation(RealmAttestation rAtt)
    {
        if (additionalMembers.isEmpty())
        {
            BigInteger id = recoverId(rAtt);
            MemberData tId = new MemberData(TICKET_ID, id.longValue());
            tId.setIsSchema();
            additionalMembers.put(TICKET_ID, tId);
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
        rAtt.setAttestation(attestation);
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

    private void populateMembersFromJSON(String jsonData)
    {
        try
        {
            JSONArray elements = new JSONArray(jsonData);
            int index;
            for (index = 0; index < elements.length(); index++)
            {
                JSONObject element = elements.getJSONObject(index);
                additionalMembers.put(element.getString("name"), new MemberData(element));
            }
        }
        catch (Exception e)
        {
            Timber.e(e);
        }
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
                element.put("value", type.getValue());
                element.put("isSchema", true);
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

        public BigInteger getValue()
        {
            try
            {
                String type = element.getString("type");
                if (type.startsWith("uint") || type.startsWith("int"))
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
                return element.getString("value");
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

        public boolean isSchema()
        {
            try
            {
                return element.getBoolean("isSchema");
            }
            catch (Exception e)
            {
                Timber.e(e);
            }

            return false;
        }

        public void setIsSchema()
        {
            try
            {
                element.put("isSchema", true);
            }
            catch (Exception e)
            {
                Timber.e(e);
            }
        }

        public boolean isSchemaValue()
        {
            //true if this is integer or string
            try
            {
                return element.getBoolean("isSchema")
                        && (element.getString("type").startsWith("uint")
                        || element.getString("type").startsWith("int")
                        || element.getString("type").equals("string"));
            }
            catch (Exception e)
            {
                Timber.e(e);
            }

            return false;
        }
    }
}
