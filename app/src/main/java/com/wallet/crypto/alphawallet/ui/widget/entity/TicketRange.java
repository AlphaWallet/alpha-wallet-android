package com.wallet.crypto.alphawallet.ui.widget.entity;

import android.os.Parcel;
import android.os.Parcelable;

import com.wallet.crypto.alphawallet.entity.Ticket;

import java.util.ArrayList;

/**
 * Created by James on 10/02/2018.
 */

public class TicketRange implements Parcelable
{
    public final int seatStart;
    public int seatCount;

    public final int tokenId;

    public TicketRange(int tokenId, int seatStart)
    {
        this.tokenId = tokenId;
        this.seatStart = seatStart;
        this.seatCount = 1;
    }

    private TicketRange(Parcel in)
    {
        this.tokenId = in.readInt();
        this.seatStart = in.readInt();
        this.seatCount = in.readInt();
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(this.tokenId);
        dest.writeInt(this.seatStart);
        dest.writeInt(this.seatCount);
    }

    public static final Creator<TicketRange> CREATOR = new Creator<TicketRange>() {
        @Override
        public TicketRange createFromParcel(Parcel in) {
            return new TicketRange(in);
        }

        @Override
        public TicketRange[] newArray(int size) {
            return new TicketRange[size];
        }
    };
}
