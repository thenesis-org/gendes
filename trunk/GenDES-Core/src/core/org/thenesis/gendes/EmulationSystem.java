package org.thenesis.gendes;


/**
 * The <code>EmulationSystem</code> class manages the emulation of a system of devices and events.
 * It controls the device tree, schedules all events and allows you to run emulation for a given amount of time.
 * 
 * We could use a binomial queue but it is only useful when join operation is required.
 * We could use explicit links but it is only useful when the number of elements is not known in advance.
 * So we use a simple heap based priority queue.
 */
public final class EmulationSystem {
    // Devices tree.
    protected int nbDevices;
    protected EmulationDevice devicesTree;
    // Current running event.
    protected EmulationEvent currentEvent;
    // Remaining time.
    protected final TimeStamp remainingTime=new TimeStamp();
    // Remaining time until the next event (this is a temporary variable).
    protected final TimeStamp nextEventRemainingTime=new TimeStamp();
    // Indicates if the system is running.
    protected boolean running;
    // Break the execution.
    protected boolean breakExecutionFlag;

    // Current time.
    protected final TimeStamp currentTime=new TimeStamp();
    // Local current time. 
    public final TimeStamp localCurrentTime=new TimeStamp();
    // Max number of events.
    protected int maxEvents;
    // Number of events in the heap.
    protected int nbEvents;
    // Heap of events.
    protected EmulationEvent eventsHeap[];

    /**
     * Constructs an emulation system.
     */
    public EmulationSystem() {}
    
    //********************************************************************************
    // Devices.
    //********************************************************************************
    /**
     * Adds a root device.
     * @param ed the device to add
     */
    public void addDevice(EmulationDevice ed) {
        if (running || ed.system!=null || ed.parentDevice!=null) return; // TODO: throw exception.
        
        ed.parentDevice=null;
        ed.previousSiblingDevice=null; ed.nextSiblingDevice=devicesTree;
        if (devicesTree!=null) devicesTree.previousSiblingDevice=ed;
        devicesTree=ed;
        
        attachDevice(ed);
    }

    /**
     * Removes a root device.
     * @param the device to remove
     */
    public void removeDevice(EmulationDevice ed) {
        if (running || ed.system!=this || ed.parentDevice!=null) return;

        detachDevice(ed);
        
        if (ed.previousSiblingDevice!=null) ed.previousSiblingDevice.nextSiblingDevice=ed.nextSiblingDevice;
        else devicesTree=ed.nextSiblingDevice;
        if (ed.nextSiblingDevice!=null) ed.nextSiblingDevice.previousSiblingDevice=ed.previousSiblingDevice;
        ed.parentDevice=null; ed.previousSiblingDevice=null; ed.nextSiblingDevice=null;
    }
    
    /**
     * Removes all root devices.
     */
    public void removeAllDevices() {
        if (running) return;
        EmulationDevice ed=devicesTree, ned;
        while (ed!=null) { ned=ed.nextSiblingDevice; removeDevice(ed); ed=ned; }
    }
    
    // Attach a device.
    protected void attachDevice(EmulationDevice ed) {
        if (running || ed.system!=null) return;

        // Pre order traversal.
        for (EmulationDevice ned=null; (ned=EmulationDevice.nextPreOrderDevice(ed, ned))!=null; ) {
            ned.system=this;
            nbDevices++;
            ned.register();
        }
    }
    
    // Detach a device.
    protected void detachDevice(EmulationDevice ed) {
        if (running || ed.system!=this) return;
        
        // Post order traversal.
        for (EmulationDevice ned=null; (ned=EmulationDevice.nextPostOrderDevice(ed, ned))!=null; ) {
            ned.unregister();
            ned.removeAllEvents();
            nbDevices--;
            ned.system=null;
        }
    }
    
    //********************************************************************************
    // Execution.
    //********************************************************************************
    /**
     * Run the emulation for a given time.
     * @param time the duration for which the emulation must run  
     */
    public void run(TimeStamp time) {
        EmulationClock c;
        EmulationEvent e, ce;

        if (running) {
            System.out.println("Internal error. Recursive call of EmulationSystem.run() method.");
            return;
        }

        running=true;        
        breakExecutionFlag=false;
        remainingTime.cycle=time.cycle; remainingTime.cycleFraction=time.cycleFraction;
        
        // Main emulation loop.
        while (!breakExecutionFlag) {
            // If there is no event waiting, we consume all remaining time and exit.
            if (nbEvents<=0) {
                currentTime.add(remainingTime);
                remainingTime.cycle=0; remainingTime.cycleFraction=0.0;
                break;
            }

            // If the next event is farther than the remaining time, we consume all remaining time and exit.
            e=eventsHeap[1];
            nextEventRemainingTime.sub(e.time, currentTime);
            if (TimeStamp.compare(remainingTime, nextEventRemainingTime)<=0) {
                currentTime.add(remainingTime);
                remainingTime.cycle=0; remainingTime.cycleFraction=0.0;
                break;
            }
            // Consume the time until the next event.
            currentTime.copy(e.time);
            remainingTime.sub(nextEventRemainingTime);
            
            if (e.type==EmulationEvent.TYPE_SYSTEM_CLOCK) {
                // This is a clock event.
                c=e.clock;
                c.referenceTime.copy(e.time);
                ce=c.heapRemoveNextEvent();
                if (ce!=null) {
                    // Schedule the clock for its next event.
                    c.currentCycle=ce.clockCycle;
                    c.getNextEventTime(e.time);
                    heapAddEvent(e);
                    e=ce;
                } else {
                    // There is no event in this clock but we need to keep it alive.
                    e.time.addCycles(1.0/c.frequency, TimeStamp.MAX_CYCLE);
                    heapAddEvent(e);
                    continue;
                }
            } else heapRemoveNextEvent();
            
            // Process the event.
            currentEvent=e; e.processEvent(); currentEvent=null;
        }
        
        time.sub(remainingTime);
        running=false;
    }
    
    /**
     * Breaks emulation.
     * This allows to exit the <code>run()</code> method as soon as possible.
     */
    public void breakExecution() {
        if (running) {
            breakExecutionFlag=true;
            if (currentEvent!=null) { currentEvent.breakEvent(); currentEvent=null; }
        }
    }

    //********************************************************************************
    // Time.
    //********************************************************************************
    /**
     * Returns the current time.
     * @return the current time
     */
    public TimeStamp getCurrentTime() {
        return currentTime;
    }
    
    /**
     * Converts an absolute time to a relative time.
     * @param absoluteTime the absolute time to convert
     * @param relativeTime the converted relative time
     */
    public void toRelativeTime(final TimeStamp absoluteTime, TimeStamp relativeTime) {
        relativeTime.sub(absoluteTime, currentTime);
    }
    
    /**
     * Converts an relative time to a absolute time.
     * @param relativeTime the relative time to convert
     * @param absoluteTime the converted absolute time 
     */
    public void toAbsoluteTime(final TimeStamp relativeTime, TimeStamp absoluteTime) {
        absoluteTime.add(relativeTime, currentTime);
    }
        
    /**
     * Gets the remaining time until the next event.
     * @param remainingTime the remaining time
     */
    public void getRemainingTime(TimeStamp remainingTime) {
        if (nbEvents>0) {
            remainingTime.sub(eventsHeap[1].time, currentTime);
        } else {
            remainingTime.cycle=TimeStamp.MAX_CYCLE; remainingTime.cycleFraction=0.0;
        }
    }
    
    /**
     * Gets the remaining cycles until the next event.
     * @param frequency the frequency of the clock
     * @return the number of cycles
     */
    public long getRemainingCycles(double frequency) {
        if (nbEvents>0) {
            EmulationEvent e=eventsHeap[1];
            nextEventRemainingTime.sub(e.time, currentTime);
            if (TimeStamp.compare(remainingTime, nextEventRemainingTime)>0) return nextEventRemainingTime.timeToCycle(frequency);
        }
        return remainingTime.timeToCycle(frequency);
    }
        
    //********************************************************************************
    // Events.
    //********************************************************************************
    /**
     * Gets the number of events.
     * This do not take into account the events in the <code>EmulationClock</code>s.
     * @return the number of events in the system.
     */
    public int getNbEvents() {
        return nbEvents;
    }
    
    /**
     * Gets the next event but do not remove it and do not advance the time.
     * @return the next event
     */
    public EmulationEvent getNextEvent() {
        return (nbEvents>0) ? eventsHeap[1] : null;
    }
    
    /**
     * Checks if an event is the next.
     * @param e the event to check
     * @return <code>true</code> if this is the next event, <code>false</code> otherwise
     **/
    public boolean isNextEvent(EmulationEvent e) {
        return nbEvents>0 && eventsHeap[1]==e;
    }
    
    /**
     * Adds a new event in the queue.
     * @param e the event to add
     */
    public void addEvent(EmulationEvent e) {
        if (e==currentEvent) {
            currentEvent.breakEvent();
            currentEvent=null;
        }
        
        e.system=this;
        if (e.type!=EmulationEvent.TYPE_SYSTEM_CLOCK) e.type=EmulationEvent.TYPE_SYSTEM;
        heapAddEvent(e);

        // If this event is the next to be processed, break the current event if any.
        if ((e==eventsHeap[1]) && (currentEvent!=null)) {
            currentEvent.breakEvent();
            currentEvent=null;
        }
    }
    
    /**
     * Removes an event from the queue.
     * @param e the event to remove
     */
    public void removeEvent(EmulationEvent e) {
        if (!e.used) return;
        switch (e.type) {
        case EmulationEvent.TYPE_SYSTEM:
        case EmulationEvent.TYPE_SYSTEM_CLOCK:
            if (e==currentEvent) {
                currentEvent.breakEvent();
                currentEvent=null;
            }
            heapRemoveEvent(e);
            break;
        case EmulationEvent.TYPE_CLOCK:
            EmulationClock c=e.clock;
            if (c.isNextEvent(e)) {
                c.heapRemoveNextEvent();
                e=c.clockEvent;
                if (e==currentEvent) currentEvent=null;
                c.getNextEventTime(e.time);
                heapAddEvent(e);                
            } else {
                c.heapRemoveEvent(e);
            }
            break;
        }
    }
    
    //********************************************************************************
    // Heap.
    //********************************************************************************        
    // Add an event.
    private void heapAddEvent(EmulationEvent e) {
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
    private void heapRemoveEvent(EmulationEvent e) {
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
    private EmulationEvent heapRemoveNextEvent() {
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
        while (k>1) {
            j=k>>1;
            if (TimeStamp.compare(currentTime, eventsHeap[j].time, eventsHeap[k].time)<=0) break;
            te=eventsHeap[k]; eventsHeap[k]=eventsHeap[j]; eventsHeap[j]=te;
            eventsHeap[k].index=k; eventsHeap[j].index=j;
            k=j;
        }
    }

    // Top down heapify.
    private void heapFixDown(int k) {
        EmulationEvent te;
        int j;
        while ((j=k<<1)<=nbEvents) {
            // Check if we have two children.
            if (j<nbEvents) {
                // Take the smallest of the two children.
                if (TimeStamp.compare(currentTime, eventsHeap[j].time, eventsHeap[j+1].time)>0) j++;
            }
            // If the parent is already smaller than its children, stop here.
            if (TimeStamp.compare(currentTime, eventsHeap[j].time, eventsHeap[k].time)>=0) break;
            // Exchange the parent with its smallest child.
            te=eventsHeap[k]; eventsHeap[k]=eventsHeap[j]; eventsHeap[j]=te;
            eventsHeap[k].index=k; eventsHeap[j].index=j;
            k=j;
        }
    }
}
