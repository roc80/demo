package com.pengli.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
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
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new LineBasedFrameDecoder(1024))
                                .addLast(new StringDecoder())
                                .addLast(new SimpleChannelInboundHandler<String>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
                                        // todo@lp 服务端编辑消息，相应给客户端
                                        // todo@lp 服务端持久化消息，并在断开连接时，打印所有消息
                                        // todo@lp 代码优化
                                        log.info("server received message: {}", msg);
                                        // 需要手动将消息传递给下一个handler
                                        ctx.fireChannelRead(msg);
                                    }

                                    // 至少有一个处理异常的handler, 没有的话，pipeline会忽略异常
                                    @Override
                                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                        super.exceptionCaught(ctx, cause);
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
