package io.awallet.crypto.alphawallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.web3j.abi.datatypes.generated.Bytes32;

import io.awallet.crypto.alphawallet.R;
import io.awallet.crypto.alphawallet.entity.Ticket;
import io.awallet.crypto.alphawallet.ui.widget.adapter.TicketSaleAdapter;
import io.awallet.crypto.alphawallet.ui.widget.entity.TicketRange;
import io.awallet.crypto.alphawallet.util.BalanceUtils;
import io.awallet.crypto.alphawallet.viewmodel.TransferTicketViewModel;
import io.awallet.crypto.alphawallet.viewmodel.TransferTicketViewModelFactory;
import io.awallet.crypto.alphawallet.widget.ProgressView;
import io.awallet.crypto.alphawallet.widget.SystemView;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static io.awallet.crypto.alphawallet.C.Key.TICKET;

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

    public TextView ids;
    public TextView selected;

    private Ticket ticket;
    private TicketSaleAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        ticket = getIntent().getParcelableExtra(TICKET);
        setupSalesOrder();

        toolbar();

        setTitle(getString(R.string.empty));

        systemView = findViewById(R.id.system_view);
        systemView.hide();

        progressView = findViewById(R.id.progress_view);
        progressView.hide();

        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(TransferTicketViewModel.class);

        viewModel.progress().observe(this, systemView::showProgress);
        viewModel.queueProgress().observe(this, progressView::updateProgress);
        viewModel.pushToast().observe(this, this::displayToast);

        Button nextButton = findViewById(R.id.button_next);
        nextButton.setOnClickListener(v -> {
            onNext();
        });
    }

    private void setupSalesOrder()
    {
        setContentView(R.layout.activity_transfer_ticket_select);

        RecyclerView list = findViewById(R.id.listTickets);

        adapter = new TicketSaleAdapter(this, this::onTicketIdClick, ticket);
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

            String idListStr = ticket.ticketIdToString(idList, false); //list of B32 ID's
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
