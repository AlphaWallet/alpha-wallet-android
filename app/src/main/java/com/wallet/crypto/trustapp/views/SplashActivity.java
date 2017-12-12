package com.wallet.crypto.trustapp.views;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Start home activity

        startActivity(new Intent(this, TransactionListActivity.class));

        // close splash activity

        finish();

    }

}
