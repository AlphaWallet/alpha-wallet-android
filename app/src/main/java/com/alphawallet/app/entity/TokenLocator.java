package com.alphawallet.app.entity;

import com.alphawallet.app.entity.tokenscript.TokenScriptFile;
import com.alphawallet.token.entity.ContractInfo;

public class TokenLocator extends ContractInfo
{
    private final TokenScriptFile tokenScriptFile;
    private final String name;

    public TokenLocator(String name, ContractInfo origins, TokenScriptFile file) {
        super(origins.contractInterface, origins.addresses);
        this.name = name;
        this.tokenScriptFile = file;
    }

    public String getFileName() {
        return tokenScriptFile.getName();
    }
    public String getFullFileName() { return tokenScriptFile.getAbsolutePath(); }

    public ContractInfo getContracts() {
        return this;
    }

    public boolean isDebug() { return tokenScriptFile.isDebug(); }

    public String getDefinitionName() { return name; }
}
