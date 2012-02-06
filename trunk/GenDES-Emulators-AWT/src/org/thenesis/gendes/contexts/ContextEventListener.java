package org.thenesis.gendes.contexts;

public abstract class ContextEventListener {
    ContextEventDispatcher dispatcher;
    ContextEventListener previous, next;
    
    public abstract void processEvent(ContextEvent ce);
}
