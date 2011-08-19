package de.tomsplayground.peanuts.client.rss;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

import de.tomsplayground.peanuts.util.PeanutsUtil;

public class RssView extends ViewPart {

	private static class RssLableProvider extends LabelProvider implements ITableLabelProvider {

		@Override
		public String getColumnText(Object element, int columnIndex) {
			SyndEntry entry = (SyndEntry) element;
			if (columnIndex == 0)
				return entry.getTitle();
			if (columnIndex == 1)
				return PeanutsUtil.formatDateTime(entry.getPublishedDate());
			return null;
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

	}

	private RssListViewer rssListViewer;

	@Override
	public void createPartControl(Composite parent) {
		rssListViewer = new RssListViewer(parent, SWT.MULTI | SWT.FULL_SELECTION);
//		Table table = rssListViewer.getTable();
//		table.setHeaderVisible(true);
//		table.setLinesVisible(true);
//		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//
//		TableColumn col = new TableColumn(table, SWT.LEFT);
//		col.setText("Name");
//		col.setResizable(true);
//		col.setWidth(200);
//		
//		col = new TableColumn(table, SWT.LEFT);
//		col.setText("Date");
//		col.setResizable(true);
//		col.setWidth(100);

		rssListViewer.setContentProvider(new ArrayContentProvider());
//		rssListViewer.setLabelProvider(new RssLableProvider());
		
		try {
			rssListViewer.setInput(feed());
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FeedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		MenuManager menuManager = new MenuManager();
//		table.setMenu(menuManager.createContextMenu(table));
		getSite().registerContextMenu(menuManager, rssListViewer);
		getSite().setSelectionProvider(rssListViewer);

	}

	@Override
	public void setFocus() {
	}

	private List<SyndEntry> feed() throws IOException, IllegalArgumentException, FeedException {
		URL feedSource = new URL("http://www.google.com/finance/company_news?q=ETR:ALV&output=rss");
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(feedSource));
        return feed.getEntries();
	}
	
}
