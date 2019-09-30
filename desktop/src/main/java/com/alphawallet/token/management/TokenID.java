package com.alphawallet.token.management;

import com.alphawallet.token.management.Model.ComboBoxDataModel;
import com.alphawallet.token.management.Model.TextFieldDataModel;
import com.alphawallet.token.management.Model.TokenViewModel;

import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TokenID extends JFrame{
    private JPanel contentPane;
    private JTextField fieldTokenID;

    public InputStream ticketXML = getClass().getResourceAsStream("/TicketingContract.xml");

    private static Map<String,BigInteger> encodedValueMap=new ConcurrentHashMap<>();

    private TokenViewModel tokenViewModel;
    public TokenID(){
        try {
            tokenViewModel=new TokenViewModel(ticketXML, Locale.getDefault());

            contentPane = new JPanel();
            addComponentsToPane(contentPane);
            contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
            this.setContentPane(contentPane);
            this.setTitle("TokenID Generator");
            this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            this.setLocationByPlatform(true);
            this.setResizable(true);
            this.pack();
        } catch (IOException | IllegalArgumentException | SAXException e){
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        (new TokenID()).setVisible(true);
    }

    private  void addComponentsToPane(final Container pane){
        int gridy=0;
        GridBagConstraints col1Constraints = new GridBagConstraints();
        col1Constraints.fill = GridBagConstraints.BOTH;
        col1Constraints.anchor=GridBagConstraints.CENTER;
        col1Constraints.ipadx=30;col1Constraints.ipady=30;
        col1Constraints.weightx=0.3;
        col1Constraints.gridwidth=1;
        col1Constraints.gridx = 0;
        col1Constraints.gridy = gridy;
        GridBagConstraints col2Constraints = new GridBagConstraints();
        col2Constraints.fill = GridBagConstraints.BOTH;
        col2Constraints.anchor=GridBagConstraints.CENTER;
        col2Constraints.ipadx=30;col2Constraints.ipady=30;
        col2Constraints.weightx=0.3;
        col2Constraints.gridwidth=1;
        col2Constraints.gridx = 1;
        col2Constraints.gridy = gridy;
        GridBagConstraints col3Constraints = new GridBagConstraints();
        col3Constraints.fill = GridBagConstraints.BOTH;
        col3Constraints.anchor=GridBagConstraints.CENTER;
        col3Constraints.ipadx=25;col3Constraints.ipady=25;
        col3Constraints.weightx = 0.5;
        col3Constraints.gridwidth=1;
        col3Constraints.gridx = 2;
        col3Constraints.gridy = gridy;
        GridBagConstraints col4Constraints = new GridBagConstraints();
        col4Constraints.fill = GridBagConstraints.BOTH;
        col4Constraints.anchor=GridBagConstraints.CENTER;
        col4Constraints.ipadx=25;col4Constraints.ipady=25;
        col4Constraints.weightx = 0.5;
        col4Constraints.gridwidth=1;
        col4Constraints.gridx = 3;
        col4Constraints.gridy = gridy;

        // render column title
        JPanel controlsPane = new JPanel(); //control panel to hold token attribute UI components
        controlsPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        controlsPane.setLayout(new GridBagLayout());    //full column: attribute name|type|value|encode value
        final JLabel label1 = new JLabel();
        label1.setText("Attribute Name");
        controlsPane.add(label1,col1Constraints);
        final JLabel label2 = new JLabel();
        label2.setText("Type");
        controlsPane.add(label2,col2Constraints);
        final JLabel label3 = new JLabel();
        label3.setText("Value");
        controlsPane.add(label3,col3Constraints);
        final JLabel label4 = new JLabel();
        label4.setText("Encoded Value");
        controlsPane.add(label4,col4Constraints);

        // render dropdown list
        for(ComboBoxDataModel comboBoxDataModel : tokenViewModel.comboBoxDataModelList){
            gridy+=1;
            col1Constraints.gridy=col2Constraints.gridy=col3Constraints.gridy=col4Constraints.gridy=gridy;
            JLabel labelAttrName = new JLabel();
            labelAttrName.setText(comboBoxDataModel.name);
            controlsPane.add(labelAttrName,col1Constraints);
            JLabel labelType = new JLabel();
            labelType.setText("Mapping");
            controlsPane.add(labelType,col2Constraints);
            ComboBoxDataModel.ComboBoxOption[] options=comboBoxDataModel.getComboBoxOptions();
            JComboBox comboBox = new JComboBox(options);
            comboBox.setName(comboBoxDataModel.getId());
            comboBox.setEnabled(true);
            controlsPane.add(comboBox,col3Constraints);
            JTextField textFieldEncodedValue = new JTextField();
            textFieldEncodedValue.setEditable(false);
            textFieldEncodedValue.setEnabled(true);
            textFieldEncodedValue.setText(options[0].getKey().toString(16));
            updateEncodedValueMap(comboBox.getName(),options[0].getKey(),false);
            controlsPane.add(textFieldEncodedValue,col4Constraints);
            comboBox.addItemListener(new ItemListener(){
                public void itemStateChanged(ItemEvent e) {
                    ComboBoxDataModel.ComboBoxOption c = (ComboBoxDataModel.ComboBoxOption)e.getItem();
                    textFieldEncodedValue.setText(c.getKey().toString(16).toUpperCase());
                    BigInteger value=new BigInteger(c.getKey().toString(16),16);
                    updateEncodedValueMap(comboBox.getName(),value,true);
                }

            });
        }
        for(TextFieldDataModel model : tokenViewModel.textFieldDataModelList){
            gridy+=1;
            col1Constraints.gridy=col2Constraints.gridy=col3Constraints.gridy=col4Constraints.gridy=gridy;
            JLabel labelAttrName = new JLabel();
            labelAttrName.setText(model.name);
            controlsPane.add(labelAttrName,col1Constraints);
            JLabel labelType = new JLabel();
            labelType.setText(model.type);
            controlsPane.add(labelType,col2Constraints);
            JTextField textFieldEncodedValue = new JTextField();
            textFieldEncodedValue.setName(model.id);
            textFieldEncodedValue.setEditable(false);
            textFieldEncodedValue.setEnabled(true);
            JTextField textFieldInput = new JTextField();
            textFieldInput.setEditable(true);
            textFieldInput.setEnabled(true);
            textFieldInput.addKeyListener(new KeyAdapter() {
                public void keyReleased(KeyEvent e) {
                    try {
                        BigInteger encodedValue = BigInteger.valueOf(0);
                        String inputStr = textFieldInput.getText();
                        if (inputStr != null && inputStr.length() > 0) {
                            if (model.as.equals("UTF8")) {
                                byte[] bytes = inputStr.getBytes(Charset.forName("UTF-8"));
                                encodedValue = new BigInteger(bytes);
                            } else if (model.as.equals("Unsigned")) {
                                encodedValue = new BigInteger(inputStr);
                            }
                            encodedValue = encodedValue.shiftLeft(model.getBitshift()).and(model.getBitmask());
                        }
                        textFieldEncodedValue.setText(encodedValue.toString(16).toUpperCase());
                        updateEncodedValueMap(textFieldEncodedValue.getName(),encodedValue,true);
                    }catch (Exception ex){
                        JOptionPane.showMessageDialog(null, "Invalid Data Type! Please check the type",
                                "Error", JOptionPane.ERROR_MESSAGE);
                        textFieldInput.setText("");
                        textFieldInput.requestFocusInWindow();
                    }
                }
            });
            controlsPane.add(textFieldInput,col3Constraints);

            controlsPane.add(textFieldEncodedValue,col4Constraints);
        }

//        JPanel bottomPane = new JPanel();
//        bottomPane.setLayout(new GridLayout(0,2));
        gridy+=1;
        GridBagConstraints colConstraints = new GridBagConstraints();
        colConstraints.fill = GridBagConstraints.HORIZONTAL;
        colConstraints.weightx = 0.5;
        colConstraints.gridwidth=1;
        colConstraints.gridx = 3;
        colConstraints.gridy = gridy;
        controlsPane.add(new JSeparator(),colConstraints);
        gridy+=1;
        col1Constraints.gridy=col2Constraints.gridy=col3Constraints.gridy=col4Constraints.gridy=gridy;
        JLabel labelTokenID = new JLabel();
        labelTokenID.setText("TokenID");
        controlsPane.add(labelTokenID, col1Constraints);
//        controlsPane.add(new JLabel(" "));
//        controlsPane.add(new JLabel(" "));
        fieldTokenID = new JTextField();
        fieldTokenID.setEditable(false);
        fieldTokenID.setEnabled(true);
//        GridBagConstraints constraints = new GridBagConstraints();
//        constraints.fill = GridBagConstraints.HORIZONTAL;
//        constraints.gridwidth=2;
//        constraints.gridx = 2;
//        constraints.gridy = 0;
        controlsPane.add(fieldTokenID,col4Constraints);
        pane.add(controlsPane,BorderLayout.CENTER);
        //pane.add(new JSeparator(), BorderLayout.CENTER);
        //pane.add(bottomPane,BorderLayout.SOUTH);
        updateTokenIDField();
        pane.revalidate();
        pane.repaint();

    }

    private void updateEncodedValueMap(String name, BigInteger value,boolean isUpdateTokenID){
        encodedValueMap.put(name,value);
        if(isUpdateTokenID){
            updateTokenIDField();
        }
    }
    private void updateTokenIDField(){
        StringBuilder tokenidStr= new StringBuilder();
        BigInteger tokenid=BigInteger.valueOf(0);
        for(String key:encodedValueMap.keySet()){
            tokenid=tokenid.or(encodedValueMap.get(key));
        }
        tokenidStr = new StringBuilder(tokenid.toString(16));
        while (tokenidStr.length() < 64) {
            tokenidStr.insert(0, "0");
        }
        this.fieldTokenID.setText(tokenidStr.toString().toUpperCase());
    }
}
