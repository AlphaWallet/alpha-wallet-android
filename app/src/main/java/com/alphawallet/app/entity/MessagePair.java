package com.alphawallet.app.entity;

import com.alphawallet.token.entity.SignMessageType;
import com.alphawallet.token.entity.Signable;

/**
 * Created by James on 30/01/2018.
 */

public class MessagePair implements Signable
{
    public final String selection;
    public final String message;

    public MessagePair(String selection, String message)
    {
        this.selection = selection;
        this.message = message;
    }


    @Override
    public String getMessage() {
        return message;
    }

    // TODO: Question to JB: actually, do we add the prefix here?
    @Override
    public byte[] getPrehash() {
        return message.getBytes();
    }

    @Override
    public String getOrigin()
    {
        return null;
    }

    // TODO: I actually don't know where to return to … -Weiwu
    @Override
    public long getCallbackId() {
        return 0;
    }

    @Override
    public CharSequence getUserMessage() {
        return "";
    }

    @Override
    public SignMessageType getMessageType()
    {
        return null;
    }
}
