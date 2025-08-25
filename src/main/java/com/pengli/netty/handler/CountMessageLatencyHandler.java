package com.pengli.netty.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统计消息延迟
 *
 * @author lipeng
 * @since 2025/8/25 13:04
 */
@Slf4j
public class CountMessageLatencyHandler extends SimpleChannelInboundHandler<Long> {

    private final ConcurrentHashMap<Channel, List<Long>> dbMap = new ConcurrentHashMap<>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Long latency) throws Exception {
        List<Long> latencyList = dbMap.computeIfAbsent(ctx.channel(), k -> new ArrayList<>());
        latencyList.add(latency);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        log.info("{} registered.", ctx.channel().remoteAddress());
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        log.info("{} unregistered.", ctx.channel().remoteAddress());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        log.info("{} active.", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        log.info("{} inactive.", ctx.channel().remoteAddress());
        List<Long> latencyList = dbMap.get(ctx.channel());
        long total = 0L;
        long avgLatency = -1L;
        for (Long l : latencyList) {
            total += l;
        }
        if (!latencyList.isEmpty()) {
            avgLatency = total / latencyList.size();
        }
        log.info("{} 平均发送消息延迟: {} ms", ctx.channel().remoteAddress(), avgLatency);
    }

    // 异常随着handler传播，前一个handler的异常，这一个handler也要处理
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        log.error(cause.getMessage());
    }
}
