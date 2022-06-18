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

/**
 * mainReactor负责监听连接, subReactor负责处理连接
 * 原因在于建立连接需要耗费时间和资源
 */
public class MultiReactor {

    final MainReactor mainReactor;
    final SubReactor[] subReactors;

    int next = 0;

    MultiReactor(int port) throws IOException {

        mainReactor = new MainReactor(port);
        subReactors = new SubReactor[3];

        new Thread(mainReactor).start();

        for(int i = 0; i < 3; i++) {
            subReactors[i] = new SubReactor(i);
            new Thread(subReactors[i]).start();
        }
    }

    private class ReactorTemplate implements Runnable {

        Selector selector;

        ReactorTemplate() throws IOException {
            selector = Selector.open();
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

    }

    class MainReactor extends ReactorTemplate {

        final ServerSocketChannel serverSocket;

        MainReactor(int port) throws IOException {
            serverSocket = ServerSocketChannel.open();
            serverSocket.socket().bind(new InetSocketAddress(port));
            serverSocket.configureBlocking(false);
            SelectionKey key = serverSocket.register(selector, SelectionKey.OP_ACCEPT);
            key.attach(new Acceptor());
        }

        class Acceptor implements Runnable {

            @Override
            public void run() {
                System.out.println("Accept Event ...");
                try {
                    SocketChannel socket = serverSocket.accept();
                    if(socket != null) {
                        subReactors[next].register(socket);
                    }
                    if(++next == subReactors.length)
                        next = 0;
                } catch (IOException e) {}
            }
        }
    }

    class SubReactor extends ReactorTemplate {

        int id;

        SubReactor(int id) throws IOException {
            this.id = id;
        }

        void register(SocketChannel socket) throws IOException {
            new Handler(selector, socket);
        }

        class Handler implements Runnable {

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
                System.out.println(id + ":" +new String(input.array(), "utf-8"));
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
    }


    public static void main(String[] args) throws IOException {
        new MultiReactor(9000);
    }
}
