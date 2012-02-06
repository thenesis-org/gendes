package org.thenesis.gendes.contexts;

import org.thenesis.gendes.EmulationSystem;
import org.thenesis.gendes.EmulationThread;
import org.thenesis.gendes.EmulationTimer;
import org.thenesis.gendes.SystemMillisecondTimer;


public final class SystemEmulationContext extends Context {
    public EmulationSystem emulationSystem;
    public EmulationThread emulationThread;
    public EmulationTimer emulationTimer;
    
    // Events dispatchers.
    public final ContextEventDispatcher onPauseDispatcher=new ContextEventDispatcher();
    public final ContextEventDispatcher onResumeDispatcher=new ContextEventDispatcher();
        
    // Emulation thread.
    private class ContextEmulationThread extends EmulationThread {
        ContextEmulationThread(EmulationSystem es, EmulationTimer et) { super(es, et); }
        
        public void onStart() {
        }
        
        public void onPause() {
            onPauseDispatcher.dispatch(null);
        }
        
        public void onResume() {
            onResumeDispatcher.dispatch(null);               
        }
        
        public void onStop() {
        }
    }
    
    public SystemEmulationContext() {
        initialize();
    }
    
    private void initialize() {
        emulationSystem=new EmulationSystem();
        emulationTimer=new SystemMillisecondTimer();
        emulationThread=new ContextEmulationThread(emulationSystem, emulationTimer);
    }
    
    /**
     * Shutdown.
     * 
     */
    public void shutdown() {
        detachAll();
    	synchronized (emulationSystem) {
	        onPauseDispatcher.removeAllListener();
	        onResumeDispatcher.removeAllListener();
	        
	        emulationThread.stop(false);
	        emulationSystem.removeAllDevices();
	        
	        emulationThread=null;
	        emulationSystem=null;
    	}
    }

    /**
     * Pause emulation.
     *
     */
    public void pauseEmulation() {
        emulationThread.pause();            
    }
    
    /**
     * Resume emulation.
     *
     */
    public void resumeEmulation() {
        emulationThread.resume();
    }
    
    /**
     * Break emulation.
     *
     */
    public void breakEmulation() {
        emulationThread.pause();
    	synchronized (emulationSystem) {
    		emulationSystem.breakExecution();
    	}
    }
}
