package io.awallet.crypto.alphawallet.entity;

public class Language {
    public String name;
    public String code;
    public boolean isSelected;

    public Language(String name, boolean isSelected) {
        this.name = name;
        this.isSelected = isSelected;
    }

    public Language(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public Language(String name, String code, boolean isSelected) {
        this.name = name;
        this.code = code;
        this.isSelected = isSelected;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
