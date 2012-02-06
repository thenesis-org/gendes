package org.thenesis.gendes;

public final class EmulationClock extends EmulationDevice {
    // Clock frequency.
    public double frequency=2.0; // MUST be strictly greater than 1.0 !!!!
    // Reference clock time.
    public final TimeStamp referenceTime=new TimeStamp();

    // Current clock cycle.
    public long currentCycle;
    // Current local clock cycle.
    public long localCurrentCycle;
    // Max number of events.
    private int maxEvents;
    // Number of events in the heap.
    private int nbEvents;
    // Heap of events.
    private EmulationEvent eventsHeap[];
 
    // Event.
    protected final EmulationEvent clockEvent=new EmulationEvent(this, "Clock") {
        public void processEvent() {}
        public void breakEvent() {}
    };

    /**
     * Constructs an EmulationClock.
     * @param name the name of the clock
     */
    public EmulationClock(String name) {
        super(name);
        clockEvent.type=EmulationEvent.TYPE_SYSTEM_CLOCK;
        clockEvent.clock=this;
        clockEvent.time.setMaxTime();
    }
    
    public void register() {
        referenceTime.copy(system.currentTime);
        reschedule();
    }

    public void unregister() {
        system.removeEvent(clockEvent);
    }
    
    public void saveState(EmulationState state) {
    }
    
    public void restoreState(EmulationState state) {
    }
    
    /**
     * Sets the frequency for this clock.
     * @param f the frequency of the clock in cycles/second. It must be >0.
     */
    public void setFrequency(double f) {
        frequency=f;
        if (clockEvent.used) reschedule();
    }
    
    /**
     * Reschedules.
     * Can be called only when the clock is in a system.
     */
    protected void reschedule() {
        long c=(nbEvents>0) ? (eventsHeap[1].clockCycle-currentCycle)&TimeStamp.CYCLE_MASK : TimeStamp.MAX_CYCLE;        
        clockEvent.time.addCycles(referenceTime, 1.0/frequency, c);
        system.addEvent(clockEvent);
    }
    
    //********************************************************************************
    // Time.
    //********************************************************************************            
    /**
     * Returns the current cycle.
     * @return the current cycle
     */
    public long getCurrentCycle() {
        return currentCycle;
    }
    
    /**
     * Converts an absolute time in cycles to a relative time in cycles.
     * @param absoluteCycle the absolute time in cycles
     * @return the time in cycles relative to the current clock time
     */
    public long toRelativeCycle(long absoluteCycle) {
        return (absoluteCycle-currentCycle)&TimeStamp.CYCLE_MASK;
    }
    
    /**
     * Converts a relative time in cycles to an absolute time in cycles.
     * @param relativeCycle the relative time in cycles
     * @return the absolute time in cycles
     */
    public long toAbsoluteCycle(long relativeCycle) {
        return (relativeCycle+currentCycle)&TimeStamp.CYCLE_MASK;
    }
    
    /**
     * Gets the remaining time in cycles until the next event.
     * @return the remaining time in cycles
     */
    public long getRemainingCycles() {
        return (nbEvents>0) ? (eventsHeap[1].clockCycle-currentCycle)&TimeStamp.CYCLE_MASK : TimeStamp.MAX_CYCLE;
    }
    
    /**
     * Gets the remaining time until the next event.
     * @param time a time stamp that will contain the remaining time 
     */
    public void getNextEventTime(TimeStamp time) {
        long c=(nbEvents>0) ? (eventsHeap[1].clockCycle-currentCycle)&TimeStamp.CYCLE_MASK : TimeStamp.MAX_CYCLE;;
        time.addCycles(referenceTime, 1.0/frequency, c);
    }

    /**
     * Gets the cycle corresponding to a given time.
     * @param time the time to convert
     * @return the cycle corresponding to the given time
     */
    public long timeToCycle(final TimeStamp time) {
        return (currentCycle+referenceTime.relativeTimeToRoundedCycle(time, frequency))&TimeStamp.CYCLE_MASK;
    }
    
    //********************************************************************************
    // Events.
    //********************************************************************************        
    /**
     * Gets the number of events.
     * @return the current number of events in the clock
     */
    public int getNbEvents() {
        return nbEvents;
    }
    
    /**
     * Gets the next event but do not remove it and do not advance the clock.
     * @return the next event in the clock
     */
    public EmulationEvent getNextEvent() {
        return (nbEvents>0) ? eventsHeap[1] : null;
    }
    
    /**
     * Checks if an event is the next.
     * @param e the event to check
     * @return <code>false</code> if this is not the next event, <code>true</code> otherwise
     */
    public boolean isNextEvent(EmulationEvent e) {
        return (nbEvents>0) && (eventsHeap[1]==e);
    }
    
    /**
     * Adds an event to the clock
     * @param e the event to add
     */
    public void addEvent(EmulationEvent e) {
        e.type=EmulationEvent.TYPE_CLOCK;
        e.clock=this;
        heapAddEvent(e);
        if (e==eventsHeap[1] && clockEvent.used) {
            long c=(e.clockCycle-currentCycle)&TimeStamp.CYCLE_MASK;
            clockEvent.time.addCycles(referenceTime, 1.0/frequency, c);
            system.addEvent(clockEvent);
        }
    }

    //********************************************************************************
    // Heap.
    //********************************************************************************        
    // Add an event.
    protected void heapAddEvent(EmulationEvent e) {
        if (!e.used) {      
            if (nbEvents>=maxEvents) {
                maxEvents+=256;
                EmulationEvent ea[]=new EmulationEvent[maxEvents];
                for (int i=0; i<nbEvents; i++) ea[i]=eventsHeap[i];
                eventsHeap=ea;
            }
            
            nbEvents++;
            e.used=true;
            e.index=nbEvents;
            eventsHeap[nbEvents]=e;
            heapFixUp(nbEvents);
        } else {
            heapFixUp(e.index);
            heapFixDown(e.index);                
        }
    }
    
    // Remove an event.
    protected void heapRemoveEvent(EmulationEvent e) {
        if (!e.used) return; // TODO: throw an exception.

        // TODO: check this !
        if (e.index==nbEvents) {
            // This handle the case where the event to remove is the last of the heap or if there is only one event left in heap.  
            e.index=-1;
            eventsHeap[nbEvents]=null; // Unreference.
            nbEvents--;            
        } else {
            EmulationEvent e2=eventsHeap[nbEvents];
            eventsHeap[e.index]=e2; e2.index=e.index;
            e.index=-1;
            eventsHeap[nbEvents]=null; // Unreference.
            nbEvents--;
            heapFixUp(e2.index);
            heapFixDown(e2.index);
        }
        
        e.used=false;
    }
        
    // Remove the next event.
    protected EmulationEvent heapRemoveNextEvent() {
        if (nbEvents<=0) return null;        
        EmulationEvent e=eventsHeap[1];
        e.used=false;
        e.index=-1;
        eventsHeap[1]=eventsHeap[nbEvents]; eventsHeap[1].index=1;
        eventsHeap[nbEvents]=null; // Unreference.
        nbEvents--;
        heapFixDown(1);
        return e;
    }
    
    // Bottom up heapify.
    private void heapFixUp(int k) {
        EmulationEvent te;
        int j;
        long t0, t1;
        while (k>1) {
            j=k>>1;
            t0=(eventsHeap[j].clockCycle-currentCycle)&TimeStamp.CYCLE_MASK;
            t1=(eventsHeap[k].clockCycle-currentCycle)&TimeStamp.CYCLE_MASK;
            if (t0<=t1) break;
            te=eventsHeap[k]; eventsHeap[k]=eventsHeap[j]; eventsHeap[j]=te;
            eventsHeap[k].index=k; eventsHeap[j].index=j;
            k=j;
        }
    }

    // Top down heapify.
    private void heapFixDown(int k) {
        EmulationEvent te;
        int j;
        long t0, t1;
        while ((j=k<<1)<=nbEvents) {
            // Check if we have two children.
            if (j<nbEvents) {
                // Take the smallest of the two children.
                t0=(eventsHeap[j].clockCycle-currentCycle)&TimeStamp.CYCLE_MASK;
                t1=(eventsHeap[j+1].clockCycle-currentCycle)&TimeStamp.CYCLE_MASK;
                if (t0>t1) j++;
            }
            // If the parent is already smaller than its children, stop here.
            t0=(eventsHeap[j].clockCycle-currentCycle)&TimeStamp.CYCLE_MASK;
            t1=(eventsHeap[k].clockCycle-currentCycle)&TimeStamp.CYCLE_MASK;
            if (t0>=t1) break;
            // Exchange the parent with its smallest child.
            te=eventsHeap[k]; eventsHeap[k]=eventsHeap[j]; eventsHeap[j]=te;
            eventsHeap[k].index=k; eventsHeap[j].index=j;
            k=j;
        }
    }    

}
