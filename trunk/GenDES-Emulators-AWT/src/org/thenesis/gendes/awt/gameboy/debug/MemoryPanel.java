package org.thenesis.gendes.awt.gameboy.debug;


import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import org.thenesis.gendes.EmulationThread;
import org.thenesis.gendes.Tools;
import org.thenesis.gendes.awt.AddressDialog;
import org.thenesis.gendes.awt.MyCheckboxMenu;
import org.thenesis.gendes.awt.MyCheckboxMenuItem;
import org.thenesis.gendes.contexts.ContextEvent;
import org.thenesis.gendes.contexts.ContextEventListener;
import org.thenesis.gendes.contexts.ContextNode;
import org.thenesis.gendes.contexts.GameBoyEmulationContext;
import org.thenesis.gendes.contexts.SystemEmulationContext;
import org.thenesis.gendes.debug.MemoryScreen;
import org.thenesis.gendes.gameboy.GameBoy;


//******************************************************************************
// Memory panel.
//******************************************************************************
public class MemoryPanel extends DebugPanel {
    private Frame parentFrame;
    
    private SystemEmulationContext systemContext;
    private EmulationThread emulationThread;
    private GameBoyEmulationContext gameBoyContext;    
    private GameBoy gameBoy;
    
    private int addressSpace=GameBoy.ADDRESS_SPACE_CPU;
    private final MemoryScreen.AddressSpaceState addressSpaceStates[]=new MemoryScreen.AddressSpaceState[5]; 
    
    private BufferedImage screenImg;
    private final int palette[]=new int[256];
    private boolean forceUpdateFlag=false;
    
    private final ContextNode gameBoyNode=new ContextNode() {
    	public void detach() { MemoryPanel.this.detach(); }
    };
    
    private final MemoryScreen memoryScreen=new MemoryScreen() {
        public int read8(int address) { return gameBoy.cpu.cpuRead8(address);  }
        public void write8(int address, int data) { gameBoy.cpu.cpuWrite8(address, data); }
    };
    
    private final Point mouseCharacter=new Point();
    
    private final ContextEventListener onPauseListener=new ContextEventListener() {
        public void processEvent(ContextEvent e) { memoryScreen.updateScreen(true); repaint(); }
    };
    
    private final ContextEventListener onResetListener=new ContextEventListener() {
        public void processEvent(ContextEvent e) {
            resetAddressSpaces();
            // memoryScreen.updateScreen(true);
            repaint();
        }
    };
    
    private final PopupMenu memoryMenu=new PopupMenu("Memory");
    private final MyCheckboxMenu groupMenu=new MyCheckboxMenu("Group") {
        public void onItemChanged(MyCheckboxMenuItem lastItem, MyCheckboxMenuItem newItem) {
        }        
    };
    private final MyCheckboxMenuItem groupMenuItems[]={
            new MyCheckboxMenuItem("None", 0),
            new MyCheckboxMenuItem("1", 1),
            new MyCheckboxMenuItem("2", 2),
            new MyCheckboxMenuItem("3", 3),
            new MyCheckboxMenuItem("4", 4),
    };
    private final MyCheckboxMenuItem insertionMenu=new MyCheckboxMenuItem("Insertion mode") {
        protected void processActionEvent(ActionEvent e) { }
    };
    private final MyCheckboxMenu addressSpaceMenu=new MyCheckboxMenu("Address space") {
        public void onItemChanged(MyCheckboxMenuItem lastItem, MyCheckboxMenuItem newItem) {
        }
    };
    private final MyCheckboxMenuItem addressSpaceMenuItems[]={
            new MyCheckboxMenuItem("CPU", GameBoy.ADDRESS_SPACE_CPU),
            new MyCheckboxMenuItem("Work RAM", GameBoy.ADDRESS_SPACE_WRAM),
            new MyCheckboxMenuItem("Video RAM", GameBoy.ADDRESS_SPACE_VRAM),
            new MyCheckboxMenuItem("Cartridge ROM", GameBoy.ADDRESS_SPACE_CROM),
            new MyCheckboxMenuItem("Cartridge RAM", GameBoy.ADDRESS_SPACE_CRAM)
    };
    
    public MemoryPanel() {
        super("Memory");
        
        palette[0]=0x00ffffff; palette[1]=0x00000000; palette[2]=0x00e0ffe0; palette[3]=0x00000000; palette[4]=0x0000ff00; palette[5]=0x00000000; palette[6]=0x00000000; palette[7]=0x0000ff00;
        palette[8]=0x00ffffff; palette[9]=0x00000000; palette[10]=0x00ffe0e0; palette[11]=0x00000000; palette[12]=0x00ff0000; palette[13]=0x00000000; palette[14]=0x00000000; palette[15]=0x00ff0000;

        buildMenu();
        
        addComponentListener(componentAdapter);
        addKeyListener(keyAdapter);
        addMouseListener(mouseAdapter);
    }
    
    public void attach(GameBoyEmulationContext gbc, Frame pf) {
        detach();
        
        parentFrame=pf;
        
        systemContext=gbc.systemContext; emulationThread=systemContext.emulationThread;
        gameBoyContext=gbc; gameBoy=gbc.gameBoy;
        systemContext.onPauseDispatcher.addListener(onPauseListener);
        gameBoyContext.add(gameBoyNode);
        gameBoyContext.onResetDispatcher.addListener(onResetListener);
        
        initializeAddressSpaces();
        resizeScreen();
        repaint();
    }
    
    public void detach() {
        if (gameBoyContext==null) return;
        
        systemContext.onPauseDispatcher.removeListener(onPauseListener);
        gameBoyContext.onResetDispatcher.removeListener(onResetListener);
        gameBoyContext.remove(gameBoyNode);
        systemContext=null; emulationThread=null;
        gameBoyContext=null; gameBoy=null;
        
        parentFrame=null;
        
        repaint();
    }

    public void forceUpdate() {
    	forceUpdateFlag=true;
    }
            
    private void initializeAddressSpaces() {
        MemoryScreen.AddressSpaceState ass;
        
        for (int i=0; i<5; i++) {
            ass=new MemoryScreen.AddressSpaceState();
            addressSpaceStates[i]=ass;
            ass.startAddress=0;
            ass.cursorAddress=0; ass.cursorAddressNibble=0;            
        }
        
        resetAddressSpaces();
    }
    
    public void resetAddressSpaces() {
        MemoryScreen.AddressSpaceState ass;
        ass=addressSpaceStates[GameBoy.ADDRESS_SPACE_CPU]; ass.addressMask=0xffff;
        ass=addressSpaceStates[GameBoy.ADDRESS_SPACE_CRAM]; ass.addressMask=0xffff;
        ass=addressSpaceStates[GameBoy.ADDRESS_SPACE_CROM]; ass.addressMask=0xffff;
        ass=addressSpaceStates[GameBoy.ADDRESS_SPACE_VRAM]; ass.addressMask=0xffff;
        ass=addressSpaceStates[GameBoy.ADDRESS_SPACE_WRAM]; ass.addressMask=0xffff;        
        memoryScreen.restoreAddressSpaceState(addressSpaceStates[addressSpace]);
    }
    
    public void changeAddressSpace(int as) {
        memoryScreen.saveAddressSpaceState(addressSpaceStates[addressSpace]);
        memoryScreen.restoreAddressSpaceState(addressSpaceStates[as]);
        addressSpace=as;
    }
   
    private void resizeScreen() {
        if (gameBoyContext==null) return;
        int fw=memoryScreen.getFontWidth(), fh=memoryScreen.getFontHeight(), w=getWidth(), h=getHeight();
        int sw=w/fw, sh=h/fh;
        if (sw<1) sw=1; if (sh<1) sh=1;
        memoryScreen.setScreenSize(sw, sh);
        
        int spw=memoryScreen.getScreenPixelsWidth(), sph=memoryScreen.getScreenPixelsHeight();
        screenImg=new BufferedImage(spw, sph, BufferedImage.TYPE_INT_RGB);
    }
    
    private void draw(Graphics g) { 
        if (gameBoyContext==null) return;
        if (emulationThread.isPaused() || forceUpdateFlag) {
        	forceUpdateFlag=false;
        	synchronized (systemContext.emulationSystem) {
        		memoryScreen.updateScreen(false);
        	}
        }
        DataBufferInt dataBuffer=(DataBufferInt)screenImg.getRaster().getDataBuffer();
        memoryScreen.drawScreen(dataBuffer.getData(), screenImg.getWidth(), screenImg.getHeight(), palette);
        g.drawImage(screenImg, 1, 1, this);    
	}
    
    public void paint(Graphics g) { drawBackground(g); draw(g); }
    public void update(Graphics g) { paint(g); }
    
    private void buildMenu() {
        groupMenu.setItems(groupMenuItems);
        memoryMenu.add(groupMenu);
        memoryMenu.add(insertionMenu);
        memoryMenu.addSeparator();
        addressSpaceMenu.setItems(addressSpaceMenuItems);
        memoryMenu.add(addressSpaceMenu);
        add(memoryMenu);
    }
    
    private final ComponentAdapter componentAdapter=new ComponentAdapter() {
        public void componentResized(ComponentEvent e) { resizeScreen(); repaint(); }        
    };
    
    private final KeyAdapter keyAdapter=new KeyAdapter() {
        public void keyTyped(KeyEvent e) {
            if (gameBoyContext==null) return;
            if (!emulationThread.isPaused()) return;
            
            char key=e.getKeyChar();
        	if (memoryScreen.isInsertionModeActivated()) {
                memoryScreen.insertKey(key);
                repaint();
        	} else {
	            switch (key) {
	            case '=':
		            memoryScreen.goTo(gameBoy.cpu.rPC);
		            memoryScreen.resetCursor();
	                repaint();
	                break;
	            case '@': {
	                String s=AddressDialog.getValue(parentFrame);
	                if (s!=null) {
	                    memoryScreen.goTo(Tools.stringToInt(s)&0xffff);
	                    memoryScreen.resetCursor();
	                    repaint();
	                }
	            }
	            break;
	            default:
	                memoryScreen.insertKey(key);
	                repaint();
	                break;
	            }
        	}
        }
        
        public void keyPressed(KeyEvent e) {
            if (gameBoyContext==null) return;
            if (!emulationThread.isPaused()) return;
            
            int key=e.getKeyCode();
            switch (key) {
            case KeyEvent.VK_CONTROL:
                if (memoryScreen.isInsertionModeActivated()) memoryScreen.setAsciiFieldEdition(!memoryScreen.isAsciiFieldEdited());
                repaint();
                break;
            case KeyEvent.VK_INSERT:
                memoryScreen.activateInsertionMode(!memoryScreen.isInsertionModeActivated());
                if (memoryScreen.isInsertionModeActivated()) memoryScreen.resetCursor();
                repaint();
                break;
            case KeyEvent.VK_HOME:
                if (memoryScreen.isInsertionModeActivated()) memoryScreen.moveCursorToStartOfLine();
                repaint();
                break;
            case KeyEvent.VK_END:
                if (memoryScreen.isInsertionModeActivated()) memoryScreen.moveCursorToEndOfLine();
                repaint();
                break;
            case KeyEvent.VK_UP:
                if (memoryScreen.isInsertionModeActivated()) memoryScreen.moveCursorUp(); 
                else memoryScreen.goToPreviousLine();
                repaint();
                break;
            case KeyEvent.VK_DOWN:
                if (memoryScreen.isInsertionModeActivated()) memoryScreen.moveCursorDown();
                else memoryScreen.goToNextLine();
                repaint();
                break;
            case KeyEvent.VK_LEFT:
                if (memoryScreen.isInsertionModeActivated()) memoryScreen.moveCursorLeft();
                else memoryScreen.goTo(memoryScreen.getStartAddress()-1);
                repaint();
                break;
            case KeyEvent.VK_RIGHT:
                if (memoryScreen.isInsertionModeActivated()) memoryScreen.moveCursorRight();
                else memoryScreen.goTo(memoryScreen.getStartAddress()+1);
                repaint();
                break;
            case KeyEvent.VK_PAGE_UP:
                if (memoryScreen.isInsertionModeActivated()) memoryScreen.moveCursorToPreviousPage();
                else memoryScreen.goToPreviousPage();
                repaint();
                break;
            case KeyEvent.VK_PAGE_DOWN:
                if (memoryScreen.isInsertionModeActivated()) memoryScreen.moveCursorToNextPage();
                else memoryScreen.goToNextPage();
                repaint();
                break;
            }
        }        
    };

    private final MouseAdapter mouseAdapter=new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
            int x=e.getX(), y=e.getY();
            if (memoryScreen.positionToCharacter(x, y, mouseCharacter)) return;
            if (memoryScreen.isInsertionModeActivated()) {
                memoryScreen.setCursorPosition(mouseCharacter.x, mouseCharacter.y);
                repaint();
            }
        }        
    };    
}
//******************************************************************************
