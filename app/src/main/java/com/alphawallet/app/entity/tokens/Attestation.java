package com.alphawallet.app.entity.tokens;

import static com.alphawallet.app.repository.TokensRealmSource.attestationDatabaseKey;
import static com.alphawallet.app.repository.TokensRealmSource.databaseKey;

import android.text.TextUtils;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.repository.entity.RealmAttestation;
import com.alphawallet.token.entity.AttestationValidation;
import com.alphawallet.token.entity.AttestationValidationStatus;
import com.alphawallet.token.entity.TokenScriptResult;
import com.alphawallet.token.tools.Numeric;

import org.web3j.abi.datatypes.Type;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

/**
 * Created by JB on 23/02/2023.
 */
public class Attestation extends Token
{
    private final byte[] attestation;
    private BigInteger attestationId;
    private String attestationSubject;
    private String issuerKey;
    private boolean issuerValid;
    private String issuerAddress;
    private long validFrom;
    private long validUntil;
    private Map<String, Type<?>> additionalMembers;
    private boolean isValid;
    private ContractType baseTokenType = ContractType.ERC721; // default to ERC721

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

        attestationId = attValidation._attestationId;
        attestationSubject = attValidation._subjectAddress;
        issuerAddress = attValidation._issuerAddress;
        isValid = attValidation._isValid;
        additionalMembers = attValidation.additionalMembers;
        issuerKey = attValidation._issuerKey;
        issuerValid = attValidation._issuerValid || (!TextUtils.isEmpty(issuerKey) && (TextUtils.isEmpty(issuerAddress) || !issuerKey.equalsIgnoreCase(issuerAddress)));
    }

    public AttestationValidationStatus isValid()
    {
        //Check has expired
        if (!isValid)
        {
            return AttestationValidationStatus.Expired;
        }

        //Check attestation is being collected by the correct wallet (TODO: if not correct wallet, and wallet is present in user's wallets offer to switch wallet)
        if (!TextUtils.isEmpty(attestationSubject) && !attestationSubject.equalsIgnoreCase(getWallet()))
        {
            return AttestationValidationStatus.Incorrect_Subject;
        }

        //Check issuer - if not valid issuer fail.
        if (!issuerValid)
        {
            return AttestationValidationStatus.Issuer_Not_Valid;
        }
//        if (!TextUtils.isEmpty(issuerKey) && (TextUtils.isEmpty(issuerAddress) || !issuerKey.equalsIgnoreCase(issuerAddress)))
//        {
//
//        }

        return AttestationValidationStatus.Pass;
    }

    public BigInteger getAttestationId()
    {
        return attestationId;
    }

    public String getIssuer()
    {
        return issuerAddress;
    }

    public void loadAttestationData(RealmAttestation rAtt)
    {
        String attestationIdStr = rAtt.getId();
        if (!TextUtils.isEmpty(attestationIdStr))
        {
            attestationId = new BigInteger(attestationIdStr);
        }
        else
        {
            attestationId = BigInteger.ZERO;
        }

        isValid = rAtt.isValid();
    }

    public String getDatabaseKey()
    {
        //This should not be required: attestation shouldn't be able to not have an ID
        BigInteger id = BigInteger.ZERO;
        if (attestationId != null && attestationId.compareTo(BigInteger.ZERO) > 0)
        {
            id = attestationId;
        }
        return attestationDatabaseKey(tokenInfo.chainId, tokenInfo.address, id);
    }

    public void setBaseTokenType(ContractType baseType)
    {
        baseTokenType = baseType;
    }

    public ContractType getBaseTokenType()
    {
        return baseTokenType;
    }
}
