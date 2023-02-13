package com.alphawallet.app.entity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import org.junit.Test;

public class ContractLocatorTest
{
    @Test
    public void should_from_addresses()
    {
        String[] addresses = {"add1", "add2"};

        ContractLocator[] contractLocators = ContractLocator.fromAddresses(addresses, 1L);

        assertThat(contractLocators.length, equalTo(2));
        assertThat(contractLocators[0].address, equalTo("add1"));
        assertThat(contractLocators[0].chainId, equalTo(1L));
        assertThat(contractLocators[1].address, equalTo("add2"));
        assertThat(contractLocators[1].chainId, equalTo(1L));
    }
}