package org.thenesis.gendes.awt.gameboy;

import java.awt.Image;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

import org.thenesis.gendes.awt.ViewCanvas;
import org.thenesis.gendes.contexts.ContextEvent;
import org.thenesis.gendes.contexts.ContextEventListener;
import org.thenesis.gendes.contexts.ContextNode;
import org.thenesis.gendes.contexts.GameBoyEmulationContext;
import org.thenesis.gendes.contexts.SystemEmulationContext;
import org.thenesis.gendes.gameboy.GameBoy;

public class VideoCanvas extends ViewCanvas {
    public SystemEmulationContext systemContext;
    public GameBoyEmulationContext gameBoyContext;

    private final ContextNode node=new ContextNode() {
    	public void detach() { VideoCanvas.this.detach(); }
    };
        
    private final ContextEventListener onEndOfFrameListener=new ContextEventListener() {
        public void processEvent(ContextEvent e) { repaint(); }
    };
    
    public VideoCanvas() {
        super();
        
        addKeyListener(keyAdapter);
    }
    
    public void attach(GameBoyEmulationContext gbc) {
        detach();
        
        systemContext=gbc.systemContext; gameBoyContext=gbc;
        
        synchronized(systemContext.emulationSystem) {
        	gameBoyContext.add(node);
            gameBoyContext.onEndOfFrameDispatcher.addListener(onEndOfFrameListener);
        }
        
        updateZoom();
    }
    
    public void detach() {
        if (gameBoyContext==null) return;
        
        context.detachAll();
        
        synchronized(systemContext.emulationSystem) {
	        gameBoyContext.onEndOfFrameDispatcher.removeListener(onEndOfFrameListener);
	        gameBoyContext.remove(node);
        }
        
        systemContext=null; gameBoyContext=null;
    }

    protected Image lockImage() {
    	if (gameBoyContext==null) return null;
    	gameBoyContext.videoLockImage();
    	return gameBoyContext.videoOutputImage[gameBoyContext.videoCurrentOutputImage];
    }
    
    protected void unlockImage() {
    	gameBoyContext.videoUnlockImage();
    }
    
    protected void snapshot() {
        if (gameBoyContext!=null) gameBoyContext.videoSnapshot();                                 
    }
    
    protected void onDropString(String s) {
    	if (gameBoyContext!=null) {
    		try {
    			gameBoyContext.cartridgeInsert(s);
    		} catch (IOException e) {
    			
    		}
    	}
    }
    
    protected void onDropFile(File f) {
    	if (gameBoyContext!=null) {
    		try {
    			gameBoyContext.cartridgeInsert(f);
    		} catch (IOException e) {
    			
    		}
    	}
    }
        
    private final KeyAdapter keyAdapter=new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
            int key=e.getKeyCode();
            int padKeys=0;
            boolean padChanged=false;
            
            switch (key) {
            // Game Boy inputs.
            case KeyEvent.VK_UP: padKeys=GameBoy.PAD_UP; padChanged=true; break;
            case KeyEvent.VK_DOWN: padKeys=GameBoy.PAD_DOWN; padChanged=true; break;
            case KeyEvent.VK_LEFT: padKeys=GameBoy.PAD_LEFT; padChanged=true; break;
            case KeyEvent.VK_RIGHT: padKeys=GameBoy.PAD_RIGHT; padChanged=true; break;
            case KeyEvent.VK_E: padKeys=GameBoy.PAD_A; padChanged=true; break;
            case KeyEvent.VK_R: padKeys=GameBoy.PAD_B; padChanged=true; break;
            case KeyEvent.VK_D: padKeys=GameBoy.PAD_START; padChanged=true; break;
            case KeyEvent.VK_F: padKeys=GameBoy.PAD_SELECT; padChanged=true; break;
            
            // Image zoom.
            case KeyEvent.VK_SUBTRACT: setZoom(zoom-1); updateZoom(); break; // Zoom out.
            case KeyEvent.VK_ADD: setZoom(zoom+1); updateZoom(); break; // Zoom in.
            case KeyEvent.VK_MULTIPLY: switchZoomMode(); updateZoom(); break; // Zoom mode.
            }
            
            if ((gameBoyContext!=null) && padChanged) {
                synchronized(systemContext.emulationSystem) {
                    padKeys=gameBoyContext.gameBoy.getKeys()|padKeys;
                    gameBoyContext.gameBoy.setKeys(padKeys);
                }
            }
        }
        
        public void keyReleased(KeyEvent e) {
            int key=e.getKeyCode();
            int padKeys=0;
            boolean padChanged=false;
            
            switch (key) {
            // Game Boy inputs.
            case KeyEvent.VK_UP: padKeys=GameBoy.PAD_UP; padChanged=true; break;
            case KeyEvent.VK_DOWN: padKeys=GameBoy.PAD_DOWN; padChanged=true; break;
            case KeyEvent.VK_LEFT: padKeys=GameBoy.PAD_LEFT; padChanged=true; break;
            case KeyEvent.VK_RIGHT: padKeys=GameBoy.PAD_RIGHT; padChanged=true; break;
            case KeyEvent.VK_E: padKeys=GameBoy.PAD_A; padChanged=true; break;
            case KeyEvent.VK_R: padKeys=GameBoy.PAD_B; padChanged=true; break;
            case KeyEvent.VK_D: padKeys=GameBoy.PAD_START; padChanged=true; break;
            case KeyEvent.VK_F: padKeys=GameBoy.PAD_SELECT; padChanged=true; break;
            }
            
            if ((gameBoyContext!=null) && padChanged) {
                synchronized(systemContext.emulationSystem) {
                    padKeys=gameBoyContext.gameBoy.getKeys()&~padKeys;
                    gameBoyContext.gameBoy.setKeys(padKeys);
                }
            }
        }
    };
}
