package org.thenesis.gendes.contexts;


public class Context {
	private ContextNode children;

    public final synchronized void add(ContextNode cn) {
        if (cn.context!=null) return;
        cn.context=this; cn.previousSibling=null; cn.nextSibling=children;
        if (children!=null) children.previousSibling=cn;
        children=cn;
    }
    
    public final synchronized void remove(ContextNode cn) {
        if (cn.context!=this) return;
        if (cn.previousSibling!=null) cn.previousSibling.nextSibling=cn.nextSibling;
        else children=cn.nextSibling;
        if (cn.nextSibling!=null) cn.nextSibling.previousSibling=cn.previousSibling;
        cn.context=null; cn.previousSibling=null; cn.nextSibling=null;
    }
    
    public final synchronized void detachAll() {
        while (children!=null) {
            ContextNode cn=children;
            remove(children);
            cn.detach();
        }
    }
}
