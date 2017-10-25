package com.wallet.crypto.trustapp.views;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SplashActivity extends AppCompatActivity {

    @Override

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Start home activity

        startActivity(new Intent(SplashActivity.this, TransactionListActivity.class));

        // close splash activity

        finish();

    }

}
