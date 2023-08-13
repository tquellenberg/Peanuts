package de.tomsplayground.peanuts.domain.note;

import java.time.LocalDateTime;

import org.apache.commons.lang3.StringUtils;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("note")
public class Note implements Comparable<Note> {

	private String text;
	private LocalDateTime modificationDate;
	private final LocalDateTime creationDate = LocalDateTime.now();

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
		this.modificationDate = LocalDateTime.now();
	}
	public LocalDateTime getModificationDate() {
		return modificationDate;
	}
	public LocalDateTime getCreationDate() {
		return creationDate;
	}

	@Override
	public int compareTo(Note o) {
		return creationDate.compareTo(o.creationDate);
	}
}
