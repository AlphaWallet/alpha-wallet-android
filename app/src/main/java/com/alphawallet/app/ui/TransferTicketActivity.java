package com.alphawallet.app.ui;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.FinishReceiver;
import com.alphawallet.app.entity.tokens.ERC721Token;
import com.alphawallet.app.entity.tokens.Ticket;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.ui.widget.OnTokenClickListener;
import com.alphawallet.app.ui.widget.adapter.NonFungibleTokenAdapter;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.TransactionDetailViewModel;
import com.alphawallet.app.viewmodel.TransferTicketViewModel;
import com.alphawallet.app.viewmodel.TransferTicketViewModelFactory;
import com.alphawallet.app.widget.ProgressView;
import com.alphawallet.app.widget.SystemView;
import com.alphawallet.token.entity.TicketRange;

import java.math.BigInteger;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.alphawallet.app.C.Key.TICKET;

/**
 * Created by James on 13/02/2018.
 */
public class TransferTicketActivity extends BaseActivity implements OnTokenClickListener
{
    @Inject
    protected TransferTicketViewModelFactory viewModelFactory;
    protected TransferTicketViewModel viewModel;
    private SystemView systemView;
    private ProgressView progressView;

    private FinishReceiver finishReceiver;

    public TextView ids;
    public TextView selected;

    private Token token;
    private NonFungibleTokenAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_ticket_select);
        toolbar();
        setTitle("");

        token = getIntent().getParcelableExtra(TICKET);

        systemView = findViewById(R.id.system_view);
        systemView.hide();

        progressView = findViewById(R.id.progress_view);
        progressView.hide();

        viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(TransferTicketViewModel.class);

        viewModel.progress().observe(this, systemView::showProgress);
        viewModel.queueProgress().observe(this, progressView::updateProgress);
        viewModel.pushToast().observe(this, this::displayToast);

        setupSalesOrder();

        Button nextButton = findViewById(R.id.button_next);
        nextButton.setOnClickListener(v -> {
            onNext();
        });

        finishReceiver = new FinishReceiver(this);
    }

    private void setupSalesOrder()
    {
        RecyclerView list = findViewById(R.id.listTickets);

        adapter = new NonFungibleTokenAdapter(this, token, viewModel.getAssetDefinitionService(), null, this);
        adapter.addQuantitySelector();
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.prepare(token);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(finishReceiver);
    }

    private void onNext() {
        //look up all checked fields
        //TODO: Abstract into Token class
        if (token instanceof ERC721Token)
        {
            handleTransferERC721(token);
        }
        else if (token instanceof Ticket)
        {
            handleTransferERC875(token);
        }
    }

    private void handleTransferERC721(Token token)
    {
        TicketRange txRange = adapter.getSelectedRange(token.getArrayBalance());
        viewModel.openTransferDirectDialog(this, txRange.tokenIds.get(0).toString());
    }

    private void handleTransferERC875(Token token)
    {
        TicketRange txRange = adapter.getSelectedRange(token.getArrayBalance());
        String idListStr = Utils.bigIntListToString(txRange.tokenIds, false);
        viewModel.openSellDialog(this, idListStr);
    }

    @Override
    public void onTokenClick(View view, Token token, List<BigInteger> ids, boolean selected) {
        Context context = view.getContext();
        //TODO: what action should be performed when clicking on a range?
    }

    @Override
    public void onLongTokenClick(View view, Token token, List<BigInteger> tokenId)
    {

    }
}
