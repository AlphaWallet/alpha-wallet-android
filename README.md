# AlphaWallet - Ultimate Ethereum Wallet for Android

[![Build Status](https://travis-ci.com/James-Sangalli/alpha-wallet.svg?token=J2hT1s5bGKT1npuPugWb&branch=master)](https://travis-ci.com/James-Sangalli/alpha-wallet.svg?token=J2hT1s5bGKT1npuPugWb&branch=master)
[![License](https://img.shields.io/badge/license-GPL3-green.svg?style=flat)](https://github.com/fastlane/fastlane/blob/master/LICENSE)

[<img src="https://raw.githubusercontent.com/TrustWallet/trust-wallet-android/master/resources/android_cover.png">](https://play.google.com/store/apps/details?id=com.wallet.crypto.trustapp)

[<img src=https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png height="88">](https://play.google.com/store/apps/details?id=com.wallet.crypto.trustapp)

Welcome to Trust's open source Android app!

## Getting Started

1. Install latest Android Studio (>=3.0.0).
2. Clone this repository.
3. Register with etherscan.io and populate the API keys in Controller.java
4. Build and run.

Try the [app](https://play.google.com/store/apps/details?id=com.wallet.crypto.trustapp) on Google Play Store.

## Deploying with fastlane

`fastlane screengrab` - take screenshots
`fastlane listing` - update play store listing

## Cloning the repo notes

Since submodules are in use you will need to use this command sequence to clone:

git clone --recurse-submodules https://github.com/James-Sangalli/alpha-wallet

If you are updating a branch use this:

git checkout <branch you are switching to>
git submodule update --init --recursive

## Network Architect

## Architecture

Every aWallet application has a connection to 3 types of hosts, each of which are run by different companies. 

1. One or more trusted Ethereum nodes; these must be pre-trusted due to the absence of SPV verification. Instead of maintaining these nodes, we use a secure and fast source (infura) so long as we can identify them through a safe source like a TLS fingerprint etc.

2. A Market Queue server. αWallet will own and maintain this server. It provides accept-orders when its signer is offline. This one is a single point of failure - if it doesn't work, user's can't purcahse from the market that comes with the wallet (they can still redeem orders posted on Internet forums through Universal Link).

3. One server for dealing with Universal Links for each asset type. We called by different names: sometimes "UniversalLink Server", somethings "Payment Server", sometimes "feeMaster", for paying transaction fees for customers whom it can identify whether it is a spam source.

There is a future data source: The repository of "Asset Definitions" for each asset type. This is a read-only source of trustless data. Currently, we ship one such file at the beginning. In the future, they may be downloadable or fetchable Ethereum blockchain. It contains, among other things, the smart-contract addresses. 

How each of the servers function: 

- When aWallet launches or redraws its interface, it enquires the Trusted Ethereum Node to obtain the current balance, update the transaction history and find any incoming unconfirmed transactions from its mempool to notify the user. For any Asset Definition downloaded (shipped), the wallet inquires the corresponding smart contract for its current and incoming unconfirmed transactions.

- When aWallet user browse the market (conceptually more like eBay than "token exchanges"), the wallet queries the Market Queue server to find the available orders. Each order is a signed message of the asset identifier. If the user decides to purchase such an asset, then she must send a transaction quoting the order (including signature) and includes the corresponding amount of ether to fulfil the deal. The user can also create a sell order by signing accept-orders and sending them to the Market Queue.

- When a αWallet user uses a Universal Link, she can finalise the transaction herself by providing the amount of Ether required in the link as well as a transaction fee. She can also view the content of a UniversalLink by simply clicking it. In the case the amount required by the Universal Link is zero, she can ask the server to send the transaction to Ethereum on her behalf, paying the fee in the meanwhile. (The case which Universal Link requires zero Ether is documented in UniversalLink server document).

As different parties administrate the three servers, they will probably use different technologies.

- The Ethereum node uses RPC so it doesn't matter which technology it uses as long as the mempool can be enquired for unconfirmed transactions.

- The Market Queue server is in Python, running on AWS.

- The Universal Link Server belongs to the asset issuer since they have the best knowledge if they wish to send transactions to their users by paying the fee. We provide an example where no spam-detection is applied and hope the asset issuer's programmer add spam-protection by utilising their business knowledge.