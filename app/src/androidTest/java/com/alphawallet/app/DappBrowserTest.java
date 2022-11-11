package com.alphawallet.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.pressKey;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withSubstring;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.alphawallet.app.assertions.Should.shouldNotSee;
import static com.alphawallet.app.assertions.Should.shouldSee;
import static com.alphawallet.app.steps.Steps.createNewWallet;
import static com.alphawallet.app.steps.Steps.navigateToBrowser;
import static com.alphawallet.app.steps.Steps.openOptionsMenu;
import static com.alphawallet.app.steps.Steps.selectTestNet;
import static com.alphawallet.app.util.Helper.click;
import static com.alphawallet.app.util.Helper.waitUntilLoaded;
import static junit.framework.TestCase.assertTrue;

import static org.hamcrest.core.IsNot.not;

import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.action.ViewActions;

import com.alphawallet.app.util.Helper;

import org.junit.Before;
import org.junit.Test;

public class DappBrowserTest extends BaseE2ETest
{
    private static final String DEFAULT_HOME_PAGE = "https://courses.cs.washington.edu/courses/cse373/99sp/assignments/hw2/test1.html";
    private static final String URL_DAPP = "http://web.simmons.edu/~grovesd/comm244/notes/week3/html-test-page.html";

    @Override
    @Before
    public void setUp()
    {
        super.setUp();
        createNewWallet();
        visit(DEFAULT_HOME_PAGE);
    }

    @Test
    public void should_switch_network()
    {
        shouldSee("Ethereum");
        selectTestNet("Görli");
        navigateToBrowser();
        pressBack();
        waitUntilLoaded();
        shouldSee("Görli");
    }

    @Test
    public void should_suggest_websites()
    {
        onView(withId(R.id.url_tv)).perform(click());
        click(withId(R.id.clear_url));
        onView(withId(R.id.url_tv)).perform(pressKey(KeyEvent.KEYCODE_R), pressKey(KeyEvent.KEYCODE_A));
        Helper.wait(2);
        onView(withId(R.id.url_tv)).perform(pressKey(KeyEvent.KEYCODE_TAB), pressKey(KeyEvent.KEYCODE_DPAD_DOWN));
        waitUntilLoaded();
        onView(withId(R.id.url_tv)).perform(pressKey(KeyEvent.KEYCODE_ENTER));
        assertUrlContains("alphawallet.app.entity.DApp@");
    }

    @Test
    public void should_go_back_when_press_back_button_on_phone()
    {
        visit(URL_DAPP);
        assertUrlContains(URL_DAPP);
        Helper.wait(2);
        pressBack();
        waitUntilLoaded();
        assertUrlContains(DEFAULT_HOME_PAGE);
    }

    @Test
    public void should_navigate_forward_or_backward()
    {
        visit(URL_DAPP);
        assertUrlContains(URL_DAPP);
        Helper.wait(2);
        click(withId(R.id.back));
        waitUntilLoaded();
        assertUrlContains(DEFAULT_HOME_PAGE);
        Helper.wait(2);
        click(withId(R.id.next));
        waitUntilLoaded();
        assertUrlContains(URL_DAPP);
    }

    @Test
    public void should_clear_url_and_show_keyboard()
    {
        assertUrlContains(DEFAULT_HOME_PAGE);
        onView(withId(R.id.url_tv)).perform(click());
        click(withId(R.id.clear_url));
        assertUrlContains("");
        assertTrue(Helper.isSoftKeyboardShown(ApplicationProvider.getApplicationContext()));
    }

    @Test
    public void should_hide_buttons_when_typing_url()
    {
        shouldSee(R.id.back);
        shouldSee(R.id.next);

        onView(withId(R.id.url_tv)).perform(ViewActions.click());

        shouldNotSee(R.id.back);
        shouldNotSee(R.id.next);
    }

    @Test
    public void should_set_homepage()
    {
        visit(URL_DAPP);
        waitUntilLoaded();
        openOptionsMenu();
        Helper.wait(1);
        click(withText("Set as Home Page"));
        Helper.wait(2);
        click(withId(R.id.home));
        waitUntilLoaded();
        assertUrlContains(URL_DAPP);
    }

    @NonNull
    private void visit(String url)
    {
        navigateToBrowser();
        onView(withId(R.id.url_tv)).perform(replaceText(url), pressImeActionButton());
        waitUntilLoaded();
    }

    @NonNull
    private void assertUrlContains(String expectedUrl)
    {
        onView(withId(R.id.url_tv)).check(matches(withSubstring(expectedUrl)));
    }
}
