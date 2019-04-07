const currentTokenInstance = {
%1$s
}

const walletAddress = '%2$s'

class TokenScriptDef {
        constructor(addr, tokenDef) {
          this.address = addr;
          this.token = tokenDef;
        }
      }

var web3 = new TokenScriptDef(walletAddress, currentTokenInstance)

web3.tokens = {
   data: {
       currentInstance: {
       },
   },
   dataChanged: (tokens) => {
       console.log('web3.tokens.data changed.')
    }
}

web3.tokens.data.currentInstance = currentTokenInstance

function refresh() {
   web3.tokens.dataChanged('test', 'test')
}

window.onload = refresh;