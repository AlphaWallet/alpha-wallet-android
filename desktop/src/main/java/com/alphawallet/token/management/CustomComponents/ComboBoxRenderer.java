package com.alphawallet.token.management.CustomComponents;

import javax.swing.*;
import java.awt.*;

public class ComboBoxRenderer extends JLabel
        implements ListCellRenderer {
    private String[] options;
    public ComboBoxRenderer(String[] options) {
        this.options=options;
        setOpaque(true);
        setHorizontalAlignment(CENTER);
        setVerticalAlignment(CENTER);
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        //Get the selected index. (The index param isn't
        //always valid, so just use the value.)
        int selectedIndex = ((Integer)value).intValue();
        if(selectedIndex!=-1) {
            String pet = options[selectedIndex];
        }
        return this;
    }
}
