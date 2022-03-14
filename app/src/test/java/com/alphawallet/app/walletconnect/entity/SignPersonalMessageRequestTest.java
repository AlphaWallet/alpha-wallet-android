package com.alphawallet.app.walletconnect.entity;

import com.alphawallet.token.entity.SignMessageType;
import com.alphawallet.token.entity.Signable;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class SignPersonalMessageRequestTest
{
    private SignPersonalMessageRequest request;

    @Before
    public void setUp()
    {
        request = new SignPersonalMessageRequest("[0x4d7920656d61696c206973206a6f686e40646f652e636f6d202d2031363437323232373636323838, 0xD0c424B3016E9451109ED97221304DeC639b3F84]");
    }

    @Test
    public void testGetWalletAddress()
    {
        assertThat(request.getWalletAddress(), equalTo("0xD0c424B3016E9451109ED97221304DeC639b3F84"));
    }

    @Test
    public void testGetSignable()
    {
        Signable signable = request.getSignable();
        assertThat(signable.getMessage(), equalTo("0x4d7920656d61696c206973206a6f686e40646f652e636f6d202d2031363437323232373636323838"));
        assertThat(signable.getMessageType(), equalTo(SignMessageType.SIGN_PERSONAL_MESSAGE));
    }
}