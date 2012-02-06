package org.thenesis.emulation.eclipse;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

public class DeviceView extends ViewPart {
	//--------------------------------------------------------------------------------
	// WorkbenchPart.
	//--------------------------------------------------------------------------------
	Composite parentComposite;
	
	public void createPartControl(Composite parent) {        
        parentComposite=parent;
	}

    public void dispose() {
        super.dispose();
    }

	public void setFocus() {
	}

}
