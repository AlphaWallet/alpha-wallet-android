package com.alphawallet.app.entity;
import com.alphawallet.app.entity.cryptokeys.KeyEncodingType;
import com.alphawallet.app.service.KeyService;

public interface ImportWalletCallback
{
    void walletValidated(String address, KeyEncodingType type, KeyService.AuthenticationLevel level);
}
