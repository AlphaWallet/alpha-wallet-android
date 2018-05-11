package io.stormbird.token.management;

import javax.swing.*;

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

    public ContractFunctions() {
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
