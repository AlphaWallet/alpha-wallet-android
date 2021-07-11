package com.alphawallet.app.entity.nftassets;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.alphawallet.app.repository.entity.RealmNFTAsset;
import com.alphawallet.app.util.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by JB on 1/07/2021.
 */
public class NFTAsset implements Parcelable
{
    private static final String LOADING_TOKEN = "*Loading*";
    private static final String NAME = "name";
    private static final String IMAGE = "image";
    private static final String IMAGE_URL = "image_url";
    private static final String IMAGE_PREVIEW = "image_preview_url";
    private static final String DESCRIPTION = "description";
    private static final String IMAGE_ORIGINAL_URL = "image_original_url";
    private static final String[] IMAGE_DESIGNATORS = { IMAGE, IMAGE_URL, IMAGE_ORIGINAL_URL, IMAGE_PREVIEW };
    private static final String[] IMAGE_THUMBNAIL_DESIGNATORS = { IMAGE_PREVIEW, IMAGE, IMAGE_URL, IMAGE_ORIGINAL_URL };
    private static final String BACKGROUND_COLOUR = "background_color";
    private static final String EXTERNAL_LINK = "external_link";
    private static final List<String> DESIRED_PARAMS = Arrays.asList(NAME, BACKGROUND_COLOUR, IMAGE_URL, IMAGE, IMAGE_ORIGINAL_URL, IMAGE_PREVIEW, DESCRIPTION, EXTERNAL_LINK);
    private static final List<String> ATTRIBUTE_DESCRIPTOR = Arrays.asList("attributes", "traits");

    private final Map<String, String> assetMap = new HashMap<>();
    private final Map<String, String> attributeMap = new HashMap<>();

    public boolean isChecked = false;
    public boolean exposeRadio = false;

    public NFTAsset(String metaData)
    {
        assetMap.clear();
        attributeMap.clear();
        loadFromMetaData(metaData);
    }

    public NFTAsset()
    {
        assetMap.clear();
        attributeMap.clear();
        assetMap.put(LOADING_TOKEN, ".");
    }

    public NFTAsset(BigInteger tokenId)
    {
        assetMap.clear();
        attributeMap.clear();
        assetMap.put(NAME, "ID #" + tokenId.toString());
    }

    public String getAssetValue(String key)
    {
        return assetMap.get(key);
    }

    public Map<String, String> getAttributes() { return attributeMap; }

    public String getAttributeValue(String key)
    {
        return attributeMap.get(key);
    }

    public String getName()
    {
        return assetMap.get(NAME);
    }

    public String getImage()
    {
        for (String key : IMAGE_DESIGNATORS)
        {
            if (assetMap.containsKey(key))
            {
                return Utils.parseIPFS(assetMap.get(key));
            }
        }

        return "";
    }

    public String getThumbnail()
    {
        for (String key : IMAGE_THUMBNAIL_DESIGNATORS)
        {
            if (assetMap.containsKey(key))
            {
                return Utils.parseIPFS(assetMap.get(key));
            }
        }

        return "";
    }

    public String getBackgroundColor()
    {
        return assetMap.get(BACKGROUND_COLOUR);
    }

    public String getDescription()
    {
        return assetMap.get(DESCRIPTION);
    }

    public String getExternalLink()
    {
        return assetMap.get(EXTERNAL_LINK);
    }

    protected NFTAsset(Parcel in)
    {
        int assetCount = in.readInt();
        for (int i = 0; i < assetCount; i++)
        {
            assetMap.put(in.readString(), in.readString());
        }

        int attrCount = in.readInt();
        for (int i = 0; i < attrCount; i++)
        {
            attributeMap.put(in.readString(), in.readString());
        }
    }

    private void loadFromMetaData(String metaData)
    {
        //build asset and trait map
        try
        {
            JSONObject jsonData = new JSONObject(metaData);
            Iterator<String> keys = jsonData.keys();
            while (keys.hasNext())
            {
                String key = keys.next();
                String value = jsonData.getString(key);
                if (!ATTRIBUTE_DESCRIPTOR.contains(key))
                {
                    if (validJSONString(value) && DESIRED_PARAMS.contains(key)) assetMap.put(key, value);
                }
                else
                {
                    JSONArray attrArray = jsonData.getJSONArray(key);
                    for (int i = 0; i < attrArray.length(); i++)
                    {
                        JSONObject order = (JSONObject) attrArray.get(i);
                        if (validJSONString(order.getString("value"))) attributeMap.put(order.getString("trait_type"), order.getString("value"));
                    }
                }
            }
        }
        catch (JSONException e)
        {
            //
        }
    }

    private boolean validJSONString(String value)
    {
        return (!TextUtils.isEmpty(value) && !value.equals("null"));
    }

    public String jsonMetaData()
    {
        JSONObject jsonData = new JSONObject();
        try
        {
            for (String key : assetMap.keySet())
            {
                jsonData.put(key, assetMap.get(key));
            }

            JSONArray attrs = new JSONArray();
            int index = 0;
            for (String key : attributeMap.keySet())
            {
                JSONObject thisEntry = new JSONObject().put("trait_type", key).put("value", attributeMap.get(key));
                attrs.put(index, thisEntry);
                index++;
            }

            if (attributeMap.size() > 0) jsonData.put("attributes", attrs);
        }
        catch (JSONException e)
        {
            //
        }

        return jsonData.toString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(assetMap.size());
        for (String key : assetMap.keySet())
        {
            dest.writeString(key);
            dest.writeString(assetMap.get(key));
        }
        dest.writeInt(attributeMap.size());
        for (String key : attributeMap.keySet())
        {
            dest.writeString(key);
            dest.writeString(attributeMap.get(key));
        }
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    public static final Creator<NFTAsset> CREATOR = new Creator<NFTAsset>()
    {
        @Override
        public NFTAsset createFromParcel(Parcel in)
        {
            return new NFTAsset(in);
        }

        @Override
        public NFTAsset[] newArray(int size)
        {
            return new NFTAsset[size];
        }
    };

    public boolean needsLoading()
    {
        return (assetMap.size() == 0 || assetMap.containsKey(LOADING_TOKEN));
    }

    public boolean requiresReplacement()
    {
        return (needsLoading() || !assetMap.containsKey(NAME) || TextUtils.isEmpty(getImage()));
    }

    @Override
    public int hashCode()
    {
        return assetMap.hashCode() + attributeMap.hashCode();
    }

    public boolean equals(RealmNFTAsset realmAsset)
    {
        return (hashCode() == new NFTAsset(realmAsset.getMetaData()).hashCode());
    }

    public boolean isBlank()
    {
        return assetMap.size() == 0;
    }

    public void updateFromRaw(NFTAsset oldAsset)
    {
        //add thumbnail if it existed
        if (oldAsset.assetMap.containsKey(IMAGE_PREVIEW)) assetMap.put(IMAGE_PREVIEW, oldAsset.getAssetValue(IMAGE_PREVIEW));
    }

    public void updateAsset(BigInteger tokenId, Map<BigInteger, NFTAsset> oldAssets)
    {
        NFTAsset oldAsset = oldAssets != null ? oldAssets.get(tokenId) : null;
        updateAsset(oldAsset);
    }

    public void updateAsset(NFTAsset oldAsset)
    {
        if (oldAsset != null)
        {
            for (String param : oldAsset.assetMap.keySet()) //add anything that the old asset had, which new one doesn't (if it's still required)
            {
                if (assetMap.get(param) == null && DESIRED_PARAMS.contains(param)) attributeMap.put(param, oldAsset.assetMap.get(param));
            }
        }
    }
}