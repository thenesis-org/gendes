package org.thenesis.gendes.awt;

import java.awt.AWTEvent;
import java.awt.MenuItem;


public class MyMenuItem extends MenuItem {
    public int userValue;
    
    public MyMenuItem(String label) {
        super(label);
        this.enableEvents(AWTEvent.ACTION_EVENT_MASK);
    }
    
    public MyMenuItem(String label, int userValue) {
        super(label);
        this.userValue=userValue;
        this.enableEvents(AWTEvent.ACTION_EVENT_MASK);
    }
}
