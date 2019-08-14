package io.stormbird.token.tools;

public class TokenScriptTrustAddressRequest {
    String contractAddress;
    String tokenScript;

    public String getContractAddress() {
        return contractAddress;
    }

    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }

    public String getTokenScript() {
        return tokenScript;
    }

    public void setTokenScript(String tokenScript) {
        this.tokenScript = tokenScript;
    }

    public TokenScriptTrustAddressRequest(String contractAddress, String tokenScript) {
        this.contractAddress = contractAddress;
        this.tokenScript = tokenScript;
    }

    public TokenScriptTrustAddressRequest() {
    }
}
