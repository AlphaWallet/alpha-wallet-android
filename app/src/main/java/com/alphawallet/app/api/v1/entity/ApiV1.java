package com.alphawallet.app.api.v1.entity;

import java.util.Arrays;
import java.util.List;

public class ApiV1
{
    public static final Method CONNECT = new Method(ApiV1.Path.CONNECT, ApiV1.CallType.CONNECT);
    public static final Method SIGN_PERSONAL_MESSAGE = new Method(ApiV1.Path.SIGN_PERSONAL_MESSAGE, ApiV1.CallType.SIGN_PERSONAL_MESSAGE);

    public static final List<Method> VALID_METHODS = Arrays.asList(
            CONNECT,
            SIGN_PERSONAL_MESSAGE
    );

    public static class Path
    {
        public static final String CONNECT = "/wallet/v1/connect";
        public static final String SIGN_PERSONAL_MESSAGE = "/wallet/v1/signpersonalmessage";
    }

    public static class CallType
    {
        public static final String CONNECT = "connect";
        public static final String SIGN_PERSONAL_MESSAGE = "signpersonalmessage";
    }

    public static class RequestParams
    {
        public static final String REDIRECT_URL = "redirecturl";
        public static final String METADATA = "metadata";
        public static final String MESSAGE = "message";
        public static final String ADDRESS = "address";
    }

    public static class ResponseParams
    {
        public static final String CALL = "call";
        public static final String ADDRESS = "address";
        public static final String SIGNATURE = "signature";
    }
}
