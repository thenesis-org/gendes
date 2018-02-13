package org.thenesis.gendes.awt.gameboy.debug;


import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import org.thenesis.gendes.FastString;
import org.thenesis.gendes.Tools;
import org.thenesis.gendes.awt.AddressDialog;
import org.thenesis.gendes.contexts.ContextEvent;
import org.thenesis.gendes.contexts.ContextEventListener;
import org.thenesis.gendes.contexts.ContextNode;
import org.thenesis.gendes.contexts.GameBoyEmulationContext;
import org.thenesis.gendes.contexts.SystemEmulationContext;
import org.thenesis.gendes.cpu_gameboy.CPU;
import org.thenesis.gendes.cpu_gameboy.Disassembler;
import org.thenesis.gendes.debug.DisassemblerScreen;
import org.thenesis.gendes.debug.Point;


//******************************************************************************
// Disassembler panel.
//******************************************************************************
public class DisassemblerPanel extends DebugPanel {
    public Frame parentFrame;
    
    public SystemEmulationContext systemContext;    
    public GameBoyEmulationContext gameBoyContext;    
    public CPU cpu;
    
    private final ContextNode gameBoyNode=new ContextNode() {
    	public void detach() { DisassemblerPanel.this.detach(); }
    };
    
    public final Disassembler disassembler=new Disassembler() {
        public int read8(int address) {
            return cpu.cpuRead8(address);
        }
    };

    public final DisassemblerScreen disassemblerScreen=new DisassemblerScreen() {
        protected int read8(int address) {
        	return cpu.cpuRead8(address);
        }
        
        protected void write8(int address, int data) {
        	cpu.cpuWrite8(address, data);
        }
        
        protected int getPC() {
        	return cpu.rPC&0xffff;
        }
        
        protected int findPreviousInstructionLength(int address) {
        	return disassembler.findPreviousInstructionLength(address);
        }
        
        protected int findNextInstructionLength(int address) {
        	return disassembler.findInstructionLength(address);
        }
        
        protected int disassemble(int address, FastString opcode, FastString instruction) {
        	int l=disassembler.disassemble(address);
        	opcode.clear();
            disassembler.printOpcodeBytes(opcode);
            instruction.clear();
            disassembler.printInstruction(instruction);
            return l;
        }

    };
    
    private BufferedImage screenImg;
    private final int palette[]=new int[256];
    private boolean forceUpdateFlag=false;
    
    private final Point mouseCharacter=new Point();
    
    private final ContextEventListener onPauseListener=new ContextEventListener() {
        public void processEvent(ContextEvent e) {
            disassemblerScreen.followPc();
            disassemblerScreen.updateScreen(true);
            repaint();
        }
    };
    
    private final ContextEventListener onResetListener=new ContextEventListener() {
        public void processEvent(ContextEvent e) { repaint(); }
    };
    
    public DisassemblerPanel() {
        super("Disassembler");
        
        disassemblerScreen.setInstructionLength(3);
        disassemblerScreen.setAddressSpace(0xffff, 4);
        
        palette[0]=0x00ffffff; palette[1]=0x00000000; palette[2]=0x00ffff00; palette[3]=0x00000000;
        palette[4]=0x0000ffff; palette[5]=0x00000000; palette[6]=0x0000ff00; palette[7]=0x00000000;
        
        addComponentListener(componentAdapter);
        addKeyListener(keyAdapter);
        addMouseListener(mouseAdapter);
    }
    
    public void attach(GameBoyEmulationContext gbc, Frame pf) {
        detach();
        
        parentFrame=pf;
        
        systemContext=gbc.systemContext; gameBoyContext=gbc;                
        systemContext.onPauseDispatcher.addListener(onPauseListener);
        gameBoyContext.add(gameBoyNode);
        gameBoyContext.onResetDispatcher.addListener(onResetListener);
        
        cpu=gameBoyContext.gameBoy.cpu;
        disassemblerScreen.setBreakpointList(cpu.getBreakpoints());
        
        resizeScreen();
        repaint();
    }

    public void detach() {
        if (gameBoyContext==null) return;
        
        cpu=null;
        disassemblerScreen.setBreakpointList(null);
        
        systemContext.onPauseDispatcher.removeListener(onPauseListener);
        gameBoyContext.onResetDispatcher.removeListener(onResetListener);
        gameBoyContext.remove(gameBoyNode);        
        systemContext=null; gameBoyContext=null;	        

        parentFrame=null;

        repaint();
    }

    public void forceUpdate() {
    	forceUpdateFlag=true;
    }
    
    private void draw(Graphics g) {
        if (gameBoyContext==null) return;
        if (systemContext.emulationThread.isPaused() || forceUpdateFlag) {
        	forceUpdateFlag=false;
        	synchronized (systemContext.emulationSystem) {
        		disassemblerScreen.updateScreen(true);
        	}
        }
        
        DataBufferInt dataBuffer=(DataBufferInt)screenImg.getRaster().getDataBuffer();
        disassemblerScreen.drawScreen(dataBuffer.getData(), screenImg.getWidth(), screenImg.getHeight(), palette);
        g.drawImage(screenImg, 1, 1, this);
    }
    
    private void resizeScreen() {
        if (gameBoyContext==null) return;
        int h=getHeight();
        int sh=h/disassemblerScreen.getFontHeight();
        if (sh<1) sh=1;
        disassemblerScreen.setScreenHeight(sh);        
        
        int spw=disassemblerScreen.getScreenPixelsWidth(), sph=disassemblerScreen.getScreenPixelsHeight();
        screenImg=new BufferedImage(spw, sph, BufferedImage.TYPE_INT_RGB);
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
            case '#': disassemblerScreen.setPcFollowing(!disassemblerScreen.getPcFollowing()); repaint(); break;
            case '=': disassemblerScreen.goToPc(); repaint(); break;
            case '@': {
                String s=AddressDialog.getValue(parentFrame);
                if (s!=null) { disassemblerScreen.goTo(Tools.stringToInt(s)&0xffff); repaint(); }
                }
                break;
            }
        }
        
        public void keyPressed(KeyEvent e) {
            if (gameBoyContext==null) return;
            if (!systemContext.emulationThread.isPaused()) return;
            
            int key=e.getKeyCode();
            
            switch (key) {
            case KeyEvent.VK_INSERT: // TODO: implement edition.
                
                break;
            case KeyEvent.VK_UP: disassemblerScreen.goToPreviousInstruction(); repaint(); break;
            case KeyEvent.VK_DOWN: disassemblerScreen.goToNextInstruction(); repaint(); break;
            case KeyEvent.VK_LEFT: disassemblerScreen.goTo(disassemblerScreen.getStartAddress()-1); repaint(); break;
            case KeyEvent.VK_RIGHT: disassemblerScreen.goTo(disassemblerScreen.getStartAddress()+1); repaint(); break;
            case KeyEvent.VK_PAGE_UP: disassemblerScreen.goToPreviousPage(); repaint(); break;
            case KeyEvent.VK_PAGE_DOWN: disassemblerScreen.goToNextPage(); repaint(); break;            
            }
        }        
    };
    
    private final MouseAdapter mouseAdapter=new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
            if (gameBoyContext==null) return;
            int x=e.getX(), y=e.getY();
            if (disassemblerScreen.positionToCharacter(x, y, mouseCharacter)) return;
            if (mouseCharacter.x<=1) disassemblerScreen.setBreakpoint(mouseCharacter.y);
            repaint();
        }        
    };    
}
//******************************************************************************
