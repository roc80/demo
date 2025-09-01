package com.pengli.event_loop;

import java.util.Queue;

/**
 * @author lipeng
 * @since 2025/8/31 20:51
 */
public interface SimpleEventLoop extends SimpleEventLoopGroup {

    Queue<ScheduleTask> getScheduleTaskQueue();

}