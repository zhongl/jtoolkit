package com.github.zhongl.jtoolkit;

import org.junit.After;
import org.junit.Test;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static com.github.zhongl.jtoolkit.CentralExecutor.Policy.OPTIMISM;
import static com.github.zhongl.jtoolkit.CentralExecutor.Policy.PESSIMISM;
import static com.github.zhongl.jtoolkit.CentralExecutor.*;
import static java.lang.Thread.sleep;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * {@link CentralExecutorTest }...
 *
 * @author  <a href="mailto:zhong.lunfu@gmail.com">zhongl</a>
 * @created 11-3-2
 */
public class CentralExecutorTest {
  private CentralExecutor executor;

  @After
  public void tearDown() throws Exception {
    executor.shutdown();
    if (!executor.awaitTermination(1L, TimeUnit.SECONDS)) executor.shutdownNow();
  }

  @Test
  public void reserveInPessimism() throws Exception {
    executor = new CentralExecutor(2, PESSIMISM);
    executor.quota(Placeholder.class, reserve(1), nil());

    final Placeholder ph1 = new Placeholder();
    final Placeholder ph2 = new Placeholder();

    executor.execute(ph1);
    executor.execute(ph2);

    sleep(100L);
    assertThat(ph1.running, is(true));
    assertThat(ph2.running, is(false));

    ph1.running = false;
    sleep(100L);
    assertThat(ph2.running, is(true));

    ph2.running = false;
  }

  @Test
  public void elasticInOptimism() throws Exception {
    executor = new CentralExecutor(3, OPTIMISM);
    executor.quota(Placeholder.class, reserve(1), elastic(1));

    final Placeholder ph1 = new Placeholder();
    final Placeholder ph2 = new Placeholder();
    final Placeholder ph3 = new Placeholder();

    executor.execute(ph1);
    executor.execute(ph2);
    executor.execute(ph3);

    sleep(100L);

    assertThat(ph1.running,is(true));
    assertThat(ph2.running, is(true));
    assertThat(ph3.running, is(false));

    ph1.running = false;
    sleep(100L);
    assertThat(ph3.running,is(true));

    ph2.running = false;
    ph3.running = false;
  }

  @Test(expected = RejectedExecutionException.class)
  public void unquotaedTaskCantBeExecutedInPessimism() throws Exception {
    (executor = new CentralExecutor(1, PESSIMISM)).execute(new Placeholder());
  }

  @Test(expected = IllegalArgumentException.class)
  public void noResourceForReserve() throws Exception {
    (executor = new CentralExecutor(1)).quota(Runnable.class, reserve(3), elastic(5));
  }


  @Test(expected = IllegalArgumentException.class)
  public void quotaShouldNotLessThanZero() throws Exception {
    (executor = new CentralExecutor(1)).quota(Runnable.class, reserve(-1), elastic(2));
  }

  @Test(expected = IllegalArgumentException.class)
  public void noneReserveTaskWillNeverBeExecutedInPessimism() throws Exception {
    (executor = new CentralExecutor(1)).quota(Runnable.class, reserve(0), elastic(1));
  }

  /** {@link Placeholder }... */
  private class Placeholder implements Runnable {

    public volatile boolean running = false;

    @Override
    public void run() {
      running = true;
      try {
        while (running) sleep(50L);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
