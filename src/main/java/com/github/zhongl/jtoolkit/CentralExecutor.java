package com.github.zhongl.jtoolkit;

import static com.github.zhongl.jtoolkit.CentralExecutor.Policy.*;
import static java.util.concurrent.Executors.*;

import java.util.List;
import java.util.Map;
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

  /** @see ExecutorService#shutdownNow() */
  public List<Runnable> shutdownNow() { return service.shutdownNow(); }

  /** @see ExecutorService#shutdown() */
  public void shutdown() { service.shutdown(); }

  /** @see ExecutorService#isShutdown() */
  public boolean isShutdown() { return service.isShutdown(); }

  /** @see ExecutorService#isTerminated() */
  public boolean isTerminated() { return service.isTerminated(); }

  /** @see ExecutorService#awaitTermination(long, java.util.concurrent.TimeUnit) */
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return service.awaitTermination(timeout, unit);
  }

  @Override
  public void execute(Runnable task) {
    final Submitter submitter = quotas.get(task.getClass());
    if (submitter != null) submitter.submit(task, this);
    else policy.defaultSubmitter().submit(task, this);
  }

  /** @return 预留配额. */
  public static Quota reserve(int value) { return new Quota(value); }

  /** @return 弹性配额. */
  public static Quota elastic(int value) { return new Quota(value); }

  /** @return 零配额. */
  public static Quota nil() { return new Quota(0); }

  /**
   * 设定taskClass的保留和限制配额.
   *
   * @param taskClass
   * @param reserve
   * @param elastic
   *
   * @throws IllegalArgumentException
   */
  public void quota(Class<? extends Runnable> taskClass, Quota reserve, Quota elastic) {

    synchronized (this) {
      if (reserve.value > threadSize - reserved) throw new IllegalArgumentException("No resource for reserve");
      reserved += reserve.value;
    }

    quotas.put(taskClass, policy.submitter(reserve, elastic));
  }

  private synchronized boolean hasUnreserved() { return threadSize > reserved; }

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

    /** 乐观策略, 在存在为分配的配额情况下, 一旦出现闲置线程, 允许任务抢占, 抢占的优先级由提交的先后顺序决定. */
    OPTIMISM {

      /** 未定义配额的任务将直接进入等待队列, 但优先级低于所有定义了配额的任务. */
      private final Submitter defaultSubmitter = new Submitter() {
        @Override
        public void submit(Runnable task, CentralExecutor executor) { enqueue(new ComparableTask(task, Integer.MAX_VALUE)); }
      };

      @Override
      Submitter defaultSubmitter() { return defaultSubmitter; }

      @Override
      Submitter submitter(final Quota reserve, final Quota elastic) {
        return new Submitter() {
          @Override
          public void submit(final Runnable task, CentralExecutor executor) {
            if (reserve.acquire()) doSubmit(task, executor, reserve);
              // 若存在为分配的预留配额, 则弹性配额进行争抢
            else if (executor.hasUnreserved() && elastic.acquire()) doSubmit(task, executor, elastic);
              // 同悲观策略进入等待队列
            else enqueue(new ComparableTask(task, reserve.value));
          }
        };
      }
    },

    /** 悲观策略, 在所有线程都被预留的情况下, 即使当前预留之外的线程是空闲, 也不会被抢占, 即Elastic的设定将被忽略. */
    PESSIMISM {

      private final Submitter defaultSubmitter = new Submitter() {
        @Override
        public void submit(Runnable task, CentralExecutor executor) {
          throw new RejectedExecutionException("Unquotaed task can not be executed in pessimism.");
        }
      };

      @Override
      Submitter defaultSubmitter() { return defaultSubmitter; }

      @Override
      Submitter submitter(final Quota reserve, final Quota elastic) {
        if (reserve.value == 0)
          throw new IllegalArgumentException("None-reserve task will never be executed in pessimism.");

        return new Submitter() {
          @Override
          public void submit(final Runnable task, CentralExecutor executor) {
            if (reserve.acquire()) doSubmit(task, executor, reserve);
              // 耗尽预留配额后, 进入等待队列, 按预留额度大小排优先级, 大者优先.
            else enqueue(new ComparableTask(task, reserve.value));
          }
        };
      }
    };


    /** 优先级等待队列. */
    private final PriorityBlockingQueue<ComparableTask> queue = new PriorityBlockingQueue<ComparableTask>();

    abstract Submitter submitter(Quota reserve, Quota elastic);

    abstract Submitter defaultSubmitter();

    /** 将任务入等待队列. */
    void enqueue(ComparableTask task) {
      queue.put(task);
      LOGGER.debug("Enqueue {}", task.original);
    }

    /** 将任务出列重新提交给执行器. */
    void dequeueTo(CentralExecutor executor) {
      try {
        final Runnable task = queue.take().original;
        LOGGER.debug("Dequeue {}", task);
        executor.execute(task);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        LOGGER.debug("Dequeue has been interrupted ", e);
      }
    }

    void doSubmit(Runnable task, CentralExecutor executor, Quota quota) {
      executor.service.execute(new Decorator(task, quota, executor));
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
      public int compareTo(ComparableTask o) { return -(quota - o.quota); }
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
