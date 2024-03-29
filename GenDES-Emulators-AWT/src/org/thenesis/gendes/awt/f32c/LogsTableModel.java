package org.thenesis.gendes.awt.f32c;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.thenesis.gendes.mips32.util.Log;

public class LogsTableModel extends AbstractTableModel {
	
	private final List<Log> logs = new ArrayList<>();
	private final List<Log> viewlogs = new ArrayList<>();
	private String filter;
	private boolean conj;
	
	public void addLogs (Log[] a) {
		if (logs.size() > 10000) {
			logs.subList(0, 1000).clear();
		}
		// int i = logs.size();
		logs.addAll(Arrays.asList(a));
		setFilter(filter, conj);
	}
	
	public void setFilter (String filter, boolean conj) {
		this.filter = filter;
		this.conj = conj;
		viewlogs.clear();
		if (filter != null && filter.length() > 0) {
			String[] a = filter.split(" +");
			for (Log l : logs) {
				boolean x;
				if (conj) {
					x = true;
					for (String t : a) {
						if (t.length() > 0 && !(l.msg.contains(t) || l.name.contains(t))) {
							x = false;
							break;
						}
					}
				} else {
					x = false;
					for (String t : a) {
						if (t.length() > 0 && (l.msg.contains(t) || l.name.contains(t))) {
							x = true;
							break;
						}
					}
				}
				if (x) {
					viewlogs.add(l);
				}
			}
		} else {
			viewlogs.addAll(logs);
		}
		fireTableDataChanged();
		// fireTableRowsInserted(i, this.logs.size() - 1);
	}

	@Override
	public int getRowCount () {
		return viewlogs.size();
	}
	
	@Override
	public int getColumnCount () {
		return 5;
	}
	
	@Override
	public String getColumnName (int col) {
		switch (col) {
			case 0:
				return "Cycle";
			case 1:
				return "Mode";
			case 2:
				return "Name";
			case 3:
				return "Message";
			case 4:
				return "Name";
			default:
				throw new RuntimeException();
		}
	}
	
	public Log getRow (int row) {
		return viewlogs.get(row);
	}
	
	@Override
	public Object getValueAt (int row, int col) {
		Log l = viewlogs.get(row);
		switch (col) {
			case 0:
				return Long.valueOf(l.cycle);
			case 1:
				return (l.km ? "K" : "") + (l.ie ? "I" : "") + (l.ex ? "X" : "");
			case 2:
				return l.name;
			case 3:
				return l.msg;
			case 4:
				return l.sym;
			default:
				throw new RuntimeException();
		}
	}
	
}
