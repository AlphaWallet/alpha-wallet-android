package com.alphawallet.app.ui.widget.entity;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.alphawallet.token.entity.TicketRange;

/**
 * Created by James on 10/02/2018.
 */

/**
 * This should purely be a container class of NonFungibleToken
 *
 */
public class TicketRangeParcel implements Parcelable
{
    public TicketRange range;

    public TicketRangeParcel(TicketRange t)
    {
        range = t;
    }

    private TicketRangeParcel(Parcel in)
    {
        Object[] readObjArray = in.readArray(Object.class.getClassLoader());
        List tIds = new ArrayList<>();
        tIds.addAll(Arrays.asList(readObjArray));

        boolean isChecked = in.readInt() == 1;

        String contractAddress = in.readString();

        range = new TicketRange(tIds, contractAddress, isChecked);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeArray(range.tokenIds.toArray());
        dest.writeInt(range.isChecked ? 1:0);
        dest.writeString(range.contractAddress);
    }

    public static final Creator<TicketRangeParcel> CREATOR = new Creator<TicketRangeParcel>() {
        @Override
        public TicketRangeParcel createFromParcel(Parcel in) {
            return new TicketRangeParcel(in);
        }

        @Override
        public TicketRangeParcel[] newArray(int size) {
            return new TicketRangeParcel[size];
        }
    };
}
