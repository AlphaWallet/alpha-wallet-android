package io.stormbird.wallet.entity;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Created by James on 3/10/2018.
 * Stormbird in Singapore
 */
public class OpenseaElement implements Parcelable {
    public long tokenId;
    public String imageUrl;
    public String name;
    public String description;
    public String externalLink;
    public String backgroundColor;
    public ArrayList<ERC721Attribute> traits = new ArrayList<>();

    protected OpenseaElement(Parcel in) {
        tokenId = in.readLong();
        imageUrl = in.readString();
        name = in.readString();
        description = in.readString();
        externalLink = in.readString();
        backgroundColor = in.readString();
        traits = in.createTypedArrayList(ERC721Attribute.CREATOR);
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

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(tokenId);
        dest.writeString(imageUrl);
        dest.writeString(name);
        dest.writeString(description);
        dest.writeString(externalLink);
        dest.writeString(backgroundColor);
        dest.writeTypedList(traits);
    }

    public OpenseaElement() {
        traits.clear();
    }

    @Override
    public int describeContents() {
        return traits.size();
    }

    public ERC721Attribute getTraitFromType(String key) {
        for (ERC721Attribute trait : this.traits) {
            if (trait.getTraitType() != null && trait.getTraitType().equals(key)) {
                return trait;
            }
        }
        return null;
    }
}
