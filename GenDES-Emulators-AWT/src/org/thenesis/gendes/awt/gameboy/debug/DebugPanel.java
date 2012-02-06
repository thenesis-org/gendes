package org.thenesis.gendes.awt.gameboy.debug;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Panel;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import org.thenesis.gendes.debug.Screen;


//******************************************************************************
// Debug panel.
//******************************************************************************
public class DebugPanel extends Panel {
    
    protected String debugTitle;

    protected DebugPanel(String t) {
        super();
        addFocusListener(focusAdapter);
        debugTitle=t;
    }
    
    protected void drawBackground(Graphics g) {
        int w=getWidth(), h=getHeight();
        g.setColor(hasFocus() ? Color.BLUE : Color.LIGHT_GRAY);
        g.drawRect(0, 0, w-1, h-1);
    }

    protected void setSizeWithScreen(int font, int sw, int sh) {
        int fw=Screen.getFontWidth(font), fh=Screen.getFontHeight(font);
        int w=sw*fw, h=sh*fh;
        Dimension d=new Dimension();
        d.setSize(w+2, h+2);
        setMinimumSize(d); setPreferredSize(d);    	
    }
    
    private final FocusAdapter focusAdapter=new FocusAdapter() {
        public void focusGained(FocusEvent e) { repaint(); }
        public void focusLost(FocusEvent arg0) { repaint(); }        
    };
}
//******************************************************************************
