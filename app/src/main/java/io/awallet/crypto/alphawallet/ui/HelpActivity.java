package io.awallet.crypto.alphawallet.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.LinearLayout;

import io.awallet.crypto.alphawallet.R;
import io.awallet.crypto.alphawallet.entity.HelpItem;
import io.awallet.crypto.alphawallet.ui.widget.adapter.HelpAdapter;
import io.awallet.crypto.alphawallet.viewmodel.HelpViewModel;
import io.awallet.crypto.alphawallet.viewmodel.HelpViewModelFactory;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class HelpActivity extends BaseActivity {
    @Inject
    HelpViewModelFactory helpViewModelFactory;
    private HelpViewModel viewModel;
    private Handler handler = new Handler();

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
                //R.string.help_answer1,
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
            contactUs.setBackgroundColor(ContextCompat.getColor(getApplication(), R.color.dark_yellow));
            new RemoveEffect(contactUs);
            helpIntent();
        });
    }

    private class RemoveEffect
    {
        public LinearLayout layout;
        public RemoveEffect(LinearLayout layoutEffect)
        {
            layout = layoutEffect;
            handler.postDelayed(returnButton, 10);
        }

        private final Runnable returnButton = () -> layout.setBackgroundColor(ContextCompat.getColor(getApplication(), R.color.golden_yellow));//  .setBackgroundResource(R.drawable.background_help_dark);
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
