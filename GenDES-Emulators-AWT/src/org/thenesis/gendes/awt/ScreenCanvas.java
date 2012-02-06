package org.thenesis.gendes.awt;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

public class ScreenCanvas extends Canvas {
    protected static final int FONT_SIZE=12;
    protected Font font=new Font("Monospaced", Font.BOLD, FONT_SIZE);
    protected int fontWidth, fontHeight;

    ScreenCanvas() {
        super();
        font=new Font("Monospaced", Font.BOLD, FONT_SIZE);
        fontWidth=8; fontHeight=12;        
    }
    
    public void drawString(Graphics g, int x, int y) {
        
    }
    
    public void drawBackground(Graphics g) {
        int w=getWidth(), h=getHeight();        
        g.setColor(hasFocus() ? Color.LIGHT_GRAY : Color.WHITE);
        g.fillRect(0, 0, w-1, h-1);
    }
}
