package org.thenesis.emulation.eclipse;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

public class MemoryEditor extends EditorPart {
	
	MemoryEditor() {
		
	}
	
	//--------------------------------------------------------------------------------
	// WorkbenchPart.
	//--------------------------------------------------------------------------------
	protected Composite parentComposite;

	public void createPartControl(Composite parent) {        
        parentComposite=parent;
	}

    public void dispose() {
        super.dispose();
    }

	public void setFocus() {
	}

	//--------------------------------------------------------------------------------
	// EditorPart.
	//--------------------------------------------------------------------------------
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
	}

	public boolean isDirty() {
		return false;
	}

	public boolean isSaveAsAllowed() {
		return false;
	}

	public void doSave(IProgressMonitor monitor) {
	}

	public void doSaveAs() {
	}

	
}
