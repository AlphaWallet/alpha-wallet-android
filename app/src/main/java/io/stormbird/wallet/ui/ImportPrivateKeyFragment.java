package io.stormbird.wallet.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.stormbird.token.tools.Numeric;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.AuthenticationCallback;
import io.stormbird.wallet.ui.widget.OnImportPrivateKeyListener;
import io.stormbird.wallet.widget.InputView;
import io.stormbird.wallet.widget.SignTransactionDialog;

public class ImportPrivateKeyFragment extends Fragment implements View.OnClickListener, AuthenticationCallback
{

    private static final OnImportPrivateKeyListener dummyOnImportPrivateKeyListener = key -> { };

    private InputView privateKey;
    private OnImportPrivateKeyListener onImportPrivateKeyListener = dummyOnImportPrivateKeyListener;
    private SignTransactionDialog signDialog;

    public static ImportPrivateKeyFragment create() {
        return new ImportPrivateKeyFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return LayoutInflater.from(getContext())
                .inflate(R.layout.fragment_import_private_key, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        privateKey = view.findViewById(R.id.input_private_key);
        view.findViewById(R.id.import_action).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        privateKey.setError(null);
        String value = privateKey.getText().toString();

        if (!TextUtils.isEmpty(value))
        {
            signDialog = new SignTransactionDialog(this.getActivity(), 1);
            signDialog.setBigText("Authenticate Credentials");
            signDialog.setSecondaryButtonText(R.string.action_cancel);
            signDialog.setPrimaryButtonText(R.string.dialog_title_sign_message);
            signDialog.show();
            signDialog.getFingerprintAuthorisation(this);
        }

        privateKey.setError(getString(R.string.error_field_required));
    }

    public void setOnImportPrivateKeyListener(OnImportPrivateKeyListener onImportPrivateKeyListener) {
        this.onImportPrivateKeyListener = onImportPrivateKeyListener == null
                ? dummyOnImportPrivateKeyListener
                : onImportPrivateKeyListener;
    }

    @Override
    public void authenticatePass(int callbackId)
    {
        switch (callbackId)
        {
            case 1:
                String value = privateKey.getText().toString();
                break;
            default:
                break;
        }
    }

    @Override
    public void authenticateFail(String fail)
    {

    }
}
