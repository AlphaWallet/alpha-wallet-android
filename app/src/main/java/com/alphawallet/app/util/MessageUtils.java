package com.alphawallet.app.util;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;

import com.alphawallet.app.web3j.StructuredDataEncoder;
import com.alphawallet.token.entity.EthereumWriteBuffer;
import com.alphawallet.token.entity.ProviderTypedData;

import org.web3j.utils.Numeric;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by JB on 27/08/2020.
 */
public class MessageUtils
{
    /**
     * Encode params for hashing - the algorithm is very simple, reduce the types like this:
     * (string Message, uint32 value, bytes32 data) into a string list like this:
     * "string Messageuint32 valuebytes32 data"
     *
     * @param rawData
     * @return
     */
    public static byte[] encodeParams(ProviderTypedData[] rawData)
    {
        //form the params for hashing
        StringBuilder sb = new StringBuilder();
        int len = rawData.length;
        for (int i = 0; i < len; i++)
        {
            sb.append(rawData[i].type).append(" ").append(rawData[i].name);
        }

        return sb.toString().getBytes();
    }

    /**
     * This routine ported from the reference implementation code at https://github.com/MetaMask/eth-sig-util
     * @param rawData
     * @return
     */
    public static byte[] encodeValues(ProviderTypedData[] rawData) throws IOException
    {
        int size;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        EthereumWriteBuffer eb = new EthereumWriteBuffer(buffer);

        for (ProviderTypedData data : rawData)
        {
            String type = data.type;
            String value = (String)data.value;

            if (type.equals("bytes"))
            {
                eb.write(Numeric.hexStringToByteArray(value));
            }
            else if (type.equals("string"))
            {
                eb.write(value.getBytes());
            }
            else if (type.equals("bool"))
            {
                eb.write(((boolean) data.value) ? (byte)0x01 : (byte)0x00);
            }
            else if (type.equals("address"))
            {
                eb.writeAddress(value);
            }
            else if (type.startsWith("bytes"))
            {
                size = parseTypeN(type);
                if (size < 1 || size > 32) {
                    throw new NumberFormatException("Invalid bytes<N> width: " + size);
                }

                eb.writeBytes(value, size);
            }
            else if (type.startsWith("uint"))
            {
                size = parseTypeN(type);
                if ((size < 8) || (size > 256))
                {
                    throw new NumberFormatException("Invalid uint<N> width: " + size);
                }

                eb.writeValue(value, size / 8);
            }
            else if (type.startsWith("int"))
            {
                size = parseTypeN(type);
                if ((size < 8) || (size > 256))
                {
                    throw new NumberFormatException("Invalid uint<N> width: " + size);
                }

                eb.writeValue(value, size / 8);
            }
            else
            {
                // FIXME: support all other types
                throw new NumberFormatException("Unsupported or invalid type: " + type);
            }
        }

        return buffer.toByteArray();
    }

    public static CharSequence formatTypedMessage(ProviderTypedData[] rawData)
    {
        //produce readable text to display in the signing prompt
        SpannableStringBuilder sb = new SpannableStringBuilder();
        boolean firstVal = true;
        List<SpanType> spanners = new ArrayList<>();
        for (ProviderTypedData data : rawData)
        {
            if (!firstVal) sb.append("\n");
            int begin = sb.length();
            sb.append(data.name).append(":");
            spanners.add(new SpanType(begin, sb.length(), new StyleSpan(Typeface.BOLD)));
            sb.append("\n  ").append(data.value.toString());
            firstVal = false;
        }

        for (SpanType s : spanners)
        {
            sb.setSpan(new StyleSpan(Typeface.BOLD), s.begin, s.end, Spanned.SPAN_POINT_POINT);
        }

        return sb;
    }

    public static CharSequence formatEIP721Message(StructuredDataEncoder messageData)
    {
        HashMap<String, Object> messageMap = (HashMap<String, Object>) messageData.jsonMessageObject.getMessage();
        SpannableStringBuilder sb = new SpannableStringBuilder();
        List<SpanType> spanners = new ArrayList<>();
        for (String entry : messageMap.keySet())
        {
            int begin = sb.length();
            sb.append(entry).append(":").append("\n");
            spanners.add(new SpanType(begin, sb.length(), new StyleSpan(Typeface.BOLD)));
            Object v = messageMap.get(entry);
            if (v instanceof LinkedHashMap)
            {
                HashMap<String, Object> valueMap = (HashMap<String, Object>) messageMap.get(entry);
                for (String paramName : valueMap.keySet())
                {
                    String value = valueMap.get(paramName).toString();
                    begin = sb.length();
                    sb.append(" ").append(paramName).append(": ");
                    spanners.add(new SpanType(begin, sb.length(), new StyleSpan(Typeface.BOLD)));
                    sb.append(value).append("\n");
                }
            }
            else
            {
                sb.append(" ").append(v.toString()).append("\n");
            }
        }

        for (SpanType s : spanners)
        {
            sb.setSpan(new StyleSpan(Typeface.BOLD), s.begin, s.end, Spanned.SPAN_POINT_POINT);
        }

        return sb;
    }

    private static int parseTypeN(String type)
    {
        Matcher m = Pattern.compile("^\\D+(\\d+)$").matcher(type);
        if (m.find())
        {
            String match = m.group(1);
            if (match != null && match.length() > 0)
            {
                return Integer.parseInt(match);
            }
        }

        return 256; //if no value then default to 256
    }

    private static class SpanType
    {
        int begin;
        int end;
        StyleSpan style;

        public SpanType(int begin, int end, StyleSpan style)
        {
            this.begin = begin;
            this.end = end;
            this.style = style;
        }
    }
}
