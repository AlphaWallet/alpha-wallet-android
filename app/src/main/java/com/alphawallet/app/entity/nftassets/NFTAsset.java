package com.alphawallet.app.entity.nftassets;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.alphawallet.app.entity.opensea.OpenSeaAsset;
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

import io.reactivex.disposables.Disposable;

/**
 * Created by JB on 1/07/2021.
 */
public class NFTAsset implements Parcelable
{
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
    private static final String LOADING_TOKEN = "*Loading*";
    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String IMAGE = "image";
    private static final String IMAGE_URL = "image_url";
    private static final String IMAGE_PREVIEW = "image_preview_url";
    private static final String DESCRIPTION = "description";
    private static final String IMAGE_ORIGINAL_URL = "image_original_url";
    private static final String IMAGE_ANIMATION = "animation_url";
    private static final String[] IMAGE_DESIGNATORS = {IMAGE, IMAGE_URL, IMAGE_ANIMATION, IMAGE_ORIGINAL_URL, IMAGE_PREVIEW};
    private static final String[] SVG_OVERRIDE = {IMAGE_ORIGINAL_URL, IMAGE_ANIMATION, IMAGE, IMAGE_URL};
    private static final String[] IMAGE_THUMBNAIL_DESIGNATORS = {IMAGE_PREVIEW, IMAGE, IMAGE_URL, IMAGE_ORIGINAL_URL, IMAGE_ANIMATION};
    private static final String BACKGROUND_COLOUR = "background_color";
    private static final String EXTERNAL_LINK = "external_link";
    private static final List<String> DESIRED_PARAMS = Arrays.asList(NAME, BACKGROUND_COLOUR, IMAGE_URL, IMAGE, IMAGE_ORIGINAL_URL, IMAGE_PREVIEW, DESCRIPTION, EXTERNAL_LINK, IMAGE_ANIMATION);
    private static final List<String> ATTRIBUTE_DESCRIPTOR = Arrays.asList("attributes", "traits");
    private final Map<String, String> assetMap = new HashMap<>();
    private final Map<String, String> attributeMap = new HashMap<>();
    @Nullable
    public Disposable metaDataLoader;
    public boolean isChecked = false;
    public boolean exposeRadio = false;
    private BigDecimal balance; //for ERC1155
    private BigDecimal selected; //for ERC1155 transfer
    private List<BigInteger> tokenIdList; // for ERC1155 collections
    private OpenSeaAsset openSeaAsset;

    public NFTAsset(String metaData)
    {
        loadFromMetaData(metaData);
        balance = BigDecimal.ONE;
    }

    public NFTAsset(RealmNFTAsset realmAsset)
    {
        String metaData = realmAsset.getMetaData() != null ? realmAsset.getMetaData() : new NFTAsset(new BigInteger(realmAsset.getTokenId())).jsonMetaData();
        loadFromMetaData(metaData);
        balance = realmAsset.getBalance();
    }

    public NFTAsset()
    {
        assetMap.clear();
        attributeMap.clear();
        balance = BigDecimal.ONE;
        assetMap.put(LOADING_TOKEN, ".");
    }

    public NFTAsset(BigInteger tokenId)
    {
        assetMap.clear();
        attributeMap.clear();
        balance = BigDecimal.ONE;
        assetMap.put(NAME, "ID #" + tokenId.toString());
    }

    public NFTAsset(NFTAsset asset)
    {
        assetMap.putAll(asset.assetMap);
        attributeMap.putAll(asset.attributeMap);
        balance = asset.balance;

        if (asset.tokenIdList != null)
        {
            tokenIdList = new ArrayList<>(tokenIdList);
        }
        else
        {
            tokenIdList = null;
        }

        isChecked = asset.isChecked;
        exposeRadio = asset.exposeRadio;
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

        if (tokenIdCount > 0)
        {
            tokenIdList = new ArrayList<>();
        }

        for (int i = 0; i < tokenIdCount; i++)
        {
            tokenIdList.add(new BigInteger(in.readString()));
        }
    }

    public String getAssetValue(String key)
    {
        return assetMap.get(key);
    }

    public Map<String, String> getAttributes()
    {
        return attributeMap;
    }

    public String getAttributeValue(String key)
    {
        return attributeMap.get(key);
    }

    public String getName()
    {
        return assetMap.get(NAME);
    }

    public String getAnimation()
    {
        return assetMap.get(IMAGE_ANIMATION);
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
        if (!TextUtils.isEmpty(svgOverride))
        {
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

    public boolean setBalance(BigDecimal value)
    {
        boolean retval = false;
        if (this.balance == null || !this.balance.equals(value))
        {
            retval = true;
        }
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

    private void loadFromMetaData(String metaData)
    {
        //build asset and trait map
        try
        {
            JSONObject jsonData = new JSONObject(metaData);
            Iterator<String> keys = jsonData.keys();
            String id = null;

            while (keys.hasNext())
            {
                String key = keys.next();
                String value = jsonData.getString(key);
                if (key.equals(ID))
                {
                    id = value;
                }
                else if (!ATTRIBUTE_DESCRIPTOR.contains(key))
                {
                    if (validJSONString(value) && DESIRED_PARAMS.contains(key))
                        assetMap.put(key, value);
                }
                else
                {
                    JSONArray attrArray = jsonData.getJSONArray(key);
                    for (int i = 0; i < attrArray.length(); i++)
                    {
                        JSONObject order = (JSONObject) attrArray.get(i);
                        if (validJSONString(order.getString("value")))
                            attributeMap.put(order.getString("trait_type"), order.getString("value"));
                    }
                }
            }

            //create name if none present and metadata is valid (this handles an edge condition where there's no name)
            if (!TextUtils.isEmpty(getImage()) && TextUtils.isEmpty(getName()) && id != null)
            {
                assetMap.put(NAME, "ID #" + id);
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
     *
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
        if (oldAsset != null)
        {
            if (oldAsset.assetMap.containsKey(IMAGE_PREVIEW))
                assetMap.put(IMAGE_PREVIEW, oldAsset.getAssetValue(IMAGE_PREVIEW));
            balance = oldAsset.balance;

            updateAsset(oldAsset);

            if (assetMap.size() > 1)
            {
                assetMap.remove(LOADING_TOKEN);
            }

            // Check OpenSeaAsset for meaningful data
            if (oldAsset.openSeaAsset != null)
            {
                String osName = oldAsset.openSeaAsset.name;
                if (TextUtils.isEmpty(getName()) && !TextUtils.isEmpty(osName))
                {
                    assetMap.put(NAME, osName);
                }

                String osImageUrl = oldAsset.openSeaAsset.getImageUrl();
                if (TextUtils.isEmpty(getImage()) && !TextUtils.isEmpty(osImageUrl))
                {
                    assetMap.put(IMAGE, osImageUrl);
                }

                String osDescription = oldAsset.openSeaAsset.description;
                if (TextUtils.isEmpty(getDescription()) && !TextUtils.isEmpty(osDescription))
                {
                    assetMap.put(DESCRIPTION, osDescription);
                }
            }
        }
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
                if (assetMap.get(param) == null && DESIRED_PARAMS.contains(param))
                    attributeMap.put(param, oldAsset.assetMap.get(param));
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

    public BigDecimal getSelectedBalance()
    {
        return selected != null ? this.selected : BigDecimal.ZERO;
    }

    public void setSelectedBalance(BigDecimal amount)
    {
        this.selected = amount;
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
        Collections.sort(tokenIdList);
        return tokenIdList;
    }

    public Category getAssetCategory(BigInteger tokenId)
    {
        if (tokenIdList != null && isCollection())
        {
            return Category.COLLECTION;
        }
        else if (ERC1155Token.isNFT(tokenId))
        {
            if (balance.equals(BigDecimal.ONE))
            {
                return Category.NFT;
            }
            else
            {
                return Category.SEMI_FT; //Should not see this, but there could have been a mis-labelling
            }
        }
        else
        {
            return Category.FT;
        }
    }

    public void attachOpenSeaAssetData(OpenSeaAsset openSeaAsset)
    {
        this.openSeaAsset = openSeaAsset;
    }

    public OpenSeaAsset getOpenSeaAsset()
    {
        return this.openSeaAsset;
    }

    public enum Category
    {
        NFT("NFT"), FT("Fungible Token"), COLLECTION("Collection"), SEMI_FT("Semi-Fungible");

        private final String category;

        Category(String category)
        {
            this.category = category;
        }

        public String getValue()
        {
            return this.category;
        }
    }
}