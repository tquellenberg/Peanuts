package de.tomsplayground.peanuts.domain.note;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("note")
public class Note implements Comparable<Note> {

	private String text;
	private DateTime modificationDate;
	private final DateTime creationDate = new DateTime();

	public Note(String text) {
		this.text = StringUtils.defaultString(text);
		updateModificationDate();
	}
	public void setText(String text) {
		this.text = StringUtils.defaultString(text);
		updateModificationDate();
	}
	public String getText() {
		return StringUtils.defaultString(text);
	}
	public final void updateModificationDate() {
		this.modificationDate = new DateTime();
	}
	public DateTime getModificationDate() {
		return modificationDate;
	}
	public DateTime getCreationDate() {
		return creationDate;
	}

	@Override
	public int compareTo(Note o) {
		return creationDate.compareTo(o.creationDate);
	}
}
