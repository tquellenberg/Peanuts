package de.tomsplayground.peanuts.client.widgets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistentColumWidth {
	
	private final static Logger log = LoggerFactory.getLogger(PersistentColumWidth.class);
	
	private final ControlListener saveSizeOnResize = new ControlListener() {
		@Override
		public void controlResized(ControlEvent e) {
			saveState();
		}
		@Override
		public void controlMoved(ControlEvent e) {
		}
	};
	
	private interface Column {
		int getWidth();
		void setWidth(int width);
		
		void addControlListener(ControlListener listener);
	}
	
	private static final class TreeColumnAdapter implements Column {
		private TreeColumn column;
		TreeColumnAdapter(TreeColumn column) {
			this.column = column;
		}
		@Override
		public int getWidth() {
			return column.getWidth();
		}
		@Override
		public void setWidth(int width) {
			column.setWidth(width);
		}
		@Override
		public void addControlListener(ControlListener listener) {
			column.addControlListener(listener);
		}
	}
	private static final class TableColumnAdapter implements Column {
		private TableColumn column;
		TableColumnAdapter(TableColumn column) {
			this.column = column;
		}
		@Override
		public int getWidth() {
			return column.getWidth();
		}
		@Override
		public void setWidth(int width) {
			column.setWidth(width);
		}
		@Override
		public void addControlListener(ControlListener listener) {
			column.addControlListener(listener);
		}
	}
	
	private final List<Column> columns;

	private final IPreferenceStore pref;

	private final String name;
	
	public PersistentColumWidth(Tree tree, IPreferenceStore pref, String name) {
		this(Arrays.stream(tree.getColumns())
			.map(c -> (Column)(new TreeColumnAdapter(c)))
			.toList(), pref, name);
	}
	
	public PersistentColumWidth(Table table, IPreferenceStore pref, String name) {
		this(Arrays.stream(table.getColumns())
			.map(c -> (Column)(new TableColumnAdapter(c)))
			.toList(), pref, name);
	}
	
	private PersistentColumWidth(List<Column> columns, IPreferenceStore pref, String name) {
		this.columns = columns;
		this.pref = pref;
		this.name = name;
		init();
	}
	
	private void init() {
		List<Integer> columWidth = new ArrayList<>();
		String prefValue = pref.getString(name+".colWidth");
		if (StringUtils.isNotBlank(prefValue)) {
			try {
				columWidth = Arrays.stream(prefValue.split(","))
					.map(Integer::parseInt)
					.toList();
			} catch (NumberFormatException e) {
				log.error(name+".colWidth="+prefValue, e);
			}
		}
		int i = 0;
		for (Column column : columns) {
			int width = 0;
			if (i < columWidth.size()) {
				width = columWidth.get(i);
			}
			if (width > 0) {
				column.setWidth(width);
			}
			column.addControlListener(saveSizeOnResize);
			i++;
		}
	}

	private void saveState() {
		pref.setValue(name+".colWidth", columns.stream()
			.map(c -> String.valueOf(c.getWidth()))
			.collect(Collectors.joining(",")));
		try {
			((IPersistentPreferenceStore) pref).save();
		} catch (IOException e) {
			log.error("PreferenceStore.save", e);
		}
	}
}
