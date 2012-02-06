package org.thenesis.emulation.eclipse.gameboy;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.thenesis.gendes.contexts.ContextEvent;
import org.thenesis.gendes.contexts.ContextEventListener;
import org.thenesis.gendes.contexts.GameBoyEmulationContext;
import org.thenesis.gendes.contexts.SystemEmulationContext;
import org.thenesis.gendes.gameboy.GameBoy;

/**
 * This is the top-level class of the plugin.
 */
public class GameBoyPlugin extends AbstractUIPlugin {
	
    public static final String PLUGIN_ID="org.thenesis.emulation.eclipse.gameboy";
    
    /**
     * Default instance of the receiver
     */ 
    private static GameBoyPlugin inst;
    

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
	
    
    /**
     * Creates the plugin and caches its default instance.
     */
    public GameBoyPlugin() {
        if (inst==null) {
            inst=this;
            initialize();
        }
    }

    private void initialize() {
        initializeEmulation();
        initializeUI();
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

        // TODO: move the remaining of this method elsewhere.
        // Cartridge.
        try {
	        String fileName = "Super Mario Land.gb";
	        InputStream cartridgeInputStream = getClass().getResourceAsStream("/cartridges/" + fileName);
	        gameBoyContext.cartridgeInsert(cartridgeInputStream);
		} catch (IOException ioe) {
		}
        gameBoyContext.switchPower(true);
        
//        systemContext.emulationThread.resume();
    }
    
    private void initializeUI() {
    	
    }
    
    /**
     * Gets the plugin singleton.
     *
     * @return the default GameBoyPlugin instance
     */
    public static GameBoyPlugin getDefault() {
        return inst;
    }
    
	public SystemEmulationContext getSystemContext() {
		return systemContext;
	}

	public GameBoyEmulationContext getGameBoyContext() {
		return gameBoyContext;
	}
}
