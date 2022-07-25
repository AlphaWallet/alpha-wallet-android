package com.alphawallet.app.entity;

import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenFactory;
import com.alphawallet.app.entity.tokens.TokenInfo;

import org.junit.Test;

import java.math.BigDecimal;

public class EventSyncTest
{
    private EventSync eventSync;
    private Token token;

    @Test
    public void getUSDCTransferEvents()
    {

        TokenInfo tInfo = new TokenInfo("0x495f947276749Ce646f68AC8c248420045cb7b5e", "OpenSea Shared Storefront", "OPENSTORE", 0, true, 1);
        //createToken(TokenInfo tokenInfo, BigDecimal balance, List<BigInteger> balances, long updateBlancaTime, ContractType type, String networkName, long lastBlockCheck)
        Token token = TokenFactory.createToken(tInfo, BigDecimal.ZERO, null ,0, ContractType.ERC721, "Ethereum", 0);
        token.setTokenWallet("0"); // Fetch all events

        eventSync = new EventSync(token);

        //TODO: Issues:
        // - TransactionsService can't fetch latest (mock?)
        // - Web3j needs to be created separately 
        token.updateBalance(null);
    }

    /*private void fetchEvents()
    {
        //first get current block
        SyncDef sync = getSyncDef();

        DefaultBlockParameter startBlock = DefaultBlockParameter.valueOf(sync.eventReadStartBlock);
        DefaultBlockParameter endBlock = DefaultBlockParameter.valueOf(sync.eventReadEndBlock);
        if (sync.eventReadEndBlock.compareTo(BigInteger.valueOf(-1L)) == 0) endBlock = DefaultBlockParameterName.LATEST;

        //take a note of the current block#
        BigInteger currentBlock = TransactionsService.getCurrentBlock(token.tokenInfo.chainId);

        try
        {
            final Web3j web3j = TokenRepository.getWeb3jService(token.tokenInfo.chainId);
            Pair<Integer, Pair<HashSet<BigInteger>, HashSet<BigInteger>>> evRead = eventSync.processTransferEvents(web3j,
                    getTransferEvents(), startBlock, endBlock, null);

            updateEventReads(sync, currentBlock, evRead.first); //means our event read was fine

            HashSet<BigInteger> allMovingTokens = new HashSet<>(evRead.second.first);
            allMovingTokens.addAll(evRead.second.second);

            HashSet<BigInteger> tokenIdsHeld = checkBalances(web3j, allMovingTokens);
        }
        catch (LogOverflowException e)
        {
            //handle log read overflow; reduce search size
            if (eventSync.handleEthLogError(e.error, startBlock, endBlock, sync, realm))
            {
                //recurse until we find a good value
                fetchEvents();
            }
        }
        catch (Exception e)
        {
            Timber.w(e);
        }
    }*/
}
