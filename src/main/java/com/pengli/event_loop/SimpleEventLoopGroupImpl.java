package com.pengli.event_loop;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author lipeng
 * @since 2025/8/31 20:58
 */
public class SimpleEventLoopGroupImpl implements SimpleEventLoopGroup {

    private final SimpleEventLoop[] children;
    private final AtomicInteger count = new AtomicInteger();

    public SimpleEventLoopGroupImpl(int threadNum) {
        this.children = new SimpleEventLoopImpl[threadNum];
        for (int i = 0; i < children.length; i++) {
            children[i] = new SimpleEventLoopImpl();
        }
    }

    @Override
    public void execute(Runnable runnable) {
        SimpleEventLoop eventLoop = findEventLoop(runnable);
        eventLoop.execute(runnable);
    }

    @Override
    public SimpleEventLoop next() {
        return children[count.getAndIncrement() % children.length];
    }

    @Override
    public void schedule(Runnable runnable, long delay, TimeUnit timeUnit) {
        SimpleEventLoop eventLoop = findEventLoop(runnable);
        eventLoop.schedule(runnable, delay, timeUnit);
    }

    @Override
    public void scheduleAtFixRate(Runnable runnable, long initialDelay, long period, TimeUnit timeUnit) {
        SimpleEventLoop eventLoop = findEventLoop(runnable);
        eventLoop.scheduleAtFixRate(runnable, initialDelay, period, timeUnit);
    }

    private SimpleEventLoop getSpec(int index) {
        return children[index % children.length];
    }

    private SimpleEventLoop findEventLoop(Runnable runnable) {
        SimpleEventLoop eventLoop;
        if (runnable instanceof IndexRunnable) {
            int eventLoopIndex = ((IndexRunnable) runnable).getIndex();
            eventLoop = getSpec(eventLoopIndex);
        } else {
            eventLoop = next();
        }
        return eventLoop;
    }
}
