package org.thenesis.gendes.lynx;

import java.io.InputStream;

import org.thenesis.gendes.EmulationClock;
import org.thenesis.gendes.EmulationDevice;
import org.thenesis.gendes.EmulationState;

public final class Lynx extends EmulationDevice {

	//********************************************************************************
    // Public.
    //********************************************************************************
    /** Lynx models. */
    public static final int MODEL_I=0, MODEL_II=1;

    /** Screen size. */
    public static final int SCREEN_WIDTH=160, SCREEN_HEIGHT=102;

    public Lynx() {
        super("Lynx");
//	    globalInitialize();
	}

    protected void register() {}
    
    protected void unregister() {}
    
    /**
     * Save the state.
     * Do not call asynchronously.
     */
    protected void saveState(EmulationState s) {    
    }
    
    /**
     * Restore the state of the emulator.
     * Do not call asynchronously.
     */
    protected void restoreState(EmulationState s) {    
    }
    
    /**
     * Set the listener.
     * Do not call asynchronously.
     */
    /*
    public void setListener(LynxListener l, LynxEvent e) {
        globalListener=l;
        globalEvent=e;
    }
    */
    
	/**
     * Set the model of Game Boy.
     * Do not call asynchronously.
     */
	public void setModel(int model) {
	    if (globalPowered) return;
	    globalLynxModel=model;
	}

	/** Get the model of Game Boy. */
	public int getModel() {
	    return globalLynxModel;
	}

	/**
     * Set the cartridge.
     * Do not call asynchronously.
     */
	public boolean setCartridge(InputStream inputStream) {
	    if (globalPowered) return true;
	    return true;
    }

    /**
     * Remove the current cartridge.
     * Do not call asynchronously.
     */
    public void removeCartridge() {
        if (globalPowered) return;
    }
    
    /** Return true if a cartridge is inserted. */
    public boolean isCartridgeInserted() {
        return cartridgeInserted;
    }
    
    /** Return the cartridge infos. */
/*
  	public CartridgeInfos getCartridgeInfos() {
        return cartridgeInfos;
    }
*/
    //********************************************************************************
    // Private.
    //********************************************************************************
    //--------------------------------------------------------------------------------
    // Global.
    //--------------------------------------------------------------------------------
    // Lynx model.
    protected int globalLynxModel;
    // Global state.
    protected boolean globalPowered=false;
    // Listener.
//    private LynxListener globalListener;
//    private LynxEvent globalEvent;
    // Initialize the emulator. Must be done before any other call.
    private void globalInitialize() {
        addChildDevice(clock);

        globalLynxModel=MODEL_I;
        globalPowered=false;        
    }
    
    // Global reset of the Lynx.
    private void globalReset() {
    }

	//--------------------------------------------------------------------------------
    // Clock.
	//--------------------------------------------------------------------------------    
    public static final int CLOCK_FREQUENCY=16777216;
	// Current clock.
    protected EmulationClock clock=new EmulationClock("Lynx clock");
    protected long clockCurrentCycle;

    // Reset the clock.
    private void clockReset() {
        clock.setFrequency(CLOCK_FREQUENCY);
    }

	//--------------------------------------------------------------------------------
    // Internal ROM.
	//--------------------------------------------------------------------------------
    
	//--------------------------------------------------------------------------------
	// Internal RAM.
	//--------------------------------------------------------------------------------
    public static final int RAM_SIZE=0x10000;
    protected byte workRam[]=new byte[RAM_SIZE];

	//--------------------------------------------------------------------------------
    // Keys.
	//--------------------------------------------------------------------------------
    
	//--------------------------------------------------------------------------------
	// Serial communication.
	//--------------------------------------------------------------------------------

	//--------------------------------------------------------------------------------
	// Cartridge.
	//--------------------------------------------------------------------------------
    protected boolean cartridgeInserted;
//    protected CartridgeInfos cartridgeInfos=new CartridgeInfos();

	//--------------------------------------------------------------------------------
	// Timers.
	//--------------------------------------------------------------------------------
}
