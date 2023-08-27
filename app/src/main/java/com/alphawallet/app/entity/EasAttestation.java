package com.alphawallet.app.entity;


import android.text.TextUtils;

import com.alphawallet.app.entity.attestation.AttestationCoreData;
import com.alphawallet.app.service.KeystoreAccountService;
import com.alphawallet.token.tools.Numeric;

import org.json.JSONArray;
import org.json.JSONObject;
import org.web3j.abi.datatypes.Address;
import org.web3j.crypto.Sign;

import java.math.BigInteger;

import timber.log.Timber;

public class EasAttestation
{
    public String version;
    public long chainId;
    public String verifyingContract;
    public String r;
    public String s;
    public long v;
    public String recipient;
    public String uid;
    private String schema;
    public String signer;
    public long time;
    public long expirationTime;
    public String refUID;
    public boolean revocable;
    public String data;
    public long nonce;
    public long messageVersion;
    private String refSchema;

    public EasAttestation(String version, long chainId, String verifyingContract, String r, String s, long v, String signer, String uid, String schema, String recipient, long time, long expirationTime, String refUID, boolean revocable, String data, long nonce, long messageVersion)
    {
        this.version = version;
        this.chainId = chainId;
        this.verifyingContract = verifyingContract;
        this.r = r;
        this.s = s;
        this.v = v;
        this.recipient = recipient;
        this.uid = uid;
        this.schema = schema;
        this.signer = signer;
        this.time = time;
        this.expirationTime = expirationTime;
        this.refUID = refUID;
        this.revocable = revocable;
        this.data = data;
        this.nonce = nonce;
        this.refSchema = null;
        this.messageVersion = messageVersion;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public long getChainId()
    {
        return chainId;
    }

    public void setChainId(long chainId)
    {
        this.chainId = chainId;
    }

    public String getVerifyingContract()
    {
        return verifyingContract;
    }

    public void setVerifyingContract(String verifyingContract)
    {
        this.verifyingContract = verifyingContract;
    }

    public String getR()
    {
        return r;
    }

    public void setR(String r)
    {
        this.r = r;
    }

    public String getS()
    {
        return s;
    }

    public void setS(String s)
    {
        this.s = s;
    }

    public long getV()
    {
        if (v == 0 || v == 1)
        {
            v += 27;
        }
        return v;
    }

    public void setV(long v)
    {
        this.v = v;
    }

    public String getRecipient()
    {
        return recipient;
    }

    public void setRecipient(String recipient)
    {
        this.recipient = recipient;
    }

    public String getUid()
    {
        return uid;
    }

    public void setUid(String uid)
    {
        this.uid = uid;
    }

    public String getSchema()
    {
        BigInteger schemaVal = new BigInteger(Numeric.cleanHexPrefix(schema), 16);
        if (schemaVal.equals(BigInteger.ZERO))
        {
            return getRefSchema();
        }
        else
        {
            return schema;
        }
    }

    private String getFixedSchema()
    {
        BigInteger schemaVal = new BigInteger(Numeric.cleanHexPrefix(schema), 16);
        if (schemaVal.equals(BigInteger.ZERO))
        {
            return Numeric.toHexStringWithPrefixZeroPadded(BigInteger.ZERO, 64);
        }
        else
        {
            return schema;
        }
    }

    private String getRefSchema()
    {
        if (TextUtils.isEmpty(refSchema))
        {
            return Numeric.toHexStringWithPrefixZeroPadded(BigInteger.ZERO, 64);
        }
        else
        {
            return refSchema;
        }
    }

    public void setSchema(String schema)
    {
        this.schema = schema;
    }

    public String getSigner()
    {
        return signer;
    }

    public void setSigner(String signer)
    {
        this.signer = signer;
    }

    public long getTime()
    {
        return time;
    }

    public void setTime(long time)
    {
        this.time = time;
    }

    public long getExpirationTime()
    {
        return expirationTime;
    }

    public void setExpirationTime(long expirationTime)
    {
        this.expirationTime = expirationTime;
    }

    public String getRefUID()
    {
        if (refUID.equals("0"))
        {
            return Numeric.toHexStringWithPrefixZeroPadded(BigInteger.ZERO, 64);
        }
        else
        {
            return refUID;
        }
    }

    public void setRefUID(String refUID)
    {
        this.refUID = refUID;
    }

    public boolean isRevocable()
    {
        return revocable;
    }

    public void setRevocable(boolean revocable)
    {
        this.revocable = revocable;
    }

    public String getData()
    {
        return data;
    }

    public void setData(String data)
    {
        this.data = data;
    }

    public long getNonce()
    {
        return nonce;
    }

    public void setNonce(long nonce)
    {
        this.nonce = nonce;
    }

    public byte[] getSignatureBytes()
    {
        byte[] r = Numeric.hexStringToByteArray(getR());
        byte[] s = Numeric.hexStringToByteArray(getS());
        byte v = (byte)(getV() & 0xFF);

        Sign.SignatureData sig = new Sign.SignatureData(v, r, s);

        return KeystoreAccountService.bytesFromSignature(sig);
    }

    public String getEIP712Attestation()
    {
        JSONObject eip712 = new JSONObject();

        try
        {
            JSONObject types = new JSONObject();
            JSONArray jsonType = new JSONArray();
            putElement(jsonType, "name", "string");
            putElement(jsonType, "version", "string");
            putElement(jsonType, "chainId", "uint256");
            putElement(jsonType, "verifyingContract", "address");
            types.put("EIP712Domain", jsonType);

            JSONArray attest = new JSONArray();
            if (messageVersion > 0)
            {
                putElement(attest, "version", "uint16");
            }
            putElement(attest, "schema", "bytes32");
            putElement(attest, "recipient", "address");
            putElement(attest, "time", "uint64");
            putElement(attest, "expirationTime", "uint64");
            putElement(attest, "revocable", "bool");
            putElement(attest, "refUID", "bytes32");
            putElement(attest, "data", "bytes");

            types.put("Attest", attest);

            eip712.put("types", types);

            JSONObject jsonDomain = new JSONObject();
            jsonDomain.put("name", "EAS Attestation");
            jsonDomain.put("version", version);
            jsonDomain.put("chainId", chainId);
            jsonDomain.put("verifyingContract", verifyingContract);

            //"primaryType": "Attest",
            eip712.put("primaryType", "Attest");
            eip712.put("domain", jsonDomain);

            JSONObject jsonMessage = formMessage();

            eip712.put("message", jsonMessage);
        }
        catch (Exception e)
        {
            Timber.e(e);
        }

        return eip712.toString();
    }

    public String getEIP712Message()
    {
        String message;
        try
        {
            JSONObject jsonMessage = formMessage();
            message = jsonMessage.toString();
        }
        catch (Exception e)
        {
            message = "";
            Timber.e(e);
        }

        return message;
    }

    private JSONObject formMessage() throws Exception
    {
        JSONObject jsonMessage = new JSONObject();
        if (messageVersion > 0)
        {
            jsonMessage.put("version", messageVersion);
        }
        jsonMessage.put("time", time);
        jsonMessage.put("data", data);
        jsonMessage.put("expirationTime", expirationTime);
        jsonMessage.put("recipient", recipient);
        jsonMessage.put("refUID", getRefUID());
        jsonMessage.put("revocable", revocable);
        jsonMessage.put("schema", getFixedSchema());

        return jsonMessage;
    }

    private void putElement(JSONArray jsonType, String name, String type) throws Exception
    {
        JSONObject element = new JSONObject();
        element.put("name", name);
        element.put("type", type);

        jsonType.put(element);
    }

    public AttestationCoreData getAttestationCore()
    {
        BigInteger refVal = new BigInteger(refUID);
        byte[] refBytes = Numeric.toBytesPadded(refVal, 32);
        BigInteger schemaVal = Numeric.toBigInt(schema);
        byte[] schemaBytes = Numeric.toBytesPadded(schemaVal, 32);
        byte[] dataBytes = Numeric.hexStringToByteArray(data);

        return new AttestationCoreData(schemaBytes,
                new Address(recipient), time, expirationTime, revocable, refBytes,
                dataBytes);
    }
}
