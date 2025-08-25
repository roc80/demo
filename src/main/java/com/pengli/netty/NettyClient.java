package com.pengli.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
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
                        ch.pipeline()
                                .addLast(new LineBasedFrameDecoder(1024))
                                .addLast(new StringDecoder())
                                .addLast(new PrintServerMsg())
                                .addLast(new StringEncoder());
                    }
                })
                .connect("127.0.0.1", 8080);

        connectFuture.addListener(f -> {
            if (f.isSuccess()) {
                log.info("client connect server succeed");
                EventLoop eventLoop = connectFuture.channel().eventLoop();
                eventLoop.scheduleAtFixedRate(() -> {
                            connectFuture.channel().writeAndFlush(String.format("%d\n", System.currentTimeMillis()));
                        },
                        0,
                        1,
                        TimeUnit.SECONDS
                );
            } else {
                log.error("client connect server failed");
            }
        });
    }

    static class PrintServerMsg extends SimpleChannelInboundHandler<String> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
            log.info(msg);
        }
    }
}
