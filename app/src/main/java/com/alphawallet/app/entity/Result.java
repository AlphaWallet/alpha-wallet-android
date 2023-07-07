package com.alphawallet.app.entity;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

public class Result
{
    @SerializedName("count")
    @Expose
    public String count;

    @SerializedName("next")
    @Expose
    public String next;

    @SerializedName("previous")
    @Expose
    public String previous;

    @SerializedName("results")
    @Expose
    public List<Signature> signatures;

    public static class Signature implements Comparable<Signature>
    {
        @SerializedName("id")
        @Expose
        public long id;

        @SerializedName("created_at")
        @Expose
        public String created_at;

        @SerializedName("text_signature")
        @Expose
        public String text_signature;

        @SerializedName("hex_signature")
        @Expose
        public String hex_signature;

        @SerializedName("bytes_signature")
        @Expose
        public String bytes_signature;

        @Override
        public int compareTo(Signature signature)
        {
            return Long.compare(id, signature.id);
        }
    }

    public Signature getFirst()
    {
        if (signatures != null && signatures.size() > 0)
        {
            Collections.sort(signatures);
            return signatures.get(0);
        }
        else
        {
            return null;
        }
    }
}