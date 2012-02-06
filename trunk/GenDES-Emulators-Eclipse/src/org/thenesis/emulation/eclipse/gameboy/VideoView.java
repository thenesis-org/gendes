package org.thenesis.emulation.eclipse.gameboy;

import java.awt.Frame;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.thenesis.gendes.awt.gameboy.VideoCanvas;
import org.thenesis.gendes.contexts.GameBoyEmulationContext;

public class VideoView extends ViewPart {
	
	private Frame frame;
    private VideoCanvas canvas;

	public VideoView() {
	}

	public void createPartControl(Composite parent) {
		Composite SWT_AWT_container = new Composite(parent, SWT.EMBEDDED);
        frame = SWT_AWT.new_Frame(SWT_AWT_container);
        
        GameBoyEmulationContext gbc=GameBoyPlugin.getDefault().getGameBoyContext();
        canvas=new VideoCanvas();
        canvas.attach(gbc);
        frame.add(canvas);
	}
	
    public void dispose() {
        super.dispose();
    }

	public void setFocus() {
		canvas.requestFocus();		
	}

}
