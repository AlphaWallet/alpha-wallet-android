package com.alphawallet.app.ui;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.ui.widget.adapter.SwapProviderAdapter;
import com.alphawallet.app.viewmodel.SelectExchangesViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SelectExchangesActivity extends BaseActivity
{
    private SelectExchangesViewModel viewModel;
    private RecyclerView recyclerView;
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
                .get(SelectExchangesViewModel.class);
    }

    private void initViews()
    {
        recyclerView = findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SwapProviderAdapter(this, viewModel.getTools(this));
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onBackPressed()
    {
        viewModel.savePreferences(adapter.getSelectedProviders());
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            this.onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }
}
