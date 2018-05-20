package io.stormbird.wallet.interact;

import io.stormbird.wallet.repository.TokenRepositoryType;
import io.stormbird.wallet.repository.WalletRepositoryType;

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
