package cn.seabornlee.myapplication;

import static com.alphawallet.app.service.KeystoreAccountService.KEYSTORE_FOLDER;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.alphawallet.app.entity.ImportWalletCallback;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.cryptokeys.KeyEncodingType;
import com.alphawallet.app.entity.cryptokeys.SignatureFromKey;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.KeystoreAccountService;

import java.io.File;
import java.math.BigInteger;

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

        binding.buttonFirst.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                KeyService keyService = new KeyService(getContext(), null);
                keyService.importHDKey("essence allow crisp figure tired task melt honey reduce planet twenty rookie", getActivity(), new ImportWalletCallback()
                {
                    @Override
                    public void walletValidated(String address, KeyEncodingType keyEncodingType, KeyService.AuthenticationLevel authenticationLevel)
                    {
                        binding.textviewFirst.setText(address);

                        Wallet wallet = new Wallet(address);
                        keyService.getAuthenticationForSignature(wallet, getActivity(), new SignAuthenticationCallback()
                        {

                            @Override
                            public void gotAuthorisation(boolean b)
                            {
                                Log.d("seaborn", String.valueOf(b));
                                File file = new File(getContext().getFilesDir(), KEYSTORE_FOLDER);
                                KeystoreAccountService keystoreAccountService = new KeystoreAccountService(file, getContext().getFilesDir(), keyService);
                                SignatureFromKey signatureFromKey = keystoreAccountService.signTransaction(wallet, address, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO
                                        , 1, "".getBytes(), 1).blockingGet();
                                binding.textviewSig.setText(new String(signatureFromKey.signature));
                            }

                            @Override
                            public void cancelAuthentication()
                            {

                            }
                        });

                    }
                });
            }
        });
    }

    private void sleep()
    {
        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        binding = null;
    }

}