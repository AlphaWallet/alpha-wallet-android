
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

## The Asset Definition file ##

It came a long way for me to create asset definition in an XML format due to the complex metadata needed for a client to access a smart contract in a programmable way. The next question is: what key should be used to sign this asset definition XML?

There are 2 candidates:

1. The owner's key of the smart-contract.
2. The private key of a web certificate of the company or organisation starting the smart-contract.

There are a few advantages of using the contract owner's key:

- It doesn't force any dependencies - many smart contracts authors are reasonably anonymous; to force them to leave a trace of their identity by applying a web certificate is not our design goal.

- The admin key of a smart-contract is intended to last longer - for most contracts, being valid as long as the contract is at work. The web certificate, although can be renewed without changing the private key, is regenerated yearly, in many organisations.

- The web certificates have a designated purpose (to secure the website). It may come as a surprise to some administrator users that its use is needed to control the way the world interacts with a smart contract.

- When it comes down to trust, we trust the contract owner about their smart contract almost as much as about their definition of how apps should interact with that smart-contract. A webmaster is an external guest invited to the party from the point of view of the smart-contract owner.

- Only one signature is needed. Many organisations own more than one websites, and it is not clear which site's key should be used.

However, there are also advantages from the second option, of using the website SSL key.
True
- Most people get to know a smart-contract from a website. There should be a way to certify that "this smart contract is recommended by this website". The trust is passed from website to smart-contracts.

- Most SSL certificates are kept in a format that can be easily used for signing stuff. The smart-contract owner's key could be kept in Trezor, which has difficulty displaying a long XML file (it may be an advantage if the user took some strenuous effort to scroll down Trezor 1000 times to verify the XML file being signed is correct).

- SSL certificates can be used to sign XML file while Ethereum key might be kept by a security device which can only sign strings starting with "Ethereum Signed Message..." which breaks XML Signature standards.

I am more inclined to use a combined way: signing the XML file from the website's key and acknowledge it from the smart-contract.

First, let's define a new interface:

`contract.getDefinition()`

It either returns a full XML file or returns a hash that must match that of the XML file. In either case, no matter if the XML file is signed or not, it's considered acceptable. As long as the XML file hashes to the hash value returned by the smart-contract, we call the ADF "true".

Then, this very XML file may optionally be signed by a website SSL key. If the signature is correct, the website's certificate is verifiable and has not updated, and we call the ADF "trusted".

Then, there are cases when an XML definition is outdated, contains errors or for some reason; people are still using it after the smart-contract owner refuses to provide an update to that XML file - typically because the key is already lost. In such a case, a webmaster can provide an XML patch, which updates the True ADF, and that update has to be signed by the webmaster. If the True ADF was a signed one, and the update is signed by a key certified by a certificate of the same website, it's called "amended".

Finally, if the True ADF was not a signed one, or that a contract does not provide getDefinition(), an ADF could be signed by αWallet, in which case it is called "rectified".

The priority of selecting ADF in the event that there are conflicting versions is the following.

- Category 1. Amended (imply trusted)
- Category 2. True and Trusted.
- Category 3. True.
- Category 4. Rectified.

Let's examine this ordering method by scenarios.

### Scenario 1. The thorne builder who, if revealed, may be buried alive ###

Let's say that there is a King of Ether smart-contract: anyone can deposit money and get rewarded by the next deposit, with the condition that the next deposit has to be higher in amount.

For example, Alice deposits 100 Ethers in it. She will get rewarded when another day Bob deposits 101 Ethers in it. Alice gets exactly 101Ether in reward; Bob will be rewarded when, or if, another person comes and deposits more than 101 Ethers.

The smart contract is created by someone called James Brown. However, James didn't believe that his contract can stand the test of time. He lives in a communism dictatorship country where he is punishable in the event that his contract is hacked.

Therefore, he produced an ADF file to allow wallets to interact with his smart-contract, yet he did not sign the ADF file. The smart-contract spits out the hash of the ADF file as a way to validate the ADF file. Since there is no signature on the ADF file, the ADF file can't be "Trusted", but it still can be "True". By our ladder of priority, the only ADF file that can be accepted by the wallet is the True one, category 3.

The user who accesses this smart-contract from the wallet is given the security status message "No trust assumed".

### Scenario 2. The smart-contract admin key is lost. ###

Alice started a smart-contract and a website www.alice.com about it. She wrote an ADF file and signed it with her SSL key, together with the SSL certificate. The smart-contract is configured to spit out the hash of that very ADF file on `getDefinition()` call.

The user who accesses this smart-contract from the wallet is given the security status message "as trustworthy as www.alice.com".

For a year it worked fine - the ADF file is category 2 on the priority list. Then, like the most smart-contract authors, she lost her owner's key. She still owns the website though, and the contract is still operational.

She obtained a new SSL certificate and a new SSL key. She signed another ADF file. Although she couldn't update the smart-contract with its hash, the wallet recognises that it comes from the same website, and prioritises the ADF file over the previous one because it is a category 1 ADF file.

As time goes buy she has to move to a new smart-contract because she couldn't update the old one, and business changed since. There is no way to destroy the old contract, but she created a new one and updated a new XML file, preventing the users to interact with the old contract. The new XML file is also a category 1 ADF file, with a newer timestamp signed by the same SSL key. Therefore it replaces the old category 1 ADF file. The user was introduced to use the new contract from a prompt message defined in the new ADF file.

### Scenario 3: The smart-contract owner and the website owner isn't the same person. ###

TTM coin (short for To The Moon!) is a new ICO in town started by CEO John and his brother, CTO Joey. John hired a webmaster to build the ICO website, and Joey wrote the smart-contract. Joey also did some prototype for a new technology he called Sigmund, of which the ICO is about. Both John and Joey are led by their ICO coin buyers to believe that Sigmund is a technology with great potential, giving users the potential to be whatever they want to be.

ADF file was released as True and Trusted category 2. Having raised 10 million USD in ICO, John and Joey fought over a Youtube video on who owns the project. John believes that he got the crowd and Joey thinks he owns the contract and therefore the money. Joey decided not to give the money to John, and John, in turn, released a new ADF file blocking user's access to the smart-contract. Most simply, John can publish a really messed-up XML file to confuse the users unless Joey coughs up the money.

In this case, John's ADF file is in category 1 - Amended. Unlike Alice's case, Joey did not lose the smart-contract key. He simply updated the smart-contract to invalidate the previous True and Trusted ADF, therefore invalidating the Amended as well, moving it out of the list.

Consider that the action of buying ICO token is done to the contract, despite its reputation is assumed from the website, the fact that the contract holds users tokens and funds makes it the trusted party. Coherence between the contract and user's means to access it (ADF) is prioritized.

Furthermore, it's more sensible that the wallet should behave as intended by the contract, assuming trust from a website, not that wallet should behave as intended by a website to access a contract. Therefore, a design allowing Joey to have an upper hand wins in the lines of common sense, without judging who is the rightful actor.

### Scenario 5: The smart-contract doesn't support `getDefinition()` ###

In this case, a Rectified ADF is used which is signed by αWallet team.
