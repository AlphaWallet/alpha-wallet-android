package com.alphawallet.attestation;

import com.alphawallet.token.entity.SignMessageType;
import com.alphawallet.token.entity.Signable;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1GeneralizedTime;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.Time;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class Attestation implements Signable {
    public static final String OID_OCTETSTRING = "1.3.6.1.4.1.1466.115.121.1.40";

    // Attestation fields
    private ASN1Integer version = new ASN1Integer(18); // = 0x10+0x02 where 0x02 means x509 v3 (v1 has version 0) and 0x10 is Attestation v 0
    private ASN1Integer serialNumber;
    private AlgorithmIdentifier signature;
    private X500Name issuer;                              // Optional
    private ASN1GeneralizedTime notValidBefore;           // Optional
    private ASN1GeneralizedTime notValidAfter;            // Optional
    private X500Name subject;  // CN=Ethereum address     // Optional
    private SubjectPublicKeyInfo subjectPublicKeyInfo;    // Optional
    private ASN1Sequence smartcontracts; // ASN1integers  // Optional
    private ASN1Sequence dataObject;
    private ASN1Sequence extensions;

    public Attestation() {

    }

    public int getVersion() {
        return version.getValue().intValueExact();
    }

    public void setVersion(int version) {
        this.version = new ASN1Integer(version);
    }

    public int getSerialNumber() {
        return serialNumber.getValue().intValueExact();
    }

    public void setSerialNumber(long serialNumber) {
        this.serialNumber = new ASN1Integer(serialNumber);
    }

    public String getSignature() {
        return this.signature.getAlgorithm().getId();
    }

    /**
     * Takes as input the oid of the signature scheme to be used to sign the attestation
     */
    public void setSignature(String oid) {
        this.signature = new AlgorithmIdentifier(new ASN1ObjectIdentifier(oid));
    }

    public String getIssuer() {
        return issuer.toString();
    }

    /**
     * Constructs a name from a conventionally formatted string, such
     * as "CN=Dave, OU=JavaSoft, O=Sun Microsystems, C=US".
     */
    public void setIssuer(String issuer) {
        this.issuer = new X500Name(issuer);
    }

    public Date getNotValidBefore() {
        try {
            return notValidBefore.getDate();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public void setNotValidBefore(Date notValidBefore) {
        this.notValidBefore = new ASN1GeneralizedTime(notValidBefore);
    }

    public Date getNotValidAfter() {
        try {
            return notValidAfter.getDate();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public void setNotValidAfter(Date notValidAfter) {
        this.notValidAfter = new ASN1GeneralizedTime(notValidAfter);
    }

    public String getSubject() {
        return subject.toString();
    }

    /**
     *
     * @param subject
     */
    public void setSubject(String subject) {
        this.subject = new X500Name(subject);
    }

    public byte[] getSubjectPublicKeyInfo() {
        try {
            return subjectPublicKeyInfo.getEncoded();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Takes a public key as input along with the OID of that key
     * @param subjectPublicKey
     */
    public void setSubjectPublicKeyInfo(String oid, byte[] subjectPublicKey) {
        this.subjectPublicKeyInfo = new SubjectPublicKeyInfo(new AlgorithmIdentifier(new ASN1ObjectIdentifier(oid)), subjectPublicKey);
    }

    public List<Long> getSmartcontracts() {
        List<Long> res = new ArrayList<>();
        Iterator<ASN1Encodable> it = smartcontracts.iterator();
        while(it.hasNext()) {
            res.add(((ASN1Integer) it.next()).getValue().longValueExact());
        }
        return res;
    }

    public void setSmartcontracts(List<Long> smartcontracts) {
        ASN1EncodableVector seq = new ASN1EncodableVector();
        for (long current : smartcontracts) {
            seq.add(new ASN1Integer(current));
        }
        this.smartcontracts = new DERSequence(seq);
    }

    public ASN1Sequence getExtensions() {
        return extensions;
    }

    public void setExtensions(ASN1Sequence extensions) {
        if (dataObject != null) {
            throw new IllegalArgumentException("DataObject already set. Only one of DataObject and Extensions is allowed.");
        }
        this.extensions = extensions;
    }

    public ASN1Sequence getDataObject() {
        return dataObject;
    }

    public void setDataObject(ASN1Sequence dataObject) {
        if (extensions != null) {
            throw new IllegalArgumentException("Extensions already set. Only one of DataObject and Extensions is allowed.");
        }
        this.dataObject = dataObject;
    }

    /**
     * Returns true if the attestation obeys X509v3, RFC 5280
     * @return
     */
    public boolean isValidX509() {
        if (version.getValue().intValueExact() != 0 && version.getValue().intValueExact() != 1 && version.getValue().intValueExact() != 2) {
            return false;
        }
        if (issuer == null || issuer.getRDNs().length == 0) {
            return false;
        }
        if (notValidBefore == null || notValidAfter == null) {
            return false;
        }
        if (subject == null) {
            return false;
        }
        if (subjectPublicKeyInfo == null) {
            return false;
        }
        if (smartcontracts != null) {
            return false;
        }
        if (dataObject != null) {
            return false;
        }
        return true;
    }

    public boolean isValid() {
        if (version == null || serialNumber == null || signature == null || (extensions == null && dataObject == null)) {
            return false;
        }
        return true;
    }

    @Override
    public String getMessage() {
        throw new RuntimeException("Not allowed");
    }

    @Override
    public long getCallbackId() {
        // TODO check that dataObject is actually an Extensions
        return 0;
    }

    /**
     * Construct the DER encoded byte array to be signed.
     * Returns null if the Attestation object is not valid
     */
    @Override
    public byte[] getPrehash() {
        if (!isValid()) {
            return null;
        }
        ASN1EncodableVector res = new ASN1EncodableVector();
        res.add(new DERTaggedObject(true, 0, this.version));
        res.add(this.serialNumber);
        res.add(this.signature);
        res.add(this.issuer == null ? new DERSequence() : this.issuer);
        if (this.notValidAfter != null && this.notValidBefore != null) {
            ASN1EncodableVector date = new ASN1EncodableVector();
            date.add(new Time(this.notValidBefore));
            date.add(new Time(this.notValidAfter));
            res.add(new DERSequence(date));
        }
        res.add(this.subject == null ? new DERSequence() : this.subject);
        res.add(this.subjectPublicKeyInfo == null ? DERNull.INSTANCE : this.subjectPublicKeyInfo);
        if (this.smartcontracts != null && this.smartcontracts.size() != 0) {
            res.add(this.smartcontracts);
        }
        if (this.extensions != null) {
            res.add(new DERTaggedObject(true, 3, this.extensions));
        }
        if (this.dataObject != null) {
            res.add(new DERTaggedObject(true, 4, this.dataObject));
        }
        try {
            return new DERSequence(res).getEncoded();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getOrigin() {
        return null;
    }

    @Override
    public CharSequence getUserMessage() {
        return null;
    }

    @Override
    public SignMessageType getMessageType()
    {
        return SignMessageType.ATTESTATION;
    }
}
