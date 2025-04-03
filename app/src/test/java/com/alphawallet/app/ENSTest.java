package com.alphawallet.app;

import static com.alphawallet.app.web3j.ens.NameHash.nameHash;
import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;
import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.alphawallet.app.service.AWHttpServiceWaterfall;
import com.alphawallet.app.util.ens.AWEnsResolver;
import com.alphawallet.app.web3j.ens.Contracts;
import com.alphawallet.app.web3j.ens.EnsResolutionException;
import com.alphawallet.shadows.ShadowApp;
import com.alphawallet.shadows.ShadowKeyProviderFactory;
import com.alphawallet.shadows.ShadowKeyService;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.web3j.protocol.Web3j;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

//Note that these tests may go 'stale' if ownership of the ENS domains changes or avatars change. This is not expected to happen frequently.
@RunWith(AndroidJUnit4.class)
@Config(shadows = {ShadowApp.class, ShadowKeyProviderFactory.class, ShadowKeyService.class})
public class ENSTest
{
    private static final String Inf = "p8qs5p30583q5q65n40s8nn89s257964";
    private AWEnsResolver ensResolver;

    public static AWHttpServiceWaterfall getWeb3jService()
    {
        OkHttpClient okClient = new OkHttpClient.Builder()
            .connectTimeout(C.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .connectTimeout(C.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(C.LONG_WRITE_TIMEOUT, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
        return new AWHttpServiceWaterfall(new String[] {"https://mainnet.infura.io/v3/" + TextUtils.rot(Inf), "https://rpc.ankr.com/eth"}, 1, okClient, null, null, null, false);
    }

    public static Web3j getWeb3j(AWHttpServiceWaterfall service)
    {
        return Web3j.build(service);
    }

    @Before
    public void setUp()
    {
        AWHttpServiceWaterfall web3jService = getWeb3jService();
        Web3j web3j = getWeb3j(web3jService);
        ensResolver = new AWEnsResolver(web3j, null);
    }

    @Test
    public void testResolveRegistryContract()
    {
        assertEquals(Contracts.resolveRegistryContract(MAINNET_ID), (Contracts.MAINNET));
    }

    @Test
    public void testResolve() throws Exception
    {

        assertEquals(
            ensResolver.resolve("web3j.eth"), ("0xd8a50a7ab452c0c9e5581bac5ff15558e6f671a1").toLowerCase());

        assertEquals(
            ensResolver.resolve("1.offchainexample.eth"), ("0x41563129cdbbd0c5d3e1c86cf9563926b243834d").toLowerCase());

        assertEquals(
            ensResolver.resolve("2.offchainexample.eth"), ("0x41563129cdbbd0c5d3e1c86cf9563926b243834d").toLowerCase());

        assertEquals(
            ensResolver.resolve("offchainexample.eth"), ("0xd8dA6BF26964aF9D7eEd9e03E53415D37aA96045").toLowerCase());

        //Now test the cache
        assertEquals(
            ensResolver.resolve("1.offchainexample.eth"), ("0x41563129cdbbd0c5d3e1c86cf9563926b243834d").toLowerCase());
        assertEquals(
            ensResolver.resolve("1.offchainexample.eth"), ("0x41563129cdbbd0c5d3e1c86cf9563926b243834d").toLowerCase());

        assertEquals(
            ensResolver.resolve("web3j.eth"), ("0xd8a50a7ab452c0c9e5581bac5ff15558e6f671a1").toLowerCase());

//        assertEquals(
//            ensResolver.resolve("vladylav.wallet"), ("0xac1de5bbdc2c8d0b3e4324c87599dc66d3221c13").toLowerCase());
    }

    //Temporarily remove - DAS seems to be acting up
    /*@Test
    public void testDASResolve() throws Exception {
        assertEquals(
                ensResolver.resolve("satoshi.bit"), ("0xee8738e3d3e80482526b33c91dd343caef68e41a").toLowerCase());

        assertEquals(
                ensResolver.resolve("ponzi.bit"), ("0x04e294283fb6c2974b59d15a0bc347f8d4d4bdcd").toLowerCase());
    }*/

    @Test
    public void testAvatarResolve() throws Exception
    {
        assertEquals(
            ensResolver.resolveAvatar("alphaid.eth"), ("https://drive.google.com/a/alphawallet.com/file/d/129glXWa1Y2nOZQNwvx8sA5fR1KuGAW4m/view?usp=drivesdk"));
    }

    @Test
    public void testAvatarAddressResolve() throws Exception
    {
        assertEquals(
            ensResolver.resolveAvatarFromAddress("0xd8dA6BF26964aF9D7eEd9e03E53415D37aA96045"), ("eip155:1/erc1155:0xb32979486938aa9694bfc898f35dbed459f44424/10063"));
    }

    @Test
    public void testReverseResolve() throws Exception
    {
        assertEquals(
            ensResolver.reverseResolveEns("0xd8da6bf26964af9d7eed9e03e53415d37aa96045").blockingGet(),
            ("vitalik.eth"));
    }

    @Test
    public void testNameHash()
    {
        assertEquals(
            nameHash(""),
            ("0x0000000000000000000000000000000000000000000000000000000000000000"));

        assertEquals(
            nameHash("eth"),
            ("0x93cdeb708b7545dc668eb9280176169d1c33cfd8ed6f04690a0bcc88a93fc4ae"));

        assertEquals(
            nameHash("foo.eth"),
            ("0xde9b09fd7c5f901e23a3f19fecc54828e9c848539801e86591bd9801b019f84f"));
    }

    @Test
    public void testNPE()
    {
        Assert.assertThrows(EnsResolutionException.class,
            this::nameHashNPE);
    }

    private void nameHashNPE() throws EnsResolutionException
    {
        String test = nameHash(null);
        System.out.println("Should have triggered exception");
    }
}
