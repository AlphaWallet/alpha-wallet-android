package com.alphawallet.token.entity;

import java.util.Map;

/**
 * Created by JB on 27/07/2020.
 */
public class TSActivity
{
    public int order;
    public String exclude;
    public TSTokenView view;
    public String style = "";
    public String name;

    public Map<String, Attribute> attributes;
    public FunctionDefinition function;
}
