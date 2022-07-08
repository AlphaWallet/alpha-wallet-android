package com.alphawallet.app.util;

public class JsonUtils {
    public static final String EMPTY_RESULT = "{\"noresult\":[]}";

    public static boolean hasAssets(String jsonData)
    {
        return jsonData != null &&
                jsonData.length() >= 10 &&
                jsonData.contains("assets");
    }

    public static boolean isEmpty(String jsonData)
    {
        return jsonData.equalsIgnoreCase(EMPTY_RESULT);
    }

    public static boolean isValidAsset(String jsonData)
    {
        return !jsonData.isEmpty() &&
                jsonData.length() > 15 &&
                jsonData.contains("name");
    }

    public static boolean isValidCollection(String jsonData)
    {
        return !jsonData.isEmpty() &&
                jsonData.length() > 15 &&
                jsonData.contains("collection");
    }
}
