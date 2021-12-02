package com.alphawallet.app.entity;

import android.os.Parcel;
import android.os.Parcelable;

public class MarketplaceEvent implements Parcelable {
    private final String eventName;

    public MarketplaceEvent(String eventName) {
        this.eventName = eventName;
    }

    private MarketplaceEvent(Parcel in) {
        eventName = in.readString();
    }

    public String getEventName() {
        return this.eventName;
    }

    public static final Creator<MarketplaceEvent> CREATOR = new Creator<MarketplaceEvent>() {
        @Override
        public MarketplaceEvent createFromParcel(Parcel in) {
            return new MarketplaceEvent(in);
        }

        @Override
        public MarketplaceEvent[] newArray(int size) {
            return new MarketplaceEvent[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(eventName);
    }
}
