package com.alphawallet.token.management.Model;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class ComboBoxDataModel {
    public String id;
    public String name;
    public BigInteger bitmask;
    public ComboBoxOption[] comboBoxOptions;

    public String getId(){
        return this.id;
    }
    public void setId(String id){
        this.id=id;
    }
    public String getName(){
        return this.name;
    }
    public void setName(String name){
        this.name = name;
    }

    public ComboBoxOption[] getComboBoxOptions(){
        return this.comboBoxOptions;
    }
    public void setComboBoxOptions(ComboBoxOption[] comboBoxOptions) {
        this.comboBoxOptions = comboBoxOptions;
    }

    public class ComboBoxOption{
        private BigInteger key;
        private BigInteger bitmask;
        private BigInteger bitshift;
        private String displayText;
        private List<String> displayTextList;
        public ComboBoxOption(BigInteger key, List<String> displayTextList){
            this.key = key;
            this.displayTextList = displayTextList;
        }
        public BigInteger getKey(){
            return  this.key;
        }
        public BigInteger getBitmask(){
            return this.bitmask;
        }
        public BigInteger getBitshift(){return this.bitshift;}
        public String getDisplayText(){
            return  this.displayText;
        }
        public String toString(){
            this.displayText="";
            for(String attr: displayTextList){
                this.displayText +=attr+",";
            }
            return  this.displayText.replaceFirst(".$","");
        }
    }

    public List<ComboBoxOption> convertToComboBoxDataModel(List<Map<BigInteger, String>> membersList){
        List<ComboBoxOption> options=new ArrayList<>();
        Map<BigInteger,List<String>> keyDisplayListMap = new ConcurrentHashMap<>();
        for(Map<BigInteger,String> members:membersList) {
            for (BigInteger key : members.keySet()) {
                if(keyDisplayListMap.containsKey(key)){
                    List<String> displayList=keyDisplayListMap.get(key);
                    displayList.add(members.get(key));
                }else{
                    List<String> displayList=new ArrayList<>();
                    displayList.add(members.get(key));
                    keyDisplayListMap.put(key,displayList);
                }
            }
        }
        for(BigInteger key : keyDisplayListMap.keySet()) {
            List<String> displayNameList = keyDisplayListMap.get(key);

            options.add(new ComboBoxOption(key,displayNameList));
        }
        return  options;
    }


}
