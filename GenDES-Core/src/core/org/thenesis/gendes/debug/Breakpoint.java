package org.thenesis.gendes.debug;

import org.thenesis.gendes.debug.BreakpointList;

/**
 * This class is used to represent a breakpoint.
 * 
 */
public class Breakpoint {
    protected BreakpointList list;
    protected int index=-1;
    protected int address;
    
    public final BreakpointList getList() {
        return list;
    }
    
    public final int getAddress() {
        return address;
    }
}
