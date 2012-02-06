package org.thenesis.emulation.eclipse.system;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.thenesis.emulation.eclipse.gameboy.GameBoyPlugin;
import org.thenesis.gendes.contexts.SystemEmulationContext;

public class ResumeAction implements IWorkbenchWindowActionDelegate {
    public void init(IWorkbenchWindow window) {
    }

    public void dispose() {
    }
    
    public void run(IAction action) {
    	SystemEmulationContext sc=GameBoyPlugin.getDefault().getSystemContext();
        sc.resumeEmulation();
    }

    public void selectionChanged(IAction action, ISelection selection) {
    }

}
