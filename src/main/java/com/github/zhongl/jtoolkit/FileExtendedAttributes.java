package com.github.zhongl.jtoolkit;

import com.sun.jna.Library;
import com.sun.jna.Native;

import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import static com.github.zhongl.jtoolkit.FileExtendedAttributes.NativeLib.LIB;

/**
 * {@link FileExtendedAttributes} only support linux OS.
 *
 * @author <a href="mailto:zhongl@gmail.com">zhongl</a>
 */
public final class FileExtendedAttributes {
  public static final String ID = "user.Id";
  public static final String REPLICATION = "user.repl";
  public static final String BLOCK_SIZE = "user.blks";

  public static final int ERROR = -1;
  public static final String LENGTH = "user.length";

  private final String path;

  interface NativeLib extends Library {
    NativeLib LIB = (NativeLib) Native.loadLibrary("libc.so.6", NativeLib.class);

    int setxattr(String pathname, String name, Buffer value, int size, int flags);

    int getxattr(String pathname, String name, Buffer value, int size);
  }

  public static FileExtendedAttributes extendedAttributesOf(File file) {
    return new FileExtendedAttributes(file.getAbsolutePath());
  }

  public static FileExtendedAttributes extendedAttributesOfFile(String path) {
    return new FileExtendedAttributes(path);
  }

  private FileExtendedAttributes(String path) {this.path = path;}

  public void setId(int id) throws IOException {
    final Buffer buffer = IntBuffer.allocate(1).put(id).flip();
    final int returnCode = LIB.setxattr(path, ID, buffer, 4, 0);
    if (returnCode == ERROR) throw new IOException("Operation not supported");

  }

  public int getId() throws IOException {
    final IntBuffer buffer = IntBuffer.allocate(1);
    final int returnCode = LIB.getxattr(path, ID, buffer, 4);
    if (returnCode == ERROR) throw new IOException("Operation not supported");
    return buffer.get(0);
  }

  public void setBlockSize(long blockSize) throws IOException {
    final Buffer buffer = LongBuffer.allocate(1).put(blockSize).flip();
    final int returnCode = LIB.setxattr(path, BLOCK_SIZE, buffer, 8, 0);
    if (returnCode == ERROR) throw new IOException("Operation not supported");
  }

  public long getBlockSize() throws IOException {
    final LongBuffer buffer = LongBuffer.allocate(1);
    final int returnCode = LIB.getxattr(path, BLOCK_SIZE, buffer, 8);
    if (returnCode == ERROR) throw new IOException("Operation not supported");
    return buffer.get(0);
  }

  public void setLength(long length) throws IOException {
    final Buffer buffer = LongBuffer.allocate(1).put(length).flip();
    final int returnCode = LIB.setxattr(path, LENGTH, buffer, 8, 0);
    if (returnCode == ERROR) throw new IOException("Operation not supported");
  }

  public long getLength() throws IOException {
    final LongBuffer buffer = LongBuffer.allocate(1);
    final int returnCode = LIB.getxattr(path, LENGTH, buffer, 8);
    if (returnCode == ERROR) throw new IOException("Operation not supported");
    return buffer.get(0);
  }

  public void setReplication(short replication) throws IOException {
    final Buffer buffer = ShortBuffer.allocate(1).put(replication).flip();
    final int returnCode = LIB.setxattr(path, REPLICATION, buffer, 2, 0);
    if (returnCode == ERROR) throw new IOException("Operation not supported");
  }

  public short getReplication() throws IOException {
    final ShortBuffer buffer = ShortBuffer.allocate(1);
    final int returnCode = LIB.getxattr(path, REPLICATION, buffer, 2);
    if (returnCode == ERROR) throw new IOException("Operation not supported");
    return buffer.get(0);
  }

}
