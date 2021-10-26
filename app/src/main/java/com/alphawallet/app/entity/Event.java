package com.alphawallet.app.entity;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by JB on 26/03/2020.
 */
public class Event implements Parcelable
{
    private final String eventText;
    private final long timeStamp;
    private final long chainId;

    @Override
    public int describeContents()
    {
        return 0;
    }

    public Event(String eventTxt, long timeStamp, long chainId)
    {
        this.eventText = eventTxt;
        this.timeStamp = timeStamp;
        this.chainId = chainId;
    }

    protected Event(Parcel in)
    {
        eventText = in.readString();
        timeStamp = in.readLong();
        chainId = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(eventText);
        dest.writeLong(timeStamp);
        dest.writeLong(chainId);
    }

    public static final Creator<Transaction> CREATOR = new Creator<Transaction>()
    {
        @Override
        public Transaction createFromParcel(Parcel in) {
            return new Transaction(in);
        }

        @Override
        public Transaction[] newArray(int size) {
            return new Transaction[size];
        }
    };

    public String getEventText()
    {
        return eventText;
    }
    public long getTimeStamp() { return timeStamp; }
    public String getHash()
    {
        String hash = eventText + "-" + timeStamp;
        return String.valueOf(hash.hashCode());
    }
    public long getChainId() { return chainId; }
}
