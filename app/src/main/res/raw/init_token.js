const _currentTokenInstance = {
%1$s
}

const walletAddress = '%2$s'
const addressHex = "%2$s";
const rpcURL = "%3$s";
const chainID = "%4$s";

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
    },
    tokens: {
        data: {
            currentInstance: {},
        },
        dataChanged: (tokens, test) => {
            console.log('web3.tokens.data changed.')
        }
    },
    action: {
        setProps: function (msgParams) {
            alpha.setValues(JSON.stringify(msgParams));
        }
    }
}

web3.tokens.data.currentInstance = _currentTokenInstance

function refresh() {
   web3.tokens.dataChanged('test', web3.tokens.data, '%5$s') //TODO: Cache previous value of token to feed into first arg
}

window.onload = refresh;