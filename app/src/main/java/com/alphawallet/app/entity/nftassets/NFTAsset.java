package com.alphawallet.app.entity.nftassets;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by JB on 1/07/2021.
 */
public class NFTAsset implements Parcelable
{
    Map<String, String> assetMap = new ConcurrentHashMap<>();

    protected NFTAsset(Parcel in)
    {
        int assetCount = in.readInt();
        for (int i = 0; i < assetCount; i++)
        {
            assetMap.put(in.readString(), in.readString());
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(assetMap.size());
        for (String key : assetMap.keySet())
        {
            dest.writeString(key);
            dest.writeString(assetMap.get(key));
        }
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    public static final Creator<NFTAsset> CREATOR = new Creator<NFTAsset>()
    {
        @Override
        public NFTAsset createFromParcel(Parcel in)
        {
            return new NFTAsset(in);
        }

        @Override
        public NFTAsset[] newArray(int size)
        {
            return new NFTAsset[size];
        }
    };
}