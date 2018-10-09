
package io.stormbird.wallet.entity.opensea;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DisplayData implements Parcelable {

    @SerializedName("images")
    @Expose
    private List<String> images = null;

    protected DisplayData(Parcel in) {
        images = in.createStringArrayList();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringList(images);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<DisplayData> CREATOR = new Creator<DisplayData>() {
        @Override
        public DisplayData createFromParcel(Parcel in) {
            return new DisplayData(in);
        }

        @Override
        public DisplayData[] newArray(int size) {
            return new DisplayData[size];
        }
    };

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public DisplayData withImages(List<String> images) {
        this.images = images;
        return this;
    }

}
