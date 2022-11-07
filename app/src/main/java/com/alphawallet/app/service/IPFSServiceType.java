package com.alphawallet.app.service;

import com.alphawallet.app.entity.QueryResponse;

import java.io.IOException;

/**
 * Created by JB on 4/11/2022.
 */
public interface IPFSServiceType
{
    String getContent(String request);
    QueryResponse performIO(String request, String[] headers) throws IOException;
}
