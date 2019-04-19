package io.stormbird.token.tools;


import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import sun.security.x509.KeyIdentifier;
import sun.security.x509.X509CertImpl;

import javax.security.auth.x500.X500Principal;
import javax.xml.crypto.*;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyName;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import java.security.Key;
import java.security.PublicKey;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.util.Iterator;
import java.util.List;



/**
 * This is a simple example of validating an XML
 * Signature using the JSR 105 API. It assumes the key needed to
 * validate the signature is contained in a KeyValue KeyInfo.
 */
public class TSValidator {

    //
    // Synopsis: java Validate [document]
    //
    //    where "document" is the name of a file containing the XML document
    //    to be validated.
    //
    public static void check(Document doc) throws Exception {

        // Find Signature element
        NodeList nl =
                doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        if (nl.getLength() == 0) {
            //throw new Exception("Cannot find Signature element");
            return;
        }

        // Create a DOM XMLSignatureFactory that will be used to unmarshal the
        // document containing the XMLSignature
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

        // Create a DOMValidateContext and specify a KeyValue KeySelector
        // and document context
        DOMValidateContext valContext = new DOMValidateContext
                (new CertKeySelector(), nl.item(0));
//                (new CertKeySelector(), sig);

        // unmarshal the XMLSignature
        XMLSignature signature = fac.unmarshalXMLSignature(valContext);

        SignedInfo info = signature.getSignedInfo();
        System.out.println(info.getId());
        KeyInfo kInfo = signature.getKeyInfo();
        System.out.println(kInfo.getId());

        String keyName = null;

        // Validate the XMLSignature (generated above)
        boolean coreValidity = signature.validate(valContext);

        if (coreValidity)
        {
            for (Object o : kInfo.getContent())
            {
                XMLStructure xmlStructure = (XMLStructure) o;
                if (xmlStructure instanceof KeyName)
                {
                    keyName = ((KeyName) xmlStructure).getName();
                }
                if (xmlStructure instanceof X509Data)
                {
                    List<X509CertImpl> certList = ((X509Data) xmlStructure).getContent();
                    X509CertImpl cert = certList.get(certList.size() - 1);
                    X500Principal yoInfo = cert.getIssuerX500Principal();
                    String x500Name = yoInfo.getName();
                    X500Principal yoSub = cert.getSubjectX500Principal();
                    String subject = yoSub.getName();
                    System.out.println(x500Name);;
                    System.out.println(subject);
                }
            }
        }

        if (keyName != null)
        {
            System.out.println(keyName);
        }



        // Check core validation status
        if (coreValidity == false) {
            System.err.println("Signature failed core validation");
            boolean sv = signature.getSignatureValue().validate(valContext);
            System.out.println("signature validation status: " + sv);
            // check the validation status of each Reference
            Iterator i = signature.getSignedInfo().getReferences().iterator();
            for (int j=0; i.hasNext(); j++) {
                boolean refValid =
                        ((Reference) i.next()).validate(valContext);
                System.out.println("ref["+j+"] validity status: " + refValid);
            }
        } else {
            System.out.println("Signature passed core validation");
        }
    }

    private static class CertKeySelector extends KeySelector {
        public KeySelectorResult select(KeyInfo keyInfo,
                                        KeySelector.Purpose purpose,
                                        AlgorithmMethod method,
                                        XMLCryptoContext context)
                throws KeySelectorException {
            if (keyInfo == null) {
                throw new KeySelectorException("Null KeyInfo object!");
            }
            SignatureMethod sm = (SignatureMethod) method;
            List list = keyInfo.getContent();

            for (int i = 0; i < list.size(); i++) {
                XMLStructure xmlStructure = (XMLStructure) list.get(i);
                X509CertImpl cert;
                if (xmlStructure instanceof X509Data) {
                    List<X509CertImpl> certList = ((X509Data)xmlStructure).getContent();
                    cert = certList.get(certList.size()-1);
                    try {
                        cert.checkValidity();
                    } catch (CertificateExpiredException e) {
                        e.printStackTrace();
                    } catch (CertificateNotYetValidException e) {
                        e.printStackTrace();
                    }
                    return new SimpleKeySelectorResult(cert.getPublicKey());
                }
            }
            throw new KeySelectorException("No KeyValue element found!");
        }
    }

    private static class SimpleKeySelectorResult implements KeySelectorResult {
        private PublicKey pk;
        SimpleKeySelectorResult(PublicKey pk) {
            this.pk = pk;
        }

        public Key getKey() { return pk; }
    }
}
