package com.alphawallet.app.walletconnect;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import com.alphawallet.app.walletconnect.entity.SignRequest;
import com.alphawallet.token.entity.SignMessageType;

import org.junit.Before;
import org.junit.Test;

public class SignRequestTest
{
    private SignRequest signRequest;

    @Before
    public void setUp() throws Exception
    {
        signRequest = new SignRequest("[0xD0c424B3016E9451109ED97221304DeC639b3F84, 0x4d7920656d61696c206973206a6f686e40646f652e636f6d202d2031363437323235383333333539]");
    }

    @Test
    public void testGetWalletAddress()
    {
        assertThat(signRequest.getWalletAddress(), equalTo("0xD0c424B3016E9451109ED97221304DeC639b3F84"));
    }

    @Test
    public void testGetSignable()
    {
        assertThat(signRequest.getSignable().getMessage(), equalTo("0x4d7920656d61696c206973206a6f686e40646f652e636f6d202d2031363437323235383333333539"));
        assertThat(signRequest.getSignable().getMessageType(), equalTo(SignMessageType.SIGN_MESSAGE));
    }
}
