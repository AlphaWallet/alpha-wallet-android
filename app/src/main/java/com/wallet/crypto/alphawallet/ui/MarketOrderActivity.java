package com.wallet.crypto.alphawallet.ui;

import android.app.ActionBar;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.wallet.crypto.alphawallet.R;
import com.wallet.crypto.alphawallet.entity.Ticket;
import com.wallet.crypto.alphawallet.ui.barcode.BarcodeCaptureActivity;
import com.wallet.crypto.alphawallet.ui.widget.adapter.TicketAdapter;
import com.wallet.crypto.alphawallet.ui.widget.adapter.TicketSaleAdapter;
import com.wallet.crypto.alphawallet.ui.widget.entity.TicketRange;
import com.wallet.crypto.alphawallet.util.BalanceUtils;
import com.wallet.crypto.alphawallet.util.KeyboardUtils;
import com.wallet.crypto.alphawallet.util.QRURLParser;
import com.wallet.crypto.alphawallet.viewmodel.BaseViewModel;
import com.wallet.crypto.alphawallet.viewmodel.MarketOrderViewModel;
import com.wallet.crypto.alphawallet.viewmodel.MarketOrderViewModelFactory;
import com.wallet.crypto.alphawallet.widget.ProgressView;
import com.wallet.crypto.alphawallet.widget.SystemView;

import org.ethereum.geth.Address;

import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static com.wallet.crypto.alphawallet.C.Key.TICKET;
import static com.wallet.crypto.alphawallet.C.Key.TICKET_RANGE;

/**
 * Created by James on 5/02/2018.
 */

public class MarketOrderActivity extends BaseActivity
{
    @Inject
    protected MarketOrderViewModelFactory ticketTransferViewModelFactory;
    protected MarketOrderViewModel viewModel;
    private SystemView systemView;
    private ProgressView progressView;

    public TextView name;
    public TextView ids;
    public TextView selected;

    private String address;
    private Ticket ticket;
    private TicketRange ticketRange;
    private TicketSaleAdapter adapter;

    private EditText idsText;
    private TextInputLayout amountInputLayout;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        ticket = getIntent().getParcelableExtra(TICKET);
        if (getIntent().hasExtra(TICKET_RANGE))
        {
            ticketRange = getIntent().getParcelableExtra(TICKET_RANGE);
            setupMarketRange();
        }
        else
        {
            setupMarketOrder();
        }

        toolbar();

        address = ticket.tokenInfo.address;

        setTitle(getString(R.string.market_queue_title));

        systemView = findViewById(R.id.system_view);
        systemView.hide();

        progressView = findViewById(R.id.progress_view);
        progressView.hide();

        viewModel = ViewModelProviders.of(this, ticketTransferViewModelFactory)
                .get(MarketOrderViewModel.class);

        viewModel.ticket().observe(this, this::onTicket);
        viewModel.selection().observe(this, this::onSelected);
        viewModel.progress().observe(this, systemView::showProgress);
        viewModel.queueProgress().observe(this, progressView::updateProgress);
        viewModel.pushToast().observe(this, this::displayToast);
    }

    private void onTicket(Ticket ticket) {
        if (ticketRange == null)
        {

        }
        else
        {
            name.setText(ticket.getFullName());
            ids.setText(ticket.getStringBalance());
        }
    }

    private void setupMarketOrder()
    {
        ticketRange = null;
        setContentView(R.layout.activity_use_token);

        RecyclerView list = findViewById(R.id.listTickets);
        LinearLayout buttons = findViewById(R.id.layoutButtons);
        buttons.setVisibility(View.GONE);

        RelativeLayout rLL = findViewById(R.id.contract_address_layout);
        rLL.setVisibility(View.GONE);

        adapter = new TicketSaleAdapter(this::onTicketIdClick, ticket);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
    }

    private void setupMarketRange()
    {
        setContentView(R.layout.activity_market_queue);

        name = findViewById(R.id.textViewName);
        ids = findViewById(R.id.textViewIDs);
        idsText = findViewById(R.id.send_ids);
        selected = findViewById(R.id.textViewSelection);
        amountInputLayout = findViewById(R.id.amount_input_layout);

        name.setText(address);
        ids.setText("...");

        idsText.setImeActionLabel("Done", KeyEvent.KEYCODE_ENTER);

        idsText.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                final String balanceArray = idsText.getText().toString();
                //convert to an index array
                viewModel.newBalanceArray(balanceArray);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        idsText.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent)
            {
                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN)
                {
                    if (keyEvent.getKeyCode() == keyEvent.KEYCODE_ENTER)
                    {
                        final String balanceArray = idsText.getText().toString();
                        viewModel.generateNewSelection(balanceArray);
                    }
                }

                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.send_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_next: {
                onNext();
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.prepare(address);
    }

    private void onNext() {
        // Validate input fields
        boolean inputValid = true;

        final String amount = idsText.getText().toString();
        List<Integer> idSendList = viewModel.ticket().getValue().parseIndexList(amount);

        if (idSendList == null || idSendList.isEmpty()) {
            amountInputLayout.setError(getString(R.string.error_invalid_amount));
            inputValid = false;
        }

        if (!inputValid) {
            return;
        }

        List<Integer> actualIds = viewModel.ticket().getValue().parseIDListInteger(amount);
        String indexList = viewModel.ticket().getValue().populateIDs(idSendList, true);
        amountInputLayout.setErrorEnabled(false);

        if (actualIds != null && actualIds.size() > 0)
        {
            //let's try to generate a market order
            viewModel.generateMarketOrders(idSendList, actualIds.get(0));
        }

        //kill keyboard
        KeyboardUtils.hideKeyboard(idsText);
        //InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        //imm.hideSoftInputFromWindow(idsText.getWindowToken(), 0);
        //viewModel.openConfirmation(this, to, indexList, amount);
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
