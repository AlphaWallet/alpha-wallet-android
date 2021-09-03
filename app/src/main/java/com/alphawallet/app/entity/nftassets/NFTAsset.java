package com.alphawallet.app.entity.nftassets;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.alphawallet.app.entity.tokens.ERC1155Token;
import com.alphawallet.app.repository.entity.RealmNFTAsset;
import com.alphawallet.app.util.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    private static final String[] SVG_OVERRIDE = { IMAGE_ORIGINAL_URL, IMAGE, IMAGE_URL };
    private static final String[] IMAGE_THUMBNAIL_DESIGNATORS = { IMAGE_PREVIEW, IMAGE, IMAGE_URL, IMAGE_ORIGINAL_URL };
    private static final String BACKGROUND_COLOUR = "background_color";
    private static final String EXTERNAL_LINK = "external_link";
    private static final List<String> DESIRED_PARAMS = Arrays.asList(NAME, BACKGROUND_COLOUR, IMAGE_URL, IMAGE, IMAGE_ORIGINAL_URL, IMAGE_PREVIEW, DESCRIPTION, EXTERNAL_LINK);
    private static final List<String> ATTRIBUTE_DESCRIPTOR = Arrays.asList("attributes", "traits");

    private final Map<String, String> assetMap = new HashMap<>();
    private final Map<String, String> attributeMap = new HashMap<>();

    private BigDecimal balance; //for ERC1155
    private BigDecimal selected; //for ERC1155 transfer

    private List<BigInteger> tokenIdList; // for ERC1155 collections

    public boolean isChecked = false;
    public boolean exposeRadio = false;

    public NFTAsset(String metaData)
    {
        loadFromMetaData(metaData);
        balance = BigDecimal.ONE;
    }

    public NFTAsset(RealmNFTAsset realmAsset)
    {
        loadFromMetaData(realmAsset.getMetaData());
        balance = realmAsset.getBalance();
    }

    public NFTAsset()
    {
        assetMap.clear();
        attributeMap.clear();
        balance = BigDecimal.ZERO;
        assetMap.put(LOADING_TOKEN, ".");
    }

    public NFTAsset(BigInteger tokenId)
    {
        assetMap.clear();
        attributeMap.clear();
        balance = BigDecimal.ZERO;
        assetMap.put(NAME, "ID #" + tokenId.toString());
    }

    public NFTAsset(NFTAsset asset)
    {
        assetMap.putAll(asset.assetMap);
        attributeMap.putAll(asset.attributeMap);
        balance = asset.balance;
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
        String svgOverride = getSVGOverride(); //always use SVG if available
        if (!TextUtils.isEmpty(svgOverride)) {
            return svgOverride;
        }

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

    public void setDecimals(BigDecimal rawBalance)
    {
        if (assetMap.containsKey("decimals"))
        {
            BigDecimal decimals = new BigDecimal(assetMap.get("decimals"));
        }
    }

    public boolean setBalance(BigDecimal value)
    {
        boolean retval = false;
        if (this.balance == null || !this.balance.equals(value)) { retval = true; }
        this.balance = value;
        return retval;
    }

    public BigDecimal getBalance()
    {
        return this.balance;
    }

    public boolean isAssetMultiple()
    {
        return this.balance != null && this.balance.compareTo(BigDecimal.ONE) > 0;
    }

    protected NFTAsset(Parcel in)
    {
        balance = new BigDecimal(in.readString());
        selected = new BigDecimal(in.readString());
        int assetCount = in.readInt();
        int attrCount = in.readInt();
        int tokenIdCount = in.readInt();

        for (int i = 0; i < assetCount; i++)
        {
            assetMap.put(in.readString(), in.readString());
        }

        for (int i = 0; i < attrCount; i++)
        {
            attributeMap.put(in.readString(), in.readString());
        }

        if (tokenIdCount > 0) { tokenIdList = new ArrayList<>(); }

        for (int i = 0; i < tokenIdCount; i++)
        {
            tokenIdList.add(new BigInteger(in.readString()));
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

    /**
     * If the image has SVG, we need to display that. Opensea's preview renderer can't handle SVG
     * @return
     */
    private String getSVGOverride()
    {
        for (String key : SVG_OVERRIDE)
        {
            if (assetMap.containsKey(key) && assetMap.get(key).toLowerCase().endsWith("svg"))
            {
                return Utils.parseIPFS(assetMap.get(key));
            }
        }

        return "";
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
        dest.writeString(balance != null ? balance.toString() : "1");
        dest.writeString(selected != null ? selected.toString() : "0");
        dest.writeInt(assetMap.size());
        dest.writeInt(attributeMap.size());
        dest.writeInt(tokenIdList != null ? tokenIdList.size() : 0);

        for (String key : assetMap.keySet())
        {
            dest.writeString(key);
            dest.writeString(assetMap.get(key));
        }

        for (String key : attributeMap.keySet())
        {
            dest.writeString(key);
            dest.writeString(attributeMap.get(key));
        }

        if (tokenIdList != null)
        {
            for (BigInteger tokenId : tokenIdList)
            {
                dest.writeString(tokenId.toString());
            }
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
        return assetMap.hashCode() + attributeMap.hashCode() + (balance != null ? balance.hashCode() : 0);
    }

    public boolean equals(RealmNFTAsset realmAsset)
    {
        return (hashCode() == new NFTAsset(realmAsset).hashCode());
    }

    public boolean equals(NFTAsset other)
    {
        return (hashCode() == other.hashCode());
    }

    public boolean isBlank()
    {
        return assetMap.size() == 0;
    }

    public void updateFromRaw(NFTAsset oldAsset)
    {
        //add thumbnail if it existed
        if (oldAsset.assetMap.containsKey(IMAGE_PREVIEW)) assetMap.put(IMAGE_PREVIEW, oldAsset.getAssetValue(IMAGE_PREVIEW));
        balance = oldAsset.balance;
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

            if (oldAsset.getBalance().compareTo(BigDecimal.ZERO) > 0)
            {
                setBalance(oldAsset.getBalance());
            }
        }
    }

    public boolean isSelected()
    {
        return isChecked;
    }

    public void setSelected(boolean selected)
    {
        this.isChecked = selected;
    }

    public void setSelectedBalance(BigDecimal amount)
    {
        this.selected = amount;
    }
    public BigDecimal getSelectedBalance()
    {
        return selected != null ? this.selected : BigDecimal.ZERO;
    }

    public void addCollectionToken(BigInteger nftTokenId)
    {
        if (tokenIdList == null) tokenIdList = new ArrayList<>();
        tokenIdList.add(nftTokenId);
    }

    public boolean isCollection()
    {
        return tokenIdList != null && tokenIdList.size() > 1;
    }

    public int getCollectionCount()
    {
        return tokenIdList != null ? tokenIdList.size() : 0;
    }

    public List<BigInteger> getCollectionIds()
    {
        //return sorted list
        Collections.sort(tokenIdList);/*, (e1, e2) -> {
            BigInteger tokenId1 = e1.first;
            BigInteger tokenId2 = e2.first;
            return tokenId1.compareTo(tokenId2);
        });*/

        return tokenIdList;
    }

    public String getAssetCategory()
    {
        if (tokenIdList != null && tokenIdList.size() > 1)
        {
            return "Collection";
        }
        else if (balance.compareTo(BigDecimal.ONE) > 0)
        {
            return "Fungible Token";
        }
        else if (tokenIdList != null && tokenIdList.size() == 1
                && ERC1155Token.getNFTTokenId(tokenIdList.get(0)).compareTo(BigInteger.ZERO) > 0)
        {
            return "NFT";
        }
        else
        {
            return "Fungible Token"; //?
        }
    }
}