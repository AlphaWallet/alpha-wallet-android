package com.alphawallet.token.entity;

import java.util.Map;

/**
 * Created by JB on 23/05/2020.
 */
public abstract class EvaluateSelection
{
    private static final int STACK_CHECK = 10;

    public static boolean evaluate(TSFilterNode head, Map<String, TokenScriptResult.Attribute> attrs)
    {
        //evaluate from bottom up
        //evaluate each leaf logic
        //unevaluate all logic
        unevaluateAllNodes(head);
        evaluateLeafNodes(head, attrs);

        int stackCheck = STACK_CHECK; //prevent infinite loop in case of error

        while (stackCheck > 0 && head.logic == TSFilterNode.LogicState.NONE)
        {
            evaluateLogic(head);
            stackCheck--;
        }

        return (head.logic == TSFilterNode.LogicState.TRUE);
    }

    private static void unevaluateAllNodes(TSFilterNode node)
    {
        if (node.isNodeLogic() || node.isLeafLogic())
        {
            node.logic = TSFilterNode.LogicState.NONE;
        }

        if (node.first != null)
        {
            unevaluateAllNodes(node.first);
        }

        if (node.second != null)
        {
            unevaluateAllNodes(node.second);
        }
    }

    private static void evaluateLogic(TSFilterNode node)
    {
        //start evaluating logic nodes, start from the bottom
        if (node.isNodeLogic())
        {
            //check that children have been evaluated
            if (node.first.isEvaluated() && node.second.isEvaluated())
            {
                node.logic = node.evaluate();
            }
        }

        if (node.first != null)
        {
            evaluateLogic(node.first);
        }

        if (node.second != null)
        {
            evaluateLogic(node.second);
        }
    }

    private static void evaluateLeafNodes(TSFilterNode node, Map<String, TokenScriptResult.Attribute> attrs)
    {
        if (node.isLeafLogic())
        {
            //evaluate
            node.logic = node.evaluate(attrs);
        }

        if (node.first != null)
        {
            evaluateLeafNodes(node.first, attrs);
        }

        if (node.second != null)
        {
            evaluateLeafNodes(node.second, attrs);
        }
    }
}
