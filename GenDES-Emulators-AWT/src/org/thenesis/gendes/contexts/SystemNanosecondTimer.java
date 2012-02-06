package org.thenesis.gendes.contexts;

import org.thenesis.gendes.EmulationTimer;

// Currently System.nanoTime() is very slow (20-30x slower than System.currentTimeMillis()). So do not use it.
// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6440250
public final class SystemNanosecondTimer extends EmulationTimer {
	
    public long getTimer() {
    	return System.nanoTime();    	
    }
    
    public double computeDifference(long start, long end) {
    	long mask=0x7fffffffffffffffl;
    	long s=start&mask, e=end&mask;
    	long delta=(s<=e) ? (e-s) : (mask-s+e);
    	return delta/1000000000.0;
    }

    public double getResolution() {
    	return 1.0/1000000000.0;
    }

}
