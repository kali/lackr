package com.fotonauts.lackr.mustache.helpers;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;

public class ReverseEachHelper implements Helper<Object> {

  /**
   * A singleton instance of this helper.
   */
  public static final Helper<Object> INSTANCE = new ReverseEachHelper();

  /**
   * The helper's name.
   */
  public static final String NAME = "reverse_each";

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Override
  public CharSequence apply(final Object context, final Options options)
      throws IOException {
    if (context == null) {
      return StringUtils.EMPTY;
    }
    if (context instanceof List) {
      return iterableContext((List) context, options);
    }
    return hashContext(context, options);
  }

  /**
   * Iterate over a hash like object.
   *
   * @param context The context object.
   * @param options The helper options.
   * @return The string output.
   * @throws IOException If something goes wrong.
   */
  private CharSequence hashContext(final Object context, final Options options)
      throws IOException {
    Set<Entry<String, Object>> propertySet = options.propertySet(context);
    StringBuilder buffer = new StringBuilder();
    Context parent = options.wrap(context);
    for (Entry<String, Object> entry : propertySet) {
      Context current = Context.newContext(parent, entry.getValue())
          .data("key", entry.getKey());
      buffer.append(options.fn(current));
    }
    return buffer.toString();
  }

  /**
   * Iterate over an iterable object.
   *
   * @param context The context object.
   * @param options The helper options.
   * @return The string output.
   * @throws IOException If something goes wrong.
   */
  private CharSequence iterableContext(final List<Object> context, final Options options)
      throws IOException {
    StringBuilder buffer = new StringBuilder();
    if (options.isFalsy(context)) {
      buffer.append(options.inverse());
    } else {
      ListIterator<Object> iterator = context.listIterator(context.size());
      int index = 0;
      while (iterator.hasPrevious()) {
        Context parent = options.wrap(context);
        Object element = iterator.previous();
        boolean first = index == 0;
        boolean even = index % 2 == 0;
        boolean last = !iterator.hasPrevious();
        Context current = Context.newContext(parent, element)
            .data("index", index)
            .data("first", first ? "first" : "")
            .data("last", last ? "last" : "")
            .data("odd", even ? "" : "odd")
            .data("even", even ? "even" : "");
        buffer.append(options.fn(current));
        index++;
      }
    }
    return buffer.toString();
  }

  /**
   * Retrieve the next element available.
   *
   * @param parent The parent context.
   * @param iterator The element iterator.
   * @param index The nth position of this element. Zero base.
   * @return The next element available.
   */
  protected Object next(final Context parent, final Iterator<Object> iterator,
      final int index) {
    return iterator.next();
  }
}
