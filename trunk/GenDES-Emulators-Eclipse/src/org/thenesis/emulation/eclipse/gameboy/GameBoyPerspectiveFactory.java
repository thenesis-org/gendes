package org.thenesis.emulation.eclipse.gameboy;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;


public class GameBoyPerspectiveFactory implements IPerspectiveFactory {

	private static final String SCREEN_VIEW = "org.thenesis.emulation.eclipse.gameboy.ScreenView";
	private static final String CPU_VIEW = "org.thenesis.emulation.eclipse.gameboy.CPUView";
	private static final String DISASSEMBLER_VIEW = "org.thenesis.emulation.eclipse.gameboy.DisassemblerView";
	private static final String MEMORY_VIEW = "org.thenesis.emulation.eclipse.gameboy.MemoryView";
	private static final String CARTRIDGE_VIEW = "org.thenesis.emulation.eclipse.gameboy.CartridgeView";	
	private static final String HARDWARE_VIEW = "org.thenesis.emulation.eclipse.gameboy.HardwareView";
	
	public void createInitialLayout(IPageLayout layout) {
		// Get the editor area.
		String editorArea=layout.getEditorArea();

		// Top left: Resource Navigator view and Bookmarks view placeholder
		IFolderLayout topLeft=layout.createFolder("topLeft", IPageLayout.LEFT, 0.25f, editorArea);
		topLeft.addView(IPageLayout.ID_RES_NAV);
		topLeft.addPlaceholder(IPageLayout.ID_BOOKMARKS);

		// Bottom left: Outline view and Property Sheet view
		IFolderLayout bottomLeft=layout.createFolder("bottomLeft", IPageLayout.BOTTOM, 0.50f, "topLeft");
		bottomLeft.addView(IPageLayout.ID_OUTLINE);
		bottomLeft.addView(IPageLayout.ID_PROP_SHEET);
		bottomLeft.addView(SCREEN_VIEW);
		bottomLeft.addView(DISASSEMBLER_VIEW);
		bottomLeft.addView(MEMORY_VIEW);
		bottomLeft.addView(CPU_VIEW);
		bottomLeft.addView(CARTRIDGE_VIEW);
		bottomLeft.addView(HARDWARE_VIEW);

		// Bottom right: Task List view
		layout.addView(IPageLayout.ID_TASK_LIST, IPageLayout.BOTTOM, 0.66f, editorArea);
	}

}
