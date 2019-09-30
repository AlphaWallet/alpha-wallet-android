package com.alphawallet.token.entity;

/**
 * Created by James on 11/04/2019.
 * Stormbird in Singapore
 */
public interface ParseResult
{
    enum ParseResultId
    {
        OK,
        XML_OUT_OF_DATE,
        PARSER_OUT_OF_DATE,
        PARSE_FAILED
    };

    void parseMessage(ParseResultId parseResult);
}
