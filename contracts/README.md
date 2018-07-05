# Client specification #

When the user accesses a contract, the client downloads or updates the XML files.

“access” happens when:
- the app discovers that the user has (or had) tokens under a Contract;
- the user actively seek to use a Contract (scan its QR code or copying its address from the Internet);
- the user gets sent Universal Link which refers to a Contract or its assets
- the app launches

The XML file is downloaded or updated by accessing a link like this:
https://repo.awallet.io/0xA66A3F08068174e8F005112A8b2c7A507a822335

## For downloading the first time ##

The XML file is downloaded to the mobile phone, validated for signature (in the future, also validated against schemas). If invalid - delete it immediately, otherwise saved to the mobile's file storage by adding .xml extension.

    0xA66A3F08068174e8F005112A8b2c7A507a822335.xml

## For checking updates ##

Include the `IF-Modified-Since` header HTTP header with the local XML's last modified date in the GET request. If the XML on the server has not been modified, a `304` will be returned with an empty body. Be careful with timezone! Put a bit of thinking cases like this: the user moves from timezone A to timezone B and missing a latest update for up to 24 hours.

Example for the client to enquire repo.awallet.io for the latest update.

    curl "Accept: text/xml; charset=UTF-8" -H "IF-Modified-Since: Mon, 02 Jul 2018 13:23:00 GMT" -H "X-Client-Name: AlphaWallet" -H "X-Client-Version: 1.0.3" -H "X-Platform-Name: iOS" -H "X-Platform-Version: 11.1.2".   https://repo.awallet.io:8080/0xd8e5f58de3933e1e35f9c65eb72cb188674624f3

If an update is needed, the new file will be available in the body. Validate for signature (in the future, validate against schemas). If invalid, keep the old file and log the event (or secrectly send us an email). If valid, replace the local file with it and set the modified timestamp again:

    0xA66A3F08068174e8F005112A8b2c7A507a822335.xml

In the future, the server might return HTTP 300 and 204, for situations that I will document later. Such case will happen once the XML files / Schemas are versioned.

Note that it is intended that an XML document's author tests his work by directly replacing the file in the mobile phone's file storage (this is easily done in Android - with a bit of trouble in iOS too) because we can't, in a month or two, give them a facility to test upload their XML to our repository and then pass it to the mobile. Therefore, it is expected that sometimes XML document's last modification date is later than the server's `Last-Modified` header. When this happens, just treat it as "no updates from server". This is also why file extension ".xml" is added when the XML resource is saved, when such extension does not appear in the URI.

Note that when the mobile app requests the XML file from the link, it does not specify network ID (mainnet, Ropsten etc). However, when it stores the XML file locally, it saves the file in the corresponding directory named after network ID. The logic behind this is that: for the server, there is no need to mention network ID, because contracts of the same address on different networks must be deployed by the same person anyway. However, when the client stores the file, it must have already done its content-negotiation (choosing between schema version, crypto-kitty skin vs crypto-pony skin, signature trust level etc.). Therefore it saves the version of XML resulted from the negotiation. For now, the repo server doesn't give HTTP 300 or HTTP 204 so this is out of the question, but when we do add content-negotiation in the future we want to introduce as little change as possible.

This design also means at the current stage, 

    0xA66A3F08068174e8F005112A8b2c7A507a822335.xml
    0xd8e5f58de3933e1e35f9c65eb72cb188674624f3.xml

These are two identical files stored *twice* on the mobile's file storage, and that is intended.

## HTTP Headers ##

These headers should be included with every request:

    "Accept": "text/xml; charset=UTF-8"
    "X-Client-Name": "AlphaWallet"
    "X-Client-Version": "1.0.3"
    "X-Platform-Name": "iOS"
    "X-Platform-Version": "11.1.2"

`X-Platform-Name` should be set to `iOS` or `Android`.

The `If-Modified-Since` header should be included if the XML requested for is already cached on the device:

    "If-Modified-Since": "Wed, 21 Oct 2015 07:28:00 GMT"

# Repo specification #

All XML files in the repository are named like this:

    FIFA WC2018/schema1/www.sktravel.com-signed.xml

Where the directory name "FIFA WC2018" is the Contract's name as returned by the Contract, followed by signer's certificate name (CN, CommonName), followed by schema version. For contracts that share the same contract-name, there are multiple XML files for each contract.

It's possible to have multiple versions:

    FIFA WC2018/schema1/www.sktravel.com-signed.xml
    FIFA WC2018/schema2/www.sktravel.com-signed.xml
    FIFA WC2018/schema1/www.awallet.io-signed.xml
    FIFA WC2018/schema1/signed.xml

The last format is a special one - there is no certificate CN, since the contract's deployment key signs it, which needs no certification.

The XML signature's time stamp is used to determine which file is the latest, thus it is very important to make it correct. This data is however unreliable as we don't have a blockchain timestamp implementation yet, but it will be done in the future.

## Repo Server specication ##

When the server starts, it scans for all XML files in all directories and indexes the validate (by schema and by signature) in a table in memory:

| contract | 0xA66A...35 | 0xd8e5f...f3 |
| -------- | ------------------------------------------ | ------------------------------------------ |
| contract name | FIFA WC2018 | FIFA WC2018 |
| schema version | 1 | 1 |
| signature date | 2019-10-10 | 2019-10-10
| signer | www.sktravel.com | www.sktravel.com |
| path | `FIFA WC2018/schema1/www.sktravel.com-signed.xml` | `FIFA WC2018/schema1/www.sktravel.com-signed.xml` |

Notice that the server completely doesn't care the network ID (mainnet / testnet).

When the client connects to URI like this:
https://repo.awallet.io/0xA66A3F08068174e8F005112A8b2c7A507a822335
the server either returns the file which claims to define the behaviour of the contract in the URI, with the `Last-Modified` field being the XML signature signing time (in case of multi-signature XML file, the last signer's signing time). In other words, the repo server provides a facade as if all files are modified by the same time as its last signature, which should be true, usually, but not always (e.g. when the repo manager might want to swap in and swap out different versions of XML to experiment, or when version management (git etc) breaks the modification timestampe).

In the case that there are multiple files which claim to define the behaviour of the contract in the URI, one of the two things happens.

1. The server, knowing which version of the mobile app supports what kind of XML file, pre-select the compatible file to return.

2. If there are still more than one compatible file, e.g. one signed by contract issuers and another signed by awallet.io while the awallet.io is newer, and that we intend the user to choose, then the server returns 300 with a list of choices.
