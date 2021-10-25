package com.alphawallet.app.ui.widget.adapter;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.TokenLocator;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokenscript.TokenScriptFile;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.ui.TokenScriptManagementActivity;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.ChainName;
import com.alphawallet.token.entity.ContractInfo;
import com.alphawallet.token.entity.MagicLinkInfo;
import com.alphawallet.token.entity.SigReturnType;
import com.alphawallet.token.entity.XMLDsigDescriptor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

public class TokenScriptManagementAdapter extends RecyclerView.Adapter<TokenScriptManagementAdapter.TokenSciptCardHolder> {

    private final TokenScriptManagementActivity activity;
    private final Context context;
    private final LayoutInflater inflater;
    private final List<TokenLocator> tokenLocators;
    private final AssetDefinitionService assetDefinitionService;
    private AWalletAlertDialog dialog;
    private final Handler handler = new Handler();

    public TokenScriptManagementAdapter(TokenScriptManagementActivity activity, List<TokenLocator> locators, AssetDefinitionService assetDefinitionService) {
        this.context = activity.getBaseContext();
        this.activity = activity;
        this.tokenLocators = new ArrayList<>(locators);
        this.assetDefinitionService = assetDefinitionService;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public TokenSciptCardHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new TokenSciptCardHolder(inflater.inflate(R.layout.item_tokenscript_management, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull TokenSciptCardHolder tokenSciptCardHolder, int pos) {

        TokenLocator tokenLocator = tokenLocators.get(pos);

        if(!tokenLocator.isError())
        {
            ContractInfo originContract = tokenLocator.getContracts();
            long chainId;
            String address;

            //sweep to see if there's a mainnet holding contract
            if (originContract.addresses.get(MAINNET_ID) != null)
            {
                chainId = MAINNET_ID;
                address = originContract.addresses.get(chainId).get(0);
            }
            else
            {
                chainId = originContract.addresses.keySet().iterator().next();
                address = originContract.addresses.get(chainId).iterator().next();
            }

            //see what the current TS file serving this contract is
            TokenScriptFile servingFile = assetDefinitionService.getTokenScriptFile(chainId, address);
            final TokenScriptFile overrideFile = (servingFile != null && !tokenLocator.getFullFileName().equals(servingFile.getAbsolutePath())) ? servingFile : null;
            if (overrideFile != null)
            {
                tokenSciptCardHolder.overrideLayer.setVisibility(View.VISIBLE);
            }
            else
            {
                tokenSciptCardHolder.overrideLayer.setVisibility(View.GONE);
            }

            tokenSciptCardHolder.clickHolder.setOnClickListener(v -> displayFileDialog(overrideFile, tokenLocator));
            tokenSciptCardHolder.clickHolder.setOnLongClickListener(v -> displayDeleteFileDialog(tokenLocator));

            tokenSciptCardHolder.txtToken.setText(tokenLocator.getDefinitionName());
            tokenSciptCardHolder.txtTokenFile.setText(tokenLocator.getFileName());

            Token t = assetDefinitionService.getTokenFromService(chainId, address);
            if (t != null)
            {
                tokenSciptCardHolder.chainName.setChainID(t.tokenInfo.chainId);
                tokenSciptCardHolder.chainName.setVisibility(View.VISIBLE);
                String tokenSpec = context.getString(R.string.token_spec, address, originContract.contractInterface);
                tokenSciptCardHolder.txtTokenAddress.setText(tokenSpec);
                tokenSciptCardHolder.tokenFullName.setText(t.getFullName());
            }

            assetDefinitionService.getSignatureData(chainId, address)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(sig -> onSigData(sig, tokenSciptCardHolder), Throwable::printStackTrace).isDisposed();
        }
        else
        {
            tokenSciptCardHolder.txtToken.setText(R.string.tokenscript_file_error);
            tokenSciptCardHolder.txtTokenFile.setText(tokenLocator.getDefinitionName());
            tokenSciptCardHolder.txtTokenAddress.setVisibility(View.INVISIBLE);
            tokenSciptCardHolder.tokenFullName.setVisibility(View.GONE);
            tokenSciptCardHolder.chainName.setVisibility(View.GONE);

            tokenSciptCardHolder.imgLock.setVisibility(View.VISIBLE);
            tokenSciptCardHolder.imgLock.setImageResource(R.drawable.ic_error);

            tokenSciptCardHolder.clickHolder.setOnClickListener(v -> displayErrorDialog(tokenLocator));
            tokenSciptCardHolder.clickHolder.setOnLongClickListener(v -> displayDeleteFileDialog(tokenLocator));
        }
    }

    private void displayFileDialog(TokenScriptFile override, TokenLocator tokenLocator)
    {
        if (dialog != null && dialog.isShowing()) dialog.hide();

        dialog = new AWalletAlertDialog(activity);
        dialog.makeWide();
        dialog.setIcon(AWalletAlertDialog.NONE);
        dialog.setTitle(tokenLocator.getFileName());
        dialog.setTextStyle(AWalletAlertDialog.TEXT_STYLE.LEFT);
        StringBuilder message = new StringBuilder();
        if (override != null)
        {
            String actualFile = override.getName();
            String debug = override.isDebug() ? context.getString(R.string.is_debug_script) : "";

            message.append(context.getString(R.string.file_override, actualFile, debug));
        }

        message.append(context.getString(R.string.origin_token_title));
        if (tokenLocator != null)
        {
            for (long chainId : tokenLocator.addresses.keySet())
            {
                String chainName = MagicLinkInfo.getNetworkNameById(chainId);
                for (String address : tokenLocator.addresses.get(chainId))
                {
                    message.append(context.getString(R.string.tokenscript_contract_line, chainName, address));
                }
            }
        }
        dialog.setMessage(message);
        dialog.setButtonText(R.string.ok);
        dialog.setButtonListener(v -> {
            dialog.cancel();
        });
        dialog.show();
    }

    private void displayErrorDialog(TokenLocator tokenLocator)
    {
        if (dialog != null && dialog.isShowing()) dialog.hide();
        dialog = new AWalletAlertDialog(activity);
        dialog.makeWide();
        dialog.setIcon(AWalletAlertDialog.NONE);
        dialog.setTitle(tokenLocator.getFileName());
        dialog.setTextStyle(AWalletAlertDialog.TEXT_STYLE.LEFT);
        StringBuilder message = new StringBuilder();
        message.append(context.getString(R.string.file_error, tokenLocator.getErrorMessage()));

        dialog.setMessage(message);
        dialog.setButtonText(R.string.ok);
        dialog.setButtonListener(v -> {
            dialog.cancel();
        });
        dialog.show();
    }

    private boolean displayDeleteFileDialog(TokenLocator tokenLocator)
    {
        if (dialog != null && dialog.isShowing()) dialog.hide();
        dialog = new AWalletAlertDialog(activity);
        dialog.setIcon(AWalletAlertDialog.NONE);
        dialog.setTitle(R.string.confirm_delete_file);
        dialog.setMessage(context.getString(R.string.confirm_delete_file_message, tokenLocator.getFileName()));
        dialog.setButtonText(R.string.delete);
        dialog.setButtonListener(v -> { deleteFile(tokenLocator); } );
        dialog.setSecondaryButtonText(R.string.action_cancel);
        dialog.setSecondaryButtonListener(v -> dialog.cancel());
        dialog.show();
        return true;
    }

    private void deleteFile(TokenLocator tokenLocator)
    {
        File targetFile = new File(tokenLocator.getFullFileName());
        if (targetFile.exists() && targetFile.delete())
        {
            showWaitDialog();
            activity.refreshList(true);
        }
    }

    private void showWaitDialog()
    {
        handler.postDelayed(() -> {
            if (dialog != null && dialog.isShowing())
                dialog.hide();
            dialog = new AWalletAlertDialog(activity);
            dialog.setTitle(context.getString(R.string.refreshing_tokenscripts));
            dialog.setIcon(AWalletAlertDialog.NONE);
            dialog.setProgressMode();
            dialog.setCancelable(false);
            dialog.show();
        }, 100);
    }

    @Override
    public int getItemCount() {
        return tokenLocators.size();
    }

    public void setTokenScriptInfo(List<TokenLocator> tokenList)
    {
        tokenLocators.addAll(tokenList);
    }

    public void refreshList(List<TokenLocator> tokenList)
    {
        if (dialog != null && dialog.isShowing()) dialog.hide();
        tokenLocators.clear();
        tokenLocators.addAll(tokenList);
        notifyDataSetChanged();
    }

    static class TokenSciptCardHolder extends RecyclerView.ViewHolder {

        final TextView txtToken;
        final TextView txtTokenFile;
        final TextView txtTokenAddress;
        final TextView tokenFullName;
        final ChainName chainName;
        final LinearLayout overrideLayer;
        final ImageView imgLock;
        final LinearLayout clickHolder;

        public TokenSciptCardHolder(@NonNull View itemView) {
            super(itemView);

            txtToken = itemView.findViewById(R.id.token_definition_name);
            txtTokenFile = itemView.findViewById(R.id.token_file);
            txtTokenAddress = itemView.findViewById(R.id.token_address);
            tokenFullName = itemView.findViewById(R.id.token_name);
            chainName = itemView.findViewById(R.id.chain_name);
            imgLock = itemView.findViewById(R.id.image_lock);
            overrideLayer = itemView.findViewById(R.id.layout_override);
            clickHolder = itemView.findViewById(R.id.layout_click_holder);
        }
    }

    //TODO: Move this into a separate class to deduplicate
    private void onSigData(final XMLDsigDescriptor sigData, final TokenSciptCardHolder tokenSciptCardHolder)
    {
        SigReturnType type = sigData.type != null ? sigData.type : SigReturnType.NO_TOKENSCRIPT;
        tokenSciptCardHolder.imgLock.setVisibility(View.VISIBLE);

        switch (type)
        {
            case NO_TOKENSCRIPT:
                tokenSciptCardHolder.imgLock.setVisibility(View.GONE);
                break;
            case DEBUG_SIGNATURE_INVALID:
            case DEBUG_NO_SIGNATURE:
                tokenSciptCardHolder.imgLock.setImageResource(R.mipmap.ic_unlocked_debug);
                break;
            case DEBUG_SIGNATURE_PASS:
                tokenSciptCardHolder.imgLock.setImageResource(R.mipmap.ic_locked_debug);
                break;
            case SIGNATURE_INVALID:
            case NO_SIGNATURE:
                tokenSciptCardHolder.imgLock.setImageResource(R.mipmap.ic_unverified);
                break;
            case SIGNATURE_PASS:
                tokenSciptCardHolder.imgLock.setImageResource(R.mipmap.ic_locked);
                break;
        }
    }
}
