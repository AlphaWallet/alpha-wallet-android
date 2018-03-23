package io.awallet.crypto.alphawallet.repository.entity;

/**
 * Created by weiwu on 1/3/18.
 */

public interface NonFungibleToken {
    void setField(String id, String name, String value);
    String getFieldText(String id);
    String getFieldName(String id);
}
