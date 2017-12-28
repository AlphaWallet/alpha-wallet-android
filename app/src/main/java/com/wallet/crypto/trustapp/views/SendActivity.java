package com.wallet.crypto.trustapp.views;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.omadahealth.lollipin.lib.PinCompatActivity;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.controller.Controller;
import com.wallet.crypto.trustapp.controller.EtherStore;
import com.wallet.crypto.trustapp.controller.OnTaskCompleted;
import com.wallet.crypto.trustapp.controller.ServiceErrorException;
import com.wallet.crypto.trustapp.controller.TaskResult;
import com.wallet.crypto.trustapp.controller.TaskStatus;
import com.wallet.crypto.trustapp.util.QRURLParser;
import com.wallet.crypto.trustapp.util.PasswordStoreFactory;
import com.wallet.crypto.trustapp.views.barcode.BarcodeCaptureActivity;

import org.ethereum.geth.Address;

import java.math.BigInteger;

public class SendActivity extends PinCompatActivity {

    private Controller mController;

    private static final String LOG_TAG = SendActivity.class.getSimpleName();
    private static final int BARCODE_READER_REQUEST_CODE = 1;

    public static final String EXTRA_SENDING_TOKENS = "extra_sending_tokens";
    public static final String EXTRA_CONTRACT_ADDRESS = "extra_contract_address";
    public static final String EXTRA_SYMBOL = "extra_symbol";
    public static final String EXTRA_DECIMALS = "extra_decimals";

    private final Handler handler = new Handler();

    private EditText mTo;
    private EditText mAmount;
    private SeekBar mGasLimit;
    private SeekBar mGasPrice;

    private int gasLimitSelected;
    private int gasPriceSelected; //Gwei
    private static long weiInGwei = 1000000000;

    private TextView mResultTextView;

	private AlertDialog dialog;
	private boolean mSendingTokens = false;
	private String mContractAddress;
	private String mSymbol;
	private int mDecimals;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
	    mController = Controller.with(this);
	    mTo = findViewById(R.id.date);
        mAmount = findViewById(R.id.amount);
        String toAddress = getIntent().getStringExtra(Controller.KEY_ADDRESS);
        if (toAddress != null) {
            mTo.setText(toAddress);
        }
        mContractAddress = getIntent().getStringExtra(EXTRA_CONTRACT_ADDRESS);
        mDecimals = getIntent().getIntExtra(EXTRA_DECIMALS, -1);
        mSymbol = getIntent().getStringExtra(EXTRA_SYMBOL);
        mSendingTokens = getIntent().getBooleanExtra(EXTRA_SENDING_TOKENS, false);

        assert(!mSendingTokens || (mSendingTokens && mDecimals > -1 && mContractAddress != null));

        if (mSendingTokens && mSymbol != null) {
            mAmount.setHint(mSymbol + " amount");
        } else {
            mAmount.setHint(mController.getCurrentNetwork().getSymbol() + " amount");
        }

        final TextView gasLimitText = findViewById(R.id.gas_limit_text);
        mGasLimit = findViewById(R.id.gas_limit_slider);
        mGasLimit.setMax(EtherStore.getMaxGasLimit() - EtherStore.getMinGasLimit());
        if (mSendingTokens) {
            gasLimitSelected = EtherStore.getTokenGasLimit();
        } else {
            gasLimitSelected = EtherStore.getDefaultGasLimit();
        }
        mGasLimit.setProgress(gasLimitSelected - EtherStore.getMinGasLimit());
        mGasLimit.refreshDrawableState();
        gasLimitText.setText("" + (gasLimitSelected));
        mGasLimit.setOnSeekBarChangeListener(
            new SeekBar.OnSeekBarChangeListener() {
                 @Override
                 public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                     progress = progress / 100;
                     progress = progress * 100;
                     gasLimitSelected = progress + EtherStore.getMinGasLimit();
                     gasLimitText.setText("" + (gasLimitSelected));
                 }

                 @Override
                 public void onStartTrackingTouch(SeekBar seekBar) {
                 }

                 @Override
                 public void onStopTrackingTouch(SeekBar seekBar) {
                 }
        });

        final TextView gasPriceText = findViewById(R.id.gas_price_text);
        final int minGasPrice = (int)(EtherStore.getMinGasPrice()/weiInGwei);
        mGasPrice = findViewById(R.id.gas_price_slider);
        mGasPrice.setMax((int)(EtherStore.getMaxGasFee() / EtherStore.getMaxGasLimit() / weiInGwei - minGasPrice));
        gasPriceSelected = (int)(EtherStore.getDefaultGasPrice() / weiInGwei);
        mGasPrice.setProgress((int)(EtherStore.getDefaultGasPrice()/weiInGwei - minGasPrice));
        gasPriceText.setText("" + (gasPriceSelected));
        mGasPrice.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        gasPriceSelected = progress + minGasPrice;
                        gasPriceText.setText("" + (gasPriceSelected));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });

        Button mSendButton = findViewById(R.id.send_button);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSendClick();
            }
        });

        mResultTextView = findViewById(R.id.result_textview);
        ImageButton scanBarcodeButton = findViewById(R.id.scan_barcode_button);
        scanBarcodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), BarcodeCaptureActivity.class);
                startActivityForResult(intent, BARCODE_READER_REQUEST_CODE);
            }
        });
    }

	@Override
	protected void onPause() {
		super.onPause();

		hideSendProgress();
	}

	private void onSendClick() {
		// Validate input fields
		boolean inputValid = true;
		final String to = mTo.getText().toString();
		if (!isAddressValid(to)) {
			mTo.setError("Invalid address");
			inputValid = false;
		}
		final String amount = mAmount.getText().toString();
		if (!isValidEthAmount(amount)) {
			mAmount.setError("Invalid amount");
			inputValid = false;
		}

        if (!isValidGasLimit(gasLimitSelected)) {
            Toast.makeText(SendActivity.this, "Invalid gas limit: " + gasLimitSelected, Toast.LENGTH_LONG).show();
            inputValid = false;
        }

        if (!isValidGasPrice(gasPriceSelected)) {
            Toast.makeText(SendActivity.this, "Invalid gas price: " + gasPriceSelected, Toast.LENGTH_LONG).show();
            inputValid = false;
        }

        if (!isValidGasFee(Integer.toString(gasLimitSelected), Long.toString(gasPriceSelected * weiInGwei))) {
            Toast.makeText(SendActivity.this, "Gas fee (limit*price) is invalid", Toast.LENGTH_LONG).show();
            inputValid = false;
        }

		if (!inputValid) {
			return;
		}

        showSendProgress();
		try {
			if (mSendingTokens) {
				mController.clickSendTokens(
						mController.getCurrentAccount().getAddress(),
						mTo.getText().toString(),
						mContractAddress,
						mAmount.getText().toString(),
                        Integer.toString(gasLimitSelected),
                        Long.toString(gasPriceSelected * weiInGwei),
						mDecimals,
						onSendCompleteListener);
			} else {
				mController.clickSend(
						mController.getCurrentAccount().getAddress(),
						mTo.getText().toString(),
						mAmount.getText().toString(),
                        Integer.toString(gasLimitSelected),
                        Long.toString(gasPriceSelected * weiInGwei),
						onSendCompleteListener);
			}
		} catch (ServiceErrorException ex) {
			hideSendProgress();
			if (ex.code == ServiceErrorException.USER_NOT_AUTHENTICATED) {
				PasswordStoreFactory.showAuthenticationScreen(this, Controller.UNLOCK_SCREEN_REQUEST);
			}
		}
	}

	boolean isAddressValid(String address) {
        try {
            new Address(address);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    boolean isValidEthAmount(String eth) {
        try {
            String wei = Controller.EthToWei(eth);
            return wei != null;
        } catch (Exception e) {
            return false;
        }
    }

    boolean isValidGasLimit(int l) {
        try {
	    // Though no bound exists on gas price, it has to be at least within the bounds of the fee
	    return l >= EtherStore.getMinGasLimit() && l <= EtherStore.getMaxGasLimit();
        } catch (Exception e) {
	        return false;
        }
    }

    boolean isValidGasPrice(int price) {
        try {
            return price*weiInGwei >= EtherStore.getMinGasPrice() && price*weiInGwei <= EtherStore.getMaxGasFee();
        } catch (Exception e) {
            return false;
        }
    }

    boolean isValidGasFee(String limit, String price) {
        try {
            BigInteger biLimit = new BigInteger(limit);
            BigInteger biPrice = new BigInteger(price);
            BigInteger fee = biLimit.multiply(biPrice);

            BigInteger minFee = BigInteger.valueOf(EtherStore.getMinGasFee());
            BigInteger maxFee = BigInteger.valueOf(EtherStore.getMaxGasFee());

            return fee.compareTo(minFee) == 1 && fee.compareTo(maxFee) == -1;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BARCODE_READER_REQUEST_CODE) {
	        if (resultCode == CommonStatusCodes.SUCCESS) {
		        if (data != null) {
			        Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);

			        String extracted_address = QRURLParser.getInstance().extractAddressFromQrString(barcode.displayValue);
			        if (extracted_address == null) {
				        Toast.makeText(this, "QR code doesn't contain account address", Toast.LENGTH_SHORT).show();
				        return;
			        }
			        Point[] p = barcode.cornerPoints;
			        mTo.setText(extracted_address);
		        } else {
			        mResultTextView.setText(R.string.no_barcode_captured);
		        }
	        } else {
		        Log.e(LOG_TAG, String.format(getString(R.string.barcode_error_format),
				        CommonStatusCodes.getStatusCodeString(resultCode)));
	        }
        } else if (requestCode == Controller.UNLOCK_SCREEN_REQUEST) {
        	if (resultCode == RESULT_OK) {
		        onSendClick();
	        }
        } else {
        	super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void showSendProgress() {
    	dialog = new AlertDialog.Builder(this)
			    .setView(R.layout.layout_send_progress)
			    .create();
    	dialog.show();
    }

    private void hideSendProgress() {
		if (dialog != null && dialog.isShowing()) {
			dialog.dismiss();
			dialog = null;
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

	private final OnTaskCompleted onSendCompleteListener = new OnTaskCompleted() {
	    @Override
	    public void onTaskCompleted(TaskResult result) {
		    handler.post(new OnSendCompleteNotifier(result));
	    }
    };

    private class OnSendCompleteNotifier implements Runnable {

	    private final TaskResult result;

	    OnSendCompleteNotifier(TaskResult result) {
		    this.result = result;
	    }

	    @Override
	    public void run() {
		    hideSendProgress();

            AlertDialog alertDialog = new AlertDialog.Builder(SendActivity.this).create();
            if (result.getStatus() == TaskStatus.SUCCESS) {
                alertDialog.setTitle("Sent");
            } else {
                alertDialog.setTitle("Error");
            }
            alertDialog.setMessage(result.getMessage());
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
	    }
    }
}
