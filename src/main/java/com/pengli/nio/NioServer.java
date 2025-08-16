package com.pengli.nio;

import com.pengli.bio.BioServer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

/**
 * NIO
 *
 * @author lipeng
 * @since 2025/8/9 17:36
 */
@Slf4j
public class NioServer {
    public static void main(String[] args) throws IOException {
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
             Selector selector = Selector.open()
        ) {
            serverSocketChannel.bind(new InetSocketAddress("127.0.0.1", BioServer.PORT));
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            while (true) {
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                if (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (key.isAcceptable()) {
                        SocketChannel accepted = serverSocketChannel.accept();
                        accepted.configureBlocking(false);
                        accepted.register(selector, SelectionKey.OP_READ);
                        log.info("{} 已建立连接", accepted.getRemoteAddress());
                    }
                    if (key.isReadable()) {
                        SocketChannel socketChannel = (SocketChannel) (key.channel());
                        // 会发生 半包、粘包
                        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                        int read = socketChannel.read(byteBuffer);
                        if (read == -1) {
                            log.info("{} 已断开连接", socketChannel.getRemoteAddress());
                            socketChannel.close();
                        } else {
                            byteBuffer.flip();
                            byte[] bytes = new byte[byteBuffer.remaining()];
                            byteBuffer.get(bytes);
                            log.info("received {} message: {}",socketChannel.getRemoteAddress(), new String(bytes));
                        }
                    }
                }
            }
        }
    }
}
