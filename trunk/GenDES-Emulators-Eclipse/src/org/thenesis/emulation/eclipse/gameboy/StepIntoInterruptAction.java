package org.thenesis.emulation.eclipse.gameboy;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.thenesis.gendes.contexts.GameBoyEmulationContext;

public class StepIntoInterruptAction implements IViewActionDelegate {
	DisassemblerView view;

    public StepIntoInterruptAction() {
        super();
    }

    public void init(IViewPart view) {
        this.view=(DisassemblerView)view;
    }

    public void run(IAction action) {
    	GameBoyEmulationContext gbc=view.gameBoyContext;
        gbc.cpuStepIntoi();
    }

    public void selectionChanged(IAction action, ISelection selection) {
    }
}
