package org.thenesis.gendes.awt;

import java.awt.Menu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;


public class MyCheckboxMenu extends Menu implements ItemListener {   
    private int currentItem;
    private MyCheckboxMenuItem items[];
    private ActionListener onItemChangedDispatcher;
    
    public MyCheckboxMenu(String name) { super(name); }
    
    public void setItems(MyCheckboxMenuItem items[]) {
        for (int i=0, n=items.length; i<n; i++) {
            items[i].addItemListener(this);
            add(items[i]);
        }
        this.items=items;
    }
    
    public void setCurrentItem(MyCheckboxMenuItem item) {
        for (int i=0, n=items.length; i<n; i++) {
            if (items[i]==item) {
                items[currentItem].setState(false);
                items[i].setState(true);
                currentItem=i;                
            }
        }        
    }
    
    public void setCurrentItemByIndex(int i) {
        items[currentItem].setState(false);
        items[i].setState(true);
        currentItem=i;
    }
    
    public void setCurrentItemByValue(int v) {
        for (int i=0, n=items.length; i<n; i++) {
            if (items[i].userValue==v) {
                items[currentItem].setState(false);
                items[i].setState(true);
                currentItem=i;                
            }
        }        
    }
    
    public MyCheckboxMenuItem getCurrentItem() {
        return items[currentItem];
    }
    
    public int getCurrentItemIndex() {
        return currentItem;
    }
    
    public int getCurrentItemValue() {
        return items[currentItem].userValue;
    }
    
    public void itemStateChanged(ItemEvent e) {
        MyCheckboxMenuItem lastItem=items[currentItem], newItem=(MyCheckboxMenuItem)e.getSource();
        for (int i=0, n=items.length; i<n; i++) {
            if (items[i]==newItem) {
                items[currentItem].setState(false);
                items[i].setState(true);
                currentItem=i;
                onItemChanged(lastItem, newItem);
            }
        }
    }
    
    public void onItemChanged(MyCheckboxMenuItem lastItem, MyCheckboxMenuItem newItem) {
        if (onItemChangedDispatcher!=null) onItemChangedDispatcher.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
    }
}
