package io.stormbird.wallet.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.stormbird.wallet.R;
import io.stormbird.wallet.ui.widget.OnImportKeystoreListener;
import io.stormbird.wallet.widget.InputView;


public class ImportKeystoreFragment extends Fragment implements View.OnClickListener {

    private static final OnImportKeystoreListener dummyOnImportKeystoreListener = (k, p) -> {};

    private InputView keystore;
    private InputView password;
    @NonNull
    private OnImportKeystoreListener onImportKeystoreListener = dummyOnImportKeystoreListener;

    public static ImportKeystoreFragment create() {
        return new ImportKeystoreFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return LayoutInflater.from(getContext())
                .inflate(R.layout.fragment_import_keystore, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        keystore = view.findViewById(R.id.input_keystore);
        password = view.findViewById(R.id.input_password);
        view.findViewById(R.id.import_action).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        this.keystore.setError(null);
        String keystore = this.keystore.getText().toString();
        String password = this.password.getText().toString();
        if (TextUtils.isEmpty(keystore)) {
            this.keystore.setError(getString(R.string.error_field_required));
        } else {
            onImportKeystoreListener.onKeystore(keystore, password);
        }
    }

    public void setOnImportKeystoreListener(@Nullable OnImportKeystoreListener onImportKeystoreListener) {
        this.onImportKeystoreListener = onImportKeystoreListener == null
            ? dummyOnImportKeystoreListener
            : onImportKeystoreListener;
    }
}
