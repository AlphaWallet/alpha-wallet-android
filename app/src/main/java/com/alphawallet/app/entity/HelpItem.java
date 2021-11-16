package com.alphawallet.app.entity;

import android.os.Parcel;
import android.os.Parcelable;

public class HelpItem implements Parcelable {
    private final String question;
    private final String answer;
    private final int resource;

    private String eventName;

    public HelpItem(String question, String answer) {
        this.question = question;
        this.answer = answer;
        this.resource = 0;
    }

    public HelpItem(String question, int resource) {
        this.question = question;
        this.answer = "";
        this.resource = resource;
    }

    private HelpItem(Parcel in) {
        question = in.readString();
        answer = in.readString();
        resource = in.readInt();
    }

    public String getQuestion() {
        return this.question;
    }

    public String getAnswer() {
        return this.answer;
    }

    public int getResource() {
        return this.resource;
    }

    public static final Creator<HelpItem> CREATOR = new Creator<HelpItem>() {
        @Override
        public HelpItem createFromParcel(Parcel in) {
            return new HelpItem(in);
        }

        @Override
        public HelpItem[] newArray(int size) {
            return new HelpItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(question);
        parcel.writeString(answer);
        parcel.writeInt(resource);
    }
}
