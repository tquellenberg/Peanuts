package de.tomsplayground.peanuts.client;

/**
 * Interface defining the application's command IDs. Key bindings can be defined for specific
 * commands. To associate an action with a command, use IAction.setActionDefinitionId(commandId).
 * 
 * @see org.eclipse.jface.action.IAction#setActionDefinitionId(String)
 */
public interface ICommandIds {

	public static final String CMD_IMPORT_QIF = "de.tomsplayground.peanuts.client.import.qif";
	public static final String CMD_SAVE_BPX = "de.tomsplayground.peanuts.client.save.bpx";
	public static final String CMD_LOAD_BPX = "de.tomsplayground.peanuts.client.load.bpx";
	public static final String CMD_CSV_TRANSACTION_IMPORT = "de.tomsplayground.peanuts.client.import.csv";

}
