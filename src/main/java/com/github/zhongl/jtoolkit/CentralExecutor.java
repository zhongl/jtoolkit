package com.github.zhongl.jtoolkit;

import static com.github.zhongl.jtoolkit.CentralExecutor.Policy.*;
import static java.util.concurrent.Executors.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link CentralExecutor} 支持对各种 {@link Runnable} 任务进行线程资源的配额设定, 实现线程池统一规划管理.
 *
 * @author <a href="mailto:zhong.lunfu@gmail.com">zhongl</>
 * @created 11-3-2
 */
public class CentralExecutor implements Executor {
  private static final Logger LOGGER = LoggerFactory.getLogger(CentralExecutor.class);
  private static final String CLASS_NAME = CentralExecutor.class.getSimpleName();

  private final ExecutorService service;
  private final Policy policy;
  private final Map<Class<? extends Runnable>, Submitter> quotas;
  private final int threadSize;

  private int reserved;

  public CentralExecutor(final int threadSize, Policy policy) {
    this.threadSize = threadSize;
    this.policy = policy;
    this.service = newFixedThreadPool(threadSize, new DebugableThreadFactory(CLASS_NAME));
    this.quotas = new ConcurrentHashMap<Class<? extends Runnable>, Submitter>();
  }

  public CentralExecutor(int threadSize) { this(threadSize, PESSIMISM); }

  public void shutdown() { service.shutdown(); }

  public List<Runnable> shutdownNow() { return service.shutdownNow(); }

  public boolean isShutdown() { return service.isShutdown(); }

  public boolean isTerminated() { return service.isTerminated(); }

  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return service.awaitTermination(timeout, unit);
  }

  @Override
  public void execute(Runnable task) {
    final Submitter submitter = quotas.get(task.getClass());
    if (submitter != null) submitter.submit(task, this);
    else policy.defaultSubmitter().submit(task, this);
  }

  public static Quota reserve(int value) { return new Quota(value); }

  public static Quota limit(int value) { return new Quota(value); }

  public static Quota unlimited() { return new Quota(Integer.MAX_VALUE); }

  /**
   * 设定taskClass的保留和限制配额.
   *
   * @param taskClass
   * @param reserve
   * @param limit
   *
   * @throws IllegalArgumentException
   */
  public void quota(Class<? extends Runnable> taskClass, Quota reserve, Quota limit) {
    if (limit.value == 0) throw new IllegalArgumentException("Limit should not be 0.");
    if (reserve.value > limit.value) throw new IllegalArgumentException("Reserve should not greater than limit.");

    synchronized (this) {
      if (reserve.value > threadSize - reserved) throw new IllegalArgumentException("No resource for reserve");
      reserved += reserve.value;
    }

    quotas.put(taskClass, policy.submitter(reserve, limit));
  }

  /** {@link Quota} */
  private final static class Quota {
    private final AtomicInteger state;
    private final int value;

    private Quota(int value) {
      if (value < 0) throw new IllegalArgumentException("Quota should not less than 0.");
      this.value = value;
      this.state = new AtomicInteger(value);
    }

    /** @return 当前剩余配额. */
    public int state() { return state.get(); }

    /**
     * 占据一个配额.
     *
     * @return false 表示预留的配额以用完, 反之为true.
     */
    public boolean acquire() {
      if (state() == 0) return false;
      if (state.decrementAndGet() >= 0) return true;
      state.incrementAndGet();
      return false;
    }

    /**
     * 释放一个配额.
     *
     * @return false 表示无效的释放, 正常情况下不应出现, 反之为true.
     */
    public boolean release() {
      if (state() == value) return false;
      if (state.incrementAndGet() <= value) return true;
      state.decrementAndGet();
      return false;
    }

  }

  /** {@link Policy} */
  public static enum Policy {

    /** 乐观策略, 当预留配额出现闲置, 允许被其它任务抢占, 但此任务支持被中断(Interrupted), 以让出执行线程. */
    OPTIMISM {
      private Collection<Future<?>> shoots;

      @Override
      Submitter defaultSubmitter() {
        // TODO
        throw new UnsupportedOperationException();
      }

      @Override
      Submitter submitter(final Quota reserve, final Quota limit) {
        return new Submitter() {
          @Override
          public void submit(final Runnable task, CentralExecutor executor) {
            if (!limit.acquire()) enqueue(new ComparableTask(task, reserve.value));
            if (!reserve.acquire() && limit.acquire()) shoot(task, executor, limit);
            if (reserve.acquire() && limit.acquire()) submit(task, executor, limit, reserve);
          }

          private void shoot(final Runnable task, CentralExecutor executor, final Quota limit) {
            Future<?> future = executor.service.submit(new Runnable() {
              @Override
              public void run() {
                try {
                  task.run();
                } catch (Throwable t) {
                  LOGGER.error("Unexpected Interruption cause by", t);
                } finally {
                  limit.release();
                  // TODO fetch task
                }
              }
            });
            shoots.add(future);
          }

          private void submit(final Runnable task, CentralExecutor executor, final Quota limit,
                              final Quota reserve) {
            executor.service.execute(new Runnable() {
              @Override
              public void run() {
                try {
                  task.run();
                } catch (Throwable t) {
                  LOGGER.error("Unexpected Interruption cause by", t);
                } finally {
                  limit.release(); // TODO 一致性的隐患
                  reserve.release();
                  // TODO fetch task
                }
              }
            });
          }

        };
      }
    },

    /** 悲观策略, 在所有线程都被预留的情况下, 即使当前预留之外的线程是空闲, 也不会被抢占, 即Limit的设定是被忽略. */
    PESSIMISM {
      @Override
      Submitter defaultSubmitter() {
        return new Submitter() {
          @Override
          public void submit(Runnable task, CentralExecutor executor) {
            throw new RejectedExecutionException("Unquotaed task can not be executed in pessimism.");
          }
        };
      }

      @Override
      Submitter submitter(final Quota reserve, final Quota limit) {
        if (reserve.value == 0)
          throw new IllegalArgumentException("None-reserve task will never be executed in pessimism.");

        return new Submitter() {
          @Override
          public void submit(final Runnable task, CentralExecutor executor) {
            if (!reserve.acquire()) enqueue(new ComparableTask(task, reserve.value));
            else executor.service.execute(new Decorator(task, reserve, executor));
          }
        };
      }
    };

    final PriorityBlockingQueue<ComparableTask> queue = new PriorityBlockingQueue<ComparableTask>();

    void enqueue(ComparableTask task) {
      LOGGER.debug("Enqueue {}", task.original);
      queue.put(task);
    }

    abstract Submitter submitter(Quota reserve, Quota limit);

    abstract Submitter defaultSubmitter();

    void dequeueTo(CentralExecutor executor) {
      try {
        final Runnable task = queue.take().original;
        LOGGER.debug("Dequeue {}", task);
        executor.service.execute(task);
      } catch (InterruptedException e) {
        e.printStackTrace();
        Thread.currentThread().interrupt();
      }
    }

    /** {@link ComparableTask} */
    static class ComparableTask implements Comparable<ComparableTask> {
      final Runnable original;
      private final int quota;

      public ComparableTask(Runnable task, int quota) {
        this.original = task;
        this.quota = quota;
      }

      @Override
      public int compareTo(ComparableTask o) { return quota - o.quota; }
    }

    /** {@link Decorator} */
    class Decorator implements Runnable {
      private final Runnable task;
      private final Quota quota;
      private final CentralExecutor executor;

      public Decorator(Runnable task, Quota quota, CentralExecutor executor) {
        this.task = task;
        this.quota = quota;
        this.executor = executor;
      }

      @Override
      public void run() {
        try {
          task.run();
        } catch (Throwable t) {
          LOGGER.error("Unexpected Interruption cause by", t);
        } finally {
          quota.release();
          dequeueTo(executor);
        }
      }
    }
  }

  /** {@link Submitter} */
  private static interface Submitter {
    void submit(Runnable task, CentralExecutor executor);
  }
}
