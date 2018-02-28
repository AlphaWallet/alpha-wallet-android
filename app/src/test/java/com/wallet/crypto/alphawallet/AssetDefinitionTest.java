package com.wallet.crypto.alphawallet;

import org.junit.Test;
import java.io.File;
import java.util.regex.Pattern;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.wallet.crypto.alphawallet.repository.AssetDefinition;

public class AssetDefinitionTest {

    @Test
    public void DOMParserTest() {
        new AssetDefinition(new File("/home/weiwu/StudioProjects/trust-wallet-android/app/src/main/assets/asset_definition.xml"));
        //assertThat(EmailValidator.isValidEmail("name@email.com"), is(true));
    }
}