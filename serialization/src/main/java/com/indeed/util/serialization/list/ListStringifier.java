package com.indeed.util.serialization.list;

import com.google.common.base.CharMatcher;
import com.google.common.base.Supplier;
import com.indeed.util.serialization.CollectionSuppliers;
import com.indeed.util.serialization.Stringifier;
import com.indeed.util.serialization.splitter.EscapeAwareSplitter;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;

/**
 * @author jplaisance
 */
public final class ListStringifier<E> implements Stringifier<List<E>> {
    private static final Logger log = LoggerFactory.getLogger(ListStringifier.class);

    private static final EscapeAwareSplitter splitter = new EscapeAwareSplitter(CharMatcher.whitespace().or(CharMatcher.is(',')), EscapeAwareSplitter.ESCAPE_JAVA_LEXER_SUPPLIER);
    
    public static <T> ListStringifier<T> arrayListStringifier(Stringifier<T> stringifier) {
        return new ListStringifier<T>(new CollectionSuppliers.ArrayListSupplier<T>(), stringifier);
    }

    private final Supplier<? extends List<E>> listSupplier;
    private final Stringifier<E> stringifier;

    public ListStringifier(Supplier<? extends List<E>> listSupplier, Stringifier<E> stringifier) {
        this.listSupplier = listSupplier;
        this.stringifier = stringifier;
    }

    @Override
    public String toString(List<E> objects) {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        for (E e : objects) {
            builder.append('"');
            builder.append(StringEscapeUtils.escapeJava(stringifier.toString(e)));
            builder.append("\", ");
        }
        if (objects.size() > 0) {
            builder.setLength(builder.length()-2);
        }
        builder.append(']');
        return builder.toString();
    }

    @Override
    public List<E> fromString(String str) {
        List<E> values = listSupplier.get();
        Iterator<String> objects = splitter.split(str.substring(1, str.length()-1));
        while (objects.hasNext()) {
            E e = stringifier.fromString(objects.next());
            values.add(e);
        }
        return values;
    }
}
