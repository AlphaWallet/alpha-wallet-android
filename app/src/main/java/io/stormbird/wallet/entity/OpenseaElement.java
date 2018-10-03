package io.stormbird.wallet.entity;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by James on 3/10/2018.
 * Stormbird in Singapore
 */
public class OpenseaElement implements Parcelable
{
    public long tokenId;
    public String imageFileName;
    public String imageURL;
    public String assetName;
    public String description;

    public  Map<String, ERC721Attribute> attributes = new HashMap<String, ERC721Attribute>();

    public OpenseaElement()
    {
        attributes.clear();
    }

    @Override
    public int describeContents()
    {
        return attributes.size();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeLong(tokenId);
        dest.writeString(imageFileName);
        dest.writeString(imageURL);
        dest.writeString(assetName);
        dest.writeString(description);
        dest.writeInt(attributes.size());
        for (String key : attributes.keySet())
        {
            dest.writeString(key);
            dest.writeParcelable(attributes.get(key), flags);
        }
    }

    private OpenseaElement(Parcel in)
    {
        attributes.clear();
        tokenId = in.readLong();
        imageFileName = in.readString();
        imageURL = in.readString();
        assetName = in.readString();
        description = in.readString();
        int size = in.readInt();
        for (int i = 0; i < size; i++)
        {
            String key = in.readString();
            ERC721Attribute attr = in.readParcelable(ERC721Attribute.class.getClassLoader());
            attributes.put(key, attr);
        }
    }

    public static final Creator<OpenseaElement> CREATOR = new Creator<OpenseaElement>() {
        @Override
        public OpenseaElement createFromParcel(Parcel in) {
            return new OpenseaElement(in);
        }

        @Override
        public OpenseaElement[] newArray(int size) {
            return new OpenseaElement[size];
        }
    };
}
