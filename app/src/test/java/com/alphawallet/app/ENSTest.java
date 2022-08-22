package com.alphawallet.app;

import static com.alphawallet.app.web3j.ens.NameHash.nameHash;
import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.RINKEBY_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.ROPSTEN_ID;
import static org.junit.Assert.assertEquals;

import com.alphawallet.app.service.AWHttpService;
import com.alphawallet.app.util.AWEnsResolver;
import com.alphawallet.app.web3j.ens.Contracts;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;
import org.web3j.protocol.ObjectMapperFactory;
import org.web3j.protocol.Web3j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

//Note that these tests may go 'stale' if ownership of the ENS domains changes or avatars change. This is not expected to happen frequently.

public class ENSTest
{
    @Test
    public void testResolveRegistryContract() {
            assertEquals(Contracts.resolveRegistryContract(MAINNET_ID), (Contracts.MAINNET));
            assertEquals(Contracts.resolveRegistryContract(ROPSTEN_ID), (Contracts.ROPSTEN));
            assertEquals(Contracts.resolveRegistryContract(RINKEBY_ID), (Contracts.RINKEBY));
    }

    private final AWHttpService web3jService;
    private final Web3j web3j;
    private final AWEnsResolver ensResolver;

    private ObjectMapper om = ObjectMapperFactory.getObjectMapper();

    private static List<String> urls = new ArrayList<>();

    private String sender = "0x226159d592E2b063810a10Ebf6dcbADA94Ed68b8";
    private static String Inf = "p8qs5p30583q5q65n40s8nn89s257964";

    private String data = "0x00112233";

    public static String LOOKUP_HEX =
            "0x556f1830000000000000000000000000c1735677a60884abbcf72295e88d47764beda28200000000000000000000000000000000000000000000000000000000000000a00000000000000000000000000000000000000000000000000000000000000160f4d4d2f800000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000028000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000004768747470733a2f2f6f6666636861696e2d7265736f6c7665722d6578616d706c652e75632e722e61707073706f742e636f6d2f7b73656e6465727d2f7b646174617d2e6a736f6e0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000e49061b92300000000000000000000000000000000000000000000000000000000000000400000000000000000000000000000000000000000000000000000000000000080000000000000000000000000000000000000000000000000000000000000001701310f6f6666636861696e6578616d706c65036574680000000000000000000000000000000000000000000000000000000000000000000000000000000000243b3b57de1c9fb8c1fe76f464ccec6d2c003169598fdfcbcb6bbddf6af9c097a39fa0048c000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000e49061b92300000000000000000000000000000000000000000000000000000000000000400000000000000000000000000000000000000000000000000000000000000080000000000000000000000000000000000000000000000000000000000000001701310f6f6666636861696e6578616d706c65036574680000000000000000000000000000000000000000000000000000000000000000000000000000000000243b3b57de1c9fb8c1fe76f464ccec6d2c003169598fdfcbcb6bbddf6af9c097a39fa0048c0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";

    public static String RESOLVED_NAME_HEX =
            "0x0000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000002000000000000000000000000041563129cdbbd0c5d3e1c86cf9563926b243834d";

    public ENSTest()
    {
        web3jService = getWeb3jService();
        web3j = getWeb3j(web3jService);
        ensResolver = new AWEnsResolver(web3j);
        urls.add("https://example-1.com/gateway/{sender}/{data}.json");
        urls.add("https://example-2.com/gateway/{sender}/{data}.json");
    }

    public static AWHttpService getWeb3jService()
    {
        OkHttpClient okClient = new OkHttpClient.Builder()
                .connectTimeout(C.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .connectTimeout(C.READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(C.LONG_WRITE_TIMEOUT, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
        return new AWHttpService("https://mainnet.infura.io/v3/" + TextUtils.rot(Inf), "https://main-rpc.linkpool.io", okClient, false);
    }

    public static Web3j getWeb3j(AWHttpService service)
    {
        return Web3j.build(service);
    }

    @Test
    public void testResolve() throws Exception {

        assertEquals(
                ensResolver.resolve("web3j.eth"), ("0x7bfd522dea355ddee2be3c01dfa4419451759310").toLowerCase());

        assertEquals(
                ensResolver.resolve("1.offchainexample.eth"), ("0x41563129cdbbd0c5d3e1c86cf9563926b243834d").toLowerCase());

        assertEquals(
                ensResolver.resolve("2.offchainexample.eth"), ("0x41563129cdbbd0c5d3e1c86cf9563926b243834d").toLowerCase());

        assertEquals(
                ensResolver.resolve("offchainexample.eth"), ("0xd8dA6BF26964aF9D7eEd9e03E53415D37aA96045").toLowerCase());
    }

    @Test
    public void testDASResolve() throws Exception {
        assertEquals(
                ensResolver.resolve("satoshi.bit"), ("0xee8738e3d3e80482526b33c91dd343caef68e41a").toLowerCase());

        assertEquals(
                ensResolver.resolve("ponzi.bit"), ("0x04e294283fb6c2974b59d15a0bc347f8d4d4bdcd").toLowerCase());
    }

    @Test
    public void testAvatarResolve() throws Exception {
        assertEquals(
                ensResolver.resolveAvatar("alphaid.eth"), ("https://drive.google.com/a/alphawallet.com/file/d/129glXWa1Y2nOZQNwvx8sA5fR1KuGAW4m/view?usp=drivesdk"));
    }

    @Test
    public void testAvatarAddressResolve() throws Exception {
        assertEquals(
                ensResolver.resolveAvatarFromAddress("0xbc8dAfeacA658Ae0857C80D8Aa6dE4D487577c63"), ("eip155:1/erc721:0x222222222291749DE47895C0c0A9B17e4fcA8268/29"));
    }

    @Test
    public void testReverseResolve() throws Exception {

        assertEquals(
                ensResolver.reverseResolve("0xd8da6bf26964af9d7eed9e03e53415d37aa96045"),
                ("vitalik.eth"));
    }

    @Test
    public void testNameHash() {
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


}
