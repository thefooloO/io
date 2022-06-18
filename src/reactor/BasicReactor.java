package reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;


public class BasicReactor implements Runnable {

    final Selector selector;
    final ServerSocketChannel serverSocket;

    BasicReactor(int port) throws IOException {
        selector = Selector.open();
        serverSocket = ServerSocketChannel.open();
        serverSocket.socket().bind(new InetSocketAddress(port));
        serverSocket.configureBlocking(false);

        // register方法将Channel注册到Selector上, 并返回一个SelectionKey对象
        SelectionKey key = serverSocket.register(selector, SelectionKey.OP_ACCEPT);
        key.attach(new Acceptor());
    }
    
    @Override
    public void run() {
        try {
            while(!Thread.interrupted()) {
                selector.select();                        // 阻塞直到至少一个Channel在注册的事件上就绪了
                Set selected = selector.selectedKeys();   // 返回已选择键集合
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


    final class Handler implements Runnable {

        final SocketChannel socket;
        final SelectionKey key;

        ByteBuffer input  = ByteBuffer.allocate(10);
        ByteBuffer output = ByteBuffer.allocate(10);

        static final int READING = 0, SENDING = 1;
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

        void process() {}


        @Override
        public void run() {
            try {
                if         (state == READING) read();
                else if    (state == SENDING) send();
            } catch (IOException e) {}
        }


        void read() throws IOException {
            socket.read(input);
            System.out.println(new String(input.array(), "utf-8"));
            if(inputIsComplete()) {
                process();
                state = SENDING;
                key.interestOps(SelectionKey.OP_WRITE);
            }
        }


        void send() throws IOException {
            socket.write(output);
            if(outputIsComplete()) {
                key.cancel();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Thread basicReactor = new Thread(new BasicReactor(9000));
        basicReactor.setDaemon(false);
        basicReactor.start();
    }
}