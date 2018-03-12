package com.wallet.crypto.alphawallet;

import com.wallet.crypto.alphawallet.entity.EthereumReadBuffer;

import org.junit.Test;
import org.junit.Assert;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Created by weiwu on 12/3/18.
 */

public class EthereumReadWriteTest {
    byte[] binaryData = new byte[] {(byte)0xe0, (byte)0xf4, (byte)0xe0, (byte)0xf5};
    @Test
    public void BufferReadsCorrectly() throws IOException{
        int[] indices;
        EthereumReadBuffer in = new EthereumReadBuffer(new ByteArrayInputStream(binaryData));
        indices = in.readUint16Indices(2);
        Assert.assertEquals(0xE0F4, indices[0]);
    }

}
