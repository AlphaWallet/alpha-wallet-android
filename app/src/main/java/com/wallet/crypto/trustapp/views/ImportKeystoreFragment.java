package com.wallet.crypto.trustapp.views;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.controller.Controller;
import com.wallet.crypto.trustapp.controller.OnTaskCompleted;
import com.wallet.crypto.trustapp.controller.TaskResult;
import com.wallet.crypto.trustapp.controller.TaskStatus;

import static android.app.Activity.RESULT_OK;

/**
 * Created by marat on 11/23/17.
 */

public class ImportKeystoreFragment extends Fragment {
    public ImportKeystoreFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.import_keystore, container, false);

        final Controller mController = Controller.with(getContext());

        final EditText mKeystore = rootView.findViewById(R.id.import_keystore);
        final EditText mPassword = rootView.findViewById(R.id.import_password);

        Button mImportButton = rootView.findViewById(R.id.import_account_button);
        mImportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mController.clickImport(
                        mKeystore.getText().toString(),
                        mPassword.getText().toString(),
                        new OnTaskCompleted() {
                            @Override
                            public void onTaskCompleted(final TaskResult result) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (result.getStatus() == TaskStatus.SUCCESS) {
                                            getActivity().setResult(RESULT_OK);
                                            getActivity().finish();
                                        } else {
                                            Toast.makeText(getActivity(), result.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }
                        }
                );
            }
        });


        return rootView;
    }
}
