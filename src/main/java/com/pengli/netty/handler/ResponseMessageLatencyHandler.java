package com.pengli.netty.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * 响应每条消息的延迟
 *
 * @author lipeng
 * @since 2025/8/25 13:09
 */
@Slf4j
public class ResponseMessageLatencyHandler extends SimpleChannelInboundHandler<String> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        long currentTimeStamp = System.currentTimeMillis();
        try {
            String message;
            if (msg == null || (message = msg.trim()).isBlank()) {
                return;
            }
            long timeStampAtSend = Long.parseLong(message);
            long latency = currentTimeStamp - timeStampAtSend;
            ctx.fireChannelRead(latency);
            log.info("send message latency: {} ms.", latency);
            ctx.channel().writeAndFlush("send message latency: " + latency + "ms.\n");
        } catch (NumberFormatException e) {
            throw new RuntimeException(e);
        }
    }
}
