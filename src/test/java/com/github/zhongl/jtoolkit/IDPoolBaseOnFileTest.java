package com.github.zhongl.jtoolkit;

import java.util.BitSet;

import org.junit.*;

public class IDPoolBaseOnFileTest {
  private static final int CAPACITY = 1000;
  private static IDPoolBaseOnFile pool;

  @BeforeClass
  public static void setUpClass() throws Exception { pool = new IDPoolBaseOnFile(CAPACITY, "target/id.set"); }

  @Test(expected = IllegalArgumentException.class)
  public void illegalIdLessThanZero() throws Exception { pool.acquire(-1); }

  @Test(expected = IllegalArgumentException.class)
  public void illegalIdEqualsCapacity() throws Exception { pool.acquire(CAPACITY); }

  @Test(expected = IllegalStateException.class)
  public void noMoreId() throws Exception {
    for (int i = 0; i < CAPACITY; i++) pool.acquire(i);
    pool.acquire();
  }

  @Test
  public void acquiredValidId() throws Exception {
    final BitSet ids = new BitSet(CAPACITY);
    for (int i = 0; i < CAPACITY; i++) {
      final int id = pool.acquire();
      if (id > 999 || id < 0 || ids.get(id)) throw new IllegalStateException("invalid id :" + id);
      ids.set(id);
    }
  }

  @After
  public void tearDown() throws Exception { pool.reset(); }

  @AfterClass
  public static void tearDownClass() throws Exception { if (pool != null) pool.dispose(); }
}
