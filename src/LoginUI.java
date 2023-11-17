import java.awt.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.swing.*;

public class LoginUI extends JFrame {
    String serverIp;
    int serverPort;

    public LoginUI() {
        this.setTitle("Login Page");
        this.setSize(200, 150);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        Container container = this.getContentPane();
        container.setLayout(new BorderLayout());
        JPanel panel1 = new JPanel();
        JPanel panel2 = new JPanel();

        JButton btn = new JButton("Start");
        Label lb = new Label("Please input your name:");
        JTextField username = new JTextField(10);

        panel1.add(btn);
        panel2.add(lb);
        panel2.add(username);

        container.add(panel2);
        container.add(panel1, BorderLayout.SOUTH);


        btn.addActionListener( i -> {
            // change username to anonymous is no input
            if (username.getText().equals("")) {
                username.setText("anonymous");
            }

            btn.setEnabled(false);
            try {


                req(username.getText());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    void req(String username) throws UnknownHostException, IOException {


        DatagramSocket socket = new DatagramSocket();
        DatagramPacket packet = new DatagramPacket(username.getBytes(), username.length(),
                InetAddress.getByName("255.255.255.255"), 5555);
        socket.send(packet);
        DatagramPacket received = new DatagramPacket(new byte[1024], 1024);


        while (true) {
            System.out.println("Listening...");

            socket.receive(received);
            String content = new String(received.getData(), 0, received.getLength());

            if (content != null) {
                serverIp = received.getAddress().toString();
                // remove the first "/" of the address string
                serverIp = serverIp.substring(1);
                serverPort = Integer.parseInt(content);
                System.out.println("Server IP: " + serverIp);
                System.out.println("Server Port: " + serverPort);

                socket.close();
                this.setVisible(false);

                UI ui = UI.getInstance(serverIp, serverPort, username);

                ui.setData(new int[50][50], 20);
                ui.setVisible(true);
                break;
            }
        }

    }

}
