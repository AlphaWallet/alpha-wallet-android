package com.alphawallet.attestation;

public interface Verifiable {
  /**
   * Method for verifying cryptographic correctness of the data within a given object.
   * This only covers cryptographic aspects thus dates and other meta-data that must be verified must
   * be done manually.
   */
  public boolean verify();
}
