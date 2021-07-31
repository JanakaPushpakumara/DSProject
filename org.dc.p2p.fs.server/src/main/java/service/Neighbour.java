package service;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Neighbour {

    private static final Logger log = LoggerFactory.getLogger(Neighbour.class);
    private String ip;
    private int port;

    public Neighbour(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String getIp() {
        return this.ip;
    }

    public int getPort() {
        return this.port;
    }

    public boolean NeighbourConnect(List<Neighbour> neighbourList, String clientIP, int clientPort, int soTimeout, List<Neighbour> routingTable) throws Exception {

        String joinMessage = "JOIN " + clientIP + " " + clientPort;
        joinMessage = String.format("%04d", joinMessage.length() + 5) + " " + joinMessage;

        InetAddress ia = InetAddress.getByName(getIp());
        log.info(joinMessage+" sent to ip "+ia +" port "+getPort());
        byte[] messageBytes = joinMessage.getBytes();
        DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, ia, getPort());

        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(soTimeout);
        socket.send(packet);

        byte[] b1 = new byte[2048];
        DatagramPacket dpResponse = new DatagramPacket(b1, b1.length);

        try {
            socket.receive(dpResponse);
        } catch (SocketTimeoutException e) {
            log.error("Timeout reached while receiving data from the Neighbour server.");
            socket.close();
            return false;
        }

        String nodeResponse = new String(dpResponse.getData());
        log.info("neighbour response is " + nodeResponse);
        socket.close();
        return processNodeResponse(nodeResponse.trim(), neighbourList, routingTable);
    }

    public boolean processNodeResponse(String message, List<Neighbour> neighbourList, List<Neighbour> routingTable)
            throws Exception {

        String[] mes = message.split(" ");
        log.info(message);
        log.info("processNodeResponse: " + mes[2]);

        boolean neighbourExist = false;
        if (mes[1].equals("JOINOK")) {
            if (mes[2].equals("0")) {
                for (Neighbour neb : neighbourList) {
                    if (neb.getIp().equals(this.getIp()) && neb.getPort() == this.getPort()) {
                        neighbourExist = true;
                        break;
                    }
                }
                if (neighbourExist == false) {
                    neighbourList.add(this);
                }
                neighbourExist = false;
                for (Neighbour neb : routingTable) {
                    if (neb.getIp().equals(this.getIp()) && neb.getPort() == this.getPort()) {
                        neighbourExist = true;
                        break;
                    }
                }
                if (neighbourExist == false) {
                    routingTable.add(this);
                }
            } else if (mes[2].equals("9999")) {
                throw new Exception("failed, can’t register." + " initial join failed");
            }
            return true;
        } else {
            return false;
        }
    }
}
