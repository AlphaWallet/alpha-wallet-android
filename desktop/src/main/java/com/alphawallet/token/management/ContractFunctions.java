package com.alphawallet.token.management;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Locale;

import com.alphawallet.token.entity.NonFungibleToken;
import com.alphawallet.token.tools.TokenDefinition;
import org.xml.sax.SAXException;

public class ContractFunctions extends JFrame {
    private JPanel contentPane;
    private JComboBox localityCombo;
    private JTextField locality;
    private JTextField countryA;
    private JTextField countryB;
    private JTextField match;
    private JTextField category;
    private JTextField nomero;
    private JTextField time;
    private JTextField venue;
    private JComboBox venuCombo;
    private JTextField timeField;
    private JTextField countryAField;
    private JTextField countryBField;
    private JTextField MatchField;
    private JTextField categoryField;
    private JTextField numeroField;
    private JTextPane statusBar;

    public InputStream ticketXML = getClass().getResourceAsStream("/TicketingContract.xml");

    public ContractFunctions() {
        try {
            TokenDefinition ad = new TokenDefinition(ticketXML, Locale.getDefault(), null);
            NonFungibleToken ticket = new NonFungibleToken(BigInteger.valueOf(0x010CCB53), ad);
        } catch (IOException | IllegalArgumentException | SAXException e){
            e.printStackTrace();
        }
        setContentPane(contentPane);
        setSize(1000,600); // without this, the window won't display. TODO: find a way to auto size.
        setTitle("Simple");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }
    public static void main(String args[]) {
        (new ContractFunctions()).setVisible(true);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}
