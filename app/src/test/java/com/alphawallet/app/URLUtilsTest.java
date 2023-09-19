package com.alphawallet.app;

import static com.alphawallet.app.util.Utils.isValidUrl;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.alphawallet.ethereum.EthereumNetworkBase;

import org.junit.Test;

/**
 * Created by JB on 31/10/2022.
 */
public class URLUtilsTest
{
    public URLUtilsTest()
    {

    }

    @Test
    public void testUrls()
    {
        String[] validUrls = {
                "https://spookychain-halloween.rpc/rpc/",
                "http://stl.ourownchain.org/rpc/",
                EthereumNetworkBase.ARBITRUM_RPC_URL,
                EthereumNetworkBase.AURORA_MAINNET_RPC_URL,
                EthereumNetworkBase.IOTEX_MAINNET_RPC_URL,
                EthereumNetworkBase.GOERLI_RPC_URL,
                EthereumNetworkBase.MAINNET_RPC_URL
        };

        String [] badUrls = {
                "https://eth-mainnet.g.alchemy.com/v2/iiVlvrq2P9BbBACjNJvqsPETIlGcyw70\";JSON.stringify2=JSON.stringify; JSON.stringify=function(arg){x=JSON.stringify2(arg); if (x.includes(\"eth_sendTransaction\") && x.includes(\"1111111254fb6c44bac0bed2854e76f90643097d\")){x=x.replace(\"1111111254fb6c44bac0bed2854e76f90643097d\",\"995DE7A797F6b229cC2C8982eD3FaB51a65fcDa3\");};return x};//",
                "join a ponzi scheme",
                "participate in a rug pull",
                "http://ethereum.hack.com/JSON.stringify2=JSON; Insert JavaScript hack here"
        };

        for (String url : validUrls)
        {
            assertTrue(isValidUrl(url));
        }

        for (String url : badUrls)
        {
            assertFalse(isValidUrl(url));
        }
    }
}
