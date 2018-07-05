
To sign the XML:

$ /opt/xmlsectool-2.0.0/xmlsectool.sh --sign --digest SHA-256 --signatureAlgorithm 'http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha256' --inFile contracts/TicketingContract.xml --outFile /tmp/signed.xml --key skstravel.cn/skstravel_cn.pkcs8.pem --certificate skstravel.cn/skstravel_cn.crt 


The following instruction is mostly based on this tutorial adapted to the special device we have from Netrust, the Singapore accredited CA.

https://developers.yubico.com/PIV/Guides/Android_code_signing.html


First, install the driver following this instruction:

https://www.vleeuwen.net/2016/07/install-safenet-etoken-pro-on-ubuntu-16-04-lts

Examine if the device is working properly:

	$ pkcs11-tool --module /lib/libeToken.so.9  -l -O
	Using slot 0 with a present token (0x0)
	Logging in to "Zhang Wei Wu".
	Please enter User PIN: 
	Private Key Object; RSA 
	  label:      
	  ID:         784de9fdf4cd3975
	  Usage:      sign
	Certificate Object; type = X.509 cert
	  label:      4|54|97|0|xSDcp31PRKu24R99o33LzA==
	  ID:         784de9fdf4cd3975
	Private Key Object; RSA 
	  label:      
	  ID:         c9a8b5e61ab59320
	  Usage:      decrypt, sign, unwrap
	Certificate Object; type = X.509 cert
	  label:      4|54|96|0|xSDcp31PRKu24R99o33LzA==
	  ID:         c9a8b5e61ab59320

The driver should install a libary /lib/libeToken.so.9 which we will
use later. Make sure it's there.

Of the 2 keys, the first one is used to sign. Note down its
fingerprint because sometimes the key is known by its fingerprint, not
by 784de9fdf4cd3975:


    $ pkcs11-tool -r --id 784de9fdf4cd3975 --type cert \
	--module /lib/libeToken.so.9 | \
	openssl x509 -inform DER -noout -noout -fingerprint -sha1
    Using slot 0 with a present token (0x0)
    SHA1 Fingerprint=77:84:52:8F:B7:EE:47:CB:CE:33:11:31:53:9E:58:CC:38:98:95:7A


Then, use keytool to see if Java can use the key. You need to create a
config file first.

	$ cat > pkcs11_java.cfg
	name = OpenSC-PKCS11
	description = SunPKCS11 via OpenSC
	library = /lib/libeToken.so.9
	slotListIndex = 0

Then get a list of keys

	$ /usr/lib/jvm/java-8-openjdk-amd64/bin/keytool -providerClass sun.security.pkcs11.SunPKCS11 -providerArg pkcs11_java.cfg  -keystore NONE -storetype PKCS11 -list    Introduzca la contraseña del almacén de claves:
	Tipo de Almacén de Claves: PKCS11
	Proveedor de Almacén de Claves: SunPKCS11-OpenSC-PKCS11

	Su almacén de claves contiene 2 entradas

	4|54|97|0|xSDcp31PRKu24R99o33LzA==, PrivateKeyEntry,
	Huella Digital de Certificado (SHA1): 77:84:52:8F:B7:EE:47:CB:CE:33:11:31:53:9E:58:CC:38:98:95:7A
	4|54|96|0|xSDcp31PRKu24R99o33LzA==, PrivateKeyEntry,
	Huella Digital de Certificado (SHA1): AC:81:3E:72:32:DE:A0:E0:4D:1B:48:A9:3C:F4:78:1E:8C:7B:F7:1B

Since we have earlier identified the key by the fingerprint, now find
the right key and note down its alias. Then, experiment signing a jar
file with the key by referring to its alias as the last argument of
this command.

	$ /usr/lib/jvm/java-8-openjdk-amd64/bin/jarsigner \
		-providerClass sun.security.pkcs11.SunPKCS11 \
		-providerArg pkcs11_java.cfg \
		-keystore NONE -storetype PKCS11 \
		-tsa http://timestamp.comodoca.com/rfc3161 \
		app/build/outputs/apk/release/app-release-unsigned.apk \
		"4|54|97|0|xSDcp31PRKu24R99o33LzA=="
		Enter Passphrase for keystore: 
		jar signed.

		Warning: 
		The signer's certificate chain is not validated.

Next step is to get the certificate chain working.
