package com.alphawallet.attestation;

import java.math.BigInteger;
import org.bouncycastle.math.ec.ECPoint;

public class ProofOfExponent {
  private final ECPoint base;
  private final ECPoint riddle;
  private final ECPoint tPoint;
  private final BigInteger challenge;

  public ProofOfExponent(ECPoint base, ECPoint riddle, ECPoint tPoint, BigInteger challenge) {
    this.base = base;
    this.riddle = riddle;
    this.tPoint = tPoint;
    this.challenge = challenge;
  }

  public ECPoint getBase() {
    return base;
  }

  public ECPoint getRiddle() {
    return riddle;
  }

  public ECPoint gettPoint() {
    return tPoint;
  }

  public BigInteger getChallenge() {
    return challenge;
  }

}
