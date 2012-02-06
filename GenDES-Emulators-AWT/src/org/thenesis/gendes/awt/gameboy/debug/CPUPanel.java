package org.thenesis.gendes.awt.gameboy.debug;


import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import org.thenesis.gendes.contexts.ContextEvent;
import org.thenesis.gendes.contexts.ContextEventListener;
import org.thenesis.gendes.contexts.ContextNode;
import org.thenesis.gendes.contexts.GameBoyEmulationContext;
import org.thenesis.gendes.contexts.SystemEmulationContext;
import org.thenesis.gendes.cpu_gameboy.CPUScreen;


//******************************************************************************
// CPU panel.
//******************************************************************************
public class CPUPanel extends DebugPanel {
    private SystemEmulationContext systemContext;
    private GameBoyEmulationContext gameBoyContext;    
    
    private BufferedImage screenImg;
    private final int palette[]=new int[256];
    private CPUScreen screen;
    
    private final ContextNode gameBoyNode=new ContextNode() {
    	public void detach() { CPUPanel.this.detach(); }
    };
    
    private final ContextEventListener onPauseListener=new ContextEventListener() {
        public void processEvent(ContextEvent e) { repaint(); }
    };
    
    private final ContextEventListener onResumeListener=new ContextEventListener() {
        public void processEvent(ContextEvent e) { repaint(); }
    };
    
    private final ContextEventListener onResetListener=new ContextEventListener() {
        public void processEvent(ContextEvent e) { repaint(); }
    };
    
    public CPUPanel() {
        super("CPU");                
        palette[0]=0x00ffffff; palette[1]=0x00000000;
        setSizeWithScreen(CPUScreen.FONT_DEFAULT, CPUScreen.SCREEN_WIDTH, CPUScreen.SCREEN_HEIGHT);
    }
    
    public void attach(GameBoyEmulationContext gbc) {
        detach();
        
        systemContext=gbc.systemContext; gameBoyContext=gbc;        
        systemContext.onPauseDispatcher.addListener(onPauseListener);
        systemContext.onResumeDispatcher.addListener(onResumeListener);
        gameBoyContext.add(gameBoyNode);
        gameBoyContext.onResetDispatcher.addListener(onResetListener);
	        
        int w, h;
        screen=new CPUScreen(gameBoyContext.gameBoy.cpu);
//	        screen.setFont(Screen.FONT_8X16);
        w=screen.getScreenPixelsWidth(); h=screen.getScreenPixelsHeight();
        screenImg=new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        
        repaint();
    }
    
    public void detach() {
        if (gameBoyContext==null) return;
        
        screen=null; screenImg=null;
        
        gameBoyContext.onResetDispatcher.removeListener(onResetListener);
        gameBoyContext.remove(gameBoyNode);
        systemContext.onPauseDispatcher.removeListener(onPauseListener);
        systemContext.onResumeDispatcher.removeListener(onResumeListener);
        systemContext=null; gameBoyContext=null;
        
        repaint();
    }
    
    private void draw(Graphics g) {
        if (gameBoyContext==null) return;
        screen.updateScreen();
        
        DataBufferInt dataBuffer=(DataBufferInt)screenImg.getRaster().getDataBuffer();
        screen.drawScreen(dataBuffer.getData(), screenImg.getWidth(), screenImg.getHeight(), palette);
        g.drawImage(screenImg, 1, 1, this);    
    }

    public void paint(Graphics g) { drawBackground(g); draw(g); }
    public void update(Graphics g) { paint(g); }
}
//******************************************************************************
