package com.alphawallet.token.tools;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyName;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.alphawallet.token.entity.XMLDsigVerificationResult;

/**
 * James Sangalli mans this project since July 2019.
 * Stormbird Pte Ltd, in Sydney
 */

/**
 * This verifiers an XML Signature using the JSR 105 API. It assumes
 * the key needed to verify the signature is certified by one of the
 * X.509 certificates in the KeyInfo, and that X.509 certificate,
 * together with any other found in KeyInfo, form a chain of
 * certificate to a top level certified by one of the trusted
 * authorities of the installed JRE
 *
 * Out of scope:
 * - Multi-signature XML file
 * - Ignores any public key provided in KeyInfo
 *
 * See the test case for usage examples.
 */
public class XMLDSigVerifier {

    public XMLDsigVerificationResult VerifyXMLDSig(InputStream fileStream)
    {
        XMLDsigVerificationResult result = new XMLDsigVerificationResult();
        try
        {
            //Signature will also be validated in this call, if it fails an exception is thrown
            //No point to validate the certificate is this signature is invalid to begin with
            //And TrustAddressGenerator needs to get an XMLSignature too.
            XMLSignature signature = getValidXMLSignature(fileStream);
            result.isValid = true; //would go to catch if this was not the case
            //check that the tsml file is signed by a valid certificate
            return validateCertificateIssuer(signature, result);
        }
        catch(Exception e)
        {
            result.isValid = false;
            result.failureReason = e.getMessage();
            return result;
        }
    }

    XMLSignature getValidXMLSignature(InputStream fileStream)
            throws ParserConfigurationException,
            IOException,
            SAXException,
            MarshalException,
            XMLSignatureException,
            DOMException
    {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document xml = dBuilder.parse(fileStream);
        xml.getDocumentElement().normalize();

        // Find Signature element
        NodeList nl = xml.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        if (nl.getLength() == 0)
        {
            throw new DOMException(DOMException.INDEX_SIZE_ERR, "Missing elements");
        }

        // Create a DOM XMLSignatureFactory that will be used to unmarshal the
        // document containing the XMLSignature
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

        // Create a DOMValidateContext and specify a KeyValue KeySelector
        // and document context
        DOMValidateContext valContext = new DOMValidateContext(new SigningCertSelector(), nl.item(0));

        // unmarshal the XMLSignature
        XMLSignature signature = fac.unmarshalXMLSignature(valContext);

        boolean validSig = signature.validate(valContext);
        if(!validSig)
        {
            throw new XMLSignatureException("Invalid XML signature");
        }
        return signature;
    }

    private void validateCertificateChain(List<X509Certificate> certList)
            throws NoSuchAlgorithmException,
            KeyStoreException,
            InvalidAlgorithmParameterException,
            CertificateException,
            CertPathValidatorException
    {
        // By default on Oracle JRE, algorithm is PKIX
        TrustManagerFactory tmf = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        // 'null' will initialise the tmf with the default CA certs installed
        // with the JRE.
        tmf.init((KeyStore) null);

        X509TrustManager tm = (X509TrustManager) tmf.getTrustManagers()[0];
        CertPathValidator cpv = CertPathValidator.getInstance("PKIX");
        Set<TrustAnchor> anch = new HashSet<>();
        for (X509Certificate cert : tm.getAcceptedIssuers())
        {
            anch.add(new TrustAnchor(cert, null));
        }
        PKIXParameters params = new PKIXParameters(anch);
        Security.setProperty("ocsp.enable", "true");
        params.setRevocationEnabled(true);
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        try
        {
            cpv.validate(factory.generateCertPath(certList), params);
        }
        catch (CertPathValidatorException e)
        {
            System.out.println(e.getIndex());
            //if the timestamp check fails because the cert is expired
            //we allow this to continue (code 0)
            if(e.getIndex() != 0)
            {
                throw e;
            }
        }
    }

    private X509Certificate findRootCert(List<X509Certificate> certificates) {
        X509Certificate rootCert = null;
        for (X509Certificate cert : certificates) {
            X509Certificate signer = this.findSignerCertificate(cert, certificates);
            if (signer == null || signer.equals(cert)) {
                rootCert = cert;
                break;
            }
        }
        return rootCert;
    }

    private List<X509Certificate> reorderCertificateChain(List<X509Certificate> chain)
    {
        X509Certificate[] reorderedChain = new X509Certificate[chain.size()];
        int position = chain.size() - 1;
        X509Certificate rootCert = this.findRootCert(chain);
        reorderedChain[position] = rootCert;
        for (X509Certificate cert = rootCert;
             (cert = this.findSignedCert(cert, chain)) != null && position > 0;
             reorderedChain[position] = cert
        ) {
            --position;
        }
        return Arrays.asList(reorderedChain);
    }

    private X509Certificate findSignedCert(X509Certificate signingCert, List<X509Certificate> certificates)
    {
        X509Certificate signed = null;
        for (X509Certificate cert : certificates)
        {
            Principal signingCertSubjectDN = signingCert.getSubjectDN();
            Principal certIssuerDN = cert.getIssuerDN();
            if (certIssuerDN.equals(signingCertSubjectDN) && !cert.equals(signingCert))
            {
                signed = cert;
                break;
            }
        }
        return signed;
    }


    private X509Certificate findSignerCertificate(X509Certificate signedCert, List<X509Certificate> certificates) {
        X509Certificate signer = null;
        for (X509Certificate cert : certificates) {
            Principal certSubjectDN = cert.getSubjectDN();
            Principal issuerDN = signedCert.getIssuerDN();
            if (certSubjectDN.equals(issuerDN)) {
                signer = cert;
                break;
            }
        }
        return signer;
    }

    private XMLDsigVerificationResult validateCertificateIssuer(XMLSignature signature, XMLDsigVerificationResult result) {
        try
        {
            KeyInfo xmlKeyInfo = signature.getKeyInfo();
            List<X509Certificate> certList = getCertificateChainFromXML(xmlKeyInfo.getContent());
            List<X509Certificate> orderedCerts = reorderCertificateChain(certList);
            X509Certificate signingCert = selectSigningKeyFromXML(xmlKeyInfo.getContent());
            //Throws if invalid
            validateCertificateChain(orderedCerts);
            result.issuerPrincipal = signingCert.getIssuerX500Principal().getName();
            result.subjectPrincipal = signingCert.getSubjectX500Principal().getName();
            result.keyType = signingCert.getSigAlgName();
            for (Object o : xmlKeyInfo.getContent())
            {
                XMLStructure xmlStructure = (XMLStructure) o;
                if (xmlStructure instanceof KeyName)
                {
                    result.keyName = ((KeyName) xmlStructure).getName();
                }
            }
        }
        catch(Exception e)
        {
            result.isValid = false;
            result.failureReason = e.getMessage();
        }
        return result;
    }

    private List getCertificateChainFromXML(List xmlElements) throws KeyStoreException {
        boolean found = false;
        List certs = null;
        for (int i = 0; i < xmlElements.size(); i++)
        {
            XMLStructure xmlStructure = (XMLStructure) xmlElements.get(i);
            if (xmlStructure instanceof X509Data)
            {
                if(found) throw new KeyStoreException("Duplicate X509Data element");
                found = true;
                certs = (List<X509Certificate>) ((X509Data) xmlStructure).getContent();
            }
        }
        return certs;
    }

    private PublicKey recoverPublicKeyFromXML(List xmlElements) throws KeyStoreException {
        boolean found = false;
        PublicKey keyVal = null;
        for (int i = 0; i < xmlElements.size(); i++)
        {
            XMLStructure xmlStructure = (XMLStructure) xmlElements.get(i);
            if (xmlStructure instanceof KeyValue)
            {
                //should only be one KeyValue
                if(found) throw new KeyStoreException("Duplicate Key found");
                found = true;
                KeyValue kv = (KeyValue) xmlStructure;
                try
                {
                    keyVal = kv.getPublicKey();
                }
                catch (KeyException e)
                {
                    e.printStackTrace();
                }
            }
        }
        return keyVal;
    }

    private X509Certificate selectSigningKeyFromXML(List xmlElements) throws KeyStoreException, CertificateNotYetValidException {
        PublicKey recovered = recoverPublicKeyFromXML(xmlElements);
        //Certificates from the XML might be in the wrong order
        List<X509Certificate> certList = reorderCertificateChain(getCertificateChainFromXML(xmlElements));
        for (X509Certificate crt : certList)
        {
            try
            {
                crt.checkValidity();
            }
            catch (CertificateExpiredException e)
            {
                //allow this
                System.out.println("Allowing expired cert: " + e.getMessage());
                continue;
            }
            if (recovered != null)
            {
                PublicKey certKey = crt.getPublicKey();
                if (Arrays.equals(recovered.getEncoded(), certKey.getEncoded()))
                {
                    return crt;
                }
            }
            else if (crt.getSigAlgName().equals("SHA256withECDSA"))
            {
                return crt;
            }
        }
        //if non recovered, simply return the first certificate?
        return certList.get(0);

    }

    private class SigningCertSelector extends KeySelector
    {
        public KeySelectorResult select(
                KeyInfo keyInfo,
                KeySelector.Purpose purpose,
                AlgorithmMethod method,
                XMLCryptoContext context
        ) throws KeySelectorException
        {
            if (keyInfo == null) throw new KeySelectorException("Null KeyInfo object!");
            PublicKey signer = null;
            List list = keyInfo.getContent();
            boolean found = false;
            for (Object o : list)
            {
                XMLStructure xmlStructure = (XMLStructure) o;
                if (xmlStructure instanceof KeyValue)
                {
                    if(found) throw new KeySelectorException("Duplicate KeyValue");
                    found = true;
                    KeyValue kv = (KeyValue) xmlStructure;
                    try
                    {
                        signer = kv.getPublicKey();
                    }
                    catch (KeyException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
            if(signer != null) return new SimpleKeySelectorResult(signer);
            X509Certificate signingCert = null;
            try
            {
                signingCert = selectSigningKeyFromXML(list);
            }
            catch (Exception e)
            {
                throw new KeySelectorException(e.getMessage());
            }
            ;
            if (signingCert != null)
            {
                return new SimpleKeySelectorResult(signingCert.getPublicKey());
            }
            else
            {
                throw new KeySelectorException("No KeyValue element found!");
            }
        }
    }

    private class SimpleKeySelectorResult implements KeySelectorResult
    {
        private PublicKey pk;
        SimpleKeySelectorResult(PublicKey pk) {
            this.pk = pk;
        }
        public Key getKey() { return pk; }
    }
}
