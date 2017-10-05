package com.example.marat.wal.views;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.example.marat.wal.R;

/**
 * Created by marat on 10/4/17.
 */

public class DeleteAccountDialogFragment extends DialogFragment {

    private String mAddress;

    public void setAddress(String address) {
        mAddress = address;
    }

    public interface DeleteAccountDialogListener {
        public void onDialogPositiveClick(String address, String password);
    }

    DeleteAccountDialogListener mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        //Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (DeleteAccountDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        final View view = inflater.inflate(R.layout.account_delete_dialog, null);
        TextView addressText = view.findViewById(R.id.address);
        addressText.setText(mAddress);
        builder.setView(view)
                // Add action buttons
                .setPositiveButton(R.string.account_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        TextView address = view.findViewById(R.id.address);
                        EditText pwd = view.findViewById(R.id.password);
                        mListener.onDialogPositiveClick(address.getText().toString(), pwd.getText().toString());
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        DeleteAccountDialogFragment.this.getDialog().cancel();
                    }
                });
        return builder.create();
    }
}

