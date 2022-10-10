package com.alphawallet.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.pressKey;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.alphawallet.app.assertions.Should.shouldNotSee;
import static com.alphawallet.app.assertions.Should.shouldSee;
import static com.alphawallet.app.steps.Steps.createNewWallet;
import static com.alphawallet.app.steps.Steps.navigateToBrowser;
import static com.alphawallet.app.steps.Steps.selectTestNet;
import static com.alphawallet.app.steps.Steps.visit;
import static junit.framework.TestCase.assertTrue;

import static org.hamcrest.Matchers.allOf;

import android.view.KeyEvent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.action.ViewActions;

import com.alphawallet.app.util.Helper;

import org.junit.Before;
import org.junit.Test;

public class DappBrowserTest extends BaseE2ETest
{
    public static final String URL_STRING = "https://bing.com";

    @Override
    @Before
    public void setUp()
    {
        super.setUp();
        createNewWallet();
        visit(URL_STRING);
    }

    @Test
    public void should_switch_network()
    {
        shouldSee("Ethereum");
        Helper.wait(5);
        selectTestNet("Görli");
        navigateToBrowser();
        Helper.wait(3);
        pressBack();
        shouldSee("Görli");
    }

    @Test
    public void should_work_like_a_browser()
    {
        shouldHideButtonsWhenTypingUrl();
        shouldClearUrl();
        shouldSuggestWebsites();
        shouldGotoHomepage();
    }

    @Test
    public void shouldGotoHomepage()
    {
        Helper.click(withId(R.id.home));
        assertURLIs(URL_STRING);
    }

    @Test
    public void shouldSuggestWebsites()
    {
        onView(withId(R.id.url_tv)).perform(click(), pressKey(KeyEvent.KEYCODE_R), pressKey(KeyEvent.KEYCODE_A));
        Helper.wait(2);
        onView(withId(R.id.url_tv)).perform(pressKey(KeyEvent.KEYCODE_DPAD_DOWN));
        Helper.wait(2);
        onView(withId(R.id.url_tv)).perform(pressKey(KeyEvent.KEYCODE_ENTER));
        assertURLIs("app.entity.DApp");
    }

    @Test
    public void shouldHideButtonsWhenTypingUrl()
    {
        shouldSee(R.id.back);
        shouldSee(R.id.next);
        onView(withId(R.id.url_tv)).perform(ViewActions.click());
        shouldNotSee(R.id.back);
        shouldNotSee(R.id.next);
    }

    @Test
    public void shouldClearUrl()
    {
        assertURLIs(URL_STRING);
        Helper.wait(2);
        Helper.click(withId(R.id.clear_url));
        assertURLIs("");
        assertTrue(Helper.isSoftKeyboardShown(ApplicationProvider.getApplicationContext()));
    }

    private void assertURLIs(String url)
    {
        onView(allOf(withId(R.id.url_tv), withText(url)));
    }
}
