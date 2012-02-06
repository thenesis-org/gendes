package org.thenesis.gendes;

public final class SystemMillisecondTimer extends EmulationTimer {
	
    public long getTimer() {
    	return System.currentTimeMillis();    	
    }
    
    public double computeDifference(long start, long end) {
    	int s=((int)start)&0xffff, e=((int)end)&0xffff;
    	int delta=(s<=e) ? (e-s) : (0xffff-s+e);
    	return delta/1000.0;    	
    }

    public double getResolution() {
    	return 1.0/1000.0;
    }

}
