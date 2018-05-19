package io.stormbird.wallet.repository;

import android.content.res.Resources;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

import io.stormbird.token.tools.TokenDefinition;

/**
 * Created by weiwu on 22/4/18.
 * This is a wrapper class that is intended to be discarded eventually
 */

public class AssetDefinition extends TokenDefinition {

    public AssetDefinition(InputStream xmlAsset, String locale) throws IOException, SAXException {
        super(xmlAsset, locale);
    }

    public AssetDefinition(String filename, Resources res) throws IOException, SAXException {
        super(res.getAssets().open(filename), res.getConfiguration().locale.getLanguage());
    }

}
