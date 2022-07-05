package com.alphawallet.app.ui;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.web3.entity.WalletAddEthereumChainObject;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;

public class AddEthereumChainPrompt extends BottomSheetDialog implements StandardFunctionInterface {

    interface AddChainListener {
        void onAdd(WalletAddEthereumChainObject chainObject);
    }

    private final WalletAddEthereumChainObject chainObject;

    private final AddChainListener listener;

    public AddEthereumChainPrompt(@NonNull @NotNull Context context, WalletAddEthereumChainObject chainObject, AddChainListener listener) {
        super(context);
        setContentView(R.layout.dialog_add_ethereum_chain);
        this.chainObject = chainObject;
        this.listener = listener;

        TextView message = findViewById(R.id.message);
        message.setText(context.getString(R.string.add_chain_dialog_message, chainObject.chainName, String.valueOf(chainObject.getChainId())));

        FunctionButtonBar functionBar = findViewById(R.id.layoutButtons);
        functionBar.setupFunctions(this, new ArrayList<>(Collections.singletonList(R.string.action_enable_switch_reload)));
        functionBar.revealButtons();
    }

    @Override
    public void handleClick(String action, int id) {
        // add custom chain
        listener.onAdd(chainObject);
    }
}
