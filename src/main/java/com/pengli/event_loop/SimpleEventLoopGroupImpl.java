package com.pengli.event_loop;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author lipeng
 * @since 2025/8/31 20:58
 */
public class SimpleEventLoopGroupImpl implements SimpleEventLoopGroup {

    private final SimpleEventLoop[] children;
    private AtomicInteger index = new AtomicInteger();

    public SimpleEventLoopGroupImpl(int threadNum) {
        this.children = new SimpleEventLoopImpl[threadNum];
    }

    @Override
    public SimpleEventLoop next() {
        return children[index.getAndIncrement() % children.length];
    }

    @Override
    public void execute(Runnable runnable) {
        // todo@Lp 指定特定的 eventloop 来执行
        next().execute(runnable);
    }

    @Override
    public void schedule(Runnable runnable, long delay, TimeUnit timeUnit) {
        next().schedule(runnable, delay, timeUnit);
    }

    @Override
    public void scheduleAtFixRate(Runnable runnable, long initialDelay, long period, TimeUnit timeUnit) {
        next().scheduleAtFixRate(runnable, initialDelay, period, timeUnit);
    }
}
