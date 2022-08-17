package com.alphawallet.app.widget;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.alphawallet.app.R;
import com.alphawallet.app.ui.SendActivity;
import com.alphawallet.app.ui.widget.entity.AddressReadyCallback;
import com.alphawallet.shadows.ShadowApp;
import com.alphawallet.shadows.ShadowUserAvatar;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

@RunWith(AndroidJUnit4.class)
@Config(shadows = {ShadowApp.class, ShadowUserAvatar.class})
public class InputAddressTest
{
    private InputAddress inputAddress;
    private TextView addressText;
    private TextView pasteButton;

    @Before
    public void setUp() throws Exception
    {
        ActivityController<SendActivity> activityController = Robolectric.buildActivity(SendActivity.class);
        SendActivity sendActivity = activityController.get();
        inputAddress = new InputAddress(sendActivity, null);
        inputAddress.setAddressCallback(new AddressReadyCallback()
        {
            @Override
            public void addressReady(String address, String ensName)
            {

            }
        });
        initViews();
    }

    @Test
    public void should_paste_and_clean_address()
    {
        assertThat(pasteButton.getText(), equalTo("Paste"));

        setClipboardText("this_is_an_address");
        pasteButton.performClick();

        assertThat(getAddress(), equalTo("this_is_an_address"));
        assertThat(pasteButton.getText(), equalTo("Clear"));

        pasteButton.performClick();
        assertThat(getAddress(), equalTo(""));
        assertThat(pasteButton.getText(), equalTo("Paste"));
    }

    @Test
    public void should_trigger_callback_after_text_changed()
    {
        AddressReadyCallback mock = mock(AddressReadyCallback.class);
        inputAddress.setAddressCallback(mock);

        setClipboardText("not_an_address");
        pasteButton.performClick();
        verify(mock).addressValid(false);

        pasteButton.performClick(); // Clear
        setClipboardText("0xD8790c1eA5D15F8149C97F80524AC87f56301204");
        pasteButton.performClick();
        verify(mock).addressValid(true);
    }

    @NonNull
    private String getAddress()
    {
        return addressText.getText().toString();
    }

    private void initViews()
    {
        addressText = inputAddress.findViewById(R.id.edit_text);
        pasteButton = inputAddress.findViewById(R.id.text_paste);
    }

    private void setClipboardText(String text)
    {
        ClipboardManager clipboardManager = (ClipboardManager) RuntimeEnvironment.getApplication().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("label", text);
        clipboardManager.setPrimaryClip(clip);
    }
}