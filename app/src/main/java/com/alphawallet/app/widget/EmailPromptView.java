package com.alphawallet.app.widget;

import android.content.Context;
import android.text.InputType;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.LayoutRes;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.StandardFunctionInterface;
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

    public static native String getMailchimpKey();

    private InputView emailInput;

    public EmailPromptView(Context context) {
        this(context, R.layout.layout_dialog_email_prompt);
    }

    public EmailPromptView(Context context, @LayoutRes int layoutId) {
        super(context);

        init(layoutId);
    }

    private void init(@LayoutRes int layoutId) {
        LayoutInflater.from(getContext()).inflate(layoutId, this, true);

        FunctionButtonBar functionBar = findViewById(R.id.layoutButtons);
        functionBar.setupFunctions(this, new ArrayList<>(Collections.singletonList(R.string.action_want_to_receive_email)));
        functionBar.revealButtons();

        emailInput = findViewById(R.id.email_input);
        emailInput.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
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
        }
    }
}
