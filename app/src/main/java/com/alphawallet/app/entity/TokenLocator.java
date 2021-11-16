package com.alphawallet.app.entity;

import com.alphawallet.app.entity.tokenscript.TokenScriptFile;
import com.alphawallet.token.entity.ContractInfo;

public class TokenLocator extends ContractInfo
{
    private final TokenScriptFile tokenScriptFile;
    private final String name;
    private final boolean error;
    private final String errorMessage;

    public TokenLocator(String name, ContractInfo origins, TokenScriptFile file) {
        super(origins.contractInterface, origins.addresses);
        this.name = name;
        this.tokenScriptFile = file;
        this.error = false;
        this.errorMessage = "";
    }

    public TokenLocator(String name, ContractInfo origins, TokenScriptFile file, boolean error, String errorMessage) {
        super(origins.contractInterface, origins.addresses);
        this.name = name;
        this.tokenScriptFile = file;
        this.error = error;
        this.errorMessage = errorMessage;
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

    public boolean isError() {
        return error;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
