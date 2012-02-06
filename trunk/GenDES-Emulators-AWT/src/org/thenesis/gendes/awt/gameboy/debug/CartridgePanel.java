package org.thenesis.gendes.awt.gameboy.debug;


import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import org.thenesis.gendes.contexts.ContextEvent;
import org.thenesis.gendes.contexts.ContextEventListener;
import org.thenesis.gendes.contexts.ContextNode;
import org.thenesis.gendes.contexts.GameBoyEmulationContext;
import org.thenesis.gendes.contexts.SystemEmulationContext;
import org.thenesis.gendes.gameboy.CartridgeScreen;


//******************************************************************************
// Cartridge panel.
//******************************************************************************
public final class CartridgePanel extends DebugPanel {
    private SystemEmulationContext systemContext;    
    private GameBoyEmulationContext gameBoyContext;
    
    private BufferedImage screenImg;
    private final int palette[]=new int[256];
    private CartridgeScreen screen;
    
    private final ContextNode gameBoyNode=new ContextNode() {
    	public void detach() { CartridgePanel.this.detach(); }
    };
    
    private final ContextEventListener onPauseListener=new ContextEventListener() {
        public void processEvent(ContextEvent e) { repaint(); }
    };
    
    private final ContextEventListener onResetListener=new ContextEventListener() {
        public void processEvent(ContextEvent e) { repaint(); }
    };
    
    private final ContextEventListener onCartridgeRemovedListener=new ContextEventListener() {
        public void processEvent(ContextEvent e) { repaint(); }
    };
    
    private final ContextEventListener onCartridgeInsertedListener=new ContextEventListener() {
        public void processEvent(ContextEvent e) { repaint(); }
    };
    
    public CartridgePanel() {
        super("Cartridge");
        palette[0]=0x00ffffff; palette[1]=0x00000000;
        setSizeWithScreen(CartridgeScreen.FONT_DEFAULT, CartridgeScreen.SCREEN_WIDTH, CartridgeScreen.SCREEN_HEIGHT);
    }

    public void attach(GameBoyEmulationContext gbc) {
        detach();
        
        systemContext=gbc.systemContext; gameBoyContext=gbc;
        systemContext.onPauseDispatcher.addListener(onPauseListener);
        gameBoyContext.add(gameBoyNode);
        gameBoyContext.onResetDispatcher.addListener(onResetListener);
        gameBoyContext.onCartridgeRemovedDispatcher.addListener(onCartridgeRemovedListener);
        gameBoyContext.onCartridgeInsertedDispatcher.addListener(onCartridgeInsertedListener);
	        
        int w, h;
        screen=new CartridgeScreen(gameBoyContext.gameBoy);
        w=screen.getScreenPixelsWidth(); h=screen.getScreenPixelsHeight();
        screenImg=new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        repaint();
    }
    
    public void detach() {
        if (gameBoyContext==null) return;
        
        gameBoyContext.onResetDispatcher.removeListener(onResetListener);
        gameBoyContext.onCartridgeRemovedDispatcher.removeListener(onCartridgeRemovedListener);
        gameBoyContext.onCartridgeInsertedDispatcher.removeListener(onCartridgeInsertedListener);
        gameBoyContext.remove(gameBoyNode);
        systemContext.onPauseDispatcher.removeListener(onPauseListener);
        systemContext=null; gameBoyContext=null;
        
        screen=null; screenImg=null;
        
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
