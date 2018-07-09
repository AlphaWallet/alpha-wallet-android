package io.stormbird.token.management.Model;

import io.stormbird.token.entity.NonFungibleToken;
import io.stormbird.token.tools.TokenDefinition;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.xml.sax.SAXException;

public class TokenViewModel extends TokenDefinition {
    public List<ComboBoxDataModel> comboBoxDataModelList;
    public List<TextFieldDataModel> textFieldDataModelList;
    public TokenViewModel(InputStream xmlAsset, Locale locale) throws IOException, SAXException {
        super(xmlAsset, locale);
        this.comboBoxDataModelList=new ArrayList<>();
        this.textFieldDataModelList=new ArrayList<>();
        constructTokenViewModelForGUI(this);
    }

    private void constructTokenViewModelForGUI(TokenDefinition ad){
        Map<BigInteger,List<String>> bitmaskIdsMap=new ConcurrentHashMap<>();
        Map<String, ComboBoxDataModel[]> comboBoxDataMap = new ConcurrentHashMap<String, ComboBoxDataModel[]>();

        for (String key : ad.attributes.keySet()) {
            AttributeType attr = ad.attributes.get(key);
            if(attr.members!=null && attr.members.size()>0){
                if(bitmaskIdsMap.containsKey(attr.bitmask)){
                    List<String> ids = bitmaskIdsMap.get(attr.bitmask);
                    ids.add(attr.id);
                }else{
                    List<String> ids=new ArrayList<String>();
                    ids.add(attr.id);
                    bitmaskIdsMap.put(attr.bitmask,ids);
                }
            }else{
                TextFieldDataModel textFieldDataModel=new TextFieldDataModel();
                textFieldDataModel.setId(attr.id);
                textFieldDataModel.setName(attr.name);
                textFieldDataModel.setBitmask(attr.bitmask);
                textFieldDataModel.setBitshift(attr.bitshift);
                textFieldDataModel.setType(attr.syntax.name());
                textFieldDataModel.setAs(attr.as.name());
                this.textFieldDataModelList.add(textFieldDataModel);
            }
        }
        for(BigInteger bitmask : bitmaskIdsMap.keySet()){
            ComboBoxDataModel model = new ComboBoxDataModel();
            //model.setBitmask(key);
            List<Map<BigInteger, String>> membersList = new ArrayList<Map<BigInteger, String>>();
            String idCombined="";
            String labelName="";
            for(String id: bitmaskIdsMap.get(bitmask)) {
                AttributeType attr = ad.attributes.get(id);
                membersList.add(ad.getConvertedMappingMembersByKey(id));
                //membersList.add(attr.members);
                labelName+=attr.name+",";
                idCombined+=id+"_";
            }
            model.setId(idCombined.replaceFirst(".$",""));
            model.setName(labelName.replaceFirst(".$",""));
            List<ComboBoxDataModel.ComboBoxOption> options=model.convertToComboBoxDataModel(membersList);
            model.setComboBoxOptions(options.toArray(new ComboBoxDataModel.ComboBoxOption[options.size()]));
            this.comboBoxDataModelList.add(model);
        }
    }

}