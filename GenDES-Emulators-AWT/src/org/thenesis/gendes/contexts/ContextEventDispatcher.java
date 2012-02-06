package org.thenesis.gendes.contexts;

/**
 * Synchronous event dispatcher.
 */
public final class ContextEventDispatcher {
    private ContextEventListener listeners;
    
    public synchronized void addListener(ContextEventListener cel) {
        if (cel.dispatcher!=null) return;
        cel.dispatcher=this; cel.previous=null; cel.next=listeners;
        if (listeners!=null) listeners.previous=cel;
        listeners=cel;
    }
    
    public synchronized void removeListener(ContextEventListener cel) {
        if (cel.dispatcher!=this) return;
        if (cel.previous!=null) cel.previous.next=cel.next;
        else listeners=cel.next;
        if (cel.next!=null) cel.next.previous=cel.previous;
        cel.dispatcher=null; cel.previous=null; cel.next=null;
    }
    
    public synchronized void removeAllListener() {
    	while (listeners!=null) removeListener(listeners);
    }
    
    public synchronized void dispatch(ContextEvent ce) {
        ContextEventListener cel=listeners;
        while (cel!=null) {
            cel.processEvent(ce);
            cel=cel.next;
        }
    }
}
