package com.pengli.event_loop;

import java.util.concurrent.TimeUnit;

/**
 * @author lipeng
 * @since 2025/8/31 20:48
 */
public interface SimpleEventLoopGroup {

    SimpleEventLoop next();

    void execute(Runnable runnable);

    void schedule(Runnable runnable, long delay, TimeUnit timeUnit);

    void scheduleAtFixRate(Runnable runnable, long initialDelay, long period, TimeUnit timeUnit);

}
