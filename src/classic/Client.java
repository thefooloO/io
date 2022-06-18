package classic;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Client {

    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("127.0.0.1", 9000);
        socket.getOutputStream().write("HelloWorld".getBytes(StandardCharsets.UTF_8));
        byte[] msg = new byte[10];
        socket.getInputStream().read(msg);
        System.out.println(new String(msg));
    }
}
