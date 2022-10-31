package com.alphawallet.app.entity.tokens;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import org.junit.Test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public class TicketTest
{
    @Test
    public void should_get_token_count()
    {
        Ticket ticket = new Ticket(null, Arrays.asList(BigInteger.valueOf(2), BigInteger.ZERO, BigInteger.ONE), 0L, null, null);
        assertThat(ticket.getTokenCount(), equalTo(2));

        ticket = new Ticket(null, (List<BigInteger>) null, 0L, null, null);
        assertThat(ticket.getTokenCount(), equalTo(0));
    }
}
