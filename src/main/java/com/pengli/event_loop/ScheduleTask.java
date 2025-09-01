package com.pengli.event_loop;

import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 * @author lipeng
 * @since 2025/9/1 13:23
 */
public class ScheduleTask implements Runnable, Comparable<ScheduleTask>{

    private final Runnable runnable;
    private final Queue<ScheduleTask> scheduleTaskQueue;
    private long execTimeMs;
    private long periodMs;

    public ScheduleTask(SimpleEventLoop eventLoop, Runnable runnable, long initialDelay, long period, TimeUnit timeUnit) {
        this.scheduleTaskQueue = eventLoop.getScheduleTaskQueue();
        this.runnable = runnable;
        this.execTimeMs = System.currentTimeMillis() + timeUnit.toMillis(initialDelay);
        this.periodMs = timeUnit.toMillis(period);
    }

    public ScheduleTask(SimpleEventLoop eventLoop, Runnable runnable, long delay, TimeUnit timeUnit) {
        this.scheduleTaskQueue = eventLoop.getScheduleTaskQueue();
        this.runnable = runnable;
        this.execTimeMs = System.currentTimeMillis() + timeUnit.toMillis(delay);
    }

    public long getExecTimeMs() {
        return execTimeMs;
    }

    @Override
    public void run() {
        try {
            runnable.run();
        } finally {
            if (periodMs > 0) {
                execTimeMs += periodMs;
                scheduleTaskQueue.offer(this);
            }
        }
    }

    @Override
    public int compareTo(ScheduleTask o) {
        return (int) (this.execTimeMs - o.execTimeMs);
    }
}
