package com.alphawallet.app.widget;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.text.InputType;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.LayoutRes;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.ui.HomeActivity;
import com.alphawallet.app.util.KeyboardUtils;
import com.alphawallet.app.util.Utils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.mailchimp.sdk.api.model.Contact;
import com.mailchimp.sdk.api.model.ContactStatus;
import com.mailchimp.sdk.core.MailchimpSdkConfiguration;
import com.mailchimp.sdk.main.Mailchimp;

import java.util.ArrayList;
import java.util.Collections;



public class EmailPromptView extends LinearLayout implements StandardFunctionInterface {

    static {
        System.loadLibrary("keys");
    }

    private BottomSheetDialog parentDialog;

    public void setParentDialog(BottomSheetDialog parentDialog) {
        this.parentDialog = parentDialog;
    }

    public static native String getMailchimpKey();

    private InputView emailInput;
    private final View successOverlay;
    private final Handler handler;
    private final Runnable onSuccessRunnable;

    public EmailPromptView(Context context, View successOverlay, Handler handler, Runnable onSuccessRunnable) {
        super(context);
        this.successOverlay = successOverlay;
        this.handler = handler;
        this.onSuccessRunnable = onSuccessRunnable;
        init(R.layout.layout_dialog_email_prompt);
    }

    private void init(@LayoutRes int layoutId) {
        LayoutInflater.from(getContext()).inflate(layoutId, this, true);

        FunctionButtonBar functionBar = findViewById(R.id.layoutButtons);
        functionBar.setupFunctions(this, new ArrayList<>(Collections.singletonList(R.string.action_want_to_receive_email)));
        functionBar.revealButtons();

        emailInput = findViewById(R.id.email_input);
        emailInput.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailInput.getEditText().setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    handleClick(getContext().getString(R.string.action_want_to_receive_email), 0);
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void handleClick(String action, int actionId) {

        if (action.equals(getContext().getString(R.string.action_want_to_receive_email))) {
            // validate email
            String email = emailInput.getText().toString();
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.setError(R.string.email_is_invalid);
                return ;
            }

            String sdkKey = getMailchimpKey();
            try {

                KeyboardUtils.hideKeyboard(this);

                MailchimpSdkConfiguration configuration = new MailchimpSdkConfiguration.Builder(getContext(), sdkKey)
                        .isAutoTaggingEnabled(true)
                        .build();
                Mailchimp mailchimpSdk = Mailchimp.initialize(configuration);

                Contact contact = new Contact.Builder(email)
                        .setContactStatus(ContactStatus.SUBSCRIBED)
                        .build();

                mailchimpSdk.createOrUpdateContact(contact);
            } catch (IllegalArgumentException ignored) {

            }

            parentDialog.dismiss();

            if (successOverlay != null) successOverlay.setVisibility(View.VISIBLE);
            handler.postDelayed(onSuccessRunnable, 1000);
        }
    }
}
