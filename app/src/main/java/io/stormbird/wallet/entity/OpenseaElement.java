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
    public String imageUrl;
    public String name;
    public String description;
    public String externalLink;
    public String backgroundColor;
    public  Map<String, ERC721Attribute> traits = new HashMap<String, ERC721Attribute>();

    public OpenseaElement()
    {
        traits.clear();
    }

    @Override
    public int describeContents()
    {
        return traits.size();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeLong(tokenId);
        dest.writeString(imageUrl);
        dest.writeString(name);
        dest.writeString(description);
        dest.writeString(externalLink);
        dest.writeString(backgroundColor);
        dest.writeInt(traits.size());
        for (String key : traits.keySet())
        {
            dest.writeString(key);
            dest.writeParcelable(traits.get(key), flags);
        }
    }

    private OpenseaElement(Parcel in)
    {
        traits.clear();
        tokenId = in.readLong();
        imageUrl = in.readString();
        name = in.readString();
        description = in.readString();
        externalLink = in.readString();
        backgroundColor = in.readString();
        int size = in.readInt();
        for (int i = 0; i < size; i++)
        {
            String key = in.readString();
            ERC721Attribute attr = in.readParcelable(ERC721Attribute.class.getClassLoader());
            traits.put(key, attr);
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
