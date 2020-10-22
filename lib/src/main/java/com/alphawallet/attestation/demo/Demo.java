package com.alphawallet.attestation.demo;

import com.alphawallet.attestation.Attestation;
import com.alphawallet.attestation.AttestationCrypto;
import com.alphawallet.attestation.AttestationRequest;
import com.alphawallet.attestation.Cheque;
import com.alphawallet.attestation.DERUtility;
import com.alphawallet.attestation.IdentifierAttestation;
import com.alphawallet.attestation.IdentifierAttestation.AttestationType;
import com.alphawallet.attestation.ProofOfExponent;
import com.alphawallet.attestation.RedeemCheque;
import com.alphawallet.attestation.SignedAttestation;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;

public class Demo {
  public static void main(String args[]) {
    System.out.println("starting...");
    CommandLineParser parser = new DefaultParser();
    Options options = new Options();
    Option role = new Option( "r", "role", true,
        "The role which to run. Must be either \"keys\", \"send\", \"receive\", "
            + "\"request-attest\" or \"construct-attest\"." );
    role.isRequired();
    options.addOption(role);
    CommandLine line;
    try {
      line = parser.parse( options, args );
      if(!line.hasOption( "role" ) ) {
        System.err.println("You must call with a role. Either \"keys\", \"send\", \"receive\", "
            + "\"request-attest\" or \"construct-attest\"");
        throw new RuntimeException("Could not parse role.");
      }
    }
    catch( ParseException e) {
      System.err.println( "Could not parse input");
      throw new RuntimeException(e);
    }

    SecureRandom rand = new SecureRandom();
    AttestationCrypto crypto = new AttestationCrypto(rand);
    List<String> arguments = line.getArgList();
    switch (arguments.get(0).toLowerCase()) {
      case "keys":
        try {
          AsymmetricCipherKeyPair keys = crypto.constructECKeys();
          SubjectPublicKeyInfo spki = SubjectPublicKeyInfoFactory
              .createSubjectPublicKeyInfo(keys.getPublic());
          byte[] pub = spki.getPublicKeyData().getEncoded();
          if (!writeFile(role.getValue(1), DERUtility.printDER(pub, "PUBLIC KEY"))) {
            throw new RuntimeException("Could not write public key");
          }

          PrivateKeyInfo privInfo = PrivateKeyInfoFactory.createPrivateKeyInfo(keys.getPrivate());
          byte[] priv = privInfo.getEncoded();
          if (!writeFile(role.getValue(2), DERUtility.printDER(priv, "PRIVATE KEY"))) {
            throw new RuntimeException("Could not write private key");
          }

        } catch (Exception e) {
          System.err.println("Was expecting: <output dir to public key> <output dir to private key>.");
          throw new RuntimeException(e);
        }
        break;
      case "send":
        try {
          int amount = Integer.parseInt(role.getValue(1));
          String receiverId = role.getValue(2);
          AttestationType type;
          switch (role.getValue(3).toLowerCase()) {
            case "mail":
              type = AttestationType.EMAIL;
              break;
            case "phone":
              type = AttestationType.PHONE;
              break;
            default:
              throw new IllegalArgumentException("Wrong type of identifier");
          }
          int validity = Integer.parseInt(role.getValue(4));
          AsymmetricCipherKeyPair keys = DERUtility.restoreBase64Keys(readFile(role.getValue(5)));
          String outputDirCheque = role.getValue(6);
          if (outputDirCheque.isEmpty()) {
            throw new RuntimeException("Output directory is empty");
          }
          String outputDirSecret = role.getValue(7);
          if (outputDirSecret.isEmpty()) {
            throw new RuntimeException("Output directory is empty");
          }

          BigInteger secret = crypto.makeSecret();
          Cheque cheque = new Cheque(receiverId, type, amount, validity, keys, secret);
          byte[] encoding = cheque.getDerEncoding();

          if (!writeFile(outputDirCheque, DERUtility.printDER(encoding, "CHEQUE"))) {
            throw new RuntimeException("Could not write cheque to disc");
          }

          if (!writeFile(outputDirSecret, DERUtility.printDER(DERUtility.encodeSecret(secret), "CHEQUE SECRET"))) {
            throw new RuntimeException("Could not write cheque to disc");
          }
//        Option amount = new Option("a", "amount", true, "The amount to send");
//        amount.isRequired();
//        options.addOption(amount);
//        Option id = new Option("d", "id", true, "The identifier of the receiver");
//        id.isRequired();
//        options.addOption(id);
//        Option type = new Option("t", "type", true, "The type of ID. Either \"mail\" or \"phone\"");
//        type.isRequired();
//        options.addOption(type);
        } catch (Exception e) {
          System.err.println("Was expecting: <integer amount to send> <identifier of the receiver> "
              + "<type of ID, Either \"mail\" or \"phone\"> <validity in seconds> <signing key input dir>"
              + " <output dir for cheque> <output dir for secret>");
          throw new RuntimeException(e);
        }
        break;
      case "receive":
        try {
          AsymmetricCipherKeyPair keys = DERUtility.restoreBase64Keys(readFile(role.getValue(1)));
          byte[] chequeSecretBytes = DERUtility.restoreBytes(readFile(role.getValue(2)));
          BigInteger chequeSecret = new BigInteger(chequeSecretBytes);
          byte[] attestationSecretBytes = DERUtility.restoreBytes(readFile(role.getValue(3)));
          BigInteger attestationSecret = new BigInteger(attestationSecretBytes);
          byte[] chequeBytes = DERUtility.restoreBytes(readFile(role.getValue(4)));
          Cheque cheque = new Cheque(chequeBytes);
          byte[] attestationBytes = DERUtility.restoreBytes(readFile(role.getValue(5)));
          SignedAttestation att = new SignedAttestation(attestationBytes, keys.getPublic());

          if (!cheque.checkValidity()) {
            throw new RuntimeException("Could not validate cheque");
          }
          if (!cheque.verify()) {
            throw new RuntimeException("Could not verify cheque");
          }
          if (!att.checkValidity()) {
            throw new RuntimeException("Could not validate attestation");
          }
          if (!att.verify()) {
            throw new RuntimeException("Could not verify attestation");
          }

          RedeemCheque redeem = new RedeemCheque(cheque, att, keys, attestationSecret, chequeSecret);
          if (!redeem.checkValidity()) {
            throw new RuntimeException("Could not validate redeem request");
          }
          if (!redeem.verify()) {
            throw new RuntimeException("Could not verify redeem request");
          }
          // TODO how should this actually be?
          SmartContract sc = new SmartContract();
          if (!sc.testEncoding(redeem.getPok())) {
            throw new RuntimeException("Could not submit proof of knowledge to chain");
          }
        } catch (Exception e) {
          System.err.println("Was expecting: <signing key input dir> <cheque secret input dir> "
              + "<attestation secret input dir> <cheque input dir> <attestation input dir>");
          throw new RuntimeException(e);
        }
        break;
      case "request-attest":
        try {
          AsymmetricCipherKeyPair keys = DERUtility.restoreBase64Keys(readFile(role.getValue(1)));
          String receiverId = role.getValue(2);
          AttestationType type;
          switch (role.getValue(3).toLowerCase()) {
            case "mail":
              type = AttestationType.EMAIL;
              break;
            case "phone":
              type = AttestationType.PHONE;
              break;
            default:
              throw new IllegalArgumentException("Wrong type of identifier");
          }
          String outputDirRequest = role.getValue(4);
          if (outputDirRequest.isEmpty()) {
            throw new RuntimeException("Output request directory is empty");
          }

          String outputDirSecret = role.getValue(5);
          if (outputDirSecret.isEmpty()) {
            throw new RuntimeException("Output secret directory is empty");
          }

          BigInteger secret = crypto.makeSecret();
          ProofOfExponent pok = crypto.constructProof(receiverId, type, secret);
          AttestationRequest request = new AttestationRequest(receiverId, type, pok, keys);

          if (!writeFile(outputDirRequest, DERUtility.printDER(request.getDerEncoding(), "ATTESTATION REQUEST"))) {
            throw new RuntimeException("Could not write request");
          }

          if (!writeFile(outputDirSecret, DERUtility.printDER(secret.toByteArray(), "SECRET"))) {
            throw new RuntimeException("Could not write secret");
          }
        } catch (Exception e) {
          System.err.println("Was expecting: <signing key input dir> <identifier> "
              + "<type of ID, Either \"mail\" or \"phone\"> <attestation request output dir> <secret output dir>");
          throw new RuntimeException(e);
        }
        break;
      case "construct-attest":
        // TODO very limited functionality.
        // Should use a configuration file and have a certificate to its signing key
        try {
          AsymmetricCipherKeyPair keys = DERUtility.restoreBase64Keys(readFile(role.getValue(1)));
          String issuerName = role.getValue(2);
          long validity = Integer.parseInt(role.getValue(3));
          byte[] requestBytes = DERUtility.restoreBytes(readFile(role.getValue(3)));
          AttestationRequest request = new AttestationRequest(requestBytes);
          String outputDirSecret = role.getValue(4);
          if (outputDirSecret.isEmpty()) {
            throw new RuntimeException("Output directory is empty");
          }

          if (!request.checkValidity()) {
            throw new RuntimeException("Request is not valid");
          }
          if (!request.verify()) {
            throw new RuntimeException("Request could not be verified");
          }
          Attestation att = new IdentifierAttestation(request.getIdentity(), request.getType(),
              request.getPok().getRiddle().getEncoded(false), request.getPublicKey());
          att.setIssuer("CN=" + issuerName);
          att.setSerialNumber(new Random().nextLong());
          Date now = new Date();
          att.setNotValidBefore(now);
          att.setNotValidAfter(new Date(System.currentTimeMillis() + 1000*validity));
          SignedAttestation signed = new SignedAttestation(att, keys);
          if (!writeFile(outputDirSecret, DERUtility.printDER(signed.getDerEncoding(), "ATTESTATION"))) {
            throw new RuntimeException("Could not write attestation");
          }
        } catch (Exception e) {
          System.err.println("Was expecting: <signing key input dir> <issuer name> "
              + "<validity in seconds> <attestation request input dir> "
              + "<signed attestation output dir>");
          throw new RuntimeException(e);
        }
        break;
      default:
        System.err.println("Unknown option. The role which to run. Must be either \"keys\", \"send\", \"receive\", "
            + "\"request-attest\" or \"construct-attest\".");
    }
//    Option input = new Option("i", "input", true, "The DER encoded input file.");
//
//    options.addOption(
//    options.addOption( "o", "output", true, "The path for the DER encoded output file.");
//
//    try {
//      CommandLine line = parser.parse( options, args );
//
//      // validate options have been added
//      if( line.hasOption( "block-size" ) ) {
//        // print the value of block-size
//        System.out.println( line.getOptionValue( "block-size" ) );
//      }
//    }
//    catch( ParseException exp ) {
//      System.out.println( "Unexpected exception:" + exp.getMessage() );
//    }
    System.out.println("SUCCESS!!!");
  }


  private static String readFile(String dir) throws FileNotFoundException {
      File file = new File(dir);
      Scanner reader = new Scanner(file);
      StringBuffer buf = new StringBuffer();
      while (reader.hasNextLine()) {
        buf.append(reader.nextLine());
      }
      reader.close();
      return buf.toString();
  }

  private static boolean writeFile(String dir, String data) {
    try {
      File file = new File(dir);
      if (!file.createNewFile()) {
        System.out.println("The output file \"" + data + "\" already exists");
        return false;
      }
      FileWriter writer = new FileWriter(file);
      writer.write(data);
      return true;
    } catch (IOException e) {
      return false;
    }
  }

}
