package org.thenesis.gendes.awt.f32c;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class MonoRenderer extends DefaultTableCellRenderer {
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean s, boolean f, int r, int c) {
	    super.getTableCellRendererComponent(table, value, s, f, r, c);
	    setFont(MaltaJFrame.MONO);
	    return this;
	}
}