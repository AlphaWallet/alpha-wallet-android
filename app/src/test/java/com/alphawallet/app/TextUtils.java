package com.alphawallet.app;

/**
 * Created by JB on 24/08/2020.
 */
class TextUtils
{
    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }

    public static String rot(String input)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++)
        {
            char c = input.charAt(i);
            if       (c >= 'a' && c <= 'm') c += 13;
            else if  (c >= 'A' && c <= 'M') c += 13;
            else if  (c >= 'n' && c <= 'z') c -= 13;
            else if  (c >= 'N' && c <= 'Z') c -= 13;
            else if  (c >= '1' && c <= '9') c -= 1;
            else if  (c == '0') c = '9';
            sb.append(c);
        }
        return sb.toString();
    }

    public static String rotEnc(String input)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++)
        {
            char c = input.charAt(i);
            if       (c >= 'a' && c <= 'm') c += 13;
            else if  (c >= 'A' && c <= 'M') c += 13;
            else if  (c >= 'n' && c <= 'z') c -= 13;
            else if  (c >= 'N' && c <= 'Z') c -= 13;
            else if  (c >= '0' && c <= '8') c += 1;
            else if  (c == '9') c = '0';
            sb.append(c);
        }
        return sb.toString();
    }
}
