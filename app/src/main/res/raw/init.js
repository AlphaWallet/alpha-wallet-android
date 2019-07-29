const addressHex = "%1$s";
const rpcURL = "%2$s";
const chainID = "%3$s";

(function() {
 var config = {
     address: addressHex.toLowerCase(),
     chainId: chainID,
     rpcUrl: rpcUrl
 };
 const provider = new window.AlphaWallet(config);
 window.ethereum = provider;
 window.web3 = new window.Web3(provider);
 window.web3.eth.defaultAccount = config.address;
 window.chrome = {webstore: {}};
})();