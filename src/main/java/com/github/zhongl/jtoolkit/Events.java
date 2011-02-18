package com.github.zhongl.jtoolkit;

import static java.util.concurrent.Executors.*;

import java.util.concurrent.*;

/**
 * {@link Events}
 *
 * @author <a href=mailto:zhong.lunfu@gmail.com>zhongl</a>
 * @created 2010-11-26
 */
public final class Events {

  public static int executorNum = Runtime.getRuntime().availableProcessors();
  public static int schedulerNum = 1;

  static {
    EXECUTOR = newFixThreadPool(executorNum, new DebugableThreadFactory("events-executor", true));
    SCHEDULER = newScheduledThreadPool(schedulerNum, new DebugableThreadFactory("events-scheduler", true));
    final Thread shutdownEvents = new Thread(new Runnable() {
      @Override
      public void run() { dispose(); }
    }, "shutdown-events");
    shutdownEvents.setUncaughtExceptionHandler(LoggingUncaughtExceptionHandler.SINGLETON);
    Runtime.getRuntime().addShutdownHook(shutdownEvents);
  }

  public static void dispose() {
    try {
      do {
        SCHEDULER.shutdownNow();
      } while (!SCHEDULER.awaitTermination(500L, TimeUnit.MILLISECONDS));
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    try {
      do {
        EXECUTOR.shutdownNow();
      } while (!EXECUTOR.awaitTermination(500L, TimeUnit.MILLISECONDS));
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /** @see java.util.concurrent.ExecutorService#execute(Runnable) */
  public static void enqueue(final Runnable task) {
    EXECUTOR.execute(task);
  }

  public static ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long period, final TimeUnit unit) {
    return scheduleAtFixedRate(event(command), period, period, unit);
  }

  /** @see java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit) */
  public static ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period,
                                                       TimeUnit unit) {
    return SCHEDULER.scheduleAtFixedRate(command, initialDelay, period, unit);
  }

  private static Runnable event(final Runnable command) {
    return new Runnable() {

      @Override
      public void run() {
        EXECUTOR.execute(command);
      }
    };
  }

  private static ThreadPoolExecutor newFixThreadPool(int nThreads, ThreadFactory threadFactory) {
    return new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS,
                                  new LinkedBlockingQueue<Runnable>(nThreads * 2), threadFactory,
                                  new ThreadPoolExecutor.CallerRunsPolicy());
  }

  private final static ExecutorService EXECUTOR;
  private final static ScheduledExecutorService SCHEDULER;
}
