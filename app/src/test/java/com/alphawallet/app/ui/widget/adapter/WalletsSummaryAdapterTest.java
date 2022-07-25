package com.alphawallet.app.ui.widget.adapter;

import static com.alphawallet.app.entity.WalletFactory.createHDKeyWallet;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.doReturn;

import android.content.Context;

import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.WalletRepository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;

@RunWith(PowerMockRunner.class)
@PrepareForTest(RecyclerView.Adapter.class)
public class WalletsSummaryAdapterTest
{
    @Mock
    private WalletRepository walletRepository;

    @Mock
    private Context context;

    @InjectMocks
    private GenericWalletInteract genericWalletInteract;

    private WalletsSummaryAdapter adapter;

    @Before
    public void setUp() throws Exception
    {
        doReturn("Summary").when(context).getString(anyInt());
        WalletsSummaryAdapter raw = new WalletsSummaryAdapter(context, null, genericWalletInteract, false);
        adapter = PowerMockito.spy(raw);
        doNothing().when(adapter).notifyDataSetChanged();
    }

    @Test
    public void should_get_default_wallet_index()
    {
        Wallet[] wallets = Arrays.asList(createHDKeyWallet("0x1"), createHDKeyWallet("0x2")).toArray(new Wallet[] {});
        adapter.setWallets(wallets);
        adapter.setDefaultWallet(createHDKeyWallet("0x2"));

        int index = adapter.getDefaultWalletIndex() - 3; // There are 3 title labels added as Wallet

        assertThat(index, equalTo(1));
    }

    @Test
    public void test_getDefaultWalletIndex_should_get_negative_one_when_no_default_wallet()
    {
        Wallet[] wallets = Arrays.asList(createHDKeyWallet("0x1"), createHDKeyWallet("0x2")).toArray(new Wallet[] {});
        adapter.setWallets(wallets);

        int index = adapter.getDefaultWalletIndex();

        assertThat(index, equalTo(-1));
    }

}