package org.thenesis.gendes.awt.gameboy.debug;


import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import org.thenesis.gendes.contexts.ContextEvent;
import org.thenesis.gendes.contexts.ContextEventListener;
import org.thenesis.gendes.contexts.ContextNode;
import org.thenesis.gendes.contexts.GameBoyEmulationContext;
import org.thenesis.gendes.contexts.SystemEmulationContext;
import org.thenesis.gendes.gameboy.GameBoyHardwareScreen;
import org.thenesis.gendes.gameboy.GameBoyHardwareScreen2;


//******************************************************************************
// Hardware panel.
//******************************************************************************
public class HardwarePanel extends DebugPanel {
    private SystemEmulationContext systemContext;
    private GameBoyEmulationContext gameBoyContext;    
    
    private BufferedImage screenImg;
    private final int palette[]=new int[256];
    private GameBoyHardwareScreen2 screen;
    
    private final Point mouseCharacter=new Point();

    private final ContextNode gameBoyNode=new ContextNode() {
    	public void detach() { HardwarePanel.this.detach(); }
    };
    
    private final ContextEventListener onPauseListener=new ContextEventListener() {
        public void processEvent(ContextEvent e) { screen.updateScreen(true); repaint(); }
    };
    
    private final ContextEventListener onResumeListener=new ContextEventListener() {
        public void processEvent(ContextEvent e) { screen.updateScreen(true); repaint(); }
    };
    
    private final ContextEventListener onResetListener=new ContextEventListener() {
        public void processEvent(ContextEvent e) { screen.updateScreen(true); repaint(); }
    };
    
    public HardwarePanel() {
        super("Hardware");
        palette[0]=0x00ffffff; palette[1]=0x00000000; palette[2]=0x00e0ffe0; palette[3]=0x00000000; palette[4]=0x0000ff00; palette[5]=0x00000000; palette[6]=0x00000000; palette[7]=0x0000ff00;
        palette[8]=0x00ffffff; palette[9]=0x00000000; palette[10]=0x00ffe0e0; palette[11]=0x00000000; palette[12]=0x00ff0000; palette[13]=0x00000000; palette[14]=0x00000000; palette[15]=0x00ff0000;
        setSizeWithScreen(GameBoyHardwareScreen.FONT_DEFAULT, GameBoyHardwareScreen.SCREEN_WIDTH, GameBoyHardwareScreen.SCREEN_HEIGHT);

        addComponentListener(componentAdapter);
        addKeyListener(keyAdapter);
        addMouseListener(mouseAdapter);
    }
    
    public void attach(GameBoyEmulationContext gbc) {
        detach();

        systemContext=gbc.systemContext; gameBoyContext=gbc;
        systemContext.onPauseDispatcher.addListener(onPauseListener);
        systemContext.onResumeDispatcher.addListener(onResumeListener);
        gameBoyContext.onResetDispatcher.addListener(onResetListener);
        gameBoyContext.add(gameBoyNode);

        int w, h;
        screen=new GameBoyHardwareScreen2(gameBoyContext.gameBoy);
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
    
    private void resizeScreen() {
        if (gameBoyContext==null) return;
        int fw=screen.getFontWidth(), fh=screen.getFontHeight(), w=getWidth(), h=getHeight();
        int sw=w/fw, sh=h/fh;
        if (sw<1) sw=1; if (sh<1) sh=1;
        screen.setScreenSize(sw, sh);
        
        int spw=screen.getScreenPixelsWidth(), sph=screen.getScreenPixelsHeight();
        screenImg=new BufferedImage(spw, sph, BufferedImage.TYPE_INT_RGB);
    }

    private void draw(Graphics g) {
        if (gameBoyContext==null) return;
        screen.updateScreen(true);
        
        DataBufferInt dataBuffer=(DataBufferInt)screenImg.getRaster().getDataBuffer();
        screen.drawScreen(dataBuffer.getData(), screenImg.getWidth(), screenImg.getHeight(), palette);
        g.drawImage(screenImg, 1, 1, this);
    }
    
    public void paint(Graphics g) { drawBackground(g); draw(g); }
    public void update(Graphics g) { paint(g); }

    private final ComponentAdapter componentAdapter=new ComponentAdapter() {
        public void componentResized(ComponentEvent e) { resizeScreen(); repaint(); }        
    };
    
    private final KeyAdapter keyAdapter=new KeyAdapter() {
        public void keyTyped(KeyEvent e) {
            if (gameBoyContext==null) return;
            if (!systemContext.emulationThread.isPaused()) return;
            
            char key=e.getKeyChar();
            switch (key) {
/*
            case '#': disassemblerScreen.setPcFollowing(!disassemblerScreen.getPcFollowing()); repaint(); break;
            case '=': disassemblerScreen.goToPc(); repaint(); break;
            case '@': {
                String s=AddressDialog.getValue(parentFrame);
                if (s!=null) { disassemblerScreen.goTo(Tools.stringToInt(s)&0xffff); repaint(); }
                }
                break;
  */
            }
        }
        
        public void keyPressed(KeyEvent e) {
            if (gameBoyContext==null) return;
            if (!systemContext.emulationThread.isPaused()) return;
            
            int key=e.getKeyCode();
            
            switch (key) {
            case KeyEvent.VK_INSERT: // TODO: implement edition.
                
                break;
            case KeyEvent.VK_UP: screen.moveCursorUp(); repaint(); break;
            case KeyEvent.VK_DOWN: screen.moveCursorDown(); repaint(); break;
            case KeyEvent.VK_LEFT: screen.moveCursorLeft(); repaint(); break;
            case KeyEvent.VK_RIGHT: screen.moveCursorRight(); repaint(); break;
            case KeyEvent.VK_PAGE_UP: screen.moveCursorToPreviousPage(); repaint(); break;
            case KeyEvent.VK_PAGE_DOWN: screen.moveCursorToNextPage(); repaint(); break;            
            }
        }        
    };
    
    private final MouseAdapter mouseAdapter=new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
            if (gameBoyContext==null) return;
            int x=e.getX(), y=e.getY();
            if (screen.positionToCharacter(x, y, mouseCharacter)) return;
            screen.setCursorPosition(mouseCharacter.x, mouseCharacter.y);
            repaint();
        }        
    };    
}
//******************************************************************************
