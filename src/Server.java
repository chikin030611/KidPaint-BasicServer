import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

class Client {
    String name;
    Socket socket;
}

public class Server {
    ServerSocket serverSocket;
    ArrayList<Client> list = new ArrayList<>();
    int[][] data = new int[50][50];

    /**
     * The Server class represents a server that listens for incoming connections and handles client requests.
     * It uses a DatagramSocket to receive messages from clients and a ServerSocket to accept TCP connections.
     * When a client connects, a new thread is created to handle the client's requests.
     */
    public Server() throws IOException {
        String message = "8080";
        DatagramSocket socket = new DatagramSocket(5555);
        DatagramPacket packet;
        DatagramPacket receivedPacket = new DatagramPacket(new byte[1024], 1024);

        serverSocket = new ServerSocket(8080);

        while (true) {
            System.out.println("Listening...");

            socket.receive(receivedPacket);
            String content = new String(receivedPacket.getData(), 0, receivedPacket.getLength());

            if (content != null) {
                int port = receivedPacket.getPort();
                packet = new DatagramPacket(message.getBytes(), message.length(), InetAddress.getByName("255.255.255.255"),
                        port);
                System.out.println(receivedPacket.getAddress());
                System.out.println(receivedPacket.getPort());
                socket.send(packet);

                Socket cSocket = serverSocket.accept();

                System.out.println(cSocket.getInetAddress().toString());
                Client client = new Client();
                client.name = content;
                client.socket = cSocket;

                synchronized (list) {
                    list.add(client);
                    System.out.printf("Total %d clients are connected.\n", list.size());
                }

                Thread t = new Thread(() -> {
                    try {
                        serve(client);
                    } catch (IOException e) {
                        System.err.println("connection dropped.");
                    }
                    synchronized (list) {
                        list.remove(client);
                        if (list.size() == 0) {
                            for (int i = 0; i < data.length; i++) {
                                for (int j = 0; j < data[0].length; j++) {
                                    data[i][j] = 0;
                                }
                            }
                        }
                    }
                });
                t.start();
            }
        }
    }

    /**
     * Serves the client by establishing a connection and handling data communication.
     * 
     * @param client The client to serve.
     * @throws IOException If an I/O error occurs.
     */
    private void serve(Client client) throws IOException {
        Socket clientSocket = client.socket;
        byte[] buffer = new byte[1024];
        int len;
        int type;
        System.out.printf("Established a connection to host %s:%d\n\n", clientSocket.getInetAddress(),
                clientSocket.getPort());

        DataInputStream in = new DataInputStream(clientSocket.getInputStream());
        DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
        if (list.size() > 1) {
            for (int i = 0; i < data.length; i++) {
                for (int j = 0; j < data[0].length; j++) {
                    out.writeInt(0);
                    String p = i + " " + j + " " + data[i][j];
                    out.writeInt(p.length());
                    out.write(p.getBytes(), 0, p.length());
                }
            }
        }
        while (true) {
            type = in.readInt();
            len = in.readInt();
            in.read(buffer, 0, len);
            if (type == 0) {
                String content = new String(buffer, 0, len);
                String[] p = content.split(" ");
                int col = Integer.parseInt(p[0]);
                int row = Integer.parseInt(p[1]);
                int color = Integer.parseInt(p[2]);
                data[col][row] = color;

            }

            send(buffer, len, type);
        }
    }

    /**
     * Sends the specified data to all connected clients.
     *
     * @param data The byte array containing the data to be sent.
     * @param len The length of the data to be sent.
     * @param type The type of the data to be sent.
     */
    private void send(byte[] data, int len, int type) {
        synchronized (list) {
            for (int i = 0; i < list.size(); i++) {
                try {
                    Client client = list.get(i);
                    Socket socket = client.socket;
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.writeInt(type);
                    out.writeInt(len);
                    out.write(data, 0, len);
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * The main method is the entry point of the program.
     * It creates a new instance of the Server class and handles any IOException that may occur.
     *
     * @param args The command line arguments passed to the program.
     */
    public static void main(String[] args) {
        try {
            new Server();
        } catch (IOException e) {
            System.err.println("System error: " + e.getMessage());
        }
    }

}
