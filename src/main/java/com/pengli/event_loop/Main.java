package com.pengli.event_loop;

import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

/**
 * main
 *
 * @author lipeng
 * @since 2025/9/2 14:15
 */
@Slf4j
public class Main {
    public static void main(String[] args) {
        SimpleEventLoopGroup eventLoopGroup = new SimpleEventLoopGroupImpl(4);

        eventLoopGroup.scheduleAtFixRate(
                () -> log.info(String.valueOf(System.currentTimeMillis() / 1000)),
                0,
                1,
                TimeUnit.SECONDS
        );

        for (int i = 0; i < 1000; i++) {
            int taskId = i;
            eventLoopGroup.execute(new IndexRunnable(() -> {
                log.info("{} is executing, taskId = {} ", Thread.currentThread().getName(), taskId);
                // 模拟任务耗时
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }, 1));
        }

    }
}
