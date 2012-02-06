package org.thenesis.gendes.contexts;


public abstract class ContextNode {
	Context context;
	ContextNode nextSibling, previousSibling;
	
	public Context getContext() { return context; }
	public boolean isAttached() { return context!=null; }
    public abstract void detach();
}
