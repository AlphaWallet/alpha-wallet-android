package com.langitwallet.app.entity.attestation;

import org.web3j.utils.Numeric;

import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.DynamicStruct;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint64;

import java.math.BigInteger;

import timber.log.Timber;

public class AttestationCoreData extends DynamicStruct
{
    byte[] schema;
    public Address recipient;
    long time;
    long expirationTime;
    public boolean revocable;
    byte[] refUID;
    byte[] data;

    public AttestationCoreData(byte[] schema, Address recipient, long time, long expirationTime, boolean revocable, byte[] refUID, byte[] data) {
        super(
                new Bytes32(schema),
                new Address(recipient.getValue()),
                new Uint64(BigInteger.valueOf(time)),
                new Uint64(BigInteger.valueOf(expirationTime)),
                new Bool(revocable),
                new Bytes32(refUID),
                new DynamicBytes(data));
        this.recipient = recipient;
        this.time = time;
        this.expirationTime = expirationTime;
        this.revocable = revocable;
        this.refUID = refUID;
        this.data = data;
        this.schema = schema;

        Timber.d("Format for struct: " + Numeric.toHexString(schema) + "," + recipient.getValue() + "," + time + "," + expirationTime + "," + revocable + "," + Numeric.toHexString(refUID) + ",0," + Numeric.toHexString(data));
    }

    public AttestationCoreData(Bytes32 schema, Address recipient, Uint64 time, Uint64 expirationTime, Bool revocable, Bytes32 refUID, DynamicBytes data) {
        super(schema, recipient, time, expirationTime, revocable, refUID, data);
        this.recipient = recipient;
        this.time = time.getValue().longValue();
        this.expirationTime = expirationTime.getValue().longValue();
        this.revocable = revocable.getValue();
        this.refUID = refUID.getValue();
        this.data = data.getValue();
        this.schema = schema.getValue();
    }
}
