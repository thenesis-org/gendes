package org.thenesis.emulation.eclipse.gameboy;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

public class MemoryAutoRefreshAction implements IViewActionDelegate {
	MemoryView view;

    public MemoryAutoRefreshAction() {
        super();
    }

    public void init(IViewPart view) {
        this.view=(MemoryView)view;
    }

    public void run(IAction action) {
    	view.screenComposite.redrawScreen();
    }

    public void selectionChanged(IAction action, ISelection selection) {
    }
}
