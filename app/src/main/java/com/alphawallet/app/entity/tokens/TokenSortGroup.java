package com.alphawallet.app.entity.tokens;

public class TokenSortGroup {

    public final String data;
    public final int weight;

    public TokenSortGroup(String data) {
        this.data = data;
        // TODO: calc weight, based on some logic, just hash atm
        this.weight = data.hashCode();
    }

    public int compareTo(TokenSortGroup group) {
        return Integer.compare(this.weight, group.weight);
    }
}