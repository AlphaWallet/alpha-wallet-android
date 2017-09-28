package com.example.marat.wal;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.marat.wal.controller.Controller;
import com.example.marat.wal.model.VMAccount;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView mTextMessage;
    private Controller controller;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    mTextMessage.setText(R.string.title_home);
                    return true;
                case R.id.navigation_import:
                    mTextMessage.setText(R.string.title_import);
                    Intent intent = new Intent(MainActivity.this, ItemListActivity.class);
                    startActivity(intent);
                    return true;
                case R.id.navigation_notifications:
                    mTextMessage.setText(R.string.title_notifications);
                    return true;
            }
            return false;
        }

    };

    public void navigate(View view) {
        //TODO: Start wallet detail page with selected wallet id
        Intent intent = new Intent(MainActivity.this, ItemListActivity.class);
        startActivity(intent);
    }

    /*public void showBalance(View view) {
        controller.showBalance(view);
    }

    public void createAccount(View view) {
        controller.createAccount(view);
    }

    public void importAccount(View view) {
        controller.importAccount(view);
    }

    public void exportAccount(View view) {
        controller.exportAccount(view);
    }

    public void sendTransaction(View view) {
        controller.sendTransaction(view);
    }

    public void getTransactions(View view) {
        controller.getTransactions(view);
    }

    public void getAccounts(View view) {
        controller.getAccounts(view);
    }
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);*/

        controller = new Controller(this);

        LinearLayout linear_layout = (LinearLayout) findViewById(R.id.linear_layout);

        List<VMAccount> accounts = controller.getAccounts();
        for (VMAccount acc : accounts) {
            Button b = new Button(this);
            b.setText(acc.getAddress());
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    navigate(v);
                }
            });
            linear_layout.addView(b);
        }

        Log.d("INFO", "MainActivity.onCreate");
    }

}
