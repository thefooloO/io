package reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class Client {

    public static void main(String[] args) throws IOException {
        SocketChannel socket = SocketChannel.open();
        socket.connect(new InetSocketAddress(9000));
        ByteBuffer output = ByteBuffer.allocate(1024);
        output.put("HelloWorld".getBytes(StandardCharsets.UTF_8));
        output.flip();
        socket.write(output);
    }
}
