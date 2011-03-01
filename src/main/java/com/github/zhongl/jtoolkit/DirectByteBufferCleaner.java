package com.github.zhongl.jtoolkit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * {@link DirectByteBufferCleaner}
 *
 * @author <a href=mailto:jushi@taobao.com>jushi</a>
 * @created 2011-1-14
 */
public final class DirectByteBufferCleaner {
  private DirectByteBufferCleaner() {}

  public static void clean(final ByteBuffer byteBuffer) {
    if (!byteBuffer.isDirect()) return;
    try {
      Object cleaner = invoke(byteBuffer, "cleaner");
      invoke(cleaner, "clean");
    } catch (Exception e) { /* ignore */ }
  }

  private static Object invoke(final Object target, String methodName) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    final Method method = target.getClass().getMethod(methodName);
    return AccessController.doPrivileged(new PrivilegedAction<Object>() {
      @Override
      public Object run() {
        try {
          return method.invoke(target);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

}
