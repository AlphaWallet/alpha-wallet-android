package com.alphawallet.token.entity;

import java.util.Map;

/**
 * Created by James on 2/04/2019.
 * Stormbird in Singapore
 */
public class TSAction
{
    public String type;
    public String exclude;
    public String view;
    public String style;

    public Map<String, AttributeType> attributeTypes;
    public FunctionDefinition function;
}
