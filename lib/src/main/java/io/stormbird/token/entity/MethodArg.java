package io.stormbird.token.entity;

/**
 * Created by James on 2/05/2019.
 * Stormbird in Sydney
 */

// A param to pass into a smart contract function call
public class MethodArg
{
    public String parameterType; //type of param eg uint256, address etc
    public String ref;   //reference to the value to pass
    public String value; //the actual value to pass
}
