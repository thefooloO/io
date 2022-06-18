package reactor;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * BasicReactor中的Reactor为单个线程, 需要处理accept连接, 同时将事件分发到处理器中
 * 由于只有单个线程, 处理器中的业务需要能够快速处理完, 可以使用多线程处理业务逻辑来改进
 */
public class MultithreadedReactor implements Runnable {

    // 线程池优化
    static ExecutorService pool = Executors.newScheduledThreadPool(5);

    final Selector selector;
    final ServerSocketChannel serverSocket;

    MultithreadedReactor(int port) throws IOException {
        selector = Selector.open();
        serverSocket = ServerSocketChannel.open();
        serverSocket.socket().bind(new InetSocketAddress(port));
        serverSocket.configureBlocking(false);
        SelectionKey key = serverSocket.register(selector, SelectionKey.OP_ACCEPT);
        key.attach(new Acceptor());
    }

    @Override
    public void run() {
        try {
            while(!Thread.interrupted()) {
                selector.select();
                Set selected = selector.selectedKeys();
                Iterator iterator = selected.iterator();
                while(iterator.hasNext()) {
                    dispatch((SelectionKey) iterator.next());
                }
                selected.clear();
            }
        } catch (IOException e) {}
    }


    void dispatch(SelectionKey key) {
        Runnable runnable = (Runnable) (key.attachment());
        if(runnable != null) {
            runnable.run();
        }
    }

    class Acceptor implements Runnable {

        @Override
        public void run() {
            System.out.println("Accept Event ...");
            try {
                SocketChannel socket = serverSocket.accept();
                if(socket != null) {
                    new Handler(selector, socket);
                }
            } catch (IOException e) {}
        }
    }


    class Handler implements Runnable {

        final SocketChannel socket;
        final SelectionKey key;

        ByteBuffer input  = ByteBuffer.allocate(10);
        ByteBuffer output = ByteBuffer.allocate(10);

        static final int READING = 0, SENDING = 1, PROCESSING = 3;
        int state = READING;

        Handler(Selector selector, SocketChannel socket) throws IOException {
            this.socket = socket;
            socket.configureBlocking(false);
            key = socket.register(selector, 0);
            key.attach(this);
            key.interestOps(SelectionKey.OP_READ);
            selector.wakeup();
        }


        boolean  inputIsComplete() {
            boolean res = true;
            // ...
            return res;
        }

        boolean outputIsComplete() {
            boolean res = true;
            // ...
            return res;
        }

        void process() throws UnsupportedEncodingException {
            System.out.println(new String(input.array(), "utf-8"));
        }

        @Override
        public void run() {
            try {
                if         (state == READING) read();
                else if    (state == SENDING) send();
            } catch (IOException e) {}
        }

        synchronized void read() throws IOException {
            socket.read(input);
            if(inputIsComplete()) {
                state = PROCESSING;
                pool.execute(new Processer());          // 多线程处理业务逻辑
            }
        }

        void send() throws IOException {
            socket.write(output);
            if(outputIsComplete()) {
                key.cancel();
            }
        }

        synchronized void processAndHandOff() throws UnsupportedEncodingException {
            process();
            state = SENDING;
            key.interestOps(SelectionKey.OP_WRITE);
        }


        class Processer implements Runnable {

            @Override
            public void run() {
                try {
                    processAndHandOff();
                } catch (UnsupportedEncodingException e) {}
            }
        }

    }

    public static void main(String[] args) throws IOException {
        Thread multithreadedReactor = new Thread(new MultithreadedReactor(9000));
        multithreadedReactor.setDaemon(false);
        multithreadedReactor.start();
    }

}