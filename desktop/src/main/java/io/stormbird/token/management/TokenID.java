package io.stormbird.token.management;

import io.stormbird.token.entity.NonFungibleToken;
import io.stormbird.token.management.Model.ComboBoxDataModel;
import io.stormbird.token.management.Model.TextFieldDataModel;
import io.stormbird.token.management.Model.TokenViewModel;
import io.stormbird.token.tools.TokenDefinition;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class TokenID extends JFrame{
    private JPanel contentPane;
    private JTextField fieldTokenID;
    public InputStream ticketXML = getClass().getResourceAsStream("/TicketingContract.xml");
    private static Map<String,BigInteger> tokenIDMap=new ConcurrentHashMap<>();

    private void updateTokenID(String name, BigInteger value,boolean updateUI){
        tokenIDMap.put(name,value);
        if(updateUI){
            showTokenID();
        }
    }
    private void showTokenID(){
        BigInteger tokenid=BigInteger.valueOf(0);
        for(String key:tokenIDMap.keySet()){
            tokenid=tokenid.or(tokenIDMap.get(key));
        }
        String tokenidStr=tokenid.toString(16);

        while (tokenidStr.length() < 64) {
            tokenidStr = "0" + tokenidStr;
        }
        fieldTokenID.setText(tokenidStr.toUpperCase());
    }
    public TokenID(){
        try {
            TokenDefinition ad = new TokenDefinition(ticketXML, Locale.getDefault());
            contentPane = new JPanel();
            addComponentsToPane(ad,contentPane);
        } catch (IOException | IllegalArgumentException | SAXException e){
            e.printStackTrace();
        }

        setContentPane(contentPane);
        this.revalidate();
        this.repaint();
        setSize(1000,600); // without this, the window won't display. TODO: find a way to auto size.
       setTitle("Simple");
       this.setResizable(true);
       this.pack();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }
    private  void addComponentsToPane(TokenDefinition ad,final Container pane){
        GridBagConstraints col1 = new GridBagConstraints();
        col1.fill = GridBagConstraints.HORIZONTAL;
        //col1.weightx = 0.2;
        col1.gridx = 0;
        col1.gridy = 0;
        GridBagConstraints col2 = new GridBagConstraints();
        col2.fill = GridBagConstraints.HORIZONTAL;
        //col2.weightx = 0.2;
        col2.gridx = 1;
        col2.gridy = 0;
        GridBagConstraints col3 = new GridBagConstraints();
        col3.fill = GridBagConstraints.HORIZONTAL;
        col3.weightx = 0.6;
        col3.gridx = 2;
        col3.gridy = 0;
        GridBagConstraints col4 = new GridBagConstraints();
        col4.fill = GridBagConstraints.HORIZONTAL;
        col4.weightx = 0.6;
        col4.gridx = 3;
        col4.gridy = 0;
        TokenViewModel.constructDataMapForGUI(ad);
        //int componentCount = ad.attributes.size()-comboBoxDataModelList.size();
        JPanel controls = new JPanel();
        controls.setLayout(new GridLayout(0,4));
        final JLabel label1 = new JLabel();
        label1.setText("Atrribute Name");
        //label1.setSize();
        controls.add(label1,col1);
        final JLabel label2 = new JLabel();
        label2.setText("Type");
        controls.add(label2,col2);
        final JLabel label3 = new JLabel();
        label3.setText("Value");
        controls.add(label3,col3);
        final JLabel label4 = new JLabel();
        label4.setText("Encoded Value");
        controls.add(label4,col4);
        for(ComboBoxDataModel model : TokenViewModel.comboBoxDataModelList){
            JLabel labelAttrName = new JLabel();
            labelAttrName.setText(model.name);
            controls.add(labelAttrName,col1);
            JLabel labelType = new JLabel();
            labelType.setText("Mapping");
            controls.add(labelType,col2);
            ComboBoxDataModel.ComboBoxOption[] options=model.getComboBoxOptions();
            JComboBox comboBox = new JComboBox(options);
            comboBox.setName(model.getId());
            comboBox.setEnabled(true);
            controls.add(comboBox,col3);
            JTextField field = new JTextField();
            field.setEditable(false);
            field.setEnabled(true);
            field.setText(options[0].getKey().toString(16));
            updateTokenID(comboBox.getName(),options[0].getKey(),false);
            controls.add(field,col4);
            comboBox.addItemListener(new ItemListener(){

                public void itemStateChanged(ItemEvent e) {
                    ComboBoxDataModel.ComboBoxOption c = (ComboBoxDataModel.ComboBoxOption)e.getItem();
                    field.setText(c.getKey().toString(16).toUpperCase());
                    //JComboBox cb=(JComboBox)e.getItem();
                    BigInteger value=new BigInteger(c.getKey().toString(16),16);
                    updateTokenID(comboBox.getName(),value,true);
                }

            });
        }
        for(TextFieldDataModel model : TokenViewModel.textFieldDataModelList){
            JLabel labelAttrName = new JLabel();
            labelAttrName.setText(model.name);
            controls.add(labelAttrName,col1);
            JLabel labelType = new JLabel();
            labelType.setText(model.type);
            controls.add(labelType,col2);
            JTextField fieldEncoded = new JTextField();
            fieldEncoded.setName(model.id);
            fieldEncoded.setEditable(false);
            fieldEncoded.setEnabled(true);
            JTextField fieldInput = new JTextField();//createTextField(model,fieldEncoded,this);//new JTextField();
            fieldInput.setEditable(true);
            fieldInput.setEnabled(true);

            //fieldEncoded.getDocument().addDocumentListener(new TextFieldocumentListener(fieldEncoded,model));
            fieldInput.addKeyListener(new KeyAdapter() {
                                     public void keyReleased(KeyEvent e) {
                                        // JTextField textField = (JTextField) e.getSource();
                                         //String text = fieldInput.getText();
                                         //fieldEncoded.setText(text);
                                         try {
                                             BigInteger encodedValue = BigInteger.valueOf(0);
                                             String inputStr = fieldInput.getText().toString();
                                             if (inputStr != null && inputStr.length() > 0) {
                                                 if (model.as.equals("UTF8")) {
                                                     byte[] bytes = inputStr.getBytes(Charset.forName("UTF-8"));
                                                     encodedValue = new BigInteger(bytes);
                                                 } else if (model.as.equals("Unsigned")) {
                                                     encodedValue = new BigInteger(inputStr);
                                                 }
                                                 encodedValue = encodedValue.shiftLeft(model.getBitshift()).and(model.getBitmask());
                                             }
                                             fieldEncoded.setText(encodedValue.toString(16).toUpperCase());
                                             updateTokenID(fieldEncoded.getName(),encodedValue,true);
                                         }catch (Exception ex){
                                             JOptionPane.showMessageDialog(null, "Incorrect Data Type! Numbers Only!",
                                                     "Inane error", JOptionPane.ERROR_MESSAGE);
                                             fieldInput.setText("");
                                             fieldInput.requestFocusInWindow();
                                         }
                                     }
                                 });
            controls.add(fieldInput,col3);

            controls.add(fieldEncoded,col4);
        }

//        JPanel bottomPane = new JPanel();
//        bottomPane.setLayout(new GridLayout(1,4));
        controls.add(new JSeparator());
        controls.add(new JSeparator());
        controls.add(new JSeparator());
        controls.add(new JSeparator());
        JLabel labelTokenID = new JLabel();
        labelTokenID.setText("TokenID");
        controls.add(labelTokenID);
        controls.add(new JLabel(" "));
        controls.add(new JLabel(" "));
        fieldTokenID = new JTextField();
        fieldTokenID.setEditable(false);
        fieldTokenID.setEnabled(true);
        controls.add(fieldTokenID);
        pane.add(controls,BorderLayout.CENTER);
        //pane.add(new JSeparator(), BorderLayout.CENTER);
        //pane.add(bottomPane,BorderLayout.SOUTH);
        pane.revalidate();
        pane.repaint();
        showTokenID();
    }

    private JTextField createTextField(TextFieldDataModel model,JTextField encodedField,TokenID context) {
        JTextField tf = new JTextField(30);
        tf.getDocument().addDocumentListener(new TextFieldocumentListener(tf,model,encodedField,context));
        return tf;
    }
    private class TextFieldocumentListener implements DocumentListener {
        private JTextField textField;
        private TextFieldDataModel model;
        private JTextField encodedField;
        private TokenID context;
        public TextFieldocumentListener(JTextField textField,TextFieldDataModel model,JTextField encodedField,TokenID context) {
            this.textField = textField;
            this.model=model;
            this.encodedField=encodedField;
            this.context=context;
        }
        private void updateEncodedField(){
            try {
                BigInteger encodedValue = BigInteger.valueOf(0);
                String inputStr = this.textField.getText().toString();
                if (inputStr != null && inputStr.length() > 0) {
                    if (model.as.equals("UTF8")) {
                        byte[] bytes = inputStr.getBytes(Charset.forName("UTF-8"));
                        encodedValue = new BigInteger(bytes);
                    } else if (model.as.equals("Unsigned")) {
                        encodedValue = new BigInteger(inputStr);
                    }
                    encodedValue = encodedValue.shiftLeft(model.getBitshift()).and(model.getBitmask());
                }
                this.encodedField.setText(encodedValue.toString(16).toUpperCase());
            }catch (Exception ex){
                JOptionPane.showMessageDialog(this.context, "Incorrect Data Type! Numbers Only!",
                        "Inane error", JOptionPane.ERROR_MESSAGE);
                this.textField.setText("");
                this.textField.requestFocusInWindow();
            }
        }
        @Override
        public void insertUpdate(DocumentEvent e) {
            updateEncodedField();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            updateEncodedField();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            updateEncodedField();
        }
    }
    public static void main(String args[]) {
        (new TokenID()).setVisible(true);
    }
}
