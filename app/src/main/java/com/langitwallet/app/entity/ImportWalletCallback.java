package com.langitwallet.app.entity;
import com.langitwallet.app.entity.cryptokeys.KeyEncodingType;
import com.langitwallet.app.service.KeyService;

public interface ImportWalletCallback
{
    void walletValidated(String address, KeyEncodingType type, KeyService.AuthenticationLevel level);
}
