package com.alphawallet.app.entity;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TokensMapping {

    public class Contract {

        @SerializedName("address")
        @Expose
        private String address;
        @SerializedName("chainId")
        @Expose
        private Long chainId;

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public Long getChainId() {
            return chainId;
        }

        public void setChainId(Long chainId) {
            this.chainId = chainId;
        }
    }

    @SerializedName("contracts")
    @Expose
    private List<Contract> contracts = null;
    @SerializedName("group")
    @Expose
    private String group;

    public List<Contract> getContracts() {
        return contracts;
    }

    public void setContracts(List<Contract> contracts) {
        this.contracts = contracts;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

}
