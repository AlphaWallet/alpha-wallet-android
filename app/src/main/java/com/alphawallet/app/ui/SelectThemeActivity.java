package com.alphawallet.app.ui;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.RadioGroup;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.viewmodel.SelectThemeViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SelectThemeActivity extends BaseActivity
{
    private SelectThemeViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_theme);
        initViewModel();
        initViews();
        toolbar();
        setTitle(getString(R.string.title_select_theme));
    }

    private void initViewModel()
    {
        viewModel = new ViewModelProvider(this).get(SelectThemeViewModel.class);
    }

    private void initViews()
    {
        LinearLayout lightThemeLayout = findViewById(R.id.layout_theme_light);
        LinearLayout darkThemeLayout = findViewById(R.id.layout_theme_dark);
        LinearLayout autoThemeLayout = findViewById(R.id.layout_theme_auto);
        RadioGroup radioGroup = findViewById(R.id.radio_group);

        lightThemeLayout.setOnClickListener(v -> radioGroup.check(R.id.radio_theme_light));
        darkThemeLayout.setOnClickListener(v -> radioGroup.check(R.id.radio_theme_dark));
        autoThemeLayout.setOnClickListener(v -> radioGroup.check(R.id.radio_theme_auto));

        int theme = viewModel.getTheme();
        if (theme == C.THEME_LIGHT)
        {
            radioGroup.check(R.id.radio_theme_light);
        }
        else if (theme == C.THEME_DARK)
        {
            radioGroup.check(R.id.radio_theme_dark);
        }
        else
        {
            radioGroup.check(R.id.radio_theme_auto);
        }

        radioGroup.setOnCheckedChangeListener((group, checkedId) ->
        {
            if (checkedId == R.id.radio_theme_light)
            {
                viewModel.setTheme(getApplicationContext(), C.THEME_LIGHT);
            }
            else if (checkedId == R.id.radio_theme_dark)
            {
                viewModel.setTheme(getApplicationContext(), C.THEME_DARK);
            }
            else
            {
                viewModel.setTheme(getApplicationContext(), C.THEME_AUTO);
            }
            finish();
        });
    }
}
