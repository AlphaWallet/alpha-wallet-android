package com.alphawallet.app.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.LinearLayout;

import com.alphawallet.app.entity.MediaLinks;
import com.alphawallet.app.ui.widget.adapter.HelpAdapter;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.HelpItem;
import com.alphawallet.app.viewmodel.HelpViewModel;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

@AndroidEntryPoint
public class HelpActivity extends BaseActivity {
    private HelpViewModel viewModel;
    private WebView webView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_help);
        toolbar();
        setTitle(getString(R.string.toolbar_header_help));

        RecyclerView list = findViewById(R.id.list_help);
        list.setLayoutManager(new LinearLayoutManager(this));
        HelpAdapter adapter = new HelpAdapter();
        webView = findViewById(R.id.webview);

        /* Placeholder only */
        int[] questions = {
                R.string.help_question1,
                R.string.help_question2,
                R.string.help_question3,
                R.string.help_question4,
                R.string.help_question5,
                R.string.help_question6,
                R.string.help_question7,
                R.string.help_question8
        };

        int[] answers = {
                R.string.what_is_eth,
                R.string.why_alphawallet_eth,
                R.string.how_i_get_money,
                R.string.what_is_seed_phrase,
                R.string.how_i_transfer_into_wallet,
                R.string.tokenscript_explaination,
                R.string.privacy_policy,
                R.string.terms_of_service
        };

        adapter.setWebView(webView);
        List<HelpItem> helpItems = new ArrayList<>();
        for (int i = 0; i < questions.length; i++) {
            if (isRawResource(answers[i]))
                helpItems.add(new HelpItem(getString(questions[i]), answers[i]));
            else if (getString(questions[i]).length() > 0)
                helpItems.add(new HelpItem(getString(questions[i]), getString(answers[i])));
        }
        adapter.setHelpItems(helpItems);

        list.setAdapter(adapter);

        final LinearLayout contactUs = findViewById(R.id.layout_contact);
        contactUs.setOnClickListener(v -> {
            helpIntent();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (hideWebView()) {
                super.onOptionsItemSelected(item);
            }
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (hideWebView()) {
            super.onBackPressed();
        }
    }


    private boolean hideWebView() {
        if (webView.getVisibility() == View.VISIBLE) {
            webView.setVisibility(View.GONE);
            return false;
        }
        return true;
    }

    private void helpIntent() {
        final String at = "@";
        String uriText =
                "mailto:" + MediaLinks.AWALLET_EMAIL1 + at + MediaLinks.AWALLET_EMAIL2 +
                        "?subject=" + Uri.encode(MediaLinks.AWALLET_SUBJECT) +
                        "&body=" + Uri.encode("");

        Uri uri = Uri.parse(uriText);

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(uri);
        startActivity(Intent.createChooser(emailIntent, "Send email"));
    }

    public void onClick(View v) {
        /*
        // Create an instance of CognitoCachingCredentialsProvider
        CognitoCachingCredentialsProvider cognitoProvider = new CognitoCachingCredentialsProvider(
                this.getApplicationContext(), "cn-north-1:44edb8ae-67c1-40de-b70d-ae9db5581e6e", Regions.CN_NORTH_1);

        // Create LambdaInvokerFactory, to be used to instantiate the Lambda proxy.
        LambdaInvokerFactory factory = new LambdaInvokerFactory(this.getApplicationContext(),
                Regions.CN_NORTH_1, cognitoProvider);

        // Create the Lambda proxy object with a default Json data binder.
        // You can provide your own data binder by implementing
        // LambdaDataBinder.
        final TrustAddressGenerator AWSLambdaInterface = factory.build(TrustAddressGenerator.class);

        com.alphawallet.token.tools.TrustAddressGenerator.Request request = new com.alphawallet.token.tools.TrustAddressGenerator.Request("0x63cCEF733a093E5Bd773b41C96D3eCE361464942", "z+I6NxdALVtlc3TuUo2QEeV9rwyAmKB4UtQWkTLQhpE=");
        // The Lambda function invocation results in a network call.
        // Make sure it is not called from the main thread.
        new AsyncTask<com.alphawallet.token.tools.TrustAddressGenerator.Request, Void, com.alphawallet.token.tools.TrustAddressGenerator.Response>() {
            @Override
            protected com.alphawallet.token.tools.TrustAddressGenerator.Response doInBackground(com.alphawallet.token.tools.TrustAddressGenerator.Request... params) {
                // invoke the lambda method. In case it fails, it will throw a
                // LambdaFunctionException.
                try {
                    return AWSLambdaInterface.DeriveTrustAddress(params[0]);
                } catch (LambdaFunctionException lfe) {
                    // please don't ignore such exception in production code!!
                    Log.e("Tag", "Failed to invoke AWS Lambda" + lfe.getDetails(), lfe);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(com.alphawallet.token.tools.TrustAddressGenerator.Response response) {
                if (response == null) {
                    return;
                }

                // Do a toast
                Toast.makeText(HelpActivity.this, response.getTrustAddress(), Toast.LENGTH_LONG).show();
            }
        }.execute(request);*/
    }

    private boolean isRawResource(@RawRes int rawRes) {
        try {
            InputStream in = getResources().openRawResource(rawRes);
            if (in.available() > 0) {
                in.close();
                return true;
            }
            in.close();
        } catch (Exception ex) {
            Timber.tag("READ_JS_TAG").d(ex, "Ex");
        }

        return false;
    }
}
