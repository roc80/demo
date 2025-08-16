package com.pengli.bio;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * BIO
 *
 * @author lipeng
 * @since 2025/8/9 16:29
 */
@Slf4j
public class BioServer {

    public static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                // 单个线程内，socket没有读到断开连接的标志时，会一直阻塞，无论是否有IO。
                // socket无穷而thread有穷
                new Thread(() -> {
                    try {
                        InputStream inputStream = socket.getInputStream();
                        // 一定会产生半包、粘包
                        byte[] buf = new byte[1024];
                        int length;
                        while ((length = inputStream.read(buf)) != -1) {
                            log.info("receive a message: {}", new String(buf, 0, length));
                        }
                    } catch (IOException e) {
                        log.error(e.getMessage());
                    }
                }).start();
            }
        }
    }
}
