package com.example.marat.wal.views;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.example.marat.wal.R;
import com.example.marat.wal.controller.Controller;
import com.example.marat.wal.model.VMAccount;

import java.util.ArrayList;
import java.util.List;

public class SendActivity extends AppCompatActivity {

    private Controller mController;

    private Spinner mFromSpinner;
    private EditText mTo;
    private EditText mAmount;
    private EditText mPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);

        mController = Controller.get();

        List<VMAccount> accounts = mController.getAccounts();

        List<String> account_names = new ArrayList<String>();

        for (VMAccount a: accounts) {
            account_names.add(a.getAddress());
        }

        mFromSpinner = (Spinner) findViewById(R.id.transaction_address);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item, account_names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mFromSpinner.setAdapter(adapter);

        mTo = (EditText) findViewById(R.id.date);
        mAmount = (EditText) findViewById(R.id.amount);
        mPassword = (EditText) findViewById(R.id.password);

        Button mSendButton = (Button) findViewById(R.id.send_button);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mController.clickSend(SendActivity.this, mFromSpinner.getSelectedItem().toString(), mTo.getText().toString(), mAmount.getText().toString(), mPassword.getText().toString());
            }
        });
    }
}
