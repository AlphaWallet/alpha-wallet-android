package io.stormbird.wallet.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import io.stormbird.token.tools.Numeric;
import io.stormbird.wallet.R;
import io.stormbird.wallet.ui.widget.OnImportPrivateKeyListener;
import io.stormbird.wallet.widget.InputView;

public class ImportPrivateKeyFragment extends Fragment implements View.OnClickListener {

    private static final OnImportPrivateKeyListener dummyOnImportPrivateKeyListener = key -> { };

    private InputView privateKey;
    private OnImportPrivateKeyListener onImportPrivateKeyListener = dummyOnImportPrivateKeyListener;

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
            value = Numeric.cleanHexPrefix(value.replaceAll("\\s+", "")); //remove whitespace and leading 0x
            if (value.length() == 64)
            {
                onImportPrivateKeyListener.onPrivateKey(value);
                return;
            }
        }

        privateKey.setError(getString(R.string.error_field_required));
    }

    public void setOnImportPrivateKeyListener(OnImportPrivateKeyListener onImportPrivateKeyListener) {
        this.onImportPrivateKeyListener = onImportPrivateKeyListener == null
                ? dummyOnImportPrivateKeyListener
                : onImportPrivateKeyListener;
    }
}
