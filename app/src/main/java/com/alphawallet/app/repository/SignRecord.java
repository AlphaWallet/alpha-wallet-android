package com.alphawallet.app.repository;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.alphawallet.app.repository.entity.RealmWCSignElement;

/**
 * Created by JB on 9/09/2020.
 */
public class SignRecord implements Parcelable
{
    public final long date;
    public final String type;
    public final CharSequence message;

    public SignRecord(RealmWCSignElement e)
    {
        date = e.getSignTime();
        type = e.getSignType();
        message = e.getSignMessage();
    }

    public static final Parcelable.Creator<SignRecord> CREATOR = new Parcelable.Creator<SignRecord>() {
        @Override
        public SignRecord createFromParcel(Parcel in) {
            return new SignRecord(in);
        }

        @Override
        public SignRecord[] newArray(int size) {
            return new SignRecord[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    protected SignRecord(Parcel in)
    {
        message = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        date = in.readLong();
        type = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        TextUtils.writeToParcel(message, dest, flags);
        dest.writeLong(date);
        dest.writeString(type);
    }
}
