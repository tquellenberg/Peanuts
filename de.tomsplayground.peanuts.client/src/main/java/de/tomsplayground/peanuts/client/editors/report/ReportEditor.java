package de.tomsplayground.peanuts.client.editors.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;

import de.tomsplayground.peanuts.client.editors.account.InventoryEditorPart;
import de.tomsplayground.peanuts.client.editors.account.InventoryPieEditorPart;
import de.tomsplayground.peanuts.client.editors.account.InvestmentPerformanceEditorPart;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.Account.Type;

public class ReportEditor extends MultiPageEditorPart {

	public static final String ID = "de.tomsplayground.peanuts.client.reportEditor";

	private final List<IEditorPart> editors = new ArrayList<IEditorPart>();

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if ( !(input instanceof ReportEditorInput)) {
			throw new PartInitException("Invalid Input: Must be ReportEditorInput");
		}
		super.init(site, input);
		setPartName(input.getName());
	}

	@Override
	protected void createPages() {
		createEditorPage(new TransactionListEditorPart(), "Report");
		createEditorPage(new ChartEditorPart(), "Chart");
		if (isInvestmentAccount()) {
			createEditorPage(new InventoryEditorPart(), "Inventory");
			createEditorPage(new InventoryPieEditorPart(), "InventoryPie");
		}
		createEditorPage(new InvestmentPerformanceEditorPart(), "Performance");
		createEditorPage(new MetaEditorPart(), "Meta Data");
	}

	private boolean isInvestmentAccount() {
		Set<Account> accounts = ((ReportEditorInput)getEditorInput()).getReport().getAccounts();
		for (Account account : accounts) {
			if (account.getType() == Type.INVESTMENT || account.getType() == Type.COMMODITY) {
				return true;
			}
		}
		return false;
	}

	private void createEditorPage(IEditorPart editor, String name) {
		try {
			int pageIndex = addPage(editor, getEditorInput());
			setPageText(pageIndex, name);
			editors.add(editor);
		} catch (PartInitException e) {
			ErrorDialog.openError(getSite().getShell(), "Error creating nested editor", null,
				e.getStatus());
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		for (IEditorPart editor : editors) {
			editor.doSave(monitor);
		}
		setPartName(getEditorInput().getName());
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

}
