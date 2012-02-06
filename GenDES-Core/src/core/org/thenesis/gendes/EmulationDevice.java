package org.thenesis.gendes;

public abstract class EmulationDevice {
    // The system in which this device is.
    protected EmulationSystem system;
    // Node in the tree of devices.
    protected EmulationDevice parentDevice;
    protected EmulationDevice previousSiblingDevice, nextSiblingDevice;
    protected EmulationDevice childrenDevice;
    // The device name.
    protected String deviceName;
    // List of events.
    protected EmulationEvent eventsList;
    
    /**
     * Called when a device is registered.
     */
    protected abstract void register();

    /**
     * Called when a device is unregistered.
     */
    protected abstract void unregister();
    
    /**
     * Called when the state of this device need to be saved.
     * @param state the <code>EmulationState</code> object in which the state will be saved
     */
    protected abstract void saveState(EmulationState state);
    
    /**
     * Called when the state of this device need to be restored.
     * @param state the <code>EmulationState</code> object from which the state will be restored
     */
    protected abstract void restoreState(EmulationState state);
 
    /**
     * Creates a new device with a name.
     * @param name name of the device
     */
    public EmulationDevice(String name) {
        deviceName=name;
    }
    
    /**
     * Gets the device name.
     * @return the device name
     */
    public final String getDeviceName() {
        return deviceName;
    }
    
    /**
     * Add a child device.
     * @param ed the child device
     */
    protected final void addChildDevice(EmulationDevice ed) {
        if (ed.system!=null) return; // TODO: throw exception.

        ed.previousSiblingDevice=null; ed.nextSiblingDevice=childrenDevice;
        if (childrenDevice!=null) childrenDevice.previousSiblingDevice=ed;
        childrenDevice=ed;            
        ed.parentDevice=this;

        if (system!=null) system.attachDevice(ed);
    }
    
    /**
     * Remove a device.
     */
    protected final void removeDevice() {      
        if (previousSiblingDevice!=null) previousSiblingDevice.nextSiblingDevice=nextSiblingDevice;
        else {
            if (parentDevice!=null) {
                parentDevice.childrenDevice=nextSiblingDevice;
            } else {
                if (system!=null) system.devicesTree=nextSiblingDevice;
            }
        }
        if (nextSiblingDevice!=null) nextSiblingDevice.previousSiblingDevice=previousSiblingDevice;
        parentDevice=null; previousSiblingDevice=null; nextSiblingDevice=null;

        if (system!=null) system.detachDevice(this);
    }
    
    /**
     * Returns the next device in a subtree in pre-order.
     * if <code>root</code> is equal to <code>null</code>, it always returns <code>null</code>.
     * @param root the root of the subtree in which we circulate.  
     * @param current the current device. The value <code>null</code> indicates that we must begin at the top of the subtree.
     * @return the next device or <code>null</code> if we reach the end
     */
    public static EmulationDevice nextPreOrderDevice(EmulationDevice root, EmulationDevice current) {
        if (root==null) return null;

        // If current==null, find the first object that is the top.
        if (current==null)  {
            current=root;
        } else {
            // If it has a child, process it first.
            if (current.childrenDevice!=null) current=current.childrenDevice;
            else {
                // Otherwise process siblings or go up the tree.
                while (true) {
                    // If we have reached the top, this is the end of the traversal.
                    if (current==root) { current=null; break; }
                    
                    // If it has a sibling, process it.
                    if (current.nextSiblingDevice!=null) { current=current.nextSiblingDevice; break; }
                    
                    // Go up the tree.
                    current=current.parentDevice;
                }
            }
        }
        
        return current;
    }

    /**
     * Returns the next device in a subtree in post-order.
     * if <code>root</code> is equal to <code>null</code>, it always returns <code>null</code>.
     * @param root the root of the subtree in which we circulate.  
     * @param current the current device. The value <code>null</code> indicates that we must begin at the top of the subtree.
     * @return the next device or <code>null</code> if we reach the end
     */
    public static EmulationDevice nextPostOrderDevice(EmulationDevice root, EmulationDevice current) {
        if (root==null) return null;

        // If current==null, find the first object.
        if (current==null)  {
            current=root;
            while (current.childrenDevice!=null) current=current.childrenDevice;
        } else {
            // If we have reached the top, this is the end of the traversal.
            if (current==root) return null;
            
            if (current.nextSiblingDevice!=null) {
                // If it has a right sibling, find the deepest left child.
                current=current.nextSiblingDevice;
                while (current.childrenDevice!=null) current=current.childrenDevice;
            } else {
                // Otherwise go up the tree.
                current=current.parentDevice;
            }
        }
        
        return current;
    }
        
    /**
     * Register an event.
     * @param e the event to register
     */
    protected final void registerEvent(EmulationEvent e) {
        if (e.device!=null) return; // TODO: throw an exception.
        e.device=this;
        
        // Add the event to the list of events of this device.
        e.previousEvent=null; e.nextEvent=eventsList;
        if (eventsList!=null) eventsList.previousEvent=e;
        eventsList=e;
    }

    /**
     * Unregister an event.
     * @param e the event to unregister
     */
    protected final void unregisterEvent(EmulationEvent e) {
        if (e.device!=this) return; // TODO: throw an exception.

        // Remove the event from the system.
        if (e.isUsed()) system.removeEvent(e);

        // Remove the event from the list of events of this device.
        if (e.previousEvent!=null) e.previousEvent.nextEvent=e.nextEvent;
        else eventsList=e.nextEvent;
        if (e.nextEvent!=null) e.nextEvent.previousEvent=e.previousEvent;
        e.previousEvent=null; e.nextEvent=null;
 
        e.device=null;
    }
    
    /**
     * Remove all registered events from the system.
     */
    protected final void removeAllEvents() {
        if (system==null) return;
        for (EmulationEvent e=eventsList; e!=null; e=e.nextEvent) {
            if (e.isUsed()) system.removeEvent(e);
        }
    }
 }
