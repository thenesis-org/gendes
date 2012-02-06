package org.thenesis.emulation.eclipse.gameboy;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

public class DisassemblerFollowPCAction  implements IViewActionDelegate {
	DisassemblerView view;

    public DisassemblerFollowPCAction() {
        super();
    }

    public void init(IViewPart view) {
        this.view=(DisassemblerView)view;
    }

    public void run(IAction action) {
    	view.disassemblerScreen.setPcFollowing(action.isChecked());
    	view.screenComposite.redrawScreen();
    }

    public void selectionChanged(IAction action, ISelection selection) {
    }

}
