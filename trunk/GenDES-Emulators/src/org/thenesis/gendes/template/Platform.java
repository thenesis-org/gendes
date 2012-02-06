package org.thenesis.gendes.template;

import org.thenesis.gendes.EmulationClock;
import org.thenesis.gendes.EmulationDevice;
import org.thenesis.gendes.EmulationEvent;
import org.thenesis.gendes.EmulationState;
import org.thenesis.gendes.TimeStamp;

public class Platform extends EmulationDevice {
	
    public Platform() {
    	super("Platform - template");
    }
    
	protected void register() {
		// TODO Auto-generated method stub
		
	}

	protected void unregister() {
		// TODO Auto-generated method stub
		
	}    

	protected void restoreState(EmulationState state) {
		// TODO Auto-generated method stub
		
	}

	protected void saveState(EmulationState state) {
		// TODO Auto-generated method stub
		
	}

	
    //--------------------------------------------------------------------------------
    // Global.
    //--------------------------------------------------------------------------------
    // Global state.
    protected boolean globalPowered=false;
	
	/**
     * Switch the power.
     * This method has no effect if you try to switch on while the power is already on or
     * if you try to switch off while the power is already off. 
     * Do not call asynchronously.
     * @param on <code>false</code> to switch power off, <code>true</code> to switch power on
     */
	public void switchPower(boolean on) {
	    if (on && !globalPowered) {
	        globalPowered=true;
	        reset();
	    } else if (!on && globalPowered) { 
	        globalPowered=false;
            removeAllEvents();
	    }

	}

	/**
	 * Give the state of the power switch.
	 * @return <code>false</code> if power is off, <code>true</code> if power is on
	 */
	public boolean isPowered() {
	    return globalPowered;
	}
	
	/**
     * Reset.
     * Do not call asynchronously.
     */
	public void reset() {
	    if (!globalPowered) return;
        clockCurrentCycle=clock.timeToCycle(system.getCurrentTime());
        globalReset();
	}
    
    // Initialize the emulator. Must be done before any other call.
    private void globalInitialize() {
        addChildDevice(clock);
        addChildDevice(cpu);

        globalPowered=false;
        
    }
    
    // Global reset of the Game Boy.
    private void globalReset() {
        clockReset();
        cpuReset();
    }
	
    //--------------------------------------------------------------------------------
    // Clock.
    //--------------------------------------------------------------------------------
    static final int CLOCK_FREQUENCY=	100000000;
    
    protected int clockFrequency;
    protected final EmulationClock clock=new EmulationClock("GameBoy clock");
    protected long clockCurrentCycle;

    // Reset the clock.
    private void clockReset() {
        clockFrequency=CLOCK_FREQUENCY;
        clock.setFrequency(clockFrequency);
    }

    //--------------------------------------------------------------------------------
    // CPU.
    //--------------------------------------------------------------------------------
    // Reset the CPU.
    private void cpuReset() {
        cpu.reset();
        cpuStart();
    }
    
    private void cpuStart() {
        cpuEvent.clockCycle=clockCurrentCycle;
        clock.addEvent(cpuEvent);    	
    }
    
    private void cpuStop() {
    	system.removeEvent(cpuEvent);
    }
    
    private void cpuUpdate() {
        long cycles=system.getRemainingCycles(clock.frequency);
        if (cycles<1) cycles=1;
        if (cycles>Integer.MAX_VALUE) cycles=Integer.MAX_VALUE;
        cycles=cpu.execute((int)cycles);
        cpuEvent.clockCycle=TimeStamp.add(cpuEvent.clockCycle, cycles);
        clock.addEvent(cpuEvent);
    }

    // Update current time with the CPU time.
    private void cpuSetCurrentCycle() {
        clockCurrentCycle=TimeStamp.add(cpuEvent.clockCycle, cpu.cycles);
    }
    
    // Get the current CPU time.
    private long cpuGetCurrentCycle() {
        return TimeStamp.add(cpuEvent.clockCycle, cpu.cycles);
    }
    
    // Event.
    private final EmulationEvent cpuEvent=new EmulationEvent(this, "CPU") {
        public final void processEvent() { cpuUpdate(); }
        
        public final void breakEvent() {
            if (cpu.running) cpu.breakExecution=true;
        }
    };
    
    // CPU.
    public final CPU cpu=new CPU() {
    };



}
