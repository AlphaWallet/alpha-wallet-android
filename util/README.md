# TokenScript utilities

This (in-development) module provides command-line utilities for using TokenScript.

Currently, util has a demonstration to extract token data from a smart contract. It shows, for example, that TokenScript can be used as a supernode's data extraction layer. (A supernode is an Ethereum node that not only service clients enquire about blockchain data, but also enquires about token data.)

TokenScript can serve as a data abstraction layer, help a supernode to catch and index token data.

````
+--------------------+                           +--------------------+
| Blockchain data    |    +-----------------+    | Token Data         |
| · Transactions     | -> | Data Abstraction| -> | · Token IDs        |
| · Addresses        |    | (TokenScript)   |    | · Token Balances   |
| · Smart Contracts  |    +-----------------+    | · Token Attributes |
+--------------------+                           +--------------------+
````

# Run a demonstration

To see how TokenScript extracts TokenData, first, download a tokenscript

    $ curl -O http://repo.tokenscript.org/aw.app/2019/10/fifa.tsml
    
Then run `gradle run` in this module. You should see something like this:

````
$ gradle run

> Task :util:run
Holding Token Name: FIFA
Token Addresses: 
0xa66a3f08068174e8f005112a8b2c7a507a822335 ChainID: 1

Attributes:
-----------
Contract: 0xa66a3f08068174e8f005112a8b2c7a507a822335 ChainID: 1 Type: erc875
Geth/v1.9.9-omnibus-e320ae4c-20191206/linux-amd64/go1.13.4
Contract Name (Eth): FIFA WC2018(SHANKAI)

venue: "Luzhniki Stadium",
numero: "1",
locality: "Moscow",
match: "1",
time: { generalizedTime: "20180614180000+0300", date: new Date("2018-06-14T06:00:00+0300") },
category: "Match Club",
countryA: "RUS",
countryB: "KSA",
````

Which returned the token data of a token that represents a FIFA ticket. Observe that

````
countryA: "RUS",
countryB: "KSA",
````
Means that this token is for the match Russia vs Saudi Arabia.

Note that this is unlike Token Registries where all ICO projects list their tokens. The token here represents a specific ticket (it has a unique-ID which is `1`) and has to be indexed for search by a supernode's database. If someone wishes to purchase a ticket of Russia vs Saudi Arabia, this ticket can then, in turn, be retrieved.

Each token attribute may be sourced from several origins.

- TokenID: a part of tokenID akin to ERC721 is used for the value of some attributes.
- The return value of a call to a smart contract (must be a `view` call).
- An event (Ethereum blockchain event)
- A constant
- A value from an attestation (if the supernode is in possession of such an attestation)
- A trusted 3rd party source (e.g. an HTTPS API's return value)

For example, a FIFA ticket token might have the match encoded in the TokenID. However, some tkoen attributes, like `askPrice`, might be defined in an attestation signed by the seller; another attribute, like `expiry`, might originate from the return value of the smart contract's view function `getExpiry` since the event organiser can define and change the date of the event, and `lastSoldPrice` might originate from an Ethereum blockchain event.

