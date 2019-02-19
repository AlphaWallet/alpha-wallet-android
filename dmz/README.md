The DMZ server is a web service for all services that don't have
confidential data (e.g. private keys).

It currently functions:

- as a landing page for Universal Link, it displays the content of an
  Universal Link, as well as showing if it is already used.

- as the XML repository server.

## Universal link ##

A universal link is like this:

    https://aw.app/AAGGoFq1tAC8mhAmpLxvC6i75IbR0J2lcys55AECAwQFBgcICfENh9BG3IRgrrkXGuLWKddxeI/PpXzaZ/RdyUxbrKi4MSEHa8NMnKTyjVw7uODNrpcboqSWZfIrHCFoug/YGegb

The part portion of the link is entirely made of a single base64 string. At the moment '/' (slash) is a part of the base64 string; in the future, we will use a 64-character set that is web-safe. The format of the data encoded in base-64 is explained in UniversalLinkTest.java. Put it simply, it closely resembles an order to the MarketQueue, with some limitations.

Every link contains a purchasable token. The link serves to let the user redeem the token.

It behaves differently depending on if the user has the app or not.

If the user did not install the app, the universal link shows a simple webpage, which displays the information about the token. In simple cases like ERC20, it merely presents three fields:

- token's name, e.g. "EOS"
- token's amount, e.g. "1"
- how much Ether is needed to redeem the token, e.g. "0.1"

In more complicated cases, like event tickets, it displays information like the location of the event, the venue, the entrance instruction, the time, as well as how many tickets, their seat number and how much Ether is needed to redeem them a whole.

In even more complicated cases, like CryptoKittens, a kitten is drawn on that webpage with a price tag.

Below these token information is a link to our app in App Store or Play Store, so that they can install the app to buy the token.

If the user has the app, the app will open the link from the app, presenting the same set of information as the user would get from the website. The app would download the asset definition XML file if necessary. The app would ask the user if she wants to purchase the token (or that the user doesn't have enough Ether to acquire these token).

In the long run, it should be possible to build Universal Links which asks for other tokens than Ether.

## Pickup-Links

Pickup-Links are just Universal Links except that it requires zero Ethers to redeem. There are 2 cases where this could happen.

1. The asset issuer issues the link to users who pay with non-crypto currency (e.g. Credit Card). Since the token is already paid, it has a price of 0 Ether.
   
2. When Alice transfers her tickets to her friend Bob for free. Maybe because she bought it for him.

In the case of pick-up links, the token issuer may want to send the transaction on behalf of the users, to spare the users of having to have Ether in the wallet at the outset.

## How do the apps handle universal link

If the mobile device has the app installed, Universal Link triggers
the app to open the link. The app would launch and does the following:

1. Recover the public key from the signature and base64 encoded
   message _order_. Check that it is not 0.

2. Take the recovered public key, generate an Ethereum address from it and look up the contract (which is in _order_) to check it still owns the tickets under the specified indices (which is also in _order_). There is a good chance that the link was previously used, so be clear on that.

3. Present the user with information of the token. E.g. draw a cat. Present the user with the price to redeem the link. If the price is zero, just show the transaction fee needed (if the user does not have Ether, provide the option to attempt to get issuer to send the transaction).

4. The wallet then calls `trade()` function on the smart contract, attaching required Ethers. The call should result in the token be allocated to user's address. Or, the wallet calls the issuer's server for it to send the transaction on behalf of the user.


### The logic in a fee master, example

First, the wallet reads the feemaster server defined in the Asset Definition File. Say, Shanhai's feemaster is 'feemaster.shankaisports.com'.

Then, the wallet replaces the Universal Link's hostname with the paymaster's hostname, and sends a POST request to that URL. The content of the POST request is the wallet's address, say,`0xde0B295669a9FD93d5F28D9Ec85E40f4cb697BAe`. This is an example HTTP POST:

        POST /AAGGoFq1tAC8mhAmpLxvC6i75IbR0J2lcys55AECAwQFBgcICfENh9BG3IRgrrkXGuLWKddxeI/PpXzaZ/RdyUxbrKi4MSEHa8NMnKTyjVw7uODNrpcboqSWZfIrHCFoug/YGegb HTTP/1.1
        Host: feemaster.shankaisports.com
        Content-Type: application/x-www-form-urlencoded
        Content-Length: 42
        address=0xde0B295669a9FD93d5F28D9Ec85E40f4cb697BAe

There, the server first does some spam detection - e.g. check if the date of the event is too much into the future. Finding it not a spam, the server then constructs a call to the smart-contract to allocate the token to 0xde0B295669a9FD93d5F28D9Ec85E40f4cb697BAe

        tradeTo("0xde0B295669a9FD93d5F28D9Ec85E40f4cb697BAe", 0, [3, 4], 28,
            "0x95B06BF20CD3F45497CC193028ADBADB140984E3E803E53D52356969BB408AD8", 
            "0x680EA2AB7CDC3E4D91B653BBC16886B143BDE426EF6978DFFC8C4CFBDCF883AF")

The server broadcasts the transaction and returns the transaction hash in response to the HTTP POST.

The possible statuses returned from an authorisation server can be one of these. In either case, the wallet prompts the user that they can add some Ether to their wallet and try again.

Spam: the asset is transferred more than 5 times
:   The authorisation server refused to do the transaction.

Authorisation Server is poor
:   The authorisation server has no Ether to finalise the transaction.

Authorisation Server is not available
:   This can be caused by server maintenance.

