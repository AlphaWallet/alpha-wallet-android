package com.wallet.crypto.trustapp.views;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.omadahealth.lollipin.lib.PinCompatActivity;
import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.controller.Controller;
import com.wallet.crypto.trustapp.controller.ServiceErrorException;
import com.wallet.crypto.trustapp.util.PasswordStoreFactory;

public class ExportAccountActivity extends PinCompatActivity implements View.OnClickListener {

	public static final String ADDRESS_KEY = "account_address";
	public static final int SHARE_REQUEST_CODE = 1;

	private static final int MIN_PASSWORD_LENGTH = 1;

	public static void open(Context context, String accountAddress) {
    	Intent intent = new Intent(context, ExportAccountActivity.class);
    	intent.putExtra(ADDRESS_KEY, accountAddress);
    	context.startActivity(intent);
    }

    private String accountAddress;

    private EditText passwordTxt;
    private EditText confirmPasswordTxt;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_export_account);

		accountAddress = getIntent().getStringExtra(ADDRESS_KEY);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getString(R.string.activity_title_backup, accountAddress.substring(0, 5)));
        }

        passwordTxt = findViewById(R.id.password);
        confirmPasswordTxt = findViewById(R.id.confirm_password);
        confirmPasswordTxt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
	        @Override
	        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		        return false;
	        }
        });
		findViewById(R.id.export_account_button).setOnClickListener(this);
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

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == SHARE_REQUEST_CODE) {
			if (resultCode == RESULT_OK) {
				setResult(RESULT_OK);
				finish();
			} else {
				new AlertDialog.Builder(this)
						.setMessage(R.string.do_manage_make_backup)
						.setPositiveButton(R.string.yes_continue, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								setResult(RESULT_OK);
								finish();
							}
						})
						.setNegativeButton(R.string.no_repeat, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								onExport();
							}
						})
						.show();
			}
		} else if (requestCode == Controller.UNLOCK_SCREEN_REQUEST) {
			if (resultCode == RESULT_OK) {
				onExport();
			} else {
				showError(getString(R.string.unable_unblock_device));
			}
		}
	}

	@Override
	public void onClick(View v) {
		onExport();
	}

	public void onExport() {
		final String password = passwordTxt.getText().toString();
		final String passwordConfirm = confirmPasswordTxt.getText().toString();
		if (isPasswordValid(password, passwordConfirm)) {
			try {
				String jsonData = Controller.with(this).exportAccount(accountAddress, password);
				if (!TextUtils.isEmpty(jsonData)) {
					openShareDialog(jsonData);
				} else {
					showError("Unable to export");
				}
			} catch (ServiceErrorException e) {
				if (e.code == ServiceErrorException.USER_NOT_AUTHENTICATED) {
					PasswordStoreFactory.showAuthenticationScreen(ExportAccountActivity.this, Controller.UNLOCK_SCREEN_REQUEST);
				} else {
					showError("Failed to export account " + e.getMessage());
				}
			}
		}
	}

	private void openShareDialog(String jsonData) {
		Intent sharingIntent = new Intent(Intent.ACTION_SEND);
		sharingIntent.setType("text/plain");
		sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Keystore");
		sharingIntent.putExtra(Intent.EXTRA_TEXT, jsonData);
		startActivityForResult(
				Intent.createChooser(sharingIntent, "Share via"),
				SHARE_REQUEST_CODE);
	}

	private void showError(String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	private boolean isPasswordValid(String password, String passwordConfirm) {
    	boolean isValid = true;
		if (password.length() < MIN_PASSWORD_LENGTH) {
			passwordTxt.setError(String.format(getString(R.string.min_pwd_length), MIN_PASSWORD_LENGTH));
			isValid = false;
		}


		if (!password.equals(passwordConfirm)) {
			confirmPasswordTxt.setError(getString(R.string.error_passwords_must_match));
			isValid = false;
		}
		return isValid;
	}
}
