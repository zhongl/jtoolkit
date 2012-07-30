package com.github.zhongl.jtoolkit;

import static java.util.concurrent.Executors.newFixedThreadPool;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.*;

/**
 * {@link RaceCondition}
 *
 * @author <a href=mailto:zhong.lunfu@gmail.com>zhongl</a>
 */
public final class RaceCondition {

    private RaceCondition() {}

    public static <T> List<T> parallel(boolean barried, int num, final Callable<T> task) throws Exception {
        Callable<T>[] tasks = (Callable<T>[]) Array.newInstance(Callable.class, num);
        Arrays.fill(tasks, task);
        return run(barried, tasks);
    }

    public static <T> List<T> run(boolean barried, Callable<T>... tasks) throws Exception {
        final ExecutorService executor = newFixedThreadPool(tasks.length);
        final List<Future<T>> futures = new ArrayList<Future<T>>(tasks.length);

        final CountDownLatch latch = new CountDownLatch(tasks.length);
        final CyclicBarrier barrier = barried ? new CyclicBarrier(tasks.length) : null;

        for (final Callable<T> task : tasks) {
            futures.add(executor.submit(new Callable<T>() {
                @Override
                public T call() throws Exception {
                    try {
                        if (barrier != null) barrier.await();
                        return task.call();
                    } finally {
                        latch.countDown();
                    }
                }
            }));
        }

        try {
            latch.await();
            return map(futures);
        } finally {
            executor.shutdownNow();
        }
    }

    private static <T> List<T> map(List<Future<T>> futures) throws Exception {
        final List<T> results = new ArrayList<T>(futures.size());
        for (Future<T> future : futures) results.add(future.get());
        return results;
    }

}
