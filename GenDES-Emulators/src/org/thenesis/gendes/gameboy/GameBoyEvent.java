package org.thenesis.gendes.gameboy;


public final class GameBoyEvent {
    public static final int
    	EVENT_VIDEO=					0,
    	EVENT_AUDIO=					1,
    	EVENT_BREAK=					2,
    	EVENT_VBL=						3,
    	EVENT_HBL=						4,
    	EVENT_OAM=						5,
    	EVENT_TIMER=					6;

    public GameBoy gameBoy;

    public int type; // Type of event.
    
    // Breakpoint infos.
    public int addressSpace; // Address space of the breakpoint. 
    public int address; // Address of the breakpoint.
    public Object userData; // User data.        
}
