package io.stormbird.wallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.ERC721Token;
import io.stormbird.wallet.entity.FinishReceiver;
import io.stormbird.wallet.entity.Ticket;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.ui.widget.OnTokenClickListener;
import io.stormbird.wallet.ui.widget.adapter.TicketSaleAdapter;
import io.stormbird.wallet.util.BalanceUtils;
import io.stormbird.wallet.viewmodel.TransferTicketViewModel;
import io.stormbird.wallet.viewmodel.TransferTicketViewModelFactory;
import io.stormbird.wallet.widget.ProgressView;
import io.stormbird.wallet.widget.SystemView;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.stormbird.token.entity.TicketRange;

import static io.stormbird.wallet.C.Key.TICKET;

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
    private TicketSaleAdapter adapter;

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

        viewModel = ViewModelProviders.of(this, viewModelFactory)
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

        adapter = new TicketSaleAdapter(this, token, viewModel.getAssetDefinitionService());
        adapter.setTransferTicket(token);
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
        // Validate input fields
        boolean inputValid = true;
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
        List<String> transferToken = adapter.getERC721Checked();
        if (!transferToken.isEmpty())
        {
            //take user to ERC721 transfer page
            viewModel.openTransferDirectDialog(this, transferToken.get(0));
        }
    }

    private void handleTransferERC875(Token token)
    {
        List<TicketRange> sellRange = adapter.getCheckedItems();

        if (!sellRange.isEmpty()) {
            //add this range to the sell order confirmation
            //Generate list of indices and actual ids
            List<BigInteger> idList = new ArrayList<>();
            for (TicketRange tr : sellRange)
            {
                idList.addAll(tr.tokenIds);
            }

            String idListStr = token.intArrayToString(idList, false); //list of B32 ID's
            viewModel.openSellDialog(this, idListStr);
        }
    }

    boolean isValidAmount(String eth) {
        try {
            String wei = BalanceUtils.EthToWei(eth);
            return wei != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void onSelected(String selectionStr)
    {
        selected.setText(selectionStr);
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
