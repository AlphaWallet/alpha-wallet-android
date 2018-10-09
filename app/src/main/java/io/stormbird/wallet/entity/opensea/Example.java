
package io.stormbird.wallet.entity.opensea;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Example implements Parcelable {

    @SerializedName("estimated_count")
    @Expose
    private long estimatedCount;
    @SerializedName("assets")
    @Expose
    private List<Asset> assets = null;

    protected Example(Parcel in) {
        estimatedCount = in.readLong();
    }

    public static final Creator<Example> CREATOR = new Creator<Example>() {
        @Override
        public Example createFromParcel(Parcel in) {
            return new Example(in);
        }

        @Override
        public Example[] newArray(int size) {
            return new Example[size];
        }
    };

    public long getEstimatedCount() {
        return estimatedCount;
    }

    public void setEstimatedCount(long estimatedCount) {
        this.estimatedCount = estimatedCount;
    }

    public Example withEstimatedCount(long estimatedCount) {
        this.estimatedCount = estimatedCount;
        return this;
    }

    public List<Asset> getAssets() {
        return assets;
    }

    public void setAssets(List<Asset> assets) {
        this.assets = assets;
    }

    public Example withAssets(List<Asset> assets) {
        this.assets = assets;
        return this;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(estimatedCount);
    }
}
