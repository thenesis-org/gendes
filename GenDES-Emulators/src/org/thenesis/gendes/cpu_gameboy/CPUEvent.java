package org.thenesis.gendes.cpu_gameboy;

import org.thenesis.gendes.debug.Breakpoint;

public class CPUEvent {
    public static final int EVENT_STEP_INTO=                0;
    public static final int EVENT_STEP_OVER=                1;
    public static final int EVENT_STEP_OUT=                 2;
    public static final int EVENT_STEP_INTO_I=              3;
    public static final int EVENT_STEP_OUT_I=               4;
    public static final int EVENT_BREAKPOINT=               5;
    public static final int EVENT_ILLEGAL=                  6;

    public CPU cpu;
    
    public int type; // Type of event.
    
    // Breakpoint infos.
    public Breakpoint breakpoint;
    public int address; // Address of the breakpoint.
    public Object userData; // User data.        
}
