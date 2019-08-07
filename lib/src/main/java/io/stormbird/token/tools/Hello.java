package io.stormbird.token.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.Context;

public class Hello implements RequestStreamHandler{
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        outputStream.write('h');
        int letter;
        while((letter = inputStream.read()) != -1)
        {
            outputStream.write(Character.toUpperCase(letter));
        }
    }
}