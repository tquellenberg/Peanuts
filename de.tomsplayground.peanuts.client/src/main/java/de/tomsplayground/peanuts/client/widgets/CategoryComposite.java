package de.tomsplayground.peanuts.client.widgets;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.dnd.PeanutsTransfer;
import de.tomsplayground.peanuts.client.dnd.CategoryTransferData;
import de.tomsplayground.peanuts.domain.base.Category;

public class CategoryComposite extends Composite {

	private Category category;
	private Button categoryButton;
	private Text categoryText;

	public CategoryComposite(Composite parent, int style) {
		super(parent, style);
		GridLayout gridLayout = new GridLayout(2, false);
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing = 0;
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		setLayout(gridLayout);

		categoryButton = new Button(this, SWT.FLAT);
		Image image = Activator.getDefault().getImageRegistry().get(Activator.IMAGE_CATEGORY);
		categoryButton.setImage(image);
		categoryText = new Text(this, SWT.SINGLE | SWT.READ_ONLY);
		categoryText.setToolTipText("Drop category from category tree");
		categoryText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		//
		categoryButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				final Shell dialog = new Shell(getShell(), SWT.DIALOG_TRIM);
				dialog.setLayout(new GridLayout(1, false));

				PatternFilter patternFilter = new PatternFilter();
				FilteredTree tree = new FilteredTree(dialog, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL |
					SWT.BORDER, patternFilter, true);
				TreeViewer viewer = tree.getViewer();
				viewer.setContentProvider(new CategoryContentProvider());
				viewer.setLabelProvider(new CategoryLabelProvider());
				viewer.setInput(Activator.getDefault().getAccountManager().getCategories());
				tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
				viewer.addDoubleClickListener(new IDoubleClickListener() {
					@Override
					public void doubleClick(DoubleClickEvent event) {
						Category cat = (Category) ((IStructuredSelection) event.getSelection()).getFirstElement();
						setCategory(cat);
						dialog.close();
					}
				});

				dialog.setSize(300, 400);
				Monitor primary = getDisplay().getPrimaryMonitor();
				Rectangle bounds = primary.getBounds();
				Rectangle rect = dialog.getBounds();
				int x = bounds.x + (bounds.width - rect.width) / 2;
				int y = bounds.y + (bounds.height - rect.height) / 2;
				dialog.setLocation(x, y);
				dialog.open();
			}
		});
		// Drop-Target
		Transfer[] types = new Transfer[] { PeanutsTransfer.INSTANCE };
		int operations = DND.DROP_DEFAULT | DND.DROP_LINK;
		DropTarget target = new DropTarget(categoryText, operations);
		target.setTransfer(types);
		target.addDropListener(new DropTargetAdapter() {
			@Override
			public void drop(DropTargetEvent event) {
				if (event.data == null) {
					event.detail = DND.DROP_NONE;
					return;
				}
				CategoryTransferData data = (CategoryTransferData) event.data;
				setCategory(data.getCategory());
			}

			@Override
			public void dragEnter(DropTargetEvent event) {
				if (event.detail == DND.DROP_DEFAULT) {
					event.detail = DND.DROP_LINK;
				}
			}

			@Override
			public void dragOperationChanged(DropTargetEvent event) {
				if (event.detail == DND.DROP_DEFAULT) {
					event.detail = DND.DROP_LINK;
				}
			}
		});
	}

	public void setCategory(Category category) {
		this.category = category;
		if (category == null) {
			categoryText.setText("");
		} else {
			categoryText.setText(category.getPath());
		}
	}

	public Category getCategory() {
		return category;
	}

	public void addModifyListener(ModifyListener listener) {
		categoryText.addModifyListener(listener);
	}

	public void removeModifyListener(ModifyListener listener) {
		categoryText.removeModifyListener(listener);
	}

}
