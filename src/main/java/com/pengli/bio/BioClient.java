package com.pengli.bio;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * BIO
 *
 * @author lipeng
 * @since 2025/8/9 16:55
 */
@Slf4j
public class BioClient {
    public static void main(String[] args) {
        Thread praise = new Thread(() -> {
            try {
                sendHello();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, "praise");

        Thread abyss = new Thread(() -> {
            try {
                sendHello();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, "abyss");
        abyss.start();
        praise.start();
    }

    private static void sendHello() throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", BioServer.PORT));
            OutputStream outputStream = socket.getOutputStream();
            for (int i = 0; i < 3; i++) {
                String message = String.format("%s hello %d === ", Thread.currentThread().getName(), i + 1);
                outputStream.write(message.getBytes());
                outputStream.flush();
            }
            // 模拟阻塞
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
