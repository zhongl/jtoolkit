package com.github.zhongl.jtoolkit;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * IDPoolBaseOnFile is a pool for reusing a set of num id, and can be persisted in a file.
 *
 * @author <a href="mailto:zhong.lunfu@gmail.com">zhongl</a>
 */
public class IDPoolBaseOnFile {
  private final int capacity;
  private final RandomAccessFile raf;
  private final ByteBuffer bits;

  private int inUsed = 0;

  public IDPoolBaseOnFile( int capacity,  String file) throws IOException {
    this.capacity = capacity;
    raf = new RandomAccessFile(file, "rwd");
    try {
      setFileLength(alignAt8(capacity));
      bits = raf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, raf.length());
    } catch (IOException e) {
      dispose();
      throw e;
    }
  }

  public static void main(String... args) throws Exception {
    final String file = args[0];
    final int size = Integer.parseInt(args[1]);
    final IDPoolBaseOnFile pool = new IDPoolBaseOnFile(size, file);

    long elapse = 0;

    for (int i = 0; i < size; i++) {
      long begin = System.nanoTime();
      pool.acquire();
      elapse += System.nanoTime() - begin;
    }

    System.out.println(elapse / size);
    elapse = 0;

    for (int i = 0; i < pool.capacity; i++) {
      long begin = System.nanoTime();
      pool.release(i);
      elapse += System.nanoTime() - begin;
    }
    System.out.println(elapse / size);

    System.out.println(pool.bits.equals(ByteBuffer.wrap(new byte[alignAt8(size)])));
    pool.dispose();

  }

  public synchronized void dispose() { if (raf != null) try { raf.close(); } catch (IOException e) {} }

  /**
   * Release id to pool.
   *
   * @param id
   */
  public synchronized void release(int id) { if (set(id, false)) inUsed--; }

  /**
   * Acquire a id from pool.
   *
   * @return id
   */
  public synchronized int acquire() {
    if (inUsed == capacity) throw new IllegalStateException("No more id for acquisition.");
    for (; ;) {
      final int id = nextId();
      final byte cur = bits.get(index(id));
      final byte or = (byte) (cur | bit(id));
      if (or == cur || id >= capacity) continue; // acquired id
      bits.put(index(id), or);
      inUsed++;
      return id;
    }
  }

  /**
   * Acquire specify id.
   *
   * @param id
   */
  public synchronized void acquire(int id) { if (set(id, true)) inUsed++; }

  /** Reset state to init. */
  public synchronized void reset() {
    inUsed = 0;
    for (int i = 0; i < capacity; i++) release(i);
  }

  private static int alignAt8( int num) {return num / 8 + (num % 8 == 0 ? 0 : 1);}

  private static int bit(long id) { return (1 << id % 8); }

  private static int index(int id) { return id / 8; }

  private void setFileLength(int length) throws IOException {if (raf.length() < length) raf.setLength(length);}

  /**
   * Set index bit to 0 or 1.
   *
   * @param id
   * @param b  true for aquire(1), false for release(0).
   *
   * @return false means no change.
   */
  private boolean set(int id, boolean b) {
    if (id < 0 || id >= capacity) throw new IllegalArgumentException("id : " + id);
    final byte o = bits.get(index(id));
    final byte n = b ? (byte) (o | bit(id)) : (byte) (o & ~bit(id));
    bits.put(index(id), n);
    return o != n;
  }

  private int nextId() { return (int) (System.nanoTime() % capacity); }
}
