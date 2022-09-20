package com.alphawallet.app.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.ui.widget.adapter.ExchangeAdapter;
import com.alphawallet.app.viewmodel.SelectExchangesViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SelectExchangesActivity extends BaseActivity
{
    private SelectExchangesViewModel viewModel;
    private ExchangeAdapter adapter;

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
        RecyclerView recyclerView = findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExchangeAdapter(viewModel.getTools(this));
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
