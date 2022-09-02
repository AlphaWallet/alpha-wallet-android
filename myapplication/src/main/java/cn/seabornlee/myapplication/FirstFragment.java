package cn.seabornlee.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.alphawallet.app.App;
import com.alphawallet.app.entity.ImportWalletCallback;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.cryptokeys.KeyEncodingType;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.KeystoreAccountService;

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
//                NavHostFragment.findNavController(FirstFragment.this)
//                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
                KeyService keyService = new KeyService(getContext(), null);
                keyService.importHDKey("essence allow crisp figure tired task melt honey reduce planet twenty rookie", getActivity(), new ImportWalletCallback()
                {
                    @Override
                    public void walletValidated(String s, KeyEncodingType keyEncodingType, KeyService.AuthenticationLevel authenticationLevel)
                    {
                        Log.d("seaborn", s);
                    }
                });

                Wallet wallet = new Wallet("0xD0c424B3016E9451109ED97221304DeC639b3F84");
                keyService.getAuthenticationForSignature(wallet, getActivity(), new SignAuthenticationCallback()
                {
                    @Override
                    public void gotAuthorisation(boolean b)
                    {
                        KeystoreAccountService keystoreAccountService = new KeystoreAccountService(null, null, keyService);
                        keystoreAccountService.signMessage(wallet, null, 1);
                    }

                    @Override
                    public void cancelAuthentication()
                    {

                    }
                });
            }
        });
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        binding = null;
    }

}