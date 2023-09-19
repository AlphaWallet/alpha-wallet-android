package com.alphawallet.token.entity;

import org.web3j.abi.datatypes.Type;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by JB on 15/03/2023.
 */
public class AttestationValidation
{
    public final String _issuerAddress;
    public final String _issuerKey;
    public final String _subjectAddress;
    public final BigInteger _attestationId;
    public final boolean _isValid;
    public final boolean _issuerValid;
    public final Map<String, Type<?>> additionalMembers;

    public AttestationValidation(String issuerAddress, String subjectAddress, BigInteger attestationId, boolean isValid, boolean issuerValid, String issuerKey, Map<String, Type<?>> additional)
    {
        _issuerAddress = issuerAddress;
        _subjectAddress = subjectAddress;
        _attestationId = attestationId;
        _isValid = isValid;
        additionalMembers = additional;
        _issuerKey = issuerKey;
        _issuerValid = issuerValid;
    }

    // Not a traditional builder pattern, but more appropriate for this use.
    public static class Builder
    {
        private String issuerAddress;
        private String subjectAddress;
        private BigInteger attestationId;
        private boolean isValid = false; //default to false; must provide this method
        private boolean issuerValid = false;
        private String issuerKey;
        public Map<String, Type<?>> additionalMembers;

        public void issuerAddress(String issuerAddress)
        {
            this.issuerAddress = issuerAddress;
        }

        public void subjectAddress(String subjectAddress)
        {
            this.subjectAddress = subjectAddress;
        }

        public void attestationId(BigInteger attestationId)
        {
            this.attestationId = attestationId;
        }

        public void isValid(boolean isValid)
        {
            this.isValid = isValid;
        }

        public void issuerKey(String issuerKey)
        {
            this.issuerKey = issuerKey;
        }

        public void additional(String name, Type<?> value)
        {
            if (additionalMembers == null) additionalMembers = new HashMap<>();
            additionalMembers.put(name, value);
        }

        public AttestationValidation build()
        {
            return new AttestationValidation(issuerAddress, subjectAddress, attestationId, isValid, issuerValid, issuerKey, additionalMembers);
        }

        public void issuerValid(Boolean value)
        {
            issuerValid = value;
        }
    }
}
