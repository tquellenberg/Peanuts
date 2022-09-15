package de.tomsplayground.peanuts.client.comparison;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

import de.tomsplayground.peanuts.domain.comparision.Comparison;

public class ComparisonInput implements IEditorInput {

	private Comparison comparison;
	
	public ComparisonInput(Comparison comparison) {
		this.comparison = comparison;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj.getClass() != getClass()) {
			return false;
		}
		return comparison.equals(((ComparisonInput) obj).comparison);
	}

	@Override
	public int hashCode() {
		return comparison.hashCode();
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageDescriptor.getMissingImageDescriptor();
	}

	@Override
	public String getName() {
		return comparison.getName();
	}

	@Override
	public IPersistableElement getPersistable() {
		return new IPersistableElement() {

			@Override
			public String getFactoryId() {
				return ComparisonEditorFactory.ID;
			}

			@Override
			public void saveState(IMemento memento) {
				memento.putString(ComparisonEditorFactory.COMPARISON_NAME, comparison.getName());
			}
		};
	}

	@Override
	public String getToolTipText() {
		return comparison.getName();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		if (adapter.isAssignableFrom(Comparison.class)) {
			return comparison;
		}
		return null;
	}

	public Comparison getComparison() {
		return comparison;
	}

}
