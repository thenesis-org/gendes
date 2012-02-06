package org.thenesis.gendes.template;

import org.thenesis.gendes.EmulationDevice;
import org.thenesis.gendes.EmulationState;

public class CPU extends EmulationDevice {
    // True if the CPU is running.
    public boolean running=false;
	// True if we must break execution.
    public boolean breakExecution=false;

	// Number of cycles elapsed since the last call to execute().
	public int cycles;

	public CPU() {
		super("Template CPU");
	}
	
	protected void register() {
		// TODO Auto-generated method stub
		
	}

	protected void restoreState(EmulationState state) {
		// TODO Auto-generated method stub
		
	}

	protected void saveState(EmulationState state) {
		// TODO Auto-generated method stub
		
	}

	protected void unregister() {
		// TODO Auto-generated method stub
		
	}

	public final void reset() {
		// TODO
	}
	
	public final int execute(int requestedCycles) {
        running=true;
        breakExecution=false;
        cycles=0;
        
        do {

	    } while (cycles<requestedCycles && !breakExecution);
	
	    running=false;
		requestedCycles=cycles;
		cycles=0;
		return requestedCycles;
	}

}
