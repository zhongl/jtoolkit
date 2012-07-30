package com.github.zhongl.jtoolkit;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;


/**
 * {@link DirectoryCleaner}
 *
 * @author <a href=mailto:zhong.lunfu@gmail.com>zhongl</a>
 * @created 2010-12-3
 */
public final class DirectoryCleaner {

  private DirectoryCleaner() {}

  public static void clean(final File dir) { RecurseTree.run(dir, FACTORY, CALLBACK); }

  public static void clean(final String dir) { clean(new File(dir)); }

  private final static RecurseTree.IteratorFactory<File> FACTORY = new RecurseTree.IteratorFactory<File>() {
    @Override
    public Iterator<File> iterator(final File obj) {
      final Iterator<File> empty = RecurseTree.empty();
      return obj.isDirectory() ? Arrays.asList(obj.listFiles()).iterator() : empty;
    }
  };

  private final static RecurseTree.Callback<File> CALLBACK = new RecurseTree.Callback<File>() {
    @Override
    public void onCallback(final File obj) { obj.delete(); }
  };
}
