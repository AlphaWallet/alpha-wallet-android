package com.alphawallet.app.web3;

import com.alphawallet.app.web3.entity.Message;

import java.util.Map;

/**
 * Created by JB on 1/05/2020.
 */
public interface OnSetValuesListener
{
    void setValues(Map<String, String> values);
}
