package com.alphawallet.token.entity;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.w3c.dom.Node.ELEMENT_NODE;

/**
 * Created by JB on 21/05/2020.
 */
public class TSSelection
{
    public TSFilterNode head = null;
    public String denialMessage = null;
    public Map<String, String> names = null; //use these names if the selection filter is true
    private boolean negate = false;
    public String name = null;

    public static final Pattern decodeParam = Pattern.compile("[$][{](\\w*)[}]$");

    public TSSelection(String filterExpression) throws SAXException
    {
        //tokenise
        List<String> tokens = tokeniseExpression(filterExpression);
        ListIterator<String> tokenItr = tokens.listIterator();

        //recursive parse
        head = parseNextNode(null, tokenItr);
    }

    private TSFilterNode parseNextNode(TSFilterNode currentNode, ListIterator<String> tokens) throws SAXException
    {
        if (!tokens.hasNext()) return currentNode;
        String token = tokens.next();
        FilterType type = getType(token);
        TSFilterNode thisNode;

        //If value, must be a leaf logic
        //If brace, start reading braced tree
        //If logic, start a logic tree
        switch (type)
        {
            case AND:
            case OR:
                thisNode = new TSFilterNode(type, currentNode); // now we're expecting leaf logic or the start of another tree
                if (negate)
                {
                    thisNode.negate = true;
                    negate = false;
                }
                thisNode.first = parseNextNode(thisNode, tokens);
                thisNode.second = parseNextNode(thisNode, tokens);
                return thisNode;
            case NOT:
                negate = true;
                return parseNextNode(currentNode, tokens);
            case GREATER_THAN:
            case LESS_THAN:
            case EQUAL:
                //this should occur between two leaf nodes, shouldn't see this here
                if (tokens.hasPrevious())
                {
                    throw new SAXException("PARSE ERROR: Unexpected '" + type.toString() + "' after " + tokens.previous()); // TODO: character place
                }
                else
                {
                    throw new SAXException("PARSE ERROR: Unexpected '" + type.toString() + "'");
                }
            case VALUE:
                //leaf logic; should be of the form "a" <comparator> "b"
                return parseLeafConstruct(currentNode, tokens);
            case START_BRACE:
                //parse all in here until end of this brace
                int braceLevel = 1;
                List<String> tokensInBrace = new ArrayList<>();
                while (braceLevel > 0 && tokens.hasNext())
                {
                    String braceToken = tokens.next();
                    FilterType braceType = getType(braceToken);
                    if (braceType == FilterType.END_BRACE)
                    {
                        braceLevel--;
                    }
                    else if (braceType == FilterType.START_BRACE)
                    {
                        braceLevel++;
                    }

                    if (braceLevel > 0)
                        tokensInBrace.add(braceToken);
                }

                if (braceLevel != 0)
                {
                    if (tokensInBrace.size() > 0) throw new SAXException("PARSE ERROR: Unterminated brace in filter expression after '" + tokensInBrace.get(tokensInBrace.size()-1) + "'");
                    else throw new SAXException("PARSE ERROR: Unterminated brace in filter expression");
                }

                return parseNextNode(currentNode, tokensInBrace.listIterator());
            case END_BRACE:
                if (tokens.hasPrevious())
                {
                    throw new SAXException("PARSE ERROR: Unexpected ')' after " + tokens.previous()); // TODO: character place
                }
                else
                {
                    throw new SAXException("PARSE ERROR: Unexpected ')'");
                }
        }

        return currentNode;
    }

    private TSFilterNode parseLeafConstruct(TSFilterNode currentNode, ListIterator<String> tokens) throws SAXException
    {
        //parse the form "a" <comparator> "b"
        String a = tokens.previous();
        if (!tokens.hasNext())
        {
            throw new SAXException("PARSE ERROR: No comparator after '" + a);
        }

        tokens.next(); //advance past previous

        String logic = tokens.next();
        FilterType typeLogic = getType(logic);
        if (!TSFilterNode.isLeafLogic(typeLogic))
        {
            throw new SAXException("PARSE ERROR: Unexpected comparator '" + logic + "' after '" + a);
        }

        if (!tokens.hasNext())
        {
            throw new SAXException("PARSE ERROR: No comparator subject after '" + a + logic);
        }

        String b = tokens.next();
        FilterType typeB = getType(b);
        if (!(typeB == FilterType.VALUE || typeB == FilterType.ATTRIBUTE))
        {
            throw new SAXException("PARSE ERROR: Invalid subject after '" + a + logic);
        }

        TSFilterNode comparator = new TSFilterNode(typeLogic, currentNode);
        comparator.first = new TSFilterNode(a, comparator, FilterType.ATTRIBUTE);
        comparator.second = new TSFilterNode(b, comparator, typeB);
        if (negate)
        {
            comparator.negate = true;
            negate = false;
        }

        return comparator;
    }

    private FilterType getType(String token)
    {
        FilterType type;
        switch (token)
        {
            case "|":
                type = FilterType.OR;
                break;
            case "&":
                type = FilterType.AND;
                break;
            case "<=":
                type = FilterType.LESS_THAN_OR_EQUAL_TO;
                break;
            case ">=":
                type = FilterType.GREATER_THAN_OR_EQUAL;
                break;
            case "<":
                type = FilterType.LESS_THAN;
                break;
            case ">":
                type = FilterType.GREATER_THAN;
                break;
            case "(":
                type = FilterType.START_BRACE;
                break;
            case ")":
                type = FilterType.END_BRACE;
                break;
            case "=":
                type = FilterType.EQUAL;
                break;
            case "!":
                type = FilterType.NOT;
                break;
            default:
                type = unwrapValue(token);
                break;
        }

        return type;
    }

    private List<String> tokeniseExpression(String filterExpression) throws SAXException
    {
        List<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        int remaining;
        for (int index = 0; index < filterExpression.length(); index++)
        {
            remaining = (filterExpression.length() - 1) - index;

            String c = Character.toString(filterExpression.charAt(index));
            switch (filterExpression.charAt(index))
            {
                case '|':
                    addTokens(tokens, c, token);
                    break;
                case '&':
                    addTokens(tokens, c, token);
                    break;
                case '>':
                case '<':
                    if (remaining > 1 && filterExpression.charAt(index + 1) == '=')
                    {
                        addTokens(tokens, c + filterExpression.charAt(index + 1), token);
                        index++;
                    }
                    else
                    {
                        addTokens(tokens, c, token);
                    }
                    break;
                case '(':
                    addTokens(tokens, c, token);
                    break;
                case ')':
                    addTokens(tokens, c, token);
                    break;
                case '=':
                    addTokens(tokens, c, token);
                    break;
                case '!':
                    addTokens(tokens, c, token);
                    break;
                default:
                    if (!Character.isWhitespace(c.charAt(0)))
                    {
                        token.append(c);
                    }
                    break;
            }
        }

        if (token.length() > 0)
        {
            tokens.add(token.toString());
        }

        return tokens;
    }

    private void addTokens(List<String> tokens, String logic, StringBuilder token)
    {
        if (token.length() > 0) tokens.add(token.toString());
        token.setLength(0);
        tokens.add(logic);
    }

    public List<String> getRequiredAttrs()
    {
        List<String> attrs = new ArrayList<>();
        return crawlTreeForAttrs(head, attrs);
    }

    private List<String> crawlTreeForAttrs(TSFilterNode node, List<String> attrs)
    {
        //left side
        if (node.strValue != null && node.value == null)
        {
            //leaf
            if (!attrs.contains(node.strValue)) attrs.add(node.strValue);
        }

        if (node.first != null)
        {
            crawlTreeForAttrs(node.first, attrs);
        }

        if (node.second != null)
        {
            crawlTreeForAttrs(node.second, attrs);
        }

        return attrs;
    }

    private FilterType unwrapValue(String token)
    {
        Matcher matcher = decodeParam.matcher(token);
        if (matcher.find() && !matcher.group(1).isEmpty())
        {
            //found an attribute param
            return FilterType.ATTRIBUTE;
        }
        else
        {
            return FilterType.VALUE;
        }
    }

    public boolean checkParse()
    {
        return name != null && name.length() > 0
                && head != null;
    }
}
