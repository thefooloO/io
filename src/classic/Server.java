package classic;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * each handler may be started in its own thread
 * every handler containes read、decode、compute、encode、send
 */
public class Server implements Runnable {

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(9000);
            while(!Thread.interrupted()) {
                new Thread(new Handler(serverSocket.accept())).start();
            }
        } catch (IOException e) {}
    }


    static class Handler implements Runnable {

        final Socket socket;
        Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            System.out.println(socket.getInetAddress().getHostName() + ":" + socket.getPort());
            try {
                byte[] input = new byte[10];
                socket.getInputStream().read(input);
                byte[] output = process(input);
                socket.getOutputStream().write(output);
            } catch (IOException e) {}
        }

        private byte[] process(byte[] input) {
            byte[] output = input;
            // ...
            return output;
        }
    }


    public static void main(String[] args) {
        Thread server = new Thread(new Server());
        server.setDaemon(false);
        server.start();
    }
}
