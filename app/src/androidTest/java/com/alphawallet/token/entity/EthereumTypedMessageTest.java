package com.alphawallet.token.entity;

import com.alphawallet.app.entity.CryptoFunctions;
import com.alphawallet.app.web3j.StructuredDataEncoder;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import androidx.test.runner.AndroidJUnitRunner;

import static com.alphawallet.token.entity.SignMessageType.SIGN_TYPED_DATA_V4;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

@RunWith(AndroidJUnit4ClassRunner.class)
public class EthereumTypedMessageTest
{
    @Test
    public void testV4_simple_format()
    {
        String msg = "{\n" +
                "  \"types\": {\n" +
                "    \"Person\": [\n" +
                "      {\n" +
                "        \"name\": \"name\",\n" +
                "        \"type\": \"string\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"name\": \"wallet\",\n" +
                "        \"type\": \"address\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"Mail\": [\n" +
                "      {\n" +
                "        \"name\": \"from\",\n" +
                "        \"type\": \"Person\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"name\": \"to\",\n" +
                "        \"type\": \"Person\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"name\": \"contents\",\n" +
                "        \"type\": \"string\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  \"primaryType\": \"Mail\",\n" +
                "  \"domain\": {\n" +
                "    \"name\": \"Ether Mail\",\n" +
                "    \"version\": \"1\",\n" +
                "    \"chainId\": 1,\n" +
                "    \"verifyingContract\": \"0xCcCCccccCCCCcCCCCCCcCcCccCcCCCcCcccccccC\"\n" +
                "  },\n" +
                "  \"message\": {\n" +
                "    \"from\": {\n" +
                "      \"name\": \"Cow\",\n" +
                "      \"wallet\": \"0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826\"\n" +
                "    },\n" +
                "    \"to\": {\n" +
                "      \"name\": \"Bob\",\n" +
                "      \"wallet\": \"0xbBbBBBBbbBBBbbbBbbBbbbbBBbBbbbbBbBbbBBbB\"\n" +
                "    },\n" +
                "    \"contents\": \"Hello, Bob!\"\n" +
                "  }\n" +
                "}";
        EthereumTypedMessage message = new EthereumTypedMessage(msg, "", 1, new CryptoFunctions());
        assertThat(message.getMessageType(), equalTo(SIGN_TYPED_DATA_V4));
        assertThat(message.getPrehash().length, greaterThan(0));
        assertThat(message.getUserMessage().toString(), equalTo("from:\n name: Cow\n wallet: 0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826\nto:\n name: Bob\n wallet: 0xbBbBBBBbbBBBbbbBbbBbbbbBBbBbbbbBbBbbBBbB\ncontents:\n Hello, Bob!\n"));
    }


    @Test
    public void testV4_full_format()
    {
        String msg = "{\"types\":{\"EIP712Domain\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"version\",\"type\":\"string\"},{\"name\":\"chainId\",\"type\":\"uint256\"},{\"name\":\"verifyingContract\",\"type\":\"address\"}],\"Person\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"wallet\",\"type\":\"address\"}],\"Mail\":[{\"name\":\"from\",\"type\":\"Person\"},{\"name\":\"to\",\"type\":\"Person\"},{\"name\":\"contents\",\"type\":\"string\"}]},\"primaryType\":\"Mail\",\"domain\":{\"name\":\"Ether Mail\",\"version\":\"1\",\"chainId\":1,\"verifyingContract\":\"0xCcCCccccCCCCcCCCCCCcCcCccCcCCCcCcccccccC\"},\"message\":{\"from\":{\"name\":\"Cow\",\"wallet\":\"0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826\"},\"to\":{\"name\":\"Bob\",\"wallet\":\"0xbBbBBBBbbBBBbbbBbbBbbbbBBbBbbbbBbBbbBBbB\"},\"contents\":\"Hello, Bob!\"}}";
        EthereumTypedMessage message = new EthereumTypedMessage(msg, "", 1, new CryptoFunctions());
        assertThat(message.getMessageType(), equalTo(SIGN_TYPED_DATA_V4));
        assertThat(message.getPrehash().length, greaterThan(0));
        assertThat(message.getUserMessage().toString(), equalTo("from:\n name: Cow\n wallet: 0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826\nto:\n name: Bob\n wallet: 0xbBbBBBBbbBBBbbbBbbBbbbbBBbBbbbbBbBbbBBbB\ncontents:\n Hello, Bob!\n"));
    }
}