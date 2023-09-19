package com.alphawallet.app.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.R;
import com.alphawallet.app.viewmodel.AnalyticsSettingsViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.switchmaterial.SwitchMaterial;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CrashReportSettingsActivity extends BaseActivity
{
    AnalyticsSettingsViewModel viewModel;
    SwitchMaterial crashReportSwitch;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_crash_report_settings);

        toolbar();

        setTitle(getString(R.string.settings_title_crash_reporting));

        viewModel = new ViewModelProvider(this).get(AnalyticsSettingsViewModel.class);

        initViews();
    }

    private void initViews()
    {
        crashReportSwitch = findViewById(R.id.switch_crash_report);
        crashReportSwitch.setChecked(viewModel.isCrashReportingEnabled());
        crashReportSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.toggleCrashReporting(isChecked);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
//        getMenuInflater().inflate(R.menu.menu_help, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.action_help)
        {
            showHelpUi();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showHelpUi()
    {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setCancelable(true);
        bottomSheetDialog.setCanceledOnTouchOutside(true);
//        bottomSheetDialog.setContentView(contentView);
//        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from((View) contentView.getParent());
//        bottomSheetDialog.setOnShowListener(dialog -> behavior.setPeekHeight(contentView.getHeight()));
        bottomSheetDialog.show();
    }
}
