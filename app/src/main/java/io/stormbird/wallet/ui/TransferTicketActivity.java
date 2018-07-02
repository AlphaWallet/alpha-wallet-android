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
import io.stormbird.wallet.entity.FinishReceiver;
import io.stormbird.wallet.entity.Ticket;
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
public class TransferTicketActivity extends BaseActivity
{
    @Inject
    protected TransferTicketViewModelFactory viewModelFactory;
    protected TransferTicketViewModel viewModel;
    private SystemView systemView;
    private ProgressView progressView;

    private FinishReceiver finishReceiver;

    public TextView ids;
    public TextView selected;

    private Ticket ticket;
    private TicketSaleAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        ticket = getIntent().getParcelableExtra(TICKET);

        toolbar();

        setTitle(getString(R.string.empty));

        setContentView(R.layout.activity_transfer_ticket_select);

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

        adapter = new TicketSaleAdapter(this::onTicketIdClick, ticket, viewModel.getAssetDefinitionService());
        adapter.setTransferTicket(ticket);
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
        viewModel.prepare(ticket);
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
        List<TicketRange> sellRange = adapter.getCheckedItems();

        if (!sellRange.isEmpty()) {
            //add this range to the sell order confirmation
            //Generate list of indicies and actual ids
            List<BigInteger> idList = new ArrayList<>();
            for (TicketRange tr : sellRange)
            {
                idList.addAll(tr.tokenIds);
            }

            String idListStr = ticket.intArrayToString(idList, false); //list of B32 ID's
            List<Integer> idSendList = ticket.ticketIdStringToIndexList(idListStr); //convert string list of b32 to Indexes
            String indexList = ticket.integerListToString(idSendList, true);

            //confirm other address
            //confirmation screen
            //(Context context, String to, String ids, String ticketIDs)
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

    private void onTicketIdClick(View view, TicketRange range) {
        Context context = view.getContext();
        //TODO: what action should be performed when clicking on a range?
    }
}
