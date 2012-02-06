package org.thenesis.gendes;

public abstract class EmulationTimer {

    /**
     * Return the current timer value.
     * Timer specification:
     * -Supported time interval: minimum of 2 seconds.
     * -Resolution: minimum of 1/100 second.
     */
    public abstract long getTimer();
    
    /**
     * Return the absolute diffence between 2 timer values in seconds.
     */
    public abstract double computeDifference(long start, long end);

    /**
     * Return the timer resolution in seconds.
     * @return
     */
    public abstract double getResolution();
}
