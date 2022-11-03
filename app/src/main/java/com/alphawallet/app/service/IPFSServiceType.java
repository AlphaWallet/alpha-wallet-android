package com.alphawallet.app.service;

import java.io.IOException;

import okhttp3.Response;

/**
 * Created by JB on 4/11/2022.
 */
public interface IPFSServiceType
{
    String getContent(String request);
    Response performIO(String request) throws Exception;
    Response performIO(String request, String[] headers) throws IOException;
}
