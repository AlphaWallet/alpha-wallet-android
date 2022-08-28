package com.alphawallet.app.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.viewmodel.BackupKeyViewModel;
import com.alphawallet.token.tools.Numeric;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Created by JB on 24/08/2022.
 */
@AndroidEntryPoint
public class WalletDiagnosticActivity extends BaseActivity
{
    private static final String LEGACY_CIPHER_ALGORITHM = "AES/CBC/PKCS7Padding";
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";

    private BackupKeyViewModel viewModel;

    private Wallet wallet;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet_diagnostic);
        toolbar();
        setTitle("Wallet Diagnostic");

        if (getIntent() != null)
        {
            wallet = (Wallet) getIntent().getExtras().get("wallet");
        }
        else
        {
            finish();
        }

        initViewModel();

        scanForKey();
        setCurrentKeyType();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initViewModel()
    {
        viewModel = new ViewModelProvider(this)
                .get(BackupKeyViewModel.class);
    }

    private void scanForKey()
    {
        TextView status = findViewById(R.id.key_in_enclave);
        if (viewModel.hasKey(wallet.address))
        {
            status.setText("Key found");
            status.setTextColor(getColor(R.color.green));
        }
        else
        {
            status.setText("No key");
            status.setTextColor(getColor(R.color.danger));
        }
    }

    private void setCurrentKeyType()
    {
        TextView status = findViewById(R.id.key_type);
        String walletType = wallet.type.toString();
        status.setText(walletType);

        if (wallet.type == WalletType.KEYSTORE || wallet.type == WalletType.KEYSTORE_LEGACY)
        {
            //test cipher
            testCipher();
        }
    }

    private void testCipher()
    {
        try
        {
            wallet.type = WalletType.KEYSTORE_LEGACY;
            //attempt to unlock the key like this
            viewModel.getAuthentication();

            KeyStore keyStore;
            String encryptedDataFilePath = getFilePath(this, wallet.address);
            String keyIv = getFilePath(this, wallet.address + "iv");
            boolean ivExists = new File(keyIv).exists();
            boolean aliasExists = new File(encryptedDataFilePath).exists();

            if (!ivExists) throw new Exception("iv file doesn't exist");
            if (!aliasExists) throw new Exception("Key file doesn't exist");

            //test legacy key
            byte[] iv = readBytesFromFile(keyIv);

            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            SecretKey secretKey = (SecretKey) keyStore.getKey(wallet.address, null);

            Cipher outCipher = Cipher.getInstance(LEGACY_CIPHER_ALGORITHM);
            outCipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            CipherInputStream cipherInputStream = new CipherInputStream(new FileInputStream(encryptedDataFilePath), outCipher);
            byte[] pw = readBytesFromStream(cipherInputStream);

            System.out.println("YOLESS: " + Numeric.toHexString(pw));
        }
        catch (Exception e)
        {
            //TODO: Popup
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    synchronized static String getFilePath(Context context, String fileName)
    {
        //check for matching file
        File check = new File(context.getFilesDir(), fileName);
        if (check.exists())
        {
            return check.getAbsolutePath(); //quick return
        }
        else
        {
            //find matching file, ignoring case
            File[] files = context.getFilesDir().listFiles();
            for (File checkFile : files)
            {
                if (checkFile.getName().equalsIgnoreCase(fileName))
                {
                    return checkFile.getAbsolutePath();
                }
            }
        }

        return check.getAbsolutePath(); //Should never get here
    }

    static byte[] readBytesFromFile(String path)
    {
        byte[] bytes = null;
        File file = new File(path);
        try (FileInputStream fin = new FileInputStream(file))
        {
            bytes = readBytesFromStream(fin);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return bytes;
    }

    static byte[] readBytesFromStream(InputStream in) throws IOException
    {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 2048;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = in.read(buffer)) != -1)
        {
            byteBuffer.write(buffer, 0, len);
        }

        byteBuffer.close();
        return byteBuffer.toByteArray();
    }
}
