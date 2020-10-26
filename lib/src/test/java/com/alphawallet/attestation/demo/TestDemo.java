package com.alphawallet.attestation.demo;

import java.io.File;
import org.junit.Before;
import org.junit.Test;

public class TestDemo {
  @Before
  public void cleanup() {
    String[] files = new String[]{"sender-pub.pem", "sender-priv.pem", "receiver-pub.pem",
        "receiver-priv.pem", "attestor-pub.pem", "attestor-priv.pem", "cheque.pem",
        "cheque-secret.pem", "attestation-request.pem", "attestation-secret.pem", "attestation.pem"};
    File currentKey;
    for (String current : files) {
      currentKey = new File(current);
      currentKey.delete();
    }
  }

  @Test
  public void executeFlow() {
    String[] args;
    // Keys
    args = new String[]{"keys", "sender-pub.pem", "sender-priv.pem"};
    Demo.main(args);
    args = new String[]{"keys", "receiver-pub.pem", "receiver-priv.pem"};
    Demo.main(args);
    args = new String[]{"keys", "attestor-pub.pem", "attestor-priv.pem"};
    Demo.main(args);
    // Send
    args = new String[]{"create-cheque", "42", "test@test.ts", "mail", "3600", "sender-priv.pem", "cheque.pem", "cheque-secret.pem"};
    Demo.main(args);
    // Request attestation
    args = new String[]{"request-attest", "receiver-priv.pem", "test@test.ts", "mail", "attestation-request.pem", "attestation-secret.pem"};
    Demo.main(args);
    // Construct attestation
    args = new String[]{"construct-attest", "attestor-priv.pem", "AlphaWallet", "3600", "attestation-request.pem", "attestation.pem"};
    Demo.main(args);
    // Redeem
    args = new String[]{"receive-cheque", "receiver-priv.pem", "cheque-secret.pem", "attestation-secret.pem", "cheque.pem", "attestation.pem", "attestor-pub.pem"};
    Demo.main(args);
  }
}
