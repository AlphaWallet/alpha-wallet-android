package com.alphawallet.app;

import static androidx.test.espresso.Espresso.onData;
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
import static com.alphawallet.app.util.Helper.clickListItem;
import static junit.framework.TestCase.assertTrue;

import static org.hamcrest.core.IsEqual.equalTo;

import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.RootMatchers;

import com.alphawallet.app.steps.Steps;
import com.alphawallet.app.util.Helper;

import org.junit.Before;
import org.junit.Test;

public class DappBrowserTest extends BaseE2ETest
{
    private static final String DEFAULT_HOME_PAGE = "https://alphawallet.com/browser/";
    private static final String URL_DAPP = "https://app.1inch.io";

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
        Helper.wait(5);
        selectTestNet("Görli");
        navigateToBrowser();
        Helper.wait(3);
        pressBack();
        shouldSee("Görli");
    }

    @Test
    public void should_suggest_websites()
    {
        onView(withId(R.id.url_tv)).perform(click());
        click(withId(R.id.clear_url));
        onView(withId(R.id.url_tv)).perform(pressKey(KeyEvent.KEYCODE_R));
        onView(withId(R.id.url_tv)).perform(pressKey(KeyEvent.KEYCODE_A));
        Helper.wait(2);
        onView(withId(R.id.url_tv)).perform(pressKey(KeyEvent.KEYCODE_DPAD_DOWN));
        Helper.wait(2);
        onView(withId(R.id.url_tv)).perform(pressKey(KeyEvent.KEYCODE_ENTER));
        assertUrlIs("https://make.rare.claims");
    }

    @Test
    public void should_navigate_forward_or_backward()
    {
        onView(withId(R.id.url_tv)).perform(replaceText(URL_DAPP));
        onView(withId(R.id.url_tv)).perform(pressKey(KeyEvent.KEYCODE_SLASH), pressImeActionButton());
        Helper.wait(5);
        assertUrlIs(URL_DAPP);
        onView(withId(R.id.back)).perform(click());
        Helper.wait(5);
        assertUrlIs(DEFAULT_HOME_PAGE);
        onView(withId(R.id.next)).perform(click());
        Helper.wait(5);
        assertUrlIs(URL_DAPP);
    }

    @Test
    public void should_clear_url_and_show_keyboard()
    {
        assertUrlIs(DEFAULT_HOME_PAGE);
        onView(withId(R.id.url_tv)).perform(click());
        click(withId(R.id.clear_url));
        assertUrlIs("");
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
        click(withId(R.id.home));
        assertUrlIs(DEFAULT_HOME_PAGE);
        visit(URL_DAPP);

        openOptionsMenu();
        click(withText("Set as Home Page"));
        onView(withId(R.id.back)).perform(click());
        click(withId(R.id.home));
        Helper.wait(3);
        assertUrlIs(URL_DAPP);
    }

    @NonNull
    private void visit(String url)
    {
        navigateToBrowser();
        onView(withId(R.id.url_tv)).perform(replaceText(url), pressImeActionButton());
    }

    @NonNull
    private void assertUrlIs(String expectedUrl)
    {
        onView(withId(R.id.url_tv)).check(matches(withSubstring(expectedUrl)));
    }
}
