package io.stormbird.wallet.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.HelpItem;
import io.stormbird.wallet.ui.widget.adapter.HelpAdapter;
import io.stormbird.wallet.viewmodel.HelpViewModel;
import io.stormbird.wallet.viewmodel.HelpViewModelFactory;

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

        TextView textForceCrash = findViewById(R.id.test_crash);
        textForceCrash.setOnClickListener(v -> {
            Crashlytics.log("Test Crash Log");
            Crashlytics.getInstance().crash();
        });
    }

    private void helpIntent()
    {
        String uriText =
                "mailto:support@awallet.io" +
                        "?subject=" + Uri.encode("Hi guys") +
                        "&body=" + Uri.encode("");

        Uri uri = Uri.parse(uriText);

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(uri);
        startActivity(Intent.createChooser(emailIntent, "Send email"));
    }
}
