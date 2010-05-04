package com.sampullara.mustache;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base class for Mustaches.
 * <p/>
 * User: sam
 * Date: May 3, 2010
 * Time: 10:12:47 AM
 */
public abstract class Mustache {
  protected Logger logger = Logger.getLogger(getClass().getName());
  private File root;

  public void setRoot(File root) {
    this.root = root;
  }

  public abstract void execute(Writer writer, Scope ctx) throws MustacheException;

  protected void write(Writer writer, Scope s, String name, boolean encode) throws MustacheException {
    Object value = getValue(s, name);
    if (value != null) {
      String string = String.valueOf(value);
      if (encode) {
        string = encode(string);
      }
      try {
        writer.write(string);
      } catch (IOException e) {
        throw new MustacheException("Failed to write: " + e);
      }
    }
  }

  private static Iterable emptyIterable = new ArrayList(0);

  protected Iterable<Scope> iterable(final Scope s, final String name) {
    final Object value = s.get(name);
    if (value == null || (value instanceof Boolean && !((Boolean)value))) {
      return emptyIterable;
    }
    return new Iterable<Scope>() {
      public Iterator<Scope> iterator() {
        return new Iterator<Scope>() {
          Iterator i;
          {
            if (value instanceof Iterable) {
              i = ((Iterable)value).iterator();
            } else if (value instanceof Boolean) {
              i = Arrays.asList(true).iterator();
            }
          }
          public boolean hasNext() {
            return i.hasNext();
          }
          public Scope next() {
            Scope scope = new Scope(getValue(s, name), s);
            scope.put(name, i.next());
            return scope;
          }
          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }
    };
  }

  protected Mustache compile(final Scope s, final String name) throws MustacheException {
    Compiler c = new Compiler(root);
    Object partial = getValue(s, name);
    if (partial != null) {
      return c.parse(partial.toString());
    }
    return null;
  }

  protected Mustache include(final Scope s, final String name) throws MustacheException {
    Compiler c = new Compiler();
    if (name != null) {
      return c.parseFile(name);
    }
    return null;
  }

  protected Iterable<Scope> inverted(final Scope s, final String name) {
    final Object value = s.get(name);
    boolean isntEmpty = value instanceof Iterable && ((Iterable) value).iterator().hasNext();
    if (isntEmpty || (value instanceof Boolean && ((Boolean)value))) {
      return emptyIterable;
    }
    Scope scope = new Scope(s);
    scope.put(name, true);
    return Arrays.asList(scope);
  }

  protected Object getValue(Scope s, String name) {
    try {
      return s.get(name);
    } catch (Exception e) {
      logger.warning("Failed: " + e + " using " + name);
    }
    return null;
  }

  private static Pattern findToEncode = Pattern.compile("&(?!\\w+;)|[\"<>\\\\]");

  protected static String encode(String value) {
    StringBuffer sb = new StringBuffer();
    Matcher matcher = findToEncode.matcher(value);
    while (matcher.find()) {
      char c = matcher.group().charAt(0);
      switch (c) {
        case '&':
          matcher.appendReplacement(sb, "&amp;");
        case '\\':
          matcher.appendReplacement(sb, "\\\\");
        case '"':
          matcher.appendReplacement(sb, "\"");
        case '<':
          matcher.appendReplacement(sb, "&lt;");
        case '>':
          matcher.appendReplacement(sb, "&gt;");
      }
    }
    matcher.appendTail(sb);
    return sb.toString();
  }
}