package io.stormbird.token.tools;


import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import sun.security.x509.X509CertImpl;

import javax.xml.crypto.*;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
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
    public static void check(Element element) throws Exception {

        // Find Signature element
        //NodeList nl =
        //        doc.getElementsByTagNameNS("ds", "Signature");
//        if (nl.getLength() == 0) {
//            return;
//        }

//        NodeList nl = element.getChildNodes();
//
//        // Create a DOM XMLSignatureFactory that will be used to unmarshal the
//        // document containing the XMLSignature
//        //XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
//
//        // Create a DOMValidateContext and specify a KeyValue KeySelector
//        // and document context
//        DOMValidateContext valContext = new DOMValidateContext
//                (new CertKeySelector(), nl.item(0));
//
//        // unmarshal the XMLSignature
//        XMLSignature signature = fac.unmarshalXMLSignature(valContext);
//
//        // Validate the XMLSignature (generated above)
//        boolean coreValidity = signature.validate(valContext);
//
//        // Check core validation status
//        if (coreValidity == false) {
//            System.err.println("Signature failed core validation");
//            boolean sv = signature.getSignatureValue().validate(valContext);
//            System.out.println("signature validation status: " + sv);
//            // check the validation status of each Reference
//            Iterator i = signature.getSignedInfo().getReferences().iterator();
//            for (int j=0; i.hasNext(); j++) {
//                boolean refValid =
//                        ((Reference) i.next()).validate(valContext);
//                System.out.println("ref["+j+"] validity status: " + refValid);
//            }
//        } else {
//            System.out.println("Signature passed core validation");
//        }
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
