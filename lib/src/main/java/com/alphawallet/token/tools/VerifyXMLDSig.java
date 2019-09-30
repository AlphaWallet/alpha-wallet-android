package com.alphawallet.token.tools;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import com.alphawallet.token.entity.XMLDsigVerificationResult;
import com.github.cliftonlabs.json_simple.JsonObject;

public class VerifyXMLDSig {

    //Invoke with Lambda via VerifyXMLDSig interface
    public Response VerifyTSMLFile(Request req) throws Exception {
        JsonObject result = validateSSLCertificate(req.file);
        return new Response(result);
    }

    public JsonObject validateSSLCertificate(String file) throws UnsupportedEncodingException {
        JsonObject result = new JsonObject();
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
        JsonObject result;

        public JsonObject getResult() { return result; }

        public void setResult(JsonObject result) { this.result = result; }

        public Response(JsonObject result) {
            this.result = result;
        }

        public Response() {
        }
    }

}


