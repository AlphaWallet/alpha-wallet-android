const addressHex = "%1$s";
const rpcURL = "%2$s";
const chainID = "%3$s";

window.web3CallBacks = {}

function executeCallback (id, error, value) {
    console.debug('Execute callback: ' + id + ' ' + value)
    window.web3CallBacks[id](error, value)
    delete window.web3CallBacks[id]
}

web3 = {
    personal: {
        sign: function (msgParams, cb) {
            const { data } = msgParams
            const { id = 8888 } = msgParams
            window.web3CallBacks[id] = cb
            alpha.signPersonalMessage(id, data);
        }
    }
}
