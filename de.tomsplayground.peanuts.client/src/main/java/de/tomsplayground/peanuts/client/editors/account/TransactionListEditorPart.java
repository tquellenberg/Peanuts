package de.tomsplayground.peanuts.client.editors.account;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.part.EditorPart;

import com.google.common.collect.Lists;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.widgets.PersistentColumWidth;
import de.tomsplayground.peanuts.client.widgets.TransactionListContentProvider;
import de.tomsplayground.peanuts.client.widgets.TransactionListContentProvider.TimeTreeNode;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.process.BankTransaction;
import de.tomsplayground.peanuts.domain.process.ITransaction;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;
import de.tomsplayground.peanuts.domain.process.LabeledTransaction;
import de.tomsplayground.peanuts.domain.process.Transaction;
import de.tomsplayground.peanuts.domain.process.TransferTransaction;
import de.tomsplayground.peanuts.util.Day;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class TransactionListEditorPart extends EditorPart {

	private TreeViewer transactionTree;

	private ITransactionDetail transactionDetails1;
	private ITransactionDetail transactionDetails2;

	private ITransactionDetail activeTransactionDetail;

	private Composite top;

	private Set<ITransaction> lastDayOfMonthTransactions = new HashSet<>();

	protected PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if (evt.getSource() instanceof Transaction transaction) {
				if (evt.getOldValue() == null || evt.getNewValue() == null ||
					!evt.getOldValue().equals(evt.getNewValue())) {
					if (evt.getPropertyName().equals("amount") || evt.getPropertyName().equals("date")) {
						if (evt.getPropertyName().equals("date")) {
							Day oldDay = (Day) evt.getOldValue();
							if (transaction.getDay().year != oldDay.year) {
								update(oldDay);
							}
						}
						update(transaction);
						if (evt.getPropertyName().equals("date")) {
							transactionTree.getTree().showSelection();
						}
						updateSaldoLabel();
					} else {
						Transaction t = (Transaction) evt.getSource();
						transactionTree.update(t, null);
					}
				}
			} else  if (evt.getSource() instanceof Account) {
				if (evt.getOldValue() instanceof Transaction transaction) {
					update(transaction);
				}
				if (evt.getNewValue() instanceof Transaction transaction) {
					update(transaction);
				}
				updateSaldoLabel();
			}
		}

		private void update(Transaction transaction) {
			if (transaction == null) {
				return;
			}
			Day year = transaction.getDay();
			update(year);
		}

		private void update(Day year) {
			TreeItem[] items = transactionTree.getTree().getItems();
			for (TreeItem treeItem : items) {
				TimeTreeNode time = (TimeTreeNode)treeItem.getData();
				if (time.getDate().year == year.year) {
					transactionTree.refresh(time, true);
					return;
				}
			}
			// New year
			transactionTree.refresh();
		}
	};

	private Label saldo;

	private class AccountLabelProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider, ITableFontProvider {

		private Color red;
		private Account account;
		private Font boldFont;

		public AccountLabelProvider(Color red, Account account) {
			this.red = red;
			this.account = account;
			this.boldFont = Activator.getDefault().getBoldFont();			
		}

		@Override
		public void dispose() {
			super.dispose();
			red = null;
			account = null;
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof TransactionListContentProvider.TimeTreeNode node) {
				if (columnIndex == 0) {
					return String.valueOf(node.getDate().year);
				}
				return "";
			}
			Transaction trans = (Transaction) element;
			switch (columnIndex) {
				case 0:
					return PeanutsUtil.formatDate(trans.getDay());
				case 1:
					String t;
					if (trans instanceof InvestmentTransaction investTrans) {
						t = investTrans.getSecurity().getName();
					} else {
						t = trans.getMemo();
						if (trans instanceof LabeledTransaction bankTrans) {
							if (StringUtils.isNotEmpty(bankTrans.getLabel())) {
								t = bankTrans.getLabel() + ": " + t;
							}
						}
					}
					return t;
				case 2:
					return PeanutsUtil.formatCurrency(trans.getAmount(), account.getCurrency());
				case 3:
					return PeanutsUtil.formatCurrency(account.getBalance(trans),
						account.getCurrency());
				case 4:
					if (trans instanceof TransferTransaction transfer) {
						return Activator.TRANSFER_PREFIX + transfer.getTarget().getName();
					}
					return trans.getCategory() != null ? trans.getCategory().getPath() : "";
				default:
					return "";
			}
		}

		@Override
		public Color getBackground(Object element, int columnIndex) {
			return null;
		}

		@Override
		public Color getForeground(Object element, int columnIndex) {
			if (element instanceof TransactionListContentProvider.TimeTreeNode) {
				return null;
			}
			Transaction trans = (Transaction) element;
			if (trans.getDay().after(Day.today())) {
				return Activator.getDefault().getColorProvider().get(Activator.INACTIVE_ROW);
			}
			if (columnIndex == 2 && trans.getAmount().signum() == -1) {
				return red;
			}
			if (columnIndex == 3 && account.getBalance(trans).signum() == -1) {
				return red;
			}
			return null;
		}

		@Override
		public Font getFont(Object element, int columnIndex) {
			if (element instanceof TransactionListContentProvider.TimeTreeNode) {
				return null;
			}
			Transaction trans = (Transaction) element;
			if (lastDayOfMonthTransactions.contains(trans)) {
				return boldFont;
			}
			return null;
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		top = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		top.setLayout(layout);
		// top banner
		Composite banner = new Composite(top, SWT.NONE);
		banner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		layout = new GridLayout();
		layout.marginHeight = 5;
		layout.marginWidth = 10;
		layout.numColumns = 2;
		banner.setLayout(layout);

		// setup bold font
		Font boldFont = Activator.getDefault().getBoldFont();

		Label l = new Label(banner, SWT.WRAP);
		l.setText("Saldo:");
		l.setFont(boldFont);
		saldo = new Label(banner, SWT.NONE);

		PatternFilter filter = new PatternFilter() {
			@Override
			protected boolean isLeafMatch(Viewer viewer, Object element) {
				if (element instanceof Transaction t) {
					StringBuilder text = new StringBuilder();
					if (t.getCategory() != null) {
						text.append(t.getCategory().getName()).append(' ');
					}
					text.append(t.getMemo()).append(' ');
					if (element instanceof LabeledTransaction t2) {
						text.append(t2.getLabel()).append(' ');
					}
					if (element instanceof InvestmentTransaction t2) {
						if (t2.getSecurity() != null) {
							text.append(t2.getSecurity().getName());
						}
					}
					return wordMatches(text.toString());
				}
				return true;
			}
		};
		FilteredTree filteredTree = new FilteredTree(top, SWT.MULTI | SWT.FULL_SELECTION, filter, true, true);
		transactionTree = filteredTree.getViewer();

		transactionTree.addOpenListener(new IOpenListener() {
			@Override
			public void open(OpenEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				Object firstElement = selection.getFirstElement();
				if (firstElement instanceof TransferTransaction trans) {
					Account account = (Account) trans.getTarget();
					IEditorInput input = new AccountEditorInput(account);
					try {
						AccountEditor editor = (AccountEditor) getSite().getWorkbenchWindow().getActivePage().openEditor(
							input, AccountEditor.ID);
						editor.select(trans.getComplement());
					} catch (PartInitException e) {
						throw new RuntimeException(e);
					}
				}
			}
		});
		transactionTree.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				ITreeSelection selection = (ITreeSelection) event.getSelection();
				Object firstElement = selection.getFirstElement();
				if (firstElement instanceof ITransaction) {
					TreePath[] treePaths = selection.getPathsFor(firstElement);
					Transaction parentTransaction = null;
					if (treePaths.length >= 1 && treePaths[0].getSegmentCount() > 1 &&
						treePaths[0].getSegment(treePaths[0].getSegmentCount() - 2) instanceof Transaction t) {
						parentTransaction = t;
					}
					updateTransactionDetail((ITransaction) firstElement, parentTransaction);
				}
			}
		});

		Tree tree = transactionTree.getTree();
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		tree.setFont(Activator.getDefault().getNormalFont());

		TreeColumn col;

		col = new TreeColumn(tree, SWT.LEFT);
		col.setText("Date");
		col.setWidth(120);
		col.setResizable(true);

		col = new TreeColumn(tree, SWT.LEFT);
		col.setText("Beschreibung");
		col.setWidth(300);
		col.setResizable(true);

		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Betrag");
		col.setWidth(100);
		col.setResizable(true);

		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Saldo");
		col.setWidth(100);
		col.setResizable(true);

		col = new TreeColumn(tree, SWT.LEFT);
		col.setText("BasicData");
		col.setWidth(150);
		col.setResizable(true);
		
		new PersistentColumWidth(tree, Activator.getDefault().getPreferenceStore(), 
				getClass().getCanonicalName()+"."+getEditorInput().getName());

		final Account account = ((AccountEditorInput) getEditorInput()).account;

		transactionDetails1 = new TransactionDetails(account);
		Composite transactionDetailComposite1 = transactionDetails1.createComposite(top);
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.exclude = true;
		transactionDetailComposite1.setLayoutData(gd);

		transactionDetails2 = new InvestmentTransactionDetails(account);
		Composite transactionDetailComposite2 = transactionDetails2.createComposite(top);
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.exclude = true;
		transactionDetailComposite2.setLayoutData(gd);

		Color red = Activator.getDefault().getColorProvider().get(Activator.RED);
		transactionTree.setLabelProvider(new AccountLabelProvider(red, account));
		transactionTree.setContentProvider(new TransactionListContentProvider());
		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		updateSaldoLabel();

		final IAction addBankTransactionAction = new Action() {
			@Override
			public void run() {
				setActiveTransactionDetail(transactionDetails1);
				transactionTree.setSelection(null);
				activeTransactionDetail.setInput(null, null);
			}
		};
		addBankTransactionAction.setText("Add bank transaction");

		final IAction addInvestmentTransactionAction = new Action() {
			@Override
			public void run() {
				setActiveTransactionDetail(transactionDetails2);
				transactionTree.setSelection(null);
				activeTransactionDetail.setInput(null, null);
			}
		};
		addInvestmentTransactionAction.setText("Add investment transaction");

		final IAction convertToSplitTransaction = new Action() {
			@Override
			public void run() {
				IStructuredSelection selection = (IStructuredSelection) transactionTree.getSelection();
				if ( !selection.isEmpty()) {
					Transaction transaction = (Transaction) selection.getFirstElement();
					account.removeTransaction(transaction);
					BankTransaction parentTransaction = new BankTransaction(transaction.getDay(), BigDecimal.ZERO, "");
					account.addTransaction(parentTransaction);
					parentTransaction.addSplit(transaction);
					select(parentTransaction);
				}
			}
		};
		convertToSplitTransaction.setText("Convert to splited transcation");

		final IAction addSplitTransaction = new Action() {
			@Override
			public void run() {
				IStructuredSelection selection = (IStructuredSelection) transactionTree.getSelection();
				if ( !selection.isEmpty()) {
					Transaction parentTransaction = (Transaction) selection.getFirstElement();
					setActiveTransactionDetail(transactionDetails1);
					transactionTree.setSelection(null);
					activeTransactionDetail.setInput(null, parentTransaction);
				}
			}
		};
		addSplitTransaction.setText("Add split (banking)");

		final IAction addInvestmentSplitTransaction = new Action() {
			@Override
			public void run() {
				IStructuredSelection selection = (IStructuredSelection) transactionTree.getSelection();
				if ( !selection.isEmpty()) {
					Transaction parentTransaction = (Transaction) selection.getFirstElement();
					setActiveTransactionDetail(transactionDetails2);
					transactionTree.setSelection(null);
					activeTransactionDetail.setInput(null, parentTransaction);
				}
			}
		};
		addInvestmentSplitTransaction.setText("Add split (investment)");

		final IAction duplicateTransactionAction = new Action() {
			@Override
			public void run() {
				IStructuredSelection selection = (IStructuredSelection) transactionTree.getSelection();
				if ( !selection.isEmpty()) {
					Transaction transaction = (Transaction) selection.getFirstElement();
					Transaction newTrans = (Transaction) transaction.clone();
					account.addTransaction(newTrans);
					select(newTrans);
				}
			}
		};
		duplicateTransactionAction.setText("Duplicate transaction");

		final IAction removeTransactionAction = new Action() {
			@Override
			public void run() {
				IStructuredSelection selection = (IStructuredSelection) transactionTree.getSelection();
				if ( !selection.isEmpty()) {
					Transaction transaction = (Transaction) selection.getFirstElement();
					account.removeTransaction(transaction);
				}
			}
		};
		removeTransactionAction.setText("Remove transaction");

		MenuManager menuMgr = new MenuManager("#popupMenu", AccountEditor.ID); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager menuManager) {
				menuManager.add(new Separator("top")); //$NON-NLS-1$
				menuManager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
				menuManager.add(new Separator());
				menuManager.add(addBankTransactionAction);
				menuManager.add(addInvestmentTransactionAction);
				menuManager.add(convertToSplitTransaction);
				menuManager.add(addSplitTransaction);
				menuManager.add(addInvestmentSplitTransaction);
				boolean transactionSelected = false;
				if (!transactionTree.getSelection().isEmpty() &&
					((IStructuredSelection)transactionTree.getSelection()).getFirstElement() instanceof Transaction) {
					transactionSelected = true;
				}

				boolean splitTransactionSelected = false;
				if (transactionSelected) {
					Transaction transaction = (Transaction) ((IStructuredSelection)transactionTree.getSelection()).getFirstElement();
					if (! account.isSplitTransaction(transaction)) {
						splitTransactionSelected = true;
					}
				}
				convertToSplitTransaction.setEnabled(splitTransactionSelected);
				addSplitTransaction.setEnabled(splitTransactionSelected);
				addInvestmentSplitTransaction.setEnabled(splitTransactionSelected);

				menuManager.add(duplicateTransactionAction);
				duplicateTransactionAction.setEnabled(transactionSelected);
				menuManager.add(new Separator());
				menuManager.add(removeTransactionAction);
				removeTransactionAction.setEnabled(transactionSelected);
			}
		});
		tree.setMenu(menuMgr.createContextMenu(tree));
		getSite().registerContextMenu(menuMgr, getSite().getSelectionProvider());
		getSite().setSelectionProvider(transactionTree);
		account.addPropertyChangeListener(propertyChangeListener);

		List<ITransaction> transactions = account.getTransactions();
		updateLastDayOfMonth(transactions);
		transactionTree.setInput(account);
		if (! transactions.isEmpty()) {
			Day today = Day.today();
			ITransaction t = Lists.reverse(transactions).stream()
				.filter(tx -> tx.getDay().beforeOrEquals(today))
				.findFirst()
				.orElse(transactions.get(transactions.size() - 1));
			for (TreeItem treeItem : transactionTree.getTree().getItems()) {
				if (((TimeTreeNode)treeItem.getData()).getDate().year == t.getDay().year) {
					transactionTree.expandToLevel(treeItem.getData(), 1);
					transactionTree.setSelection(new StructuredSelection(t), true);
				}
			}
			select(t);
		}
	}

	private void updateLastDayOfMonth(List<ITransaction> transactions) {
		lastDayOfMonthTransactions.clear();
		if (transactions.isEmpty()) {
			return;
		}
		
		ITransaction lastTransaction = transactions.get(0);
		for (ITransaction iTransaction : transactions) {
			if (iTransaction.getDay().getMonth() != lastTransaction.getDay().getMonth()) {
				lastDayOfMonthTransactions.add(lastTransaction);
			}
			lastTransaction = iTransaction;
		}
	}

	private void updateSaldoLabel() {
		Account account = ((AccountEditorInput) getEditorInput()).account;
		saldo.setText(PeanutsUtil.formatCurrency(account.getBalance(Day.today()), account.getCurrency()));
	}

	public void select(ITransaction trans) {
		Tree tree = transactionTree.getTree();
		tree.deselectAll();
		TreeItem[] yearItems = tree.getItems();
		for (TreeItem yearItem : yearItems) {
			TimeTreeNode data = (TimeTreeNode)yearItem.getData();
			if (data.getDate().year == trans.getDay().year) {
				TreeItem[] treeItems = yearItem.getItems();
				for (TreeItem treeItem : treeItems) {
					if (treeItem.getData() == trans) {
						tree.setSelection(treeItem);
						break;
					}
				}
				break;
			}
		}
		tree.showSelection();
		updateTransactionDetail(trans, null);
	}

	@Override
	public void dispose() {
		Account account = ((AccountEditorInput) getEditorInput()).account;
		account.removePropertyChangeListener(propertyChangeListener);
		transactionDetails1.dispose();
		transactionDetails2.dispose();
		super.dispose();
	}

	private void updateTransactionDetail(ITransaction t, Transaction parentTransaction) {
		ITransactionDetail newDetail;
		if (t instanceof InvestmentTransaction) {
			newDetail = transactionDetails2;
		} else {
			newDetail = transactionDetails1;
		}
		setActiveTransactionDetail(newDetail);
		activeTransactionDetail.setInput((Transaction) t, parentTransaction);
		transactionTree.getTree().showSelection();
	}

	private void setActiveTransactionDetail(ITransactionDetail newDetail) {
		if (activeTransactionDetail != newDetail) {
			if (activeTransactionDetail != null) {
				// hide old
				((GridData) activeTransactionDetail.getComposite().getLayoutData()).exclude = true;
				activeTransactionDetail.getComposite().setVisible(false);
			}
			// show new
			((GridData) newDetail.getComposite().getLayoutData()).exclude = false;
			newDetail.getComposite().setVisible(true);

			activeTransactionDetail = newDetail;
			top.layout();
			transactionTree.getTree().showSelection();
		}
		activeTransactionDetail.setFocus();
	}

	@Override
	public void setFocus() {
		transactionTree.getTree().setFocus();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// nothing to do
	}

	@Override
	public void doSaveAs() {
		// nothing to do
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if ( !(input instanceof AccountEditorInput)) {
			throw new PartInitException("Invalid Input: Must be AccountEditorInput");
		}
		setSite(site);
		setInput(input);
		setPartName(input.getName());
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

}
