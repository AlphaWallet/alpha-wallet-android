package com.alphawallet.app.entity;

public class TokenLocator extends ContractLocator {
    private String fileName;
    private String address;

    public TokenLocator(String name, int chainId, ContractType type, String fileName,String address) {
        super(name, chainId, type);
        this.fileName = fileName;
        this.address = address;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getAddress() {
        return address;
    }
}
