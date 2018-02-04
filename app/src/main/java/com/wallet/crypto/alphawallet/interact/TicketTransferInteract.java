package com.wallet.crypto.alphawallet.interact;

import com.wallet.crypto.alphawallet.repository.TokenRepositoryType;
import com.wallet.crypto.alphawallet.repository.WalletRepositoryType;

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
