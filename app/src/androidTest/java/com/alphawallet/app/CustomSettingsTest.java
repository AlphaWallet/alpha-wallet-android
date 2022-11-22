package com.alphawallet.app;

import static com.alphawallet.app.assertions.Should.shouldSee;
import static com.alphawallet.app.steps.Steps.createNewWallet;
import static com.alphawallet.app.util.Helper.waitUntilLoaded;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import com.alphawallet.app.util.Helper;

import org.junit.Test;

import java.io.File;
import java.net.URL;

public class CustomSettingsTest extends BaseE2ETest
{
    private static File getFileFromPath(Object obj) {
        ClassLoader classLoader = obj.getClass().getClassLoader();
        assert classLoader != null;
        URL resource = classLoader.getResource("assets/custom_view_settings.json");
        return new File(resource.getPath());
    }

    @Test
    public void fileObjectShouldNotBeNull() throws Exception {
        File file = getFileFromPath(this);
        assertThat(file, notNullValue());

        createNewWallet();
        Helper.wait(1);
        shouldSee(R.id.back);
        waitUntilLoaded();
    }

}

