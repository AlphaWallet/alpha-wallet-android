package io.stormbird.token.tools;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import io.stormbird.token.entity.XMLDsigVerificationResult;
import org.json.simple.JSONObject;

public class VerifyXMLDSig {

    //Invoke with Lambda via VerifyXMLDSig interface
    public Response VerifyTSMLFile(Request req) throws Exception {
        JSONObject result = validateSSLCertificate(req.file);
        return new Response(result);
    }

    public JSONObject validateSSLCertificate(String file) throws UnsupportedEncodingException {
        JSONObject result = new JSONObject();
        InputStream stream = new ByteArrayInputStream(file.getBytes("UTF-8"));
        XMLDsigVerificationResult XMLDsigVerificationResult = new XMLDSigVerifier().VerifyXMLDSig(stream);
        if (XMLDsigVerificationResult.isValid)
        {
            result.put("result", "pass");
            result.put("issuer", XMLDsigVerificationResult.issuerPrincipal);
            result.put("subject", XMLDsigVerificationResult.subjectPrincipal);
            result.put("keyName", XMLDsigVerificationResult.keyName);
            result.put("keyType", XMLDsigVerificationResult.keyType);
        }
        else
        {
            result.put("result", "fail");
            result.put("failureReason", XMLDsigVerificationResult.failureReason);
        }
        return result;
    }

    public static class Request {
        String file;

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }

        public Request(String file) {
            this.file = file;
        }

        public Request() {
        }
    }

    public static class Response {
        JSONObject result;

        public JSONObject getResult() { return result; }

        public void setResult(JSONObject result) { this.result = result; }

        public Response(JSONObject result) {
            this.result = result;
        }

        public Response() {
        }
    }

}


