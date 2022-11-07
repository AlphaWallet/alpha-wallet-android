package com.alphawallet.token.entity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import org.junit.Test;

public class EthereumMessageTest
{
    @Test
    public void should_get_message()
    {
        EthereumMessage message = new EthereumMessage("aaaaa", "", 1, null);
        assertThat(message.getMessage(), equalTo("aaaaa"));
        assertThat(message.getUserMessage(), equalTo("aaaaa"));

        message = new EthereumMessage("68656c6c6f", "", 1, null);
        assertThat(message.getMessage(), equalTo("68656c6c6f"));
        assertThat(message.getUserMessage(), equalTo("hello"));

        message = new EthereumMessage("0x68656c6c6f", "", 1, null);
        assertThat(message.getMessage(), equalTo("0x68656c6c6f"));
        assertThat(message.getUserMessage(), equalTo("hello"));

        message = new EthereumMessage(null, "", 1, null);
        assertThat(message.getMessage(), equalTo(""));
        assertThat(message.getUserMessage(), equalTo(""));
    }
}