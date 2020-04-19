package com.alphawallet.app.viewmodel;

import android.util.SparseArray;

import com.alphawallet.app.entity.tokenscript.TokenScriptFile;
import com.alphawallet.app.service.AssetDefinitionService;

import java.util.Map;

public class TokenScriptManagementViewModel extends BaseViewModel {

    private final AssetDefinitionService assetDefinitionService;

    public TokenScriptManagementViewModel(AssetDefinitionService assetDefinitionService) {
        this.assetDefinitionService = assetDefinitionService;
    }

    public Map<String, TokenScriptFile> getFileList() {

        Map<String, TokenScriptFile> tokenFiles = null;
        SparseArray<Map<String, TokenScriptFile>> fileList = assetDefinitionService.getAssetDefinitions();
        if (fileList != null && fileList.size() > 0) {
            tokenFiles = fileList.valueAt(0);
            for (int i = 1; i < fileList.size(); i++) {
                Map<String, TokenScriptFile> tokens = fileList.valueAt(i);
                boolean isValid = true;
                for (TokenScriptFile file : tokens.values()) {
                    if (!file.isValidTokenScript()) {
                        isValid = false;
                        break;
                    }
                }
                if (isValid) {
                    tokenFiles.putAll(tokens);
                }
            }
        }
        return tokenFiles;
    }
}
