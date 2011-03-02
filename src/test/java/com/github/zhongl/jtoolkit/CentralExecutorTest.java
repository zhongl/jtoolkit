package com.github.zhongl.jtoolkit;

import static com.github.zhongl.jtoolkit.CentralExecutor.Policy.*;
import static com.github.zhongl.jtoolkit.CentralExecutor.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Test;

public class CentralExecutorTest {
  private CentralExecutor executor;

  @After
  public void tearDown() throws Exception {
    executor.shutdown();
    if (!executor.awaitTermination(1L, TimeUnit.SECONDS)) executor.shutdownNow();
  }

  @Test()
  public void reserveOneInPessimism() throws Exception {
    executor = new CentralExecutor(2, PESSIMISM);
    executor.quota(Placeholder.class, reserve(1), unlimited());

    final Placeholder ph1 = new Placeholder();
    final Placeholder ph2 = new Placeholder();

    executor.execute(ph1);
    executor.execute(ph2);

    Thread.sleep(100L);
    assertThat(ph1.running, is(true));

    ph1.running = false;
    Thread.sleep(100L);
    assertThat(ph1.interrupted, is(false));
    assertThat(ph2.running, is(true));

    ph2.running = false;
  }

  @Test
  public void limitOne() throws Exception {
    //To change body of created methods use File | Settings | File Templates.
  }

  @Test(expected = RejectedExecutionException.class)
  public void unquotaedTaskCantBeExecutedInPessimism() throws Exception {
    (executor = new CentralExecutor(1, PESSIMISM)).execute(new Placeholder());
  }

  @Test(expected = IllegalArgumentException.class)
  public void noResourceForReserve() throws Exception {
    (executor = new CentralExecutor(1)).quota(Runnable.class, reserve(3), limit(5));
  }

  @Test(expected = IllegalArgumentException.class)
  public void reserveShouldNotGreaterThanLimit() throws Exception {
    (executor = new CentralExecutor(4)).quota(Runnable.class, reserve(3), limit(2));
  }


  @Test(expected = IllegalArgumentException.class)
  public void quotaShouldNotLessThanZero() throws Exception {
    (executor = new CentralExecutor(1)).quota(Runnable.class, reserve(-1), limit(2));
  }

  @Test(expected = IllegalArgumentException.class)
  public void limitShouldNotBeZero() throws Exception {
    (executor = new CentralExecutor(1)).quota(Runnable.class, reserve(0), limit(0));
  }

  @Test(expected = IllegalArgumentException.class)
  public void noneReserveTaskWillNeverBeExecutedInPessimism() throws Exception {
    (executor = new CentralExecutor(1)).quota(Runnable.class, reserve(0), limit(1));
  }

  private class ReserveOne implements Runnable {
    public volatile boolean ran = false;

    @Override
    public void run() {
      ran = true;
    }
  }

  private class Placeholder implements Runnable {

    public volatile boolean running = false;
    public volatile boolean interrupted = false;

    @Override
    public void run() {
      running = true;
      try {
        while (running) {
          Thread.sleep(50L);
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
        Thread.currentThread().interrupt();
        interrupted = true;
      }
    }
  }
}
