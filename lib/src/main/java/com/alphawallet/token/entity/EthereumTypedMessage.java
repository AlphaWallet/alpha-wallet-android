package com.alphawallet.token.entity;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static com.alphawallet.token.entity.MessageUtils.encodeParams;
import static com.alphawallet.token.entity.MessageUtils.encodeValues;

public class EthereumTypedMessage implements Signable {

    byte[] structuredData;
    String displayOrigin;
    long leafPosition;
    CharSequence userMessage;
    SignMessageType messageType;

    public EthereumTypedMessage(byte[] value, CharSequence userMessage, String displayOrigin, long leafPosition) {
        this.structuredData = value;
        this.displayOrigin = displayOrigin;
        this.leafPosition = leafPosition;
        this.userMessage = userMessage;
        messageType = SignMessageType.SIGN_ERROR;
    }

    public EthereumTypedMessage(String messageData, String domainName, long callbackId, CryptoFunctionsInterface cryptoFunctions)
    {
        try
        {
            try
            {
                ProviderTypedData[] rawData = new Gson().fromJson(messageData, ProviderTypedData[].class);
                ByteArrayOutputStream writeBuffer = new ByteArrayOutputStream();
                writeBuffer.write(cryptoFunctions.keccak256(encodeParams(rawData)));
                writeBuffer.write(cryptoFunctions.keccak256(encodeValues(rawData)));
                this.userMessage = cryptoFunctions.formatTypedMessage(rawData);
                this.structuredData = writeBuffer.toByteArray();
                messageType = SignMessageType.SIGN_TYPED_DATA;
            }
            catch (JsonSyntaxException e)
            {
                this.structuredData = cryptoFunctions.getStructuredData(messageData);
                this.userMessage = cryptoFunctions.formatEIP721Message(messageData);
                messageType = SignMessageType.SIGN_TYPED_DATA_V3;
            }
        }
        catch (IOException e)
        {
            this.userMessage = "";
            messageType = SignMessageType.SIGN_ERROR;
            e.printStackTrace();
        }

        this.displayOrigin = domainName;
        this.leafPosition = callbackId;
    }

    // User message is the text shown in the popup window - note CharSequence is used because message contains text formatting
    public CharSequence getUserMessage() {
        return userMessage;
    }

    public long getCallbackId() {
        return this.leafPosition;
    }

    public byte[] getPrehash() {
        return structuredData;
    }

    @Override
    public String getOrigin()
    {
        return displayOrigin;
    }

    @Override
    public String getMessage()
    {
        return userMessage.toString();
    }

    @Override
    public SignMessageType getMessageType()
    {
        return messageType;
    }
}