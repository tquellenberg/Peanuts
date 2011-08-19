package de.tomsplayground.peanuts.client.rss;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.sun.syndication.feed.synd.SyndEntry;

import de.tomsplayground.peanuts.client.controllist.ControlListItem;
import de.tomsplayground.peanuts.client.controllist.ControlListViewer;

public class RssListViewer extends ControlListViewer<SyndEntry> {

	public RssListViewer(Composite parent, int style) {
		super(parent, style);
	}
	
	@Override
	protected ControlListItem<SyndEntry> doCreateItem(Composite parent, Object element) {
		return new RssListItem(parent, SWT.NONE, (SyndEntry)element);
	}

}
