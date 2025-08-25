package com.pengli.netty;

import com.pengli.netty.handler.CountMessageLatencyHandler;
import com.pengli.netty.handler.ResponseMessageLatencyHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;

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

        ChannelFuture bindFuture = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                        ch.pipeline()
                                .addLast(new StringEncoder())
                                .addLast(new LineBasedFrameDecoder(1024))
                                .addLast(new StringDecoder())
                                .addLast(new ResponseMessageLatencyHandler())
                                .addLast(new CountMessageLatencyHandler());
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
