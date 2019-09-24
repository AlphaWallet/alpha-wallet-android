package com.alphawallet.token.management.Model;

import java.math.BigInteger;

public class TextFieldDataModel {
    public String id;
    public String name;
    public BigInteger bitmask;
    public int bitshift;
    public String as;
    public String type;

    public String getId(){
        return this.id;
    }
    public void setId(String id){
        this.id=id;
    }
    public String getName(){
        return this.name;
    }
    public void setName(String name){
        this.name = name;
    }
    public BigInteger getBitmask(){
        return this.bitmask;
    }
    public void setBitmask(BigInteger bitmask){
        this.bitmask=bitmask;
    }
    public int getBitshift(){
        return this.bitshift;
    }
    public void setBitshift(int bitshift){
        this.bitshift=bitshift;
    }
    public String getAs(){
        return this.as;
    }
    public void setAs(String as){
        this.as=as;
    }
    public String getType(){
        return this.type;
    }
    public void setType(String type){
        this.type=type;
    }
}
