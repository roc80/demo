package com.pengli.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoop;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * client
 *
 * @author lipeng
 * @since 2025/8/17 16:56
 */
@Slf4j
public class NettyClient {
    public static void main(String[] args) {
        ChannelFuture connectFuture = new Bootstrap()
                .group(new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory()))
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                        ch.pipeline().addLast(new StringEncoder());
                    }
                })
                .connect("127.0.0.1", 8080);

        connectFuture.addListener(f -> {
            if (f.isSuccess()) {
                log.info("client connect server succeed");
                EventLoop eventLoop = connectFuture.channel().eventLoop();
                eventLoop.scheduleAtFixedRate(() -> {
                    connectFuture.channel().writeAndFlush(String.format("hello, %d \n", System.currentTimeMillis() / 1000));
                }, 0, 1, TimeUnit.SECONDS);
            } else {
                log.error("client connect server failed");
            }
        });
    }
}
