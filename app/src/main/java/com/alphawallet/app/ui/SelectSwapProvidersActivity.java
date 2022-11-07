package com.alphawallet.app.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.ui.widget.adapter.SwapProviderAdapter;
import com.alphawallet.app.viewmodel.SelectSwapProvidersViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SelectSwapProvidersActivity extends BaseActivity
{
    private SelectSwapProvidersViewModel viewModel;
    private SwapProviderAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.basic_list_activity);

        toolbar();

        setTitle(getString(R.string.title_select_exchanges));

        initViewModel();

        initViews();
    }

    private void initViewModel()
    {
        viewModel = new ViewModelProvider(this)
                .get(SelectSwapProvidersViewModel.class);
    }

    private void initViews()
    {
        RecyclerView recyclerView = findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SwapProviderAdapter(this, viewModel.getSwapProviders());
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onBackPressed()
    {
        if (viewModel.savePreferences(adapter.getExchanges()))
        {
            setResult(RESULT_OK);
            super.onBackPressed();
        }
        else
        {
            Toast.makeText(this, getString(R.string.message_select_one_exchange), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
