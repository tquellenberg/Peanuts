package de.tomsplayground.peanuts.client.widgets;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.Security;

public class SecurityProposalText {

	private Text securityText;
	private ContentProposalAdapter autoCompleteSecurityAdapter;
	private SimpleContentProposalProvider securityProposalProvider;
	private PropertyChangeListener propertyChangeListener;

	public SecurityProposalText(Composite group) {
		securityText = new Text(group, SWT.SINGLE | SWT.BORDER);
		initSecurityProposalProvider();
		autoCompleteSecurityAdapter = new ContentProposalAdapter(securityText, new TextContentAdapter(),
				securityProposalProvider, null, null);
		autoCompleteSecurityAdapter.setPropagateKeys(true);
		autoCompleteSecurityAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
	}

	public void dispose() {
		Activator.getDefault().getAccountManager().removePropertyChangeListener("security", propertyChangeListener);
	}

	public Text getText() {
		return securityText;
	}

	private void initSecurityProposalProvider() {
		securityProposalProvider = new SimpleContentProposalProvider(new String[0]);
		securityProposalProvider.setFiltering(true);
		propertyChangeListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				setSecurities(Activator.getDefault().getAccountManager().getSecurities());
			}
		};
		Activator.getDefault().getAccountManager().addPropertyChangeListener("security", propertyChangeListener);
		setSecurities(Activator.getDefault().getAccountManager().getSecurities());
	}

	private void setSecurities(ImmutableList<Security> securities) {
		securityProposalProvider.setProposals(Collections2.transform(securities, new Function<Security, String>() {
			@Override
			public String apply(Security input) {
				return input.getName();
			}
		}).toArray(new String[securities.size()]));
	}

}
