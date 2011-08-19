package de.tomsplayground.peanuts.client.rss;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import com.sun.syndication.feed.synd.SyndEntry;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.controllist.ControlListItem;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class RssListItem extends ControlListItem<SyndEntry> {

	public RssListItem(Composite parent, int style, SyndEntry rssElement) {
		super(parent, style, rssElement);

		GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).equalWidth(false).applyTo(this);

//		Label iconLabel = new Label(this, SWT.NULL);
////		if (category.getIcon() != null) {
////			iconLabel.setImage(resources.getIconImage(category.getSource(), category.getIcon(), 48, true));
////		}
//		iconLabel.setBackground(null);
//		GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.BEGINNING).span(1, 2).applyTo(iconLabel);

		Composite head = new Composite(this, SWT.NONE);
		head.setBackground(null);
		GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(false).applyTo(head);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(head);
		
		Label nameLabel = new Label(head, SWT.NULL);
		nameLabel.setFont(Activator.getDefault().getHeaderFont());
		nameLabel.setText(rssElement.getTitle());
		nameLabel.setBackground(null);
		registerChild(nameLabel);

		Label dateLabel = new Label(head, SWT.NULL);
		dateLabel.setFont(Activator.getDefault().getSmallFont());
		dateLabel.setText(PeanutsUtil.formatDateTime(rssElement.getPublishedDate()));
		dateLabel.setBackground(null);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.BEGINNING).applyTo(dateLabel);
		registerChild(dateLabel);

		GridDataFactory.fillDefaults().grab(true, false).applyTo(nameLabel);

		if (rssElement.getDescription().getType().equals("text/html")) {
			String value = rssElement.getDescription().getValue();
			HtmlCleaner cleaner = new HtmlCleaner();
			TagNode cleanHtml = cleaner.clean(value);
			String text = cleanHtml.getText().toString();
			text = StringUtils.join(StringUtils.stripAll(StringUtils.split(text, "\n\r")), "\n");
			Label label = new Label(this, SWT.WRAP);
			label.setText(text);
			registerChild(label);

		} else {
			System.out.println(rssElement.getDescription().getType());
		}
	}
	
	@Override
	protected void refresh() {
	}

}
