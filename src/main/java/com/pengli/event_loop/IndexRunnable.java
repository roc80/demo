package com.pengli.event_loop;

import org.jetbrains.annotations.NotNull;

import lombok.Getter;

/**
 * 带索引的，通过索引选定由哪个EventLoop执行
 *
 * @author lipeng
 * @since 2025/9/2 14:49
 */
public class IndexRunnable implements Runnable {
    @Override
    public void run() {
        runnnable.run();
    }

    @Getter
    private final int index;
    private final Runnable runnnable;

    public IndexRunnable(@NotNull Runnable runnable, int index) {
        this.index = index;
        this.runnnable = runnable;
    }
}
