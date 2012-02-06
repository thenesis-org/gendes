package org.thenesis.emulation.eclipse.gameboy;


import java.awt.Frame;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.thenesis.gendes.awt.gameboy.debug.CPUPanel;

public class CPUView extends ViewPart {
	
	private Frame frame;
	private CPUPanel panel;
	
	public CPUView() {
       
	}

	public void createPartControl(Composite parent) {
		Composite SWT_AWT_container = new Composite(parent, SWT.EMBEDDED);
        frame = SWT_AWT.new_Frame(SWT_AWT_container);
        
        panel = new CPUPanel();
		panel.attach(GameBoyPlugin.getDefault().getGameBoyContext());
        frame.add(panel);
	}

    public void dispose() {
        super.dispose();
    }

	public void setFocus() {
		panel.requestFocus();
	}

}
