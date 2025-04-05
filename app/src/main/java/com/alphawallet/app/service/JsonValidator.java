package com.alphawallet.app.service;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;

import okhttp3.ResponseBody;
import timber.log.Timber;

public class JsonValidator
{
    static final String UNAUTHORIZED_ERROR = "unauthorized";
    static final String INTERNAL_ERROR = "internal error";
    static final String LIMIT_EXCEEDED = "limit exceeded";
    static final String JSON_ERROR = "error";

    static class CustomTeeInputStream extends FilterInputStream
    {
        private final ByteArrayOutputStream branch;

        public CustomTeeInputStream(InputStream in, ByteArrayOutputStream branch)
        {
            super(in);
            this.branch = branch;
        }

        @Override
        public int read() throws IOException
        {
            int ch = super.read();
            if (ch != -1)
            {
                branch.write(ch);
            }
            return ch;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException
        {
            int result = super.read(b, off, len);
            if (result != -1)
            {
                branch.write(b, off, result);
            }
            return result;
        }
    }

    public static InputStream validateAndGetStream(ResponseBody responseBody) throws IOException
    {
        InputStream originalStream = responseBody.byteStream();
        ByteArrayOutputStream peekedData = new ByteArrayOutputStream();

        // Create a TeeInputStream: one branch for validation, the other for the caller
        CustomTeeInputStream teeStream = new CustomTeeInputStream(originalStream, peekedData);

        // Validate the peeked JSON
        boolean isValid = isValidJson(teeStream);
        ByteArrayInputStream bais = new ByteArrayInputStream(peekedData.toByteArray());
        InputStream combinedStream = new SequenceInputStream(bais, new ByteArrayInputStream(new byte[0]));

        if (isValid)
        {
            // Return combined stream, even though bais now has all the data the original stream had
            // this way we can guarantee it will behave exactly as the original stream
            return combinedStream;
        }
        else
        {
            // If invalid, handle accordingly (e.g., throw an exception or return an error stream)
            throw new IOException("Invalid JSON format");
        }
    }

    private static boolean isValidJson(InputStream inputStream)
    {
        try (BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(inputStream)))
        {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
            {
                content.append(line);
            }

            JSONObject thisObj = new JSONObject(content.toString());
            if (thisObj.has(JSON_ERROR))
            {
                String error = thisObj.getString(JSON_ERROR);
                if (error.toLowerCase().contains(UNAUTHORIZED_ERROR) || error.toLowerCase().contains(INTERNAL_ERROR))
                {
                    return false;
                }
            }
            return !content.toString().toLowerCase().contains(LIMIT_EXCEEDED);
        }
        catch (JSONException | IOException e)
        {
            return false;
        }
    }
}
