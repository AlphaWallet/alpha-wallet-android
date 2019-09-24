package com.alphawallet.app.entity.tokenscript;

import android.content.Context;
import android.support.annotation.NonNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import com.alphawallet.token.entity.SigReturnType;

public class TokenScriptFile extends File
{
    private Context context;
    private boolean active;
    private boolean resourceFile;
    private String fileName;

    public TokenScriptFile()
    {
        super("");
        context = null;
        active = false;
        resourceFile = false;
    }

    public TokenScriptFile(Context ctx, @NonNull String pathname, @NonNull String name)
    {
        super(pathname, name);
        InitFile(ctx, pathname + "/" + name);
    }

    public TokenScriptFile(Context ctx, @NonNull String pathname)
    {
        super(pathname);
        InitFile(ctx, pathname);
    }

    private void InitFile(Context ctx, String pathname)
    {
        context = ctx;
        fileName = pathname;

        if (exists() && canRead())
        {
            active = true;
        }
        else
        {
            try
            {
                InputStream is = context.getResources().getAssets().open(pathname);
                if (is.available() > 0) resourceFile = true;
                is.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public InputStream getInputStream()
    {
        try
        {
            if (active) return new FileInputStream(this);
            else if (resourceFile) return context.getResources().getAssets().open(fileName);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public boolean isValidTokenScript()
    {
        return active || resourceFile;
    }


    public String calcMD5()
    {
        StringBuilder sb = new StringBuilder();
        try
        {
            InputStream fis = getInputStream();
            MessageDigest digest = MessageDigest.getInstance("MD5");

            byte[] byteArray = new byte[1024];
            int bytesCount = 0;

            while ((bytesCount = fis.read(byteArray)) != -1)
            {
                digest.update(byteArray, 0, bytesCount);
            }

            fis.close();

            byte[] bytes = digest.digest();
            for (byte aByte : bytes)
            {
                sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
            }
        }
        catch (IOException|NoSuchAlgorithmException e)
        {
            String rand = String.valueOf(new Random(System.currentTimeMillis()).nextInt());
            sb.append(rand); //never matches
        }

        //return complete hash
        return sb.toString();
    }

    public boolean fileUnchanged(TokenScriptFileData fd)
    {
        return fd != null && fd.sigDescriptor != null && fd.hash.equals(calcMD5()) && fd.sigDescriptor.result.equals("pass");
    }

    public boolean isDebug()
    {
        //check for the private data area
        String privateArea = context.getFilesDir().getAbsolutePath();
        return !getAbsolutePath().startsWith(privateArea);
    }

    public void determineSignatureType(TokenScriptFileData fd)
    {
        boolean isDebug = isDebug();
        if (fd.sigDescriptor.result.equals("pass"))
        {
            if (isDebug) fd.sigDescriptor.type = SigReturnType.DEBUG_SIGNATURE_PASS;
            else fd.sigDescriptor.type = SigReturnType.SIGNATURE_PASS;
        }
        else if (fd.sigDescriptor.subject != null)
        {
            if (fd.sigDescriptor.subject.contains("Invalid"))
            {
                if (isDebug) fd.sigDescriptor.type = SigReturnType.DEBUG_SIGNATURE_INVALID;
                else fd.sigDescriptor.type = SigReturnType.SIGNATURE_INVALID;
            }
            else
            {
                if (isDebug) fd.sigDescriptor.type = SigReturnType.DEBUG_NO_SIGNATURE;
                else fd.sigDescriptor.type = SigReturnType.NO_SIGNATURE;
            }
        }
        else
        {
            if (isDebug) fd.sigDescriptor.type = SigReturnType.DEBUG_NO_SIGNATURE;
            else fd.sigDescriptor.type = SigReturnType.NO_SIGNATURE;
        }
    }
}
