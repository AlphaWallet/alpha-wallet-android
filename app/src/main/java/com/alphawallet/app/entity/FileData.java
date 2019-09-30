package com.alphawallet.app.entity;

public class FileData
{
    public Long fileDate;
    public String fileName;
    public boolean modified;

    public FileData()
    {
        fileDate = 0L;
        fileName = "";
        modified = false;
    }
}