package io.stormbird.token.entity;


import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Locale;

import io.stormbird.token.tools.TokenDefinition;

/**
 * Created by weiwu on 22/4/18.
 */

public class AssetDefinition extends TokenDefinition {

    public AssetDefinition(InputStream xmlAsset, String locale) throws IOException, SAXException {
        super(xmlAsset, locale);
    }

    public AssetDefinition(String filename) throws IOException, SAXException {
        super(new FileInputStream(new File(filename)), Locale.getDefault().getLanguage());
    }

    /* take a token ID in byte-32, find all the fields in it and call back
     * token.setField(fieldID, fieldName, text-value). This is abandoned
     * temporarily for the need to retrofit the class with J.B.'s design */

    public void parseField(BigInteger tokenId, NonFungibleToken token) {
        for (String key : fields.keySet()) {
            FieldDefinition f = fields.get(key);
            BigInteger val = tokenId.and(f.bitmask).shiftRight(f.bitshift);
            token.setAttribute(f.id,
                    new NonFungibleToken.Attribute(f.id, f.name, val, f.applyToFieldValue(val)));
        }
    }

}
