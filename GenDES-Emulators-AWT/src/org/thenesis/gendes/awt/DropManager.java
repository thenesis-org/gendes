package org.thenesis.gendes.awt;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.util.List;


public class DropManager {
	@SuppressWarnings("unused")
	private DropTarget dndDropTarget;
    private static final int DND_FLAVOR_FILE_LIST=  0;
    private static final int DND_FLAVOR_STRING=     1;
    private DataFlavor dndFlavorsList[]={ DataFlavor.javaFileListFlavor, DataFlavor.stringFlavor };
    private static final int dndSupportedActions=DnDConstants.ACTION_COPY_OR_MOVE | DnDConstants.ACTION_REFERENCE;
    
    public DropManager(Component c) {
        dndDropTarget=new DropTarget(c, DnDConstants.ACTION_COPY, dropTargetAdapter, true);
    }
    
    // User routine.
    public void onDropFile(File f) {}

    // User routine.
    public void onDropString(String s) {}        

    private boolean isDragOk(DropTargetDragEvent e) {
        if (findSupportedFlavor(e)<0) return false;        
        return ((e.getSourceActions()&dndSupportedActions)!=0);
    }
    
    private int findSupportedFlavor(DropTargetDragEvent e) {
        int n=dndFlavorsList.length;
        for (int i=0; i<n; i++) {
            if (e.isDataFlavorSupported(dndFlavorsList[i])) return i;                    
        }
        return -1;
    }
    
    private int findSupportedFlavor(DropTargetDropEvent e) {
        int n=dndFlavorsList.length;
        for (int i=0; i<n; i++) {
            if (e.isDataFlavorSupported(dndFlavorsList[i])) return i;                    
        }
        return -1;
    }
    
    private DropTargetAdapter dropTargetAdapter=new DropTargetAdapter() {        
        public void dragEnter(DropTargetDragEvent e) {
            if (!isDragOk(e)) { e.rejectDrag(); return; }
            e.acceptDrag(dndSupportedActions);
        }
        
        public void dragOver(DropTargetDragEvent e) {
            if (!isDragOk(e)) { e.rejectDrag(); return; }
            e.acceptDrag(dndSupportedActions);
        }
        
        public void dropActionChanged(DropTargetDragEvent e) {
            if (!isDragOk(e)) { e.rejectDrag(); return; }
            e.acceptDrag(dndSupportedActions);
        }
        
        public void drop(DropTargetDropEvent e) {        
            int flavorType;
            DataFlavor flavor;
            Object data;
            
            flavorType=findSupportedFlavor(e);
            if (flavorType<0) { e.rejectDrop(); return; }
            if ((e.getSourceActions()&dndSupportedActions)==0) { e.rejectDrop(); return; }
            e.acceptDrop(dndSupportedActions);
            
            flavor=dndFlavorsList[flavorType];
            try {
                data=e.getTransferable().getTransferData(flavor);
                if (data==null) throw new NullPointerException();
                switch (flavorType) {
                case DND_FLAVOR_FILE_LIST:
                    if (data instanceof List) {
                        @SuppressWarnings("rawtypes")
						List l=(List)data;
                        File f=(File)l.get(0);
                        onDropFile(f);
                    } else throw new Exception();
                    break;
                case DND_FLAVOR_STRING:
                    if (data instanceof String) {
                        String s=(String)data;
                        onDropString(s);
                    } else throw new Exception();
                    break;
                default: throw new Exception();
                }
                e.dropComplete(true);
            } catch (Throwable t) {
                System.out.println("Internal error.");
                t.printStackTrace();
                e.dropComplete(false);
                return;
            }
        }
    };
    
};
