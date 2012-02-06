package org.thenesis.gendes.awt;

import java.awt.AWTEvent;
import java.awt.CheckboxMenuItem;


public class MyCheckboxMenuItem extends CheckboxMenuItem {
    public int userValue;
    
    public MyCheckboxMenuItem(String label) {
        super(label);
        this.enableEvents(AWTEvent.ITEM_EVENT_MASK);
    }
    
    public MyCheckboxMenuItem(String label, int userValue) {
        super(label);
        this.userValue=userValue;
        this.enableEvents(AWTEvent.ITEM_EVENT_MASK);
    }
}
