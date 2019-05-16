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

// class TokenScriptDef {
//         constructor(addr, tokenDef) {
//           this.address = addr;
//           this.token = tokenDef;
//         }
//       }
//
// var web3 = new TokenScriptDef(walletAddress, _currentTokenInstance)

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
    }
}

web3.tokens.data.currentInstance = _currentTokenInstance

function refresh() {
   web3.tokens.dataChanged('test', 'test')
}

window.onload = refresh;