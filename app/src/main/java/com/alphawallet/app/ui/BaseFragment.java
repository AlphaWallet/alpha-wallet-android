package com.alphawallet.app.ui;

import android.content.Intent;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.BackupTokenCallback;
import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.FragmentMessenger;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class BaseFragment extends Fragment implements Toolbar.OnMenuItemClickListener,
        BackupTokenCallback
{
    private Toolbar toolbar;
    private TextView toolbarTitle;

    private void initToolbar(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
    }
    protected void toolbar(View view) {
        if (view != null) initToolbar(view);
    }

    protected void toolbar(View view, int title, int menuResId) {
        initToolbar(view);
        setToolbarTitle(title);
        setToolbarMenu(menuResId);
    }

    protected void toolbar(View view, int menuResId) {
        initToolbar(view);
        setToolbarMenu(menuResId);
    }

    protected void toolbar(View view, int menuResId, Toolbar.OnMenuItemClickListener listener) {
        initToolbar(view);
        setToolbarMenu(menuResId);
        setToolbarMenuItemClickListener(listener);
    }

    protected void toolbar(View view, int title, int menuResId, Toolbar.OnMenuItemClickListener listener) {
        initToolbar(view);
        setToolbarTitle(title);
        setToolbarMenu(menuResId);
        setToolbarMenuItemClickListener(listener);
    }

    protected void setToolbarTitle(String title) {
        if (toolbarTitle != null) {
            toolbarTitle.setText(title);
        }
    }

    protected void setToolbarMenuItemClickListener(Toolbar.OnMenuItemClickListener listener) {
        toolbar.setOnMenuItemClickListener(listener);
    }

    protected void setToolbarTitle(int title) {
        setToolbarTitle(getString(title));
    }

    protected void setToolbarMenu(int menuRes) {
        toolbar.inflateMenu(menuRes);
    }

    public Toolbar getToolbar() {
        return toolbar;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        return false;
    }

    public void comeIntoFocus()
    {
        //
    }

    public void leaveFocus()
    {
        //
    }

    public void softKeyboardVisible() { }
    public void softKeyboardGone() { }
    public void onItemClick(String url) { }
    public void signalUpdate(int updateVersion) { }
    public void backupSeedSuccess(boolean hasNoLock) { }
    public void storeWalletBackupTime(String backedUpKey) { }
    public void resetTokens() { }
    public void resetTransactions() { }
    public void gotCameraAccess(@NotNull String[] permissions, int[] grantResults) { }
    public void gotGeoAccess(@NotNull String[] permissions, int[] grantResults) { }
    public void gotFileAccess(@NotNull String[] permissions, int[] grantResults) { }
    public void handleQRCode(int resultCode, Intent data, FragmentMessenger messenger) { }
    public void pinAuthorisation(boolean gotAuth) { }
    public void switchNetworkAndLoadUrl(long chainId, String url) { }
    public void scrollToTop() { }
    public void addedToken(List<ContractLocator> tokenContracts) { }
    public void setImportFilename(String fName) { }
    public void backPressed() { }
}
