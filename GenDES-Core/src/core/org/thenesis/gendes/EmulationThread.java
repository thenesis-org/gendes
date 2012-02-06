package org.thenesis.gendes;

/**
 * The <code>EmulationThread</code> class allows to run an <code>EmulationSystem</code> in a separate thread and to control emulation.
 * You can:
 * <ul>
 * <li>Start, pause or stop the thread.</li>
 * <li>Enable or disable throttling. If you disable throttling, the emulation will run as fast as possible.</li>
 * <li>Set the emulation speed. So you can make the emulation run slower or faster than real time. This only works if throttling is enabled.</li>
 * <li>Retrieve performance informations about:
 *  <ul>
 *  <li>The average emulation speed. This is the real speed of emulation, averaged.</li>
 *  <li>The average virtual time slice. This is the virtual time used to run the emulation system at each iteration. This is an averaged value.</li>
 *  <li>The average sleep time. This is the real time during which the sleep is sleeping after each iteration. This is an averaged value.</li>
 *  <li>The average CPU load. This is an approximation of the CPU load used for emulation. This is an averaged value.</li>
 *  </ul>
 * </li>
 * </ul>
 * Averaged values are computed over a certain period of time (currently 1 second).
 */
public class EmulationThread {
    private EmulationSystem system;
    private EmulationTimer timer;
    
    // TODO: remove volatile options if possible.
    
    // Control of the emulation thread.
    private volatile boolean stopRequested=false, started=false, pauseRequested=false, paused=false;
    // Control of throttling.
    private volatile boolean throttlingEnabled=true;
    // Control of the emulation time slice.
    private volatile double minVirtualTimeSlice=0.002, maxVirtualTimeSlice=0.1, virtualTimeSlice=0.01;
    // Control of emulation speed.
    private volatile double speed=1.0;
    // Performance.
    private volatile double averageSpeed=0.0;
    // Average time slice.
    private volatile double averageTimeSlice=0.0;
    // Average sleep time.
    private volatile double averageSleepTime=0.0;
    // CPU load estimation.
    private volatile double averageLoad=0.0;
    // Duration.
    private final TimeStamp timeSliceDuration=new TimeStamp();
    
    
    //--------------------------------------------------------------------------------
    // Public routines.
    //--------------------------------------------------------------------------------
    /**
     * Constructor.
     * @param es an <code>EmulationSystem</code> object
     * @param et an <code>EmulationTimer</code> object
     */
    public EmulationThread(EmulationSystem es, EmulationTimer et) {
    	system=es;
    	timer=et;
    }
    
    /**
     * Starts the thread.
     */
    public final synchronized void start() {
        if (!started) {
            started=true;
            startThread();
        }
    }
    
    /**
     * Stops the thread.
     * @param waitFlag <code>false</code> if you do not want to wait for the thread to stop, <code>true</code> otherwise
     */
    public final synchronized void stop(boolean waitFlag) {
        if (started) {
            stopRequested=true;
            if (paused) { paused=false; notify(); }
            if (waitFlag) while (started) ; // TODO: use semaphore ?
            stopThread();
        }
    }
        
    /**
     * Pauses the thread.
     */
    public final synchronized void pause() {
        pauseRequested=true;
    }
    
    /**
     * Resumes the thread.
     */
    public final synchronized void resume() {
        pauseRequested=false;
        if (paused) { paused=false; notify(); }
    }
    
    /**
     * Returns whether the thread is started.
     * @return <code>false</code> if the thread is started, <code>true</code> otherwise
     */
    public final synchronized boolean isStarted() {
        return started;
    }
        
    /**
     * Returns whether the thread is paused. 
     * @return <code>false</code> if the thread is paused, <code>true</code> otherwise
     */
    public final synchronized boolean isPaused() {
        return paused;
    }    
    
    /**
     * Returns whether the thread is paused or pause is requested. 
     * @return <code>false</code> if the thread is paused or pause has been requested, <code>true</code> otherwise
     */
    public final synchronized boolean isPauseRequested() {
        return paused || pauseRequested;
    }    
    
    /**
     * Enables or disable throttling.
     * @param flag <code>false</code> to disable throttling, <code>true</code> otherwise
     */
    public final synchronized void enableThrottling(boolean flag) {
        throttlingEnabled=flag;
    }
    
    /**
     * Returns whether throttling is enabled. 
     * @return <code>false</code> if throttling is enabled, <code>true</code> otherwise
     */
    public final synchronized boolean isThrottlingEnabled() {
        return throttlingEnabled;
    }
    
    /**
     * Sets the emulation speed.
     * @param es the emulation speed
     */
    public final synchronized void setSpeed(double es) {
        speed=es;
    }
    
    /**
     * Gets the emulation speed.
     * @return the emulation speed
     */
    public final synchronized double getSpeed() {
        return speed;
    }
    
    /**
     * Gets the average emulation speed.
     * This is effective emulation speed averaged over a short time period (typically 1 second).
     * If the CPU is powerful enough, its value is generally very near to the required speed.
     * In this case it can be slightly lower or slightly higher.
     * @return average emulation speed: this is a number greater or equal to 0 
     */
    public final synchronized double getAverageSpeed() {
        return averageSpeed;
    }
    
    /**
     * Gets the average time slice.
     * @return the average time slice in seconds
     */
    public final synchronized double getAverageTimeSlice() {
        return averageTimeSlice;
    }
    
    /**
     * Gets the average sleep time.
     * @return the average sleep time in seconds
     */
    public final synchronized double getAverageSleepTime() {
        return averageSleepTime;
    }
    
    /**
     * Gets the CPU load.
     * @return the average CPU load: this is number between 0.0 and 1.0
     */
    public final synchronized double getAverageLoad() {
        return averageLoad;
    }    
    
    //--------------------------------------------------------------------------------
    // Main emulation routine.
    //--------------------------------------------------------------------------------
    /**
     * Run. 
     */
    protected final void emulate() {
    	int averageIterations;
        long timerStart, timerMiddle, timerEnd, averageLastTimerUpdate;
        double averageRealElapsedTime, averageVirtualElapsedTime, averageTotalTimeSlice, averageTotalSleepTime;
        double realTime, virtualTime, realUsedTime;
 
        onStart();
        
        timerStart=timer.getTimer();
        realTime=0.0; virtualTime=0.0;
        averageIterations=0; averageLastTimerUpdate=timerStart;
        averageRealElapsedTime=0.0; averageVirtualElapsedTime=0.0; averageTotalTimeSlice=0.0; averageTotalSleepTime=0.0;
        while (!stopRequested) {
            if (pauseRequested) { 
                synchronized(this) {
                	// We check again pauseRequested otherwise a resume() that occurs here in another thread would not work !
                	if (pauseRequested) paused=true;
                }
                
            	averageSpeed=0.0; averageTimeSlice=0.0; averageSleepTime=0.0; averageLoad=0.0;
            	
                onPause(); // Do not put this inside the synchronized(this) statement.
                synchronized(this) {
                    // Remember that pause can be canceled in the onPause() callback or in another thread just after "paused=true;".
                    try { while (paused) wait(); } catch (InterruptedException e) {}
                }
                onResume(); // Do not put this inside the synchronized(this) statement.

                timerStart=timer.getTimer();
                realTime=0.0; virtualTime=0.0;
                averageIterations=0; averageLastTimerUpdate=timerStart;
                averageRealElapsedTime=0.0; averageVirtualElapsedTime=0.0; averageTotalTimeSlice=0.0; averageTotalSleepTime=0.0;
                continue; // Stop can be requested while paused. So we need to check this first.
            }
            
            onRunning();

            // Emulate.
            {
            	double virtualRequestedTime, virtualElapsedTime, realElapsedTime;
            	
	            // Avoid a large gap between realTime and virtualTime.
	            if (realTime<virtualTime) {
		            if (virtualTime-realTime>maxVirtualTimeSlice) virtualTime=realTime+maxVirtualTimeSlice;
	            } else {
		            if (realTime-virtualTime>maxVirtualTimeSlice) realTime=virtualTime+maxVirtualTimeSlice;
	            }
	            
	            // Calculate next time slice.
	            virtualRequestedTime=virtualTimeSlice+(realTime-virtualTime);
	            if (virtualRequestedTime<minVirtualTimeSlice) virtualRequestedTime=minVirtualTimeSlice;
	            if (virtualRequestedTime>maxVirtualTimeSlice) virtualRequestedTime=maxVirtualTimeSlice;
	            averageTotalTimeSlice+=virtualRequestedTime;
	            
	            // Shift realTime and emulationTime.
	            if (realTime>virtualTime)
	            	{ realTime-=virtualTime; virtualTime=0.0; }
	            else
	            	{ virtualTime-=realTime; realTime=0.0; }

	            // Run the emulation for the requested time.
	            timeSliceDuration.fromDouble(virtualRequestedTime);
	            synchronized (system) { system.run(timeSliceDuration); }
	            virtualElapsedTime=timeSliceDuration.toDouble();
	            timerMiddle=timer.getTimer();
	            realElapsedTime=timer.computeDifference(timerStart, timerMiddle);
            
	            // Add virtual time elapsed.
	            virtualTime+=virtualElapsedTime; averageVirtualElapsedTime+=virtualElapsedTime;
	            // Add real time elapsed.
	            realUsedTime=realTime+realElapsedTime*speed; averageRealElapsedTime+=realElapsedTime;            
            }
            
            // Throttling.
            if (throttlingEnabled) {
	            if (realUsedTime<virtualTime) {
	                // We are too fast.
	                double requestedSleepTime, effectiveSleepTime;
	                requestedSleepTime=virtualTime-realTime;
	                sleep(requestedSleepTime);
	                timerEnd=timer.getTimer();                
	                effectiveSleepTime=timer.computeDifference(timerMiddle, timerEnd);
	                averageTotalSleepTime+=effectiveSleepTime;
	                
		            double realTotalElapsedTime=timer.computeDifference(timerStart, timerEnd);
		            realTime+=realTotalElapsedTime*speed;
		            timerStart=timerEnd;
	            } else {
	                // We are too slow.
	                realTime=realUsedTime;
	                timerStart=timerMiddle;
	            }	            
            } else {
                realTime=virtualTime;
                timerStart=timerMiddle;            	
            }
            
            // Calculate performance.
            {
	            double averageRealTotalTime=timer.computeDifference(averageLastTimerUpdate, timerStart);
	            averageIterations++;
	            if (averageRealTotalTime>1.0) {
	                averageSpeed=averageVirtualElapsedTime/averageRealTotalTime;
	                averageLoad=averageRealElapsedTime/averageRealTotalTime;
	                averageTimeSlice=averageTotalTimeSlice/averageIterations;
	                averageSleepTime=averageTotalSleepTime/averageIterations;
	                
	                averageIterations=0; averageLastTimerUpdate=timerStart;
	                averageRealElapsedTime=0.0; averageVirtualElapsedTime=0.0; averageTotalTimeSlice=0.0; averageTotalSleepTime=0.0;
	            }
            }
        }
    
        started=false;    
        onStop();
    }

    //--------------------------------------------------------------------------------
    // Routines for thread abstraction.
    // They can be redefined if you want to avoid using a thread.
    //--------------------------------------------------------------------------------
    /**
     * The Java thread.
     */
    protected final Thread thread=new Thread() {
    	public void run() { emulate(); }
    };
    
    /**
     * Start the thread.
     */
    protected void startThread() {
    	thread.start();
    }
    
    /**
     * Stop the thread.
     */
    protected void stopThread() {
    }
    
    /**
     * Sleep.
     * @param t the time to sleep in seconds
     */
    protected void sleep(double t) {
    	try {
//    		Thread.sleep(Math.ceil(t*1000.0)); // Does not work well: better minimize rather than maximize sleep time.
    		Thread.sleep((long)(t*1000.0));
    	} catch (InterruptedException e) {}
    }
    
    //--------------------------------------------------------------------------------
    // Callbacks.
    // They are used to catch global emulation events.
    // They are called synchronously with the emulation thread.
    //--------------------------------------------------------------------------------
    /**
     * Start callback.
     * This is called just before entering the emulation loop.
     */
    protected void onStart() {}
    
    /**
     * Stop callback.
     * This is called just after exiting the emulation loop, before exiting the thread. 
     */
    protected void onStop() {}
    
    /**
     * Pause callback.
     * This is called just before entering pause.
     */
    protected void onPause() {}

    /**
     * Resume callback.
     * This is called after pause has been cancelled.
     */
    protected void onResume() {}

    /**
     * Running callback.
     * This is called once in each loop. Mainly useful if you do not use a thread.
     */
    protected void onRunning() {}
}
