package com.pengli;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 简化的多线程 Reactor（boss + worker）Echo Server
 * @author chatGPT
 */
public class MultiReactorEchoServer {

    public static void main(String[] args) throws IOException {
        int port = 8007;
        int workerCount = Runtime.getRuntime().availableProcessors(); // 或自定义
        ReactorServer server = new ReactorServer(port, workerCount);
        server.start();
        System.out.println("Server started on port " + port);
    }

    static class ReactorServer {
        private final ServerSocketChannel serverSocket;
        private final Selector bossSelector;
        private final Acceptor acceptor;
        private final Worker[] workers;
        private final AtomicInteger idx = new AtomicInteger(0);

        ReactorServer(int port, int workerCount) throws IOException {
            serverSocket = ServerSocketChannel.open();
            serverSocket.configureBlocking(false);
            serverSocket.bind(new InetSocketAddress(port));

            bossSelector = Selector.open();
            serverSocket.register(bossSelector, SelectionKey.OP_ACCEPT);

            workers = new Worker[workerCount];
            for (int i = 0; i < workerCount; i++) {
                workers[i] = new Worker("worker-" + i);
            }
            acceptor = new Acceptor(bossSelector, serverSocket, workers);
        }

        void start() {
            // 启动 workers
            for (Worker w : workers) w.start();
            // 启动 acceptor（在当前线程或独立线程）
            new Thread(acceptor, "boss").start();
        }
    }

    /**
     * Acceptor：监听 accept，然后把新的 SocketChannel 分配给某个 Worker。
     * 分配策略这里用 round-robin。
     */
    static class Acceptor implements Runnable {
        private final Selector selector;
        private final ServerSocketChannel serverSocket;
        private final Worker[] workers;
        private final AtomicInteger rr = new AtomicInteger(0);

        Acceptor(Selector selector, ServerSocketChannel serverSocket, Worker[] workers) {
            this.selector = selector;
            this.serverSocket = serverSocket;
            this.workers = workers;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    int ready = selector.select(); // 阻塞等待 accept
                    if (ready == 0) continue;
                    Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                    while (it.hasNext()) {
                        SelectionKey key = it.next();
                        // todo 为什么这里要remove
                        it.remove();
                        if (key.isAcceptable()) {
                            SocketChannel sc = serverSocket.accept();
                            if (sc != null) {
                                sc.configureBlocking(false);
                                // 轮询选择 worker
                                int i = Math.abs(rr.getAndIncrement() % workers.length);
                                workers[i].registerChannel(sc); // 安全地分发给 worker 注册
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try { selector.close(); serverSocket.close(); } catch (IOException ignored) {}
            }
        }
    }

    /**
     * Worker：每个 worker 维护自己的 Selector、一个待注册队列和线程
     */
    static class Worker implements Runnable {
        private final Selector selector;
        private final Thread thread;
        private final ConcurrentLinkedQueue<SocketChannel> registerQueue = new ConcurrentLinkedQueue<>();
        private volatile boolean started = false;
        private final String name;

        Worker(String name) throws IOException {
            this.selector = Selector.open();
            this.name = name;
            this.thread = new Thread(this, name);
        }

        void start() {
            if (!started) {
                started = true;
                thread.start();
            }
        }

        /**
         * 被 acceptor 调用（来自另一个线程）以请求将 SocketChannel 注册到本 worker 的 selector。
         * 注意：这里只把 channel 放入队列并 wakeup() selector，实际 register 在 worker 线程中完成。
         */
        void registerChannel(SocketChannel sc) {
            registerQueue.add(sc);
            selector.wakeup(); // 唤醒 selector 以便尽快处理 pending 注册
        }

        @Override
        public void run() {
            try {
                while (true) {
                    // 1) 先处理 pending registrations（必须在 selector 线程里做 register）
                    SocketChannel sc;
                    while ((sc = registerQueue.poll()) != null) {
                        try {
                            // 把客户端注册为 OP_READ，使用客户端附件保存状态
                            ClientAttachment att = new ClientAttachment();
                            sc.register(selector, SelectionKey.OP_READ, att);
                        } catch (ClosedChannelException e) {
                            // channel 已关闭，忽略
                        }
                    }

                    // 2) 等待就绪事件
                    int ready = selector.select(); // 阻塞直到有事件或被 wakeup
                    if (ready == 0) continue;

                    Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                    while (it.hasNext()) {
                        SelectionKey key = it.next();
                        it.remove();
                        try {
                            if (!key.isValid()) continue;
                            if (key.isReadable()) {
                                doRead(key);
                            }
                            if (key.isWritable()) {
                                doWrite(key);
                            }
                        } catch (IOException ex) {
                            // 出错就关闭 channel
                            closeKey(key);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try { selector.close(); } catch (IOException ignored) {}
            }
        }

        private void doRead(SelectionKey key) throws IOException {
            SocketChannel sc = (SocketChannel) key.channel();
            ClientAttachment att = (ClientAttachment) key.attachment();
            ByteBuffer readBuf = att.readBuffer;
            readBuf.clear();
            int n = sc.read(readBuf);
            if (n == -1) {
                // 客户端关闭连接
                closeKey(key);
                return;
            } else if (n == 0) {
                return; // 没有数据
            }
            readBuf.flip();
            // 应用层处理（这里示例：echo）
            // 将读到的数据复制到新的 ByteBuffer 并推入发送队列
            ByteBuffer resp = ByteBuffer.allocate(readBuf.remaining());
            resp.put(readBuf);
            resp.flip();
            att.outQueue.add(resp);

            // 注册 OP_WRITE 以便发送
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        }

        private void doWrite(SelectionKey key) throws IOException {
            SocketChannel sc = (SocketChannel) key.channel();
            ClientAttachment att = (ClientAttachment) key.attachment();

            // 逐个把队列中的 ByteBuffer 发出去（处理部分写）
            while (!att.outQueue.isEmpty()) {
                ByteBuffer buf = att.outQueue.peek();
                if (buf == null) break;
                sc.write(buf);
                if (buf.hasRemaining()) {
                    // socket 输出缓冲区满了，写不完，保持 OP_WRITE
                    break;
                } else {
                    // 本条消息写完，移除
                    att.outQueue.poll();
                }
            }

            // 如果没有待发数据，取消 OP_WRITE
            if (att.outQueue.isEmpty()) {
                key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
            }
        }

        private void closeKey(SelectionKey key) {
            try {
                key.cancel();
                Channel ch = key.channel();
                if (ch != null) ch.close();
            } catch (IOException ignored) {}
        }
    }

    /**
     * 客户端连接的附件（在同一 worker 线程内访问，通常无需同步）
     */
    static class ClientAttachment {
        final ByteBuffer readBuffer = ByteBuffer.allocate(1024); // 简单示例，生产可复用或池化
        final Queue<ByteBuffer> outQueue = new ArrayDeque<>(); // 待发送队列（保存部分写的剩余）
    }
}
