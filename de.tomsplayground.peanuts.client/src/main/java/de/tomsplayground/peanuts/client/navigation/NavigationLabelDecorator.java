package de.tomsplayground.peanuts.client.navigation;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.IDeletable;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.note.Note;
import de.tomsplayground.peanuts.domain.statistics.SecurityCategoryMapping;

public class NavigationLabelDecorator implements ILightweightLabelDecorator {

	private static final ImageDescriptor DELETED_OVR;

	private static final ImageDescriptor WARNING_OVR;

	private static final ImageDescriptor INFO_OVR;

	static {
		DELETED_OVR = AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "icons2/error_ovr.png");
		WARNING_OVR = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_DEC_FIELD_WARNING);
		INFO_OVR = AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "icons2/contassist_ovr.png");
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
	}

	@Override
	public void decorate(Object element, IDecoration decoration) {
		if (element instanceof IDeletable deletable) {
			if (deletable.isDeleted()) {
				decoration.addOverlay(DELETED_OVR, IDecoration.BOTTOM_RIGHT);
			}
		}
		if (element instanceof Security security) {
			if (emptyCategoryMapping(security)) {
				decoration.addOverlay(WARNING_OVR, IDecoration.TOP_RIGHT);
			}
			if (isNoteNotEmpty(security)) {
				decoration.addOverlay(INFO_OVR, IDecoration.TOP_LEFT);				
			}
		}
	}

	private boolean isNoteNotEmpty(Security security) {
		for (Note note : security.getNotes()) {
			if (StringUtils.isNotBlank(note.getText())) {
				return true;
			}
		}
		return false;
	}
	
	private boolean emptyCategoryMapping(Security security) {
		for (SecurityCategoryMapping securityCategoryMapping : Activator.getDefault().getAccountManager().getSecurityCategoryMappings()) {
			if (securityCategoryMapping.getAllSecurities().contains(security)) {
				return false;
			}
		}
		return true;
	}

}
