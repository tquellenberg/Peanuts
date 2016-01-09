package de.tomsplayground.peanuts.client.editors.account;

import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

import de.tomsplayground.peanuts.domain.statistics.AccountValueCsvWriter;
import de.tomsplayground.peanuts.domain.statistics.AccountValueData;

public class AccountValueExport extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
		if (activeEditor instanceof AccountEditor) {
			IEditorPart[] findEditors = ((AccountEditor)activeEditor).findEditors(activeEditor.getEditorInput());
			for (IEditorPart iEditorPart : findEditors) {
				if (iEditorPart instanceof ValueChartEditorPart) {
					ValueChartEditorPart valueChartEditorPart = (ValueChartEditorPart)iEditorPart;
					AccountValueData accountValueData = valueChartEditorPart.getAccountValueData();
					try {
						FileWriter fileWriter = new FileWriter("/Users/quelle/values.csv");
						AccountValueCsvWriter.write(accountValueData.stream(), fileWriter);
						IOUtils.closeQuietly(fileWriter);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
		return null;
	}

}
