package com.alphawallet.app;

import com.alphawallet.token.entity.EthereumReadBuffer;

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
        int[] indices = new int[2];
        EthereumReadBuffer in = new EthereumReadBuffer(new ByteArrayInputStream(binaryData));
        in.readUnsignedShort(indices);
        Assert.assertEquals(0xE0F4, indices[0]);
    }
}
