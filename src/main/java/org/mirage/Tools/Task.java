/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Mirage-MC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.mirage.Tools;

import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Mod.EventBusSubscriber
public class Task {

    /**
     * 计时器
     */
    private static ScheduledThreadPoolExecutor timer = null;

    /**
     * 工作线程池
     */
    private static ExecutorService workers = null;

    private static final Object lock = new Object();

    /** 延迟执行任务 */
    public static Future<?> delay(Runnable task, long delay, TimeUnit unit) {
        return new DelayHandle(getTimer(), getWorkers(), wrapTask(task), delay, unit);
    }

    /** 在后台线程执行任务 */
    public static Future<?> spawn(Runnable task) {
        return getWorkers().submit(wrapTask(task));
    }

    /** 周期性执行任务 */
    public static ScheduledFuture<?> repeat(Runnable task, long initialDelay, long period, TimeUnit unit) {
        Runnable wrapped = wrapTask(task);
        return getTimer().scheduleAtFixedRate(() -> {
            try {
                getWorkers().execute(wrapped);
            } catch (RejectedExecutionException ignored) {
            }
        }, initialDelay, period, unit);
    }

    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Task.sleep() was interrupted: " + e.getMessage());
        }
    }

    private static ScheduledThreadPoolExecutor getTimer() {
        if (timer == null || timer.isShutdown()) {
            synchronized (lock) {
                if (timer == null || timer.isShutdown()) {
                    timer = new ScheduledThreadPoolExecutor(
                            1,
                            new NamedThreadFactory("Mirage-TaskTimer-")
                    );
                    timer.setRemoveOnCancelPolicy(true);
                }
            }
        }
        return timer;
    }

    private static ExecutorService getWorkers() {
        if (workers == null || workers.isShutdown()) {
            synchronized (lock) {
                if (workers == null || workers.isShutdown()) {
                    int n = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
                    workers = Executors.newFixedThreadPool(
                            n,
                            new NamedThreadFactory("Mirage-TaskWorker-")
                    );
                }
            }
        }
        return workers;
    }

    public static void shutdown() {
        synchronized (lock) {
            if (timer != null && !timer.isShutdown()) {
                timer.shutdown();
            }
            // 再停 workers
            if (workers != null && !workers.isShutdown()) {
                workers.shutdown();
            }

            // 等待 timer 结束
            if (timer != null) {
                try {
                    if (!timer.awaitTermination(5, TimeUnit.SECONDS)) {
                        timer.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    timer.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            // 等待 workers 结束
            if (workers != null) {
                try {
                    if (!workers.awaitTermination(5, TimeUnit.SECONDS)) {
                        workers.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    workers.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    // 监听服务器停止事件
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        shutdown();
    }

    private static Runnable wrapTask(Runnable original) {
        return () -> {
            try {
                original.run();
            } catch (Throwable e) { // 用 Throwable，避免 Error 也把 timer/worker 搞炸
                System.err.println("Task execution failed: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }

    private static final class DelayHandle implements Future<Object> {
        private final CompletableFuture<Object> done = new CompletableFuture<>();
        private final AtomicReference<Future<?>> running = new AtomicReference<>(null);
        private final ScheduledFuture<?> scheduled;

        DelayHandle(ScheduledExecutorService timer,
                    ExecutorService workers,
                    Runnable task,
                    long delay,
                    TimeUnit unit) {

            this.scheduled = timer.schedule(() -> {
                if (done.isCancelled() || done.isDone()) return;

                try {
                    Future<?> f = workers.submit(() -> {
                        try {
                            task.run();
                            done.complete(null);
                        } catch (Throwable t) {
                            done.completeExceptionally(t);
                            throw t;
                        }
                    });
                    running.set(f);
                } catch (RejectedExecutionException rex) {
                    // workers 关闭/拒绝时，标记完成（异常）
                    done.completeExceptionally(rex);
                }
            }, delay, unit);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            boolean c1 = scheduled.cancel(mayInterruptIfRunning);
            Future<?> r = running.get();
            boolean c2 = (r != null) && r.cancel(mayInterruptIfRunning);
            boolean c3 = done.cancel(mayInterruptIfRunning);
            return c1 || c2 || c3;
        }

        @Override
        public boolean isCancelled() {
            return done.isCancelled();
        }

        @Override
        public boolean isDone() {
            return done.isDone();
        }

        @Override
        public Object get() throws InterruptedException, ExecutionException {
            return done.get();
        }

        @Override
        public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return done.get(timeout, unit);
        }
    }

    // 自定义线程工厂
    private static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(1);
        private final String prefix;

        private NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, prefix + counter.getAndIncrement());
            t.setDaemon(false);
            return t;
        }
    }
}
