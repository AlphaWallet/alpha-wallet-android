/*
 * Copyright 2019 Web3 Labs Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.alphawallet.app.web3j.ens;

import static com.alphawallet.ethereum.EthereumNetworkBase.GOERLI_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.RINKEBY_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.ROPSTEN_ID;

/** ENS registry contract addresses. */
public class Contracts {

    public static final String MAINNET = "0x00000000000C2E074eC69A0dFb2997BA6C7d2e1e";
    public static final String ROPSTEN = "0x00000000000C2E074eC69A0dFb2997BA6C7d2e1e";
    public static final String RINKEBY = "0x00000000000C2E074eC69A0dFb2997BA6C7d2e1e";
    public static final String GOERLI = "0x00000000000C2E074eC69A0dFb2997BA6C7d2e1e";

    public static String resolveRegistryContract(long chainId) {
        if (chainId == MAINNET_ID) {
            return MAINNET;
        } else if (chainId == ROPSTEN_ID) {
            return ROPSTEN;
        } else if (chainId == RINKEBY_ID) {
            return RINKEBY;
        } else if (chainId == GOERLI_ID) {
            return GOERLI;
        } else {
            throw new EnsResolutionException(
                    "Unable to resolve ENS registry contract for network id: " + chainId);
        }
    }
}
