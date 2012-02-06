package org.thenesis.gendes;

/**
 * The <code>TimeStamp</code> class represents an absolute or a relative time.
 *
 */
public final class TimeStamp {
    public long cycle;
    public double cycleFraction;

    public static final long CYCLE_MASK=    0x000fffffffffffffl; // 52 bits of precision.
    public static final long MAX_CYCLE=     0x0010000000000000l; // 52 bits of precision.
    
    public TimeStamp() {}
    public TimeStamp(long s, double f) { cycle=s; cycleFraction=f; }
    
    public void copy(final TimeStamp a) {
        cycle=a.cycle;
        cycleFraction=a.cycleFraction;
    }
    
    public void setMaxTime() {
        cycle=MAX_CYCLE;
        cycleFraction=0.0;
    }
    
    public void fromDouble(double time) {
        long s;
        double f;
        s=(long)time;
        f=time-(double)s;
        if (f<0.0) {
            f+=1.0;
            s--;
        }
        cycle=s&CYCLE_MASK;
        cycleFraction=f;
    }
    
    public double toDouble() {
        return (double)cycle+cycleFraction;
    }
    
    public static int compare(final TimeStamp a, final TimeStamp b) {
        if (a.cycle>b.cycle) return 1;
        if (a.cycle<b.cycle) return -1;
        if (a.cycleFraction>b.cycleFraction) return 1;
        if (a.cycleFraction<b.cycleFraction) return -1;
        return 0;
    }

    public static int compare(final TimeStamp reference, final TimeStamp a, final TimeStamp b) {
        long ac=(a.cycle-reference.cycle)&CYCLE_MASK, bc=(b.cycle-reference.cycle)&CYCLE_MASK;
        if (ac>bc) return 1;
        if (ac<bc) return -1;
        double acf=a.cycleFraction-reference.cycleFraction, bcf=b.cycleFraction-reference.cycleFraction;
        if (acf>bcf) return 1;
        if (acf<bcf) return -1;
        return 0;
    }
    
    public static long add(long a, long b) {
        return (a+b)&CYCLE_MASK;        
    }

    public void add(long s) {
        cycle=(cycle+s)&CYCLE_MASK;
    }
    
    public void add(double f) {
        long s;
        cycleFraction+=f;
        s=(long)cycleFraction;
        cycleFraction-=(double)s;
        if (cycleFraction<0.0) { cycleFraction+=1.0; s--; }
        cycle=(cycle+s)&CYCLE_MASK;
    }

    public void add(long s, double f) {
        long s2;
        cycleFraction+=f;
        s2=(long)cycleFraction;
        cycleFraction-=(double)s2;
        if (cycleFraction<0.0) { cycleFraction+=1.0; s2--; }
        cycle=(cycle+s+s2)&CYCLE_MASK;        
    }

    public void add(final TimeStamp ts) {
        long s2;
        cycleFraction+=ts.cycleFraction;
        s2=(long)cycleFraction;
        cycleFraction-=(double)s2;
        if (cycleFraction<0.0) { cycleFraction+=1.0; s2--; }
        cycle=(cycle+ts.cycle+s2)&CYCLE_MASK;        
    }

    public void add(final TimeStamp ts0, final TimeStamp ts1) {
        long s2;
        cycleFraction=ts0.cycleFraction+ts1.cycleFraction;
        s2=(long)cycleFraction;
        cycleFraction-=(double)s2;
        if (cycleFraction<0.0) { cycleFraction+=1.0; s2--; }
        cycle=(ts0.cycle+ts1.cycle+s2)&CYCLE_MASK;
    }
    
    // TODO: check accuracy.
    public void addCycles(double f, long c) {        
        double t=(double)c*f;
        long s2;
        c=(long)t;
        cycleFraction+=t-(double)c;
        s2=(long)cycleFraction;
        cycleFraction-=(double)s2;
        if (cycleFraction<0.0) { cycleFraction+=1.0; s2--; }
        cycle=(cycle+c+s2)&CYCLE_MASK;
    }
    
    // TODO: check accuracy.
    public void addCycles(final TimeStamp ts, double f, long c) {        
        double t=(double)c*f;
        long s2;
        c=(long)t;
        cycleFraction=ts.cycleFraction+t-(double)c;
        s2=(long)cycleFraction;
        cycleFraction-=(double)s2;
        if (cycleFraction<0.0) { cycleFraction+=1.0; s2--; }
        cycle=(ts.cycle+c+s2)&CYCLE_MASK;
    }
    
    public void sub(final TimeStamp ts) {
        long s2;
        cycleFraction-=ts.cycleFraction;
        s2=(long)cycleFraction;
        cycleFraction-=(double)s2;
        if (cycleFraction<0.0) { cycleFraction+=1.0; s2--; }
        cycle=(cycle-ts.cycle+s2)&CYCLE_MASK;        
    }

    public void sub(final TimeStamp ts0, final TimeStamp ts1) {
        long s2;
        cycleFraction=ts0.cycleFraction-ts1.cycleFraction;
        s2=(long)cycleFraction;
        cycleFraction-=(double)s2;
        if (cycleFraction<0.0) { cycleFraction+=1.0; s2--; }
        cycle=(ts0.cycle-ts1.cycle+s2)&CYCLE_MASK;        
    }

    /** Convert a time to cycles. This doesn't need to be accurate. */
    // TODO: check accuracy.
    public long timeToCycle(double f) {
        double t=((double)cycle+cycleFraction)*f;
        long c=(long)t;
        return (c>TimeStamp.MAX_CYCLE) ? TimeStamp.MAX_CYCLE : c;
    }
    
    /** Convert cycles to a time. This doesn't need to be accurate. */
    public void cycleToTime(double f, long c) {
        double t=(double)c*f;
        cycle=(long)t;
        cycleFraction=t-(double)cycle;
    }
    
    /** Convert a time to cycles. This doesn't need to be accurate but the resulting cycles must represent a time >=. */
    // TODO: check accuracy.
    public long relativeTimeToRoundedCycle(final TimeStamp ts, double f) {
        long c;
        double cf=ts.cycleFraction-cycleFraction;
        c=(long)cf;
        cf-=(double)c;
        if (cf<0.0) { cf+=1.0; c--; }
        c=(ts.cycle-cycle+c)&CYCLE_MASK;

        double t=((double)c+cf)*f;
        c=(long)Math.ceil(t);
        return (c>TimeStamp.MAX_CYCLE) ? TimeStamp.MAX_CYCLE : c;
    }
}
