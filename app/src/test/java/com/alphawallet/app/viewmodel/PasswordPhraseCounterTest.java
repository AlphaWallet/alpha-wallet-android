package com.alphawallet.app.viewmodel;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PasswordPhraseCounterTest
{

    @Test
    public void testGetText()
    {
        assertThat(new PasswordPhraseCounter(11).getText(), equalTo("11/12"));
        assertThat(new PasswordPhraseCounter(12).getText(), equalTo("12/12"));
        assertThat(new PasswordPhraseCounter(13).getText(), equalTo("13/24"));
        assertThat(new PasswordPhraseCounter(24).getText(), equalTo("24/24"));
    }

    @Test
    public void testNotEnough()
    {
        assertTrue(new PasswordPhraseCounter(11).notEnough());
        assertFalse(new PasswordPhraseCounter(12).notEnough());
        assertTrue(new PasswordPhraseCounter(13).notEnough());
        assertFalse(new PasswordPhraseCounter(24).notEnough());
        assertFalse(new PasswordPhraseCounter(25).notEnough());
    }

    @Test
    public void testExceed()
    {
        assertFalse(new PasswordPhraseCounter(11).exceed());
        assertFalse(new PasswordPhraseCounter(12).exceed());
        assertFalse(new PasswordPhraseCounter(13).exceed());
        assertFalse(new PasswordPhraseCounter(24).exceed());
        assertTrue(new PasswordPhraseCounter(25).exceed());
    }

    @Test
    public void testMatch()
    {
        assertTrue(new PasswordPhraseCounter(12).match());
        assertTrue(new PasswordPhraseCounter(24).match());
    }
}