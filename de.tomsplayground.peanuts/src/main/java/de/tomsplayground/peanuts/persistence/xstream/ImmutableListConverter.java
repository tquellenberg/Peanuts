package de.tomsplayground.peanuts.persistence.xstream;

import java.util.Collection;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.AbstractCollectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

public class ImmutableListConverter extends AbstractCollectionConverter {

	public ImmutableListConverter(Mapper mapper) {
		super(mapper);
	}

	@Override
	public boolean canConvert(@SuppressWarnings("rawtypes") Class type) {
		return type.isInstance(ImmutableList.class) ||
			type.equals(DummyImmutableList.class) ||
			type.getName().equals("com.google.common.collect.RegularImmutableList");
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
		Collection collection = (Collection) source;
		for (Object item : collection) {
			writeCompleteItem(item, context, writer);
		}
	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		Builder<Object> builder = ImmutableList.builder();
		while (reader.hasMoreChildren()) {
			builder.add(readCompleteItem(reader, context, builder));
		}
		return builder.build();
	}

}
