package com.wallet.crypto.trustapp.views;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.controller.Controller;
import com.wallet.crypto.trustapp.controller.EthplorerService;
import com.wallet.crypto.trustapp.model.EPAddressInfo;
import com.wallet.crypto.trustapp.model.EPToken;
import com.wallet.crypto.trustapp.model.EPTokenInfo;
import com.wallet.crypto.trustapp.util.LogInterceptor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TokenListActivity extends AppCompatActivity {

    private static String TAG = "TOKENS";
    private String mAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_token_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mAddress = getIntent().getStringExtra(Controller.KEY_ADDRESS);

        RecyclerView recyclerView = findViewById(R.id.token_list);

        setupRecyclerView(recyclerView);
    }

    private void setupRecyclerView(final @NonNull RecyclerView recyclerView) {
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new LogInterceptor())
                    .build();
            Retrofit retrofit = new Retrofit.Builder()
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .baseUrl("https://api.ethplorer.io")
                    .build();

            EthplorerService service = retrofit.create(EthplorerService.class);

            Call<EPAddressInfo> call = service.getAddressInfo(/*mAddress*/"0x60f7a1cbc59470b74b1df20b133700ec381f15d3", "freekey");

            call.enqueue(new Callback<EPAddressInfo>() {

                @Override
                public void onResponse(@NonNull Call<EPAddressInfo> call, @NonNull Response<EPAddressInfo> response) {
                    EPAddressInfo body = response.body();
                    if (body != null && body.getTokens() != null && body.getTokens().size() > 0) {
                        EPAddressInfo addressInfo = response.body();
                        recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter(addressInfo.getTokens()));
                    } else {
                        Toast.makeText(getApplicationContext(), "Tokens not found.", Toast.LENGTH_SHORT)
                                .show();
                    }
                }

                @Override
                public void onFailure(Call<EPAddressInfo> call, Throwable t) {
                    Log.e("ERROR", t.toString());
                    Toast.makeText(getApplicationContext(), "Error contacting token service. Check internet connection.", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final List<EPToken> mValues;

        public SimpleItemRecyclerViewAdapter(List<EPToken> items) {
            mValues = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.token_list_content, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mItem = mValues.get(position);

            EPToken token = holder.mItem;
            final EPTokenInfo info = token.getTokenInfo();

            try {
                holder.mNameView.setText(info.getName());
                holder.mSymbolView.setText(info.getSymbol());

                BigDecimal balance = new BigDecimal(token.getBalance());
                BigDecimal decimalDivisor = new BigDecimal(Math.pow(10, info.getDecimals()));
                balance = info.getDecimals() > 0 ? balance.divide(decimalDivisor) : balance;
                balance = balance.setScale(2, RoundingMode.HALF_UP);
                holder.mBalanceView.setText(balance.toString());

                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Context context = v.getContext();
                        Intent intent = new Intent(context, SendActivity.class);
                        intent.putExtra(SendActivity.EXTRA_SENDING_TOKENS, true);
                        intent.putExtra(SendActivity.EXTRA_CONTRACT_ADDRESS, info.getAddress());
                        intent.putExtra(SendActivity.EXTRA_SYMBOL, info.getSymbol());
                        intent.putExtra(SendActivity.EXTRA_DECIMALS, info.getDecimals());

                        context.startActivity(intent);
                    }
                });
            } catch (Exception e) {
                holder.mNameView.setText("N/A");
                holder.mSymbolView.setText("N/A");
                holder.mBalanceView.setText("-");
            }
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final TextView mNameView;
            public final TextView mSymbolView;
            public final TextView mBalanceView;

            public EPToken mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mNameView = view.findViewById(R.id.name);
                mSymbolView = view.findViewById(R.id.symbol);
                mBalanceView = view.findViewById(R.id.balance);
            }

            @Override
            public String toString() {
                return super.toString();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
