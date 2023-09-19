package com.alphawallet.app.entity.attestation;

import com.alphawallet.app.entity.tokens.TokenCardMeta;

public interface AttestationImportInterface
{
    void attestationImported(TokenCardMeta newToken);
    void importError(String error);
    void smartPassValidation(SmartPassReturn validation);
}
