package com.pengli.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * server
 *
 * @author lipeng
 * @since 2025/8/17 15:59
 */
@Slf4j
public class NettyServer {

    public static void main(String[] args) {
        MultiThreadIoEventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        MultiThreadIoEventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        ConcurrentHashMap<Channel, List<Long>> dbMap = new ConcurrentHashMap<>();

        ChannelFuture bindFuture = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new StringEncoder())
                                .addLast(new LineBasedFrameDecoder(1024))
                                .addLast(new StringDecoder())
                                .addLast(new SimpleChannelInboundHandler<String>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
                                        // todo@lp 代码优化
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
                                })
                                .addLast(new SimpleChannelInboundHandler<Long>() {
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
                                        // todo@lp 首次发送消息，建立连接比较耗时。
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
                                });
                    }
                })
                .bind(8080);
        bindFuture.addListener(f -> {
            if (f.isSuccess()) {
                log.info("server listen port 8080 succeed");
            } else {
                log.error("server listen port 8080 failed");
            }
        });

    }
}
