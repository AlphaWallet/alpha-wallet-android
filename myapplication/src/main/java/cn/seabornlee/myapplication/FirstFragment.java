package cn.seabornlee.myapplication;

import static com.alphawallet.app.service.KeystoreAccountService.KEYSTORE_FOLDER;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.alphawallet.app.entity.ImportWalletCallback;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.cryptokeys.KeyEncodingType;
import com.alphawallet.app.entity.cryptokeys.SignatureFromKey;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.KeystoreAccountService;

import org.web3j.crypto.Sign;

import java.io.File;
import java.math.BigInteger;
import java.util.Arrays;

import cn.seabornlee.myapplication.databinding.FragmentFirstBinding;

public class FirstFragment extends Fragment
{

    private FragmentFirstBinding binding;


    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    )
    {

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonFirst.setOnClickListener(view1 -> {
            KeyService keyService = new KeyService(getContext(), null);
            keyService.importHDKey("essence allow crisp figure tired task melt honey reduce planet twenty rookie", getActivity(), new ImportWalletCallback()
            {
                @Override
                public void walletValidated(String address, KeyEncodingType keyEncodingType, KeyService.AuthenticationLevel authenticationLevel)
                {
                    binding.textviewFirst.setText(address);

                    Wallet wallet = new Wallet(address);
                    wallet.setWalletType(WalletType.HDKEY);
                    keyService.getAuthenticationForSignature(wallet, getActivity(), new SignAuthenticationCallback()
                    {

                        @Override
                        public void gotAuthorisation(boolean b)
                        {
                            File file = new File(getContext().getFilesDir(), KEYSTORE_FOLDER);
                            KeystoreAccountService keystoreAccountService = new KeystoreAccountService(file, getContext().getFilesDir(), keyService);
                            String msg = "hello";
                            SignatureFromKey signatureFromKey = keystoreAccountService.signTransactionEIP1559(wallet, address, BigInteger.ZERO, BigInteger.ONE, BigInteger.ONE, BigInteger.ONE, 0, msg.getBytes(), 1L).blockingGet();

                            String hash = Numeric.toHexString(signatureFromKey.signature);
                            binding.textviewSig.setText(hash);
                        }

                        @Override
                        public void cancelAuthentication()
                        {

                        }
                    });

                }
            });
        });
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        binding = null;
    }

}