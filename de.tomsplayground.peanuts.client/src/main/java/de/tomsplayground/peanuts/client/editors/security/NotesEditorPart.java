package de.tomsplayground.peanuts.client.editors.security;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.note.Note;

public class NotesEditorPart extends EditorPart {

	private boolean dirty = false;
	private Text text;

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if (!(input instanceof SecurityEditorInput)) {
			throw new PartInitException("Invalid Input: Must be SecurityEditorInput");
		}
		setSite(site);
		setInput(input);
		setPartName(input.getName());
	}

	@Override
	public void createPartControl(Composite parent) {
		Composite top = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		top.setLayout(layout);

		text = new Text(top, SWT.MULTI  | SWT.WRAP | SWT.BORDER);
		GridData layoutData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		layoutData.heightHint = 100;
		text.setLayoutData(layoutData);

		Security security = getSecurity();
		ImmutableList<Note> notes = security.getNotes();
		if (! notes.isEmpty()) {
			text.setText(notes.get(0).getText());
		}

		text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				markDirty();
			}
		});
	}

	@Override
	public void setFocus() {
		text.setFocus();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		String t = text.getText();
		Security security = getSecurity();
		ImmutableList<Note> notes = security.getNotes();
		if (notes.isEmpty()) {
			security.addNote(new Note(t));
		} else {
			notes.get(0).setText(t);
		}
		dirty = false;
	}

	private Security getSecurity() {
		return ((SecurityEditorInput) getEditorInput()).getSecurity();
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	public void markDirty() {
		dirty = true;
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

}
