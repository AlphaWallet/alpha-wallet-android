package com.alphawallet.app.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import android.widget.Toast;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.lambdainvoker.LambdaFunctionException;
import com.amazonaws.mobileconnectors.lambdainvoker.LambdaInvokerFactory;
import com.amazonaws.regions.Regions;
import com.alphawallet.app.ui.widget.adapter.HelpAdapter;
import com.alphawallet.app.util.TrustAddressGenerator;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.HelpItem;
import com.alphawallet.app.viewmodel.HelpViewModel;
import com.alphawallet.app.viewmodel.HelpViewModelFactory;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class HelpActivity extends BaseActivity {
    @Inject
    HelpViewModelFactory helpViewModelFactory;
    private HelpViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_help);
        toolbar();
        setTitle(getString(R.string.toolbar_header_help));

        RecyclerView list = findViewById(R.id.list_help);
        list.setLayoutManager(new LinearLayoutManager(this));
        HelpAdapter adapter = new HelpAdapter();

        /* Placeholder only */
        int[] questions = {
                R.string.help_question1,
                R.string.help_question2,
                R.string.help_question3,
                //R.string.help_question4,
                R.string.help_question5,
        };

        int[] answers = {
                R.string.what_is_eth,
                R.string.why_alphawallet_eth,
                R.string.how_i_get_money,
                R.string.how_i_transfer_into_wallet,
        };

        List<HelpItem> helpItems = new ArrayList<>();
        for (int i = 0; i < questions.length; i++) {
            helpItems.add(new HelpItem(getString(questions[i]), getString(answers[i])));
        }
        adapter.setHelpItems(helpItems);

        list.setAdapter(adapter);

        final LinearLayout contactUs = findViewById(R.id.layout_contact);
        contactUs.setOnClickListener(v -> {
            helpIntent();
        });

//        TextView textForceCrash = findViewById(R.id.test_crash);
//        textForceCrash.setOnClickListener(v -> {
//            Crashlytics.log("Test Crash Log");
//            Crashlytics.getInstance().crash();
//        });
    }

    private void helpIntent()
    {
        final String at = "@";
        String uriText =
                "mailto:feedback+android" + at + "alphawallet.com" +
                        "?subject=" + Uri.encode("Hi guys. I've been using AlphaWallet") +
                        "&body=" + Uri.encode("");

        Uri uri = Uri.parse(uriText);

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(uri);
        startActivity(Intent.createChooser(emailIntent, "Send email"));
    }

    public void onClick(View v) {
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
        }.execute(request);
    }

}
