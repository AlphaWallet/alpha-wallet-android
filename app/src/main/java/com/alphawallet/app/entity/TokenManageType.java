package com.alphawallet.app.entity;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public abstract class TokenManageType {

    //Define the list of accepted constants
    @IntDef({LABEL_DISPLAY_TOKEN, DISPLAY_TOKEN, LABEL_HIDDEN_TOKEN, HIDDEN_TOKEN})

    //Tell the compiler not to store annotation data in the .class file
    @Retention(RetentionPolicy.SOURCE)
    //Declare the CarType annotation
    public @interface ManageType {}

    //Declare the constants
    public static final int LABEL_DISPLAY_TOKEN = 0;
    public static final int DISPLAY_TOKEN = 1;
    public static final int LABEL_HIDDEN_TOKEN = 2;
    public static final int HIDDEN_TOKEN = 3;

    @ManageType
    private int mType;

    @ManageType
    public int getTokenManageType(){
        return mType;
    };

    public void setTokenManageType(@ManageType int type){
        mType = type;
    }
}