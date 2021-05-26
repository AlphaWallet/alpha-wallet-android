package com.alphawallet.app.entity.tokens;

import android.os.Parcel;
import android.os.Parcelable;

public class ERC1155Asset implements Parcelable {
    private String iconUri;
    private String title;
    private String subtitle;
    private boolean selected;

    public ERC1155Asset(String iconUri, String title, String subtitle)
    {
        super();
        this.iconUri = iconUri;
        this.title = title;
        this.subtitle = subtitle;
    }

    protected ERC1155Asset(Parcel in)
    {
        iconUri = in.readString();
        title = in.readString();
        subtitle = in.readString();
        selected = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(iconUri);
        dest.writeString(title);
        dest.writeString(subtitle);
        dest.writeByte((byte) (selected ? 1 : 0));
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    public static final Creator<ERC1155Asset> CREATOR = new Creator<ERC1155Asset>() {
        @Override
        public ERC1155Asset createFromParcel(Parcel in)
        {
            return new ERC1155Asset(in);
        }

        @Override
        public ERC1155Asset[] newArray(int size)
        {
            return new ERC1155Asset[size];
        }
    };

    public String getIconUri()
    {
        return iconUri;
    }

    public String getTitle()
    {
        return title;
    }

    public String getSubtitle()
    {
        return subtitle;
    }

    public boolean isSelected()
    {
        return selected;
    }

    public void setSelected(boolean selected)
    {
        this.selected = selected;
    }
}
