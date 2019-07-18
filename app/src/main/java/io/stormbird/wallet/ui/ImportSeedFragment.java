package io.stormbird.wallet.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import io.stormbird.wallet.R;
import io.stormbird.wallet.ui.widget.OnImportSeedListener;
import io.stormbird.wallet.widget.PasswordInputView;


public class ImportSeedFragment extends Fragment implements View.OnClickListener, TextWatcher
{

    private static final OnImportSeedListener dummyOnImportSeedListener = (s, c) -> {};

    private PasswordInputView seedPhrase;
    @NonNull
    private OnImportSeedListener onImportSeedListener = dummyOnImportSeedListener;

    public static ImportSeedFragment create() {
        return new ImportSeedFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return LayoutInflater.from(getContext())
                .inflate(R.layout.fragment_import_seed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        seedPhrase = view.findViewById(R.id.input_seed);
        view.findViewById(R.id.import_action).setOnClickListener(this);
        seedPhrase.getEditText().addTextChangedListener(this);
    }

    @Override
    public void onClick(View view) {
        this.seedPhrase.setError(null);
        String newMnemonic = seedPhrase.getText().toString();
        if (TextUtils.isEmpty(newMnemonic)) {
            this.seedPhrase.setError(getString(R.string.error_field_required));
        } else {
            onImportSeedListener.onSeed(newMnemonic, getActivity());
        }
    }

    public void setOnImportSeedListener(@Nullable OnImportSeedListener onImportSeedListener) {
        this.onImportSeedListener = onImportSeedListener == null
                ? dummyOnImportSeedListener
                : onImportSeedListener;
    }

    public void onBadSeed()
    {
        seedPhrase.setError(R.string.bad_seed_phrase);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2)
    {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2)
    {

    }

    @Override
    public void afterTextChanged(Editable editable)
    {
        if (seedPhrase.isErrorState()) seedPhrase.setError(null);
    }
}
