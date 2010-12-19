package de.tomsplayground.peanuts.client.dnd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TransferData;

public abstract class SerializedObjectTransfer extends ByteArrayTransfer {
	protected SerializedObjectTransfer() {
		// protected constructor
	}

	@Override
	public void javaToNative(Object object, TransferData transferData) {
		if ( !isSupportedType(transferData)) {
			DND.error(DND.ERROR_INVALID_DATA);
		}
		if ( !(object instanceof Serializable)) {
			DND.error(DND.ERROR_INVALID_DATA);
		}

		byte[] data = toByteArray((Serializable) object);
		super.javaToNative(data, transferData);
	}

	@Override
	public Serializable nativeToJava(TransferData transferData) {
		if ( !isSupportedType(transferData)) {
			return null;
		}

		byte[] data = (byte[]) super.nativeToJava(transferData);
		if (data == null) {
			return null;
		}
		try {
			return toObject(data);
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw e;
		}
	}

	private static byte[] toByteArray(Serializable s) {
		ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
		try {
			ObjectOutputStream out = new ObjectOutputStream(bytesOut);
			out.writeObject(s);
			out.flush();
			return bytesOut.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static Serializable toObject(byte[] bytes) {
		ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytes);
		try {
			ObjectInputStream in = new ObjectInputStream(bytesIn);
			return (Serializable) in.readObject();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

}
