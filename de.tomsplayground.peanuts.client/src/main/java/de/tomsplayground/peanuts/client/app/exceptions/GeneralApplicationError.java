package de.tomsplayground.peanuts.client.app.exceptions;

/*
 * This error should be displayed to the user.
 */
public class GeneralApplicationError extends Exception {

	private static final long serialVersionUID = -2450215910218015606L;

	public GeneralApplicationError(Throwable e) {
		super(e);
	}
}
