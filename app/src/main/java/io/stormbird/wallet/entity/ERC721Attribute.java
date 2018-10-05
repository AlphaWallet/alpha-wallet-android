package io.stormbird.wallet.entity;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by James on 3/10/2018.
 * Stormbird in Singapore
 */
public class ERC721Attribute implements Parcelable
{
    public final ValueType type;
    public final String attributeValue;

    public ERC721Attribute(String valueType, String attributeValue)
    {
        switch (valueType)
        {
            case "number":
                type = ValueType.INTEGER;
                break;
            default:
                type = ValueType.STRING;
                break;
        }

        this.attributeValue = attributeValue;
    }

    protected ERC721Attribute(Parcel in)
    {
        int typeVal = in.readInt();
        type = ValueType.values()[typeVal];
        attributeValue = in.readString();
    }

    public static final Creator<ERC721Attribute> CREATOR = new Creator<ERC721Attribute>()
    {
        @Override
        public ERC721Attribute createFromParcel(Parcel in)
        {
            return new ERC721Attribute(in);
        }

        @Override
        public ERC721Attribute[] newArray(int size)
        {
            return new ERC721Attribute[size];
        }
    };

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(type.ordinal());
        dest.writeString(attributeValue);
    }

    public enum ValueType
    {
       INTEGER,
       STRING
    };
}
