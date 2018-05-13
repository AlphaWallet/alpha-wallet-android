
### Authorisation server use cases

In the March release, there will be 3 cases when the authorisation server is needed.

1. When a user purchases an asset from the issuer's website, using his credit card, the website generates a Universal Link. The authorisation server generates the link because it has the issuer's key. The link is sent to the user by email through the issuer's own email service.

2. When the user received the Universal Link, he can "import" the asset to his wallet. If he has Ethers, the import is done by sending a transaction. If he hasn't Ether, the import is done by sending an HTTP POST request to the authorisation server, which generates such a transaction.

3. When the user wants to transfer the ticket to someone, if he knows the other person' Ethereum address, the transfer is done through a normal `transfer()` call. If he doesn't know it, he can generate a Universal Link himself, signed by himself, and send to that someone by email or SMS. If the beneficiary has Ether, he can move the asset himself; otherwise, he will send an HTTP POST request, where the authorisation server, finding it not spam, transfer the asset for him.

### Do we separate the role of authorisation server to 2 further roles: pay-master and key-master?

It is easy to see that in the 3 cases when authorisation server is used, only the first case requires the use of authorisation key. The second and third case can all be done by a service that has some pocket money, but not the issuer's key. In fact, it is intended, so that there can be no issuer's key behind the web APIs. In the future, we can divide the roles. The first case can be done by a keymaster and the second and third case by a paymaster.

The advantages of this separation are:

1. The issuer's key is not behind the web API; the attacker who went through web API only gets some pocket money;
2. Paymaster can be outsourced to make the asset issuer's side simple;
3. the keymaster is a target of hacking and the paymaster is a target of spamming. Separating them helps define attack models and optimise security.

Do we do the separation now? It's attempting but there is a reason not to do so:

To fight spams, paymaster needs to access business logic to determine spam attacks. For a small asset issuer, it may be easier to integrate it into their system if all "blockchain" stuff is just one component.

### The security issue with Universal Link

The apparent security issue is that Universal link is an attestation upon an unknown public key. Whoever receives the attestation can "import" the asset easily. The correct cryptographic solution, by asking for a public key first, isn't applicable here, for, among the 3 cases listed, in the first case it leads to shopping cart abandonment and in the last case leads to extra user communication (suggesting user install a wallet and asking the user to submit a public key).

Two methods, both by James Brown, are these:

1. The "import" requires the beneficiary to validate phone number by SMS verification code.
2. The "import" requires the beneficiary to decrypt a part of the universal link (say, `r` or `s`). The password defaults to beneficiary's phone number.

With the first method, I expect the phone number to be in the Universal Link, signed together with other data, and the paymaster is responsible for validating the number. With the second method, the change to the scheme is little.

## The smart-contract itself ##

2 keys are used in the smart-contract:

- executioner (suicide key), which is the constructor's sender, held by either issuer, or αWallet team (if the issuer has too weak blockchain capacity).
- issuer key. always held by issuer.

