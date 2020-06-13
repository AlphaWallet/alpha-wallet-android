package com.alphawallet.token.entity;

import java.math.BigInteger;
import java.util.Map;
import java.util.regex.Matcher;

import static com.alphawallet.token.entity.TSSelection.decodeParam;

/**
 * Created by JB on 21/05/2020.
 */
public class TSFilterNode
{
    public TSFilterNode parent = null;
    public TSFilterNode first = null;
    public TSFilterNode second = null;
    public FilterType type;
    boolean negate;
    public BigInteger value = null;
    String strValue = null;
    LogicState logic = LogicState.NONE;

    public TSFilterNode(String val, TSFilterNode p, FilterType t)
    {
        if (t == FilterType.ATTRIBUTE)
        {
            val = extractAttribute(val);
        }

        type = t;
        try
        {
            value = new BigInteger(val);
            strValue = val;
        }
        catch (Exception e)
        {
            if (val.equalsIgnoreCase("true"))
            {
                logic = LogicState.TRUE;
            }
            else if (val.equalsIgnoreCase("false"))
            {
                logic = LogicState.FALSE;
            }
            else
            {
                strValue = val;
            }
            value = null;
        }
        parent = p;
    }

    public TSFilterNode(FilterType type, TSFilterNode p)
    {
        this.type = type;
        parent = p;
    }

    public boolean isLeafLogic()
    {
        return isLeafLogic(type);
    }

    public static boolean isLeafLogic(FilterType type)
    {
        switch (type)
        {
            case AND:
            case OR:
            case NOT:
                return false;
            case LESS_THAN:
            case EQUAL:
            case GREATER_THAN:
            case GREATER_THAN_OR_EQUAL:
            case LESS_THAN_OR_EQUAL_TO:
                return true;
            default:
            case VALUE:
                return false;
        }
    }

    public boolean isNodeLogic()
    {
        switch (type)
        {
            case AND:
            case OR:
                return true;
            default:
                return false;
        }
    }

    LogicState evaluate(Map<String, TokenScriptResult.Attribute> attrs)
    {
        String valueLeftStr = getValue(first, attrs);
        String valueRightStr = getValue(second, attrs);

        BigInteger valueLeft = getBIValue(first, attrs);
        BigInteger valueRight = getBIValue(second, attrs);

        boolean bothSidesValues = valueLeft != null && valueRight != null;
        if (valueLeftStr == null || valueRightStr == null) return LogicState.FALSE;

        //perform comparison
        switch (type)
        {
            case EQUAL:
                //compare strings
                return compareLogic(valueLeftStr.equalsIgnoreCase(valueRightStr));
            case GREATER_THAN:
                //both sides must be values
                if (!bothSidesValues) return LogicState.FALSE;
                return compareLogic(valueLeft.compareTo(valueRight) > 0);
            case LESS_THAN:
                //both sides must be values
                if (!bothSidesValues) return LogicState.FALSE;
                return compareLogic(valueLeft.compareTo(valueRight) < 0);
            case GREATER_THAN_OR_EQUAL:
                //both sides must be values
                if (!bothSidesValues) return LogicState.FALSE;
                return compareLogic(valueLeft.compareTo(valueRight) >= 0);
            case LESS_THAN_OR_EQUAL_TO:
                //both sides must be values
                if (!bothSidesValues) return LogicState.FALSE;
                return compareLogic(valueLeft.compareTo(valueRight) <= 0);
            default:
                // should have caught this previously
                return LogicState.FALSE;
        }
    }

    private LogicState compareLogic(boolean comparison)
    {
        if (comparison)
        {
            return negate ? LogicState.FALSE : LogicState.TRUE;
        }
        else
        {
            return negate ? LogicState.TRUE : LogicState.FALSE;
        }
    }

    private BigInteger getBIValue(TSFilterNode node, Map<String, TokenScriptResult.Attribute> attrs)
    {
        BigInteger returnValue = null;
        if (node.strValue != null && node.strValue.length() > 0)
        {
            TokenScriptResult.Attribute attr = attrs.get(node.strValue);
            if (attr != null)
            {
                //found an attribute
                returnValue = attr.value;
            }
            else
            {
                returnValue = node.value;
            }
        }

        return returnValue;
    }

    private String getValue(TSFilterNode node, Map<String, TokenScriptResult.Attribute> attrs)
    {
        String returnValue = null;

        if (node.logic != null && node.logic != LogicState.NONE)
        {
            returnValue = node.logic.toString();
        }
        else if (node.type == FilterType.ATTRIBUTE)
        {
            TokenScriptResult.Attribute attr = attrs.get(node.strValue);
            if (attr != null)
            {
                //found an attribute
                returnValue = attr.text;
            }
        }
        else if (node.strValue != null && node.strValue.length() > 0)
        {
            returnValue = node.strValue;
        }

        return returnValue;
    }

    boolean isEvaluated()
    {
        if ((isNodeLogic() || isLeafLogic()) && logic != TSFilterNode.LogicState.NONE)
        {
            return true;
        }
        else
        {
            return type == FilterType.VALUE;
        }
    }

    LogicState evaluate()
    {
        if (!isNodeLogic()) return LogicState.NONE;

        boolean eval = first.logic == LogicState.TRUE && second.logic == LogicState.TRUE;
        if (eval)
        {
            return negate ? LogicState.FALSE : LogicState.TRUE;
        }
        else
        {
            return negate ? LogicState.TRUE : LogicState.FALSE;
        }
    }

    private String extractAttribute(String val)
    {
        Matcher matcher = decodeParam.matcher(val);
        if (matcher.find() && !matcher.group(1).isEmpty())
        {
            //found an attribute param
            return matcher.group(1);
        }
        else
        {
            return val;
        }
    }

    enum LogicState
    {
        NONE, TRUE, FALSE
    }
}
