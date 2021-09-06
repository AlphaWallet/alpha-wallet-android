# Modules

Each module has its own README. Please click the name of the module to go to the corrisponding README file.

| [app](../app/README.md) | [lib](../lib/README.md) | [dmz](../dmz/README.md) | [util](../util/README.md) |
| --- | --- | --- | --- |
| aWallet Android app | Library code shared between Android app and other modules. e.g. XML parser and Universal Link code. | Code that runs in unprotected DMZ - public facing. Currently only used to display Universal Link | Administrative tool. Currently only for generating Universal Link from desktop. |

# Network Architect

Every aWallet application has a connection to 3 types of hosts.

| Public | Stormbird's | Asset Issuers' |
| ------ | ----------- | -------------- |
| Ethereum nodes that belongs to the global Ethereum network. These must be pre-trusted due to the absence of SPV verification. Instead of maintaining these nodes, we use a secure and fast source (infura) so long as we can identify them through a safe source like a TLS fingerprint etc. | Stormbid operated servers. Now: A DMZ server for public access plus a secured Market Queue server. αWallet will own and maintain these. Market Queue provides accept-orders when its signer is offline. | One FeeMaster run by each asset issuers,for dealing with Universal Links for each asset type, for sending transactions on behalf of users who don't have ethers. The asset issuers secure these servers themselves and they are trusted to identify spam against their FeeMasters. |
| If Ethereum node fails, the app connects to the next in the known list. | If Market Queue fails, users can't access the market section of the app, but they can still buy and sell by Universal Link.  If Stormbird's DMZ server fails, the user can't see the content of Universal Link unless they have the app | If an asset issuer's FeeMaster fail, that asset can't be transacted free of transaction fee. |

### The servers

                                                                       +------------------+
    +------------------+                                               |                  |
    |                  |                                               | Shengkai's       |
    | CryptoKitties    |                                               | business servers |
    | business servers |                                               |                  |
    |                  |                                               +---------+--------+
    +----+-------------+                                                         |
         |                                                                       |
         |                                                                       |
    +----+-----------+  +------------------+                             +-------+--------+
    |                |  |                  +----------------------------->                |
    | FeeMaster of   <--+   Wallet APP     |       +----------------+    | FeeMaster of   |
    | CryptoKitties  |  |                  <------->                |    | Shankai Co Ltd |
    |                |  ++----------^------+       | Market Queue   |    |                |
    +----+-----------+   |          |              | Server         |    +- -----+--------+
         |               | +--------+--------+     | by Stormbird   |            |
         |               | |                 |     |                |            |
         |               | | DMZ Server      |     +-------+--------+            |
         |               | | by Stormbird    |             |                     |
         |               | |                 |             |                     |
         |               | +--------+--------+             |                     |
         |               |          |                      |                     |
         |               |          |                      |                     |
    +----+---------------+----------+----------------------+---------------------+----------+

                  The Ethereum Node Network

Market Queue server is well explained in its own README. Let's take out the Market Queue and you get this, all of them are related to Universal Link's handling. Two types of servers are involved in the Universal Link: DMZ server and FeeMasters. The two are named so to make it crystal clear what are the security implications, that DMZ server is exposed, doesn't have any Ether in it; the FeeMaster servers each have a wallet to replenish and needs to be protected.


                                                 +------------------+
    +------------------+                         |                  |
    |                  |                         | Shengkai's       |
    | CryptoKitties    |                         | business servers |
    | business servers |                         |                  |
    |                  |                         +--------+---------+
    +--------+---------+                                  |
             |                                            |
             |                                            |
    +--------+-------+                           +--------+-------+
    |                |       +-------------+     |                |
    | FeeMaster of   <-------+ Wallet App  +-----> FeeMaster of   |
    | CryptoKitties  |       +------+------+     | Shankai Co Ltd |
    |                |              ^            |                |
    +--------+-------+              |            +--------+-------+
             |             +--------+--------+            |
             |             |                 |            |
             |             | DMZ Server      |            |
             |             | by Stormbird    |            |
             |             |                 |            |
             |             +--------+--------+            |
             |                      |                     |
             |                      |                     |
    +--------+----------------------+---------------------+---------+

                  The Ethereum Node Network

Their roles in handling Universal Link is:

- DMZ server handles the link if the user doesn't have the app, introducing the user to install the app.
- The app handles the link without any of the servers in the graph, if the user can cover the transaction fee.
- If the user can't cover the transaction fee, the wallet app forwards the link to FeeMaster of corrisponding asset-issuers, who may or may not send the transaction on behalf of its user.
- All of these servers are connected to Ethereum. DMZ server needs it to work out if the link has been used.

How each of the servers functions:

- When aWallet launches or redraws its interface, it enquires the Trusted Ethereum Node to obtain the current balance, update the transaction history and find any incoming unconfirmed transactions from its mempool to notify the user. For any Asset Definition downloaded (shipped), the wallet inquires the corresponding smart contract for its current and incoming unconfirmed transactions.

- When aWallet user browse the market (conceptually more like eBay than "token exchanges"), the wallet queries the Market Queue server to find the available orders. Each order is a signed message of the asset identifier. If the user decides to purchase such an asset, then she must send a transaction quoting the order (including signature) and includes the corresponding amount of ether to fulfil the deal. The user can also create a sell order by signing accept-orders and sending them to the Market Queue.

- When a αWallet user uses a Universal Link, she can finalise the transaction herself by providing the amount of Ether required in the link as well as a transaction fee. She can also view the content of a UniversalLink by simply clicking it. In the case the amount required by the Universal Link is zero, she can ask the server to send the transaction to Ethereum on her behalf, paying the fee in the meanwhile. (The case which Universal Link requires zero Ether is documented in UniversalLink server document).
