package com.alphawallet.app.entity;


import com.alphawallet.app.entity.attestation.AttestationCoreData;
import com.alphawallet.app.service.AssetDefinitionService;
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
    public String schema;
    public String signer;
    public long time;
    public long expirationTime;
    public String refUID;
    public boolean revocable;
    public String data;
    public long nonce;

    public EasAttestation(String version, long chainId, String verifyingContract, String r, String s, long v, String signer, String uid, String schema, String recipient, long time, long expirationTime, String refUID, boolean revocable, String data, long nonce)
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
        if (schema.equals("0"))
        {
            return Numeric.toHexStringWithPrefixZeroPadded(BigInteger.ZERO, 64);
        }
        else
        {
            return schema;
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
        jsonMessage.put("time", time);
        jsonMessage.put("data", data);
        jsonMessage.put("expirationTime", expirationTime);
        jsonMessage.put("recipient", recipient);
        jsonMessage.put("refUID", getRefUID());
        jsonMessage.put("revocable", revocable);
        jsonMessage.put("schema", getSchema());

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
        /*
verifyEASAttestation((bytes32,address,uint64,uint64,bool,bytes32,bytes),bytes)
struct AttestationCoreData {
    bytes32 schema; // The UID of the associated EAS schema
    address recipient; // The recipient of the attestation.
    uint64 time; // The time when the attestation is valid from (Unix timestamp).
    uint64 expirationTime; // The time when the attestation expires (Unix timestamp).
    bool revocable; // Whether the attestation is revocable.
    bytes32 refUID; // The UID of the related attestation.
    bytes data; // The actual Schema data (eg eventId: 12345, ticketId: 6 etc)
}
         */

        /*return new AttestationCoreData(new Address(recipient), time, expirationTime, revocable,
                Numeric.toBytesPadded(new BigInteger(refUID), 32),
                Numeric.hexStringToByteArray(data), BigInteger.ZERO,
                Numeric.hexStringToByteArray(schema));*/

        BigInteger bi = new BigInteger(refUID);

        byte[] lala = Numeric.toBytesPadded(bi, 32);

        BigInteger bi2 = Numeric.toBigInt(schema);

        byte[] lala2 = Numeric.toBytesPadded(bi2, 32);

        Address l = new Address(recipient);

        byte[] bib = Numeric.hexStringToByteArray(data);

        return new AttestationCoreData(lala2,
                new Address(recipient), time, expirationTime, revocable, lala,
                bib);
    }
}
