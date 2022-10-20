package com.alphawallet.app;

import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.alphawallet.app.steps.Steps.createNewWallet;
import static com.alphawallet.app.steps.Steps.gotoSettingsPage;
import static com.alphawallet.app.steps.Steps.selectMenu;
import static com.alphawallet.app.util.Helper.click;

import org.junit.Test;

public class I18nTest extends BaseE2ETest
{

    @Test
    public void should_switch_language()
    {
        createNewWallet();
        gotoSettingsPage();

        selectMenu("Change Language");
        click(withText("Chinese"));
        pressBack();

        selectMenu("更换语言");
        click(withText("西班牙语"));
        pressBack();

        selectMenu("Cambiar idioma");
        click(withText("Francés"));
        pressBack();

        selectMenu("Changer Langue");
        click(withText("Vietnamien"));
        pressBack();

        selectMenu("Thay đổi ngôn ngữ");
        click(withText("Tiếng Miến Điện"));
        pressBack();

        selectMenu("ဘာသာစကားပြောင်းမည်");
        click(withText("အင်ဒိုနီးရှား"));
        pressBack();
    }
}
