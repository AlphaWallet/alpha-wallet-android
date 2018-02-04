package com.wallet.crypto.trustapp.interact;

import com.wallet.crypto.trustapp.repository.TokenRepositoryType;
import com.wallet.crypto.trustapp.repository.WalletRepositoryType;

/**
 * Created by James on 28/01/2018.
 */

public class TicketTransferInteract
{
    private final TokenRepositoryType tokenRepository;
    private final WalletRepositoryType walletRepository;

    public TicketTransferInteract(
            WalletRepositoryType walletRepository, TokenRepositoryType tokenRepository) {
        this.walletRepository = walletRepository;
        this.tokenRepository = tokenRepository;
    }
}
