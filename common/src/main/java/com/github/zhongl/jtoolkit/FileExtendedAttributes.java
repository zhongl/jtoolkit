package com.github.zhongl.jtoolkit;

import com.sun.jna.Library;

import java.io.File;
import java.io.IOException;
import java.nio.*;

import static com.github.zhongl.jtoolkit.FileExtendedAttributes.NativeLib.LIB;
import static com.sun.jna.Native.loadLibrary;
import static com.sun.jna.Native.synchronizedLibrary;

/**
 * {@link FileExtendedAttributes} only support linux OS.
 *
 * @author <a href="mailto:zhong.lunfu@gmail.com">zhongl</a>
 */
public class FileExtendedAttributes {

  public static final String USER_PREFIX = "user.";
  public static final int ETOOSMALL = -525;
  public static final int ENOTSUPP = -524;
  public static final int FLAGS = 0;

  private final String path;

  interface NativeLib extends Library {
    NativeLib LIB = (NativeLib) synchronizedLibrary((NativeLib) loadLibrary("libc.so.6", NativeLib.class));

    int setxattr(String pathname, String name, Buffer value, int size, int flags);

    int getxattr(String pathname, String name, Buffer value, int size);
  }

  public static FileExtendedAttributes extendedAttributesOf(File file) {
    return new FileExtendedAttributes(file.getAbsolutePath());
  }

  public static FileExtendedAttributes extendedAttributesOfFile(String path) {
    return new FileExtendedAttributes(path);
  }

  public void set(String name, Buffer buffer, int size) throws IOException {
    throwIoExceptionIfFailed(LIB.setxattr(path, USER_PREFIX + name, buffer, size, FLAGS));
  }

  public void set(String name, short value) throws IOException { set(name, buffer(value), 2); }

  public void set(String name, int value) throws IOException { set(name, buffer(value), 4); }

  public void set(String name, long value) throws IOException { set(name, buffer(value), 8); }

  public void set(String name, String value) throws IOException { set(name, buffer(value), value.getBytes().length); }

  public void get(String name, Buffer buffer, int size) throws IOException {
    throwIoExceptionIfFailed(LIB.getxattr(path, name, buffer, size));
  }

  public short getShort(String name) throws IOException {
    final ShortBuffer buffer = ShortBuffer.allocate(1);
    get(name, buffer, 2);
    return buffer.get(0);
  }

  public int getInt(String name) throws IOException {
    final IntBuffer buffer = IntBuffer.allocate(1);
    get(name, buffer, 4);
    return buffer.get(0);
  }

  public long getLong(String name) throws IOException {
    final LongBuffer buffer = LongBuffer.allocate(1);
    get(name, buffer, 8);
    return buffer.get(0);
  }

  public String getString(String name, int size) throws IOException {
    final ByteBuffer buffer = ByteBuffer.allocate(size);
    get(name, buffer, size);
    return new String(buffer.array());
  }

  private FileExtendedAttributes(String path) {this.path = path;}

  private static Buffer buffer(short value) {return ShortBuffer.wrap(new short[]{value});}

  private static Buffer buffer(int value) {return IntBuffer.wrap(new int[]{value});}

  private static Buffer buffer(long value) {return LongBuffer.wrap(new long[]{value});}

  private static Buffer buffer(String value) {return ByteBuffer.wrap(value.getBytes());}

  private static void throwIoExceptionIfFailed(int returnCode) throws IOException {
    if (returnCode >= FLAGS) return;
    switch (returnCode) {
      case ENOTSUPP:
        throw new IOException("Operation is not supported");
      case ETOOSMALL:
        throw new IOException("Buffer or request is too small");
      default:
        throw new IOException("Operation failed, and error no is " + returnCode);
    }
  }
}
