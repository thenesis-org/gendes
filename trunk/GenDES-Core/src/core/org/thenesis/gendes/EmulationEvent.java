/*
 * .
 */
package org.thenesis.gendes;


/**
 * The <code>EmulationEvent</code> class represents an event that can be used in an <code>EmulationSystem</code> or in an <code>EmulationClock</code>. 
 * 
 */
public abstract class EmulationEvent {
    // System.
    protected EmulationSystem system;
    // The device the event belongs to.
    protected EmulationDevice device;
    // Node in the linked list of events of a clock.
    protected EmulationEvent previousEvent, nextEvent;
    // The name of the event.
    public String name;
    // Current state.
    protected boolean used=false;
    // Type of event.
    public static final int
    	TYPE_SYSTEM=            0, // System event.
    	TYPE_SYSTEM_CLOCK=      1, // System clock event.
    	TYPE_CLOCK=             2; // Clock event.
    protected int type=TYPE_SYSTEM;    
    // Time, duration, current time step of an event.
    public final TimeStamp time=new TimeStamp();
    // Clock.
    protected EmulationClock clock;
    // Clock cycle.
    public long clockCycle;
    
    // For the heap.
    protected int index=-1;

    /**
     * Constructs a new event.
     * @param name the name of the event
     */
    public EmulationEvent(String name) {
        this.name=name;    	
    } 

    /**
     * Constructs a new event and attaches it to a device.
     * @param device the device the event must belong to
     * @param name the name of the event
     */
    public EmulationEvent(EmulationDevice device, String name) {
        this.name=name;
        device.registerEvent(this);
    } 

    /**
     * Unregister this event.
     */
    public final void unregister() {
        device.unregisterEvent(this);
    }
    
    /**
     * Returns whether the event is used or not.
     * @return <code>false</code> if this event is not used, <code>true</code> otherwise
     */
    public final boolean isUsed() {
        return used;
    }
    
    /** This method is called during the processing of a non-discrete event (ie a processus). */
    public abstract void processEvent();
    /** This method is called when a processing event need to be broken. */
    public abstract void breakEvent();
}
