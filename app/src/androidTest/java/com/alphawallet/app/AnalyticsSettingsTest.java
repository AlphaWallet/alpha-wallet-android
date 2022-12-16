package com.alphawallet.app;

import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.alphawallet.app.assertions.Should.shouldSee;
import static com.alphawallet.app.steps.Steps.createNewWallet;
import static com.alphawallet.app.steps.Steps.gotoSettingsPage;
import static com.alphawallet.app.steps.Steps.selectMenu;
import static com.alphawallet.app.util.Helper.click;

import org.junit.Test;

public class AnalyticsSettingsTest extends BaseE2ETest
{
    @Test
    public void title_should_see_analytics_settings_page()
    {
        createNewWallet();
        gotoSettingsPage();
        selectMenu("Advanced");
        click(withText("Analytics"));
        shouldSee("Share Anonymous Data");
    }

    @Test
    public void title_should_see_crash_report_settings_page()
    {
        createNewWallet();
        gotoSettingsPage();
        selectMenu("Advanced");
        click(withText("Crash Reporting"));
        shouldSee("Share Anonymous Data");
    }
}
