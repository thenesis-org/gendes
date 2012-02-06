package org.thenesis.gendes.debug;

import org.thenesis.gendes.debug.Breakpoint;

public final class BreakpointList {
    // Maximum and current number of breakpoints.
    private int maxBreakpoints, nbBreakpoints;
    // Breakpoints.
    private Breakpoint breakpoints[];
    // Breakpoints address.
    private int breakpointsAddress[];
    
    /** Set the maximum number of berakpoints. */
    public void setMaxBreakpoints(int mb) {
        // Remove all breakpoints.
        for (int i=0; i<nbBreakpoints; i++) {
            breakpoints[i].list=null;
            breakpoints[i].index=-1;
            breakpoints[i]=null;
            breakpointsAddress[i]=0;
        }
        
        if (mb<=0) {
            maxBreakpoints=0;
            breakpoints=null;
            breakpointsAddress=null;
        } else {
            maxBreakpoints=mb;
            breakpoints=new Breakpoint[mb];
            breakpointsAddress=new int[mb];
        }
        nbBreakpoints=0;
    }

    public void addBreakpoint(Breakpoint bp, int address) {
        if ((bp.list!=null) || (nbBreakpoints>=maxBreakpoints)) return;
        int i, j;
        for (i=nbBreakpoints; i>0; i--) {
            j=i-1;
            if (address>=breakpointsAddress[j]) break;
            breakpoints[i]=breakpoints[j]; breakpoints[j].index=i;
            breakpointsAddress[i]=breakpointsAddress[j];
        }
        breakpoints[i]=bp;
        bp.list=this; bp.index=i; bp.address=address;
        breakpointsAddress[i]=address;
        nbBreakpoints++;
    }

    public void removeBreakpoint(Breakpoint bp) {
        if (bp.list!=this) return;
        for (int i=bp.index; i<nbBreakpoints-1; i++) {
            int j=i+1;
            breakpoints[i]=breakpoints[j]; breakpoints[j].index=i;
            breakpointsAddress[i]=breakpointsAddress[j];
        }
        nbBreakpoints--;
        breakpoints[nbBreakpoints]=null;
        breakpointsAddress[nbBreakpoints]=0;
        bp.list=null;
        bp.index=-1;
    }
    
    public Breakpoint findBreakpoint(int address) {
        // Binary search.
        int l=0, r=nbBreakpoints-1, m;
        int a;
        while (l<=r) {
            m=(l+r)>>1;
            a=breakpointsAddress[m];
            if (address==a) return breakpoints[m];
            if (address<a) r=m-1; else l=m+1;
        }
        return null;
    }
}
