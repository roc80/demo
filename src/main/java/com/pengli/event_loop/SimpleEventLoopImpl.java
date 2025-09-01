package com.pengli.event_loop;

import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author lipeng
 * @since 2025/8/31 20:59
 */
public class SimpleEventLoopImpl implements SimpleEventLoop {

    private final Queue<ScheduleTask> scheduleTaskQueue;
    private final BlockingQueue<Runnable> taskQueue;
    private final LoopThread loopThread;
    private final Runnable WAKE_UP = () -> {
    };

    public SimpleEventLoopImpl() {
        this.scheduleTaskQueue = new PriorityQueue<>();
        this.taskQueue = new ArrayBlockingQueue<>(1024);
        this.loopThread = new LoopThread();
        loopThread.start();
    }

    @Override
    public SimpleEventLoop next() {
        return this;
    }

    @Override
    public void execute(Runnable runnable) {
        if (!taskQueue.offer(runnable)) {
            throw new RuntimeException("task queue is full");
        }
    }

    @Override
    public void schedule(Runnable runnable, long delay, TimeUnit timeUnit) {
        scheduleTaskQueue.offer(new ScheduleTask(this, runnable, delay, timeUnit));
        execute(WAKE_UP);
    }

    @Override
    public void scheduleAtFixRate(Runnable runnable, long initialDelay, long period, TimeUnit timeUnit) {
        scheduleTaskQueue.offer(new ScheduleTask(this, runnable, initialDelay, period, timeUnit));
        execute(WAKE_UP);
    }

    @Override
    public Queue<ScheduleTask> getScheduleTaskQueue() {
        return this.scheduleTaskQueue;
    }

    private class LoopThread extends Thread {
        @Override
        public void run() {
            while (true) {
                Runnable runnable = getTask();
                if (runnable != null) {
                    runnable.run();
                }
            }
        }
    }

    private Runnable getTask() {
        ScheduleTask scheduleTask = scheduleTaskQueue.peek();
        if (scheduleTask == null) {
            try {
                // 这里正在等普通队列，如果此时定时任务队列有新任务需要立刻执行，会被这里的逻辑阻塞，所以添加WAKE_UP事件。
                Runnable task = taskQueue.take();
                if (task == WAKE_UP) {
                    task = null;
                }
                return task;
            } catch (InterruptedException e) {
                return null;
            }
        }
        if (scheduleTask.getExecTimeMs() <= System.currentTimeMillis()) {
            return scheduleTaskQueue.poll();
        }
        try {
            long latestScheduleExecTime = scheduleTask.getExecTimeMs() - System.currentTimeMillis();
            // 因为最近要执行的定时任务还有 latestScheduleExecTime 才执行，所以这里可以容忍普通队列最多等这么些时间来获取新任务。
            Runnable task = taskQueue.poll(latestScheduleExecTime, TimeUnit.MICROSECONDS);
            // 这里正在等普通队列，如果此时定时任务队列有新任务需要立刻执行，会被这里的逻辑阻塞，所以添加WAKE_UP事件。
            if (task == WAKE_UP) {
                task = null;
            }
            return task;
        } catch (InterruptedException e) {
            return null;
        }
    }
}
