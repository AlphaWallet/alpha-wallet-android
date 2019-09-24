package com.alphawallet.app.entity;

import android.os.Parcel;
import android.os.Parcelable;

public class HelpItem implements Parcelable {
    private String question;
    private String answer;

    private String eventName;

    public HelpItem(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }

    private HelpItem(Parcel in) {
        question = in.readString();
        answer = in.readString();
    }

    public String getQuestion() {
        return this.question;
    }

    public String getAnswer() {
        return this.answer;
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
    }
}
