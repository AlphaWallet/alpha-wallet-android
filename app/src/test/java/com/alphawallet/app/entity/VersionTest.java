package com.alphawallet.app.entity;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class VersionTest
{
    @Test
    public void compareVersions()
    {
        Version a = new Version("1.1");
        Version b = new Version("1.1.1");
        assertThat(a.compareTo(b), equalTo(-1));
        assertThat(a.equals(b), equalTo(false));

        Version c = new Version("2.0");
        Version d = new Version("1.9.9");
        assertThat(c.compareTo(d), equalTo(1));
        assertThat(c.equals(d), equalTo(false));

        Version e = new Version("1.0");
        Version f = new Version("1");
        assertThat(e.compareTo(f), equalTo(0));
        assertThat(e.equals(f), equalTo(true));
    }
}
