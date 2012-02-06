package org.thenesis.gendes.awt.gameboy.application;


import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.io.IOException;

import org.thenesis.gendes.contexts.ContextEvent;
import org.thenesis.gendes.contexts.ContextEventListener;
import org.thenesis.gendes.contexts.GameBoyEmulationContext;
import org.thenesis.gendes.contexts.SystemEmulationContext;
import org.thenesis.gendes.gameboy.GameBoy;


//******************************************************************************
// Main class.
//******************************************************************************
public class GameBoyEmulatorApplication {
    
	private SystemEmulationContext systemContext;
	private GameBoyEmulationContext gameBoyContext;
	
    private final ContextEventListener onCartridgeRemovedListener=new ContextEventListener() {
        public void processEvent(ContextEvent e) {
        	try {
            	gameBoyContext.cartridgeSaveBackup();
        	} catch (IOException ioe) {
        	}
        }
    };
    
    private final ContextEventListener onCartridgeInsertedListener=new ContextEventListener() {
        public void processEvent(ContextEvent e) {
        	try {
        		gameBoyContext.cartridgeLoadBackup();        		
        	} catch (IOException ioe) {
        	}
        }
    };
	
	private MainFrame mainFrame;
	private DebugFrame debugFrame;
    
    public static void main(String[] args) {

        try {
            GameBoyEmulatorApplication emulator=new GameBoyEmulatorApplication();
            emulator.initialize();
            
            if (args.length==1) {
            	try {
	                String filename=args[0];
	                emulator.gameBoyContext.cartridgeInsert(filename);
	                emulator.gameBoyContext.switchPower(true);
            	} catch (IOException e) {
            		System.out.println("Cannot load cartridge.");
            	}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void initialize() {        
        initializeEmulation();
        initializeGUI();        
    }
    
    private void initializeEmulation() {
        systemContext=new SystemEmulationContext();
        systemContext.emulationThread.pause();
        systemContext.emulationThread.start();
        
        gameBoyContext=new GameBoyEmulationContext(systemContext);
        gameBoyContext.audioEnable(true);
        gameBoyContext.gameBoy.setModel(GameBoy.MODEL_GAME_BOY_COLOR);
        gameBoyContext.gameBoy.cpu.activateDebugMode(true);
        gameBoyContext.gameBoy.cpu.getBreakpoints().setMaxBreakpoints(256);
        gameBoyContext.onCartridgeRemovedDispatcher.addListener(onCartridgeRemovedListener);
        gameBoyContext.onCartridgeInsertedDispatcher.addListener(onCartridgeInsertedListener);
    }

    private void initializeGUI() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyEventDispatcher);

        mainFrame=new MainFrame(this);
        mainFrame.attach(gameBoyContext);
        mainFrame.setLocation(0, 0);
        mainFrame.setSize(400, 400);
        mainFrame.setVisible(true);
        
        debugFrame=new DebugFrame();
        debugFrame.attach(gameBoyContext);
        int w=96*8+16, h=512;
        debugFrame.setLocation(400, 0);
        debugFrame.setSize(w, h);
        debugFrame.setVisible(true);
    }
    
	public void shutdown() {
        systemContext.shutdown();
		System.exit(0);
    }
    
    //------------------------------------------------------------------------------
    // Global GUI management.
    //------------------------------------------------------------------------------
    private final KeyEventDispatcher keyEventDispatcher=new KeyEventDispatcher() {
        public boolean dispatchKeyEvent(KeyEvent e) {
            if (e.getID()!=KeyEvent.KEY_PRESSED) return false;
            
            int key=e.getKeyCode();
    
            switch (key) {
            case KeyEvent.VK_ESCAPE:
            	mainFrame.videoPanel.switchFullScreen();
                break;
            case KeyEvent.VK_F1:
                gameBoyContext.reset();
                return true;
            case KeyEvent.VK_F2:
                gameBoyContext.cpuTraceInterrupts=!gameBoyContext.cpuTraceInterrupts;
                return true;
            case KeyEvent.VK_F3:
                gameBoyContext.cpuStepIntoi();
                return true;
            case KeyEvent.VK_F4:
                gameBoyContext.cpuStepOuti();
                return true;
            case KeyEvent.VK_F5:
                gameBoyContext.cpuStepInto();
                return true;
            case KeyEvent.VK_F6:
                gameBoyContext.cpuStepOver();
                return true;
            case KeyEvent.VK_F7:
                gameBoyContext.cpuStepOut();
                return true;
            case KeyEvent.VK_F8:
                if (systemContext.emulationThread.isPaused()) systemContext.emulationThread.resume();
                else systemContext.emulationThread.pause();
                return true;
            case KeyEvent.VK_F9:
                systemContext.emulationThread.enableThrottling(!systemContext.emulationThread.isThrottlingEnabled());
                debugFrame.performancePanel.repaint();
                return true;
            case KeyEvent.VK_F10:
                gameBoyContext.videoGenerationEnabled=!gameBoyContext.videoGenerationEnabled;
                debugFrame.performancePanel.repaint();
                return true;
            case KeyEvent.VK_F11:
                gameBoyContext.videoPresentationEnabled=!gameBoyContext.videoPresentationEnabled;
                debugFrame.performancePanel.repaint();
                return true;
            case KeyEvent.VK_F12:
                gameBoyContext.videoBreakOnEndOfFrame();
                return true;
            }
            
            return false;
        }    
    };    
}
//******************************************************************************


