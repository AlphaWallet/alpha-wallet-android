package com.wallet.crypto.alphawallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wallet.crypto.alphawallet.R;
import com.wallet.crypto.alphawallet.entity.HelpItem;
import com.wallet.crypto.alphawallet.ui.widget.adapter.HelpAdapter;
import com.wallet.crypto.alphawallet.viewmodel.HelpViewModel;
import com.wallet.crypto.alphawallet.viewmodel.HelpViewModelFactory;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;

public class HelpFragment extends Fragment implements View.OnClickListener {
    @Inject
    HelpViewModelFactory helpViewModelFactory;
    private HelpViewModel viewModel;

    private HelpAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        AndroidSupportInjection.inject(this);
        View view = inflater.inflate(R.layout.fragment_help, container, false);
        viewModel = ViewModelProviders.of(this, helpViewModelFactory).get(HelpViewModel.class);

        RecyclerView list = view.findViewById(R.id.list_help);
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new HelpAdapter();

        /* Placeholder only */
        int[] questions = {
                R.string.help_question1,
                R.string.help_question2,
                R.string.help_question3,
                R.string.help_question4,
                R.string.help_question5,
        };

        int[] answers = {
                R.string.help_answer1,
                R.string.help_answer1,
                R.string.help_answer1,
                R.string.help_answer1,
                R.string.help_answer1,
        };

        List<HelpItem> helpItems = new ArrayList<>();
        for (int i = 0; i < questions.length; i++) {
            helpItems.add(new HelpItem(getString(questions[i]), getString(answers[i])));
        }
        adapter.setHelpItems(helpItems);

        list.setAdapter(adapter);

        return view;
    }

    @Override
    public void onClick(View v) {

    }
}
