
const addressHex = "%1$s";
const rpcURL = "%2$s";
const chainID = "%3$s";

function executeCallback (id, error, value) {
  AlphaWallet.executeCallback(id, error, value)
}

window.AlphaWallet.init(rpcURL, {
  getAccounts: function (cb) { cb(null, [addressHex]) },
  processTransaction: function (tx, cb){
    console.log('signing a transaction', tx)
    const { id = 8888 } = tx
    AlphaWallet.addCallback(id, cb)

    var gasLimit = tx.gasLimit || tx.gas || null;
    var gasPrice = tx.gasPrice || null;
    var data = tx.data || null;
    var nonce = tx.nonce || -1;
    alpha.signTransaction(id, tx.to || null, tx.value, nonce, gasLimit, gasPrice, data);
  },
  signMessage: function (msgParams, cb) {
    const { data } = msgParams
    const { id = 8888 } = msgParams
    console.log('signMessage', msgParams)
    AlphaWallet.addCallback(id, cb)
    alpha.signMessage(id, data);
  },
  signPersonalMessage: function (msgParams, cb) {
    console.log('spm', cb)
    const { data } = msgParams
    const { id = 8888 } = msgParams
    console.log('signPersonalMessage', msgParams)
    cb.fn = (function () {
                var counter = 0;
                return function () {counter += 1; console.log('fn: ' + counter);}
            })();
    AlphaWallet.addCallback(id, cb, false)
    alpha.signPersonalMessage(id, data);
  },
  signTypedMessage: function (msgParams, cb) {
    console.log('signTypedMessage ', msgParams)
    const { data } = msgParams
    const { id = 8888 } = msgParams
    AlphaWallet.addCallback(id, cb)
    alpha.signTypedMessage(id, JSON.stringify(data))
  }
}, {
    address: addressHex,
    networkVersion: chainID
})

window.web3.setProvider = function () {
  console.debug('Alpha Wallet - overrode web3.setProvider')
}

window.web3.version.getNetwork = function(cb) {
    cb(null, chainID)
}
window.web3.eth.getCoinbase = function(cb) {
    return cb(null, addressHex)
}
window.web3.eth.defaultAccount = addressHex
window.web3.currentProvider.isAlphaWallet = false
