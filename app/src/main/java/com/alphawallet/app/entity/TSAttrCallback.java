package com.alphawallet.app.entity;

import com.alphawallet.token.entity.TokenScriptResult;

import java.util.List;

public interface TSAttrCallback
{
    void showTSAttributes(List<TokenScriptResult.Attribute> attrs, boolean updateRequired);
}
