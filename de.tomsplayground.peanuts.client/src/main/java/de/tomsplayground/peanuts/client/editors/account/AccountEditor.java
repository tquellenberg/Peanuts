package de.tomsplayground.peanuts.client.editors.account;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableEditor;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;

import de.tomsplayground.peanuts.client.editors.credit.CreditChartEditorPart;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.process.Transaction;

public class AccountEditor extends MultiPageEditorPart implements IPersistableEditor {

	public static final String ID = "de.tomsplayground.peanuts.client.accountEditor";
	private TransactionListEditorPart accountEditorPart;

	private final List<IEditorPart> editors = new ArrayList<IEditorPart>();
	private IMemento mementoForRestore;
	private MetaEditorPart metaEditorPart;

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if ( !(input instanceof AccountEditorInput)) {
			throw new PartInitException("Invalid Input: Must be AccountEditorInput");
		}
		super.init(site, input);
		setPartName(input.getName());
	}

	@Override
	protected void createPages() {
		accountEditorPart = new TransactionListEditorPart();
		createEditorPage(accountEditorPart, "Transactions");
		createEditorPage(new ValueChartEditorPart(), "Chart");
		if (((AccountEditorInput) getEditorInput()).getAccount().getType() == Account.Type.INVESTMENT) {
			createEditorPage(new InventoryEditorPart(), "Inventory");
			createEditorPage(new InventoryPieEditorPart(), "InventoryPie");
			createEditorPage(new InvestmentPerformanceEditorPart(), "Performance");
		} else if (((AccountEditorInput) getEditorInput()).getAccount().getType() == Account.Type.CREDIT) {
			createEditorPage(new CreditChartEditorPart(), "Credit");
		}
		metaEditorPart = new MetaEditorPart();
		metaEditorPart.initialize(this);
		createEditorPage(metaEditorPart, "Meta Data");
	}

	private void createEditorPage(IEditorPart editor, String name) {
		try {
			if (editor instanceof IPersistableEditor) {
				IPersistableEditor pEditor = (IPersistableEditor) editor;
				if (mementoForRestore != null) {
					IMemento[] childMemento = mementoForRestore.getChildren(editor.getClass().getName());
					if (childMemento != null) {
						for (IMemento iMemento : childMemento) {
							if (iMemento.getID().equals(getEditorInput().getName())) {
								pEditor.restoreState(iMemento);
							}
						}
					}
				}
			}
			int pageIndex = addPage(editor, getEditorInput());
			setPageText(pageIndex, name);
			editors.add(editor);
		} catch (PartInitException e) {
			ErrorDialog.openError(getSite().getShell(), "Error creating nested editor", null,
				e.getStatus());
		}
	}

	public void select(Transaction trans) {
		setActivePage(0);
		accountEditorPart.select(trans);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		for (IEditorPart editor : editors) {
			editor.doSave(monitor);
		}
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}

	@Override
	public void doSaveAs() {
		// nothing to do
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	public void editorDirtyStateChanged() {
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}

	@Override
	public void restoreState(IMemento memento) {
		// Editors not yet created.
		this.mementoForRestore = memento;
	}

	@Override
	public void saveState(IMemento memento) {
		for (IEditorPart editor : editors) {
			if (editor instanceof IPersistableEditor) {
				IPersistableEditor pEditor = (IPersistableEditor) editor;
				pEditor.saveState(memento.createChild(editor.getClass().getName(), getEditorInput().getName()));
			}
		}
	}

}
