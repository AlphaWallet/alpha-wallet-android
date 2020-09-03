package com.alphawallet.app;

import com.alphawallet.token.entity.EthereumReadBuffer;
import com.alphawallet.token.entity.EthereumWriteBuffer;
import com.alphawallet.token.tools.Numeric;

import org.junit.Test;
import org.junit.Assert;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

    //TODO: Make a sign type test
    @Test
    public void SignCreationTest() throws IOException
    {
        String result1 = "0x48692c20416c6963652100000539";
        String result2 = "0x48692c20416c69636521fffffac7";
        String result3 = "0xEA674fdDe714fd979de3EdF0F56AA9716B898ec8".toLowerCase() + "fffffffffffffac7" + "00000539";

        String address = "0xEA674fdDe714fd979de3EdF0F56AA9716B898ec8";
        String testValue = "Hi, Alice!";
        String decimalVal1 = "1337";
        String decimalVal2 = "-1337";

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        EthereumWriteBuffer eb = new EthereumWriteBuffer(buffer);

        //write a string
        eb.write(testValue.getBytes());
        eb.writeValue(decimalVal1, 4); //write 4 byte positive value
        System.out.println("YOL " + Numeric.toHexString(buffer.toByteArray()));
        Assert.assertEquals(result1, Numeric.toHexString(buffer.toByteArray()));

        buffer.reset();
        eb.write(testValue.getBytes());
        eb.writeValue(decimalVal2, 4); //4 byte negative
        Assert.assertEquals(result2, Numeric.toHexString(buffer.toByteArray()));

        buffer.reset();
        eb.writeAddress(address);
        eb.writeValue(decimalVal2, 8);
        eb.writeValue(decimalVal1, 4);
        Assert.assertEquals(result3, Numeric.toHexString(buffer.toByteArray()));
    }
}
