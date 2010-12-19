package de.tomsplayground.peanuts.client.dnd;

import java.io.Serializable;

import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TransferData;

public class PeanutsTransfer extends SerializedObjectTransfer {
	public static final PeanutsTransfer INSTANCE = new PeanutsTransfer();

	private static final String TYPE_NAME = "peanuts_category"; //$NON-NLS-1$
	private static final int TYPE_ID = registerType(TYPE_NAME);

	@Override
	protected int[] getTypeIds() {
		return new int[] { TYPE_ID };
	}

	@Override
	protected String[] getTypeNames() {
		return new String[] { TYPE_NAME };
	}

	@Override
	public void javaToNative(Object object, TransferData transferData) {
		if ( !(object instanceof IPeanutsTransferData)) {
			DND.error(DND.ERROR_INVALID_DATA);
		}

		super.javaToNative(object, transferData);
	}

	@Override
	public Serializable nativeToJava(TransferData transferData) {
		Serializable result = super.nativeToJava(transferData);
		if ( !(result instanceof IPeanutsTransferData)) {
			return null;
		}
		return result;
	}
}
