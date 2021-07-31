package util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.Neighbour;
import service.Node;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public class MessageProcessor implements Runnable {

    private Node node;
    private boolean stopListner = false;
    private List<Neighbour> connectedNeighbourList;
    private List<Neighbour> routingTable;
    private final String nodeIP;
    private final int serverPort;
    private final Service service = new Service();
    private static final Logger log = LoggerFactory.getLogger(MessageProcessor.class);

    public MessageProcessor(Node node, String nodeIP, int port) {

        this.node = node;
        this.connectedNeighbourList = node.getConnectedNeighboursList();
        this.nodeIP = nodeIP;
        this.serverPort = port;
    }

    @Override
    public void run() {

        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(serverPort);
            log.info("Listener starting on " + serverPort);
            while (!stopListner) {

                byte[] b1 = new byte[2048];

                DatagramPacket packet = new DatagramPacket(b1, b1.length);
                socket.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength());
                String res = this.processMessage(msg);
                byte[] b2 = res.getBytes();

                DatagramPacket dp1 = new DatagramPacket(b2, b2.length, packet.getAddress(), packet.getPort());
                socket.send(dp1);
            }
        } catch (IOException e) {
            socket.close();
            log.error("Error in socket listener");
        }
    }

    public String processMessage(String message) throws IOException {

        String[] mes = message.split(" ");
        int port;
        String ip;
        String responseMsg = "";

        log.info("Message received by listener " + message);

        switch (mes[1]) {
            case "JOIN":

                ip = mes[2];
                port = Integer.parseInt(mes[3]);
                log.info("New JOIN message ");

                Neighbour neighbour = new Neighbour(ip, port);
                boolean neighbourExist = false;
                String joinResMessage = "";
                for (Neighbour neb : connectedNeighbourList) {
                    if (neb.getIp().equals(ip) && neb.getPort() == port) {
                        neighbourExist = true;
                        break;
                    }
                }
                if (!neighbourExist) {
                    connectedNeighbourList.add(neighbour);
                    joinResMessage = "JOINOK " + "0";
                } else {
                    log.warn("Already existing neighbour");
                    joinResMessage = "JOINOK " + "0";
                }
                neighbourExist = false;
                for (Neighbour neb : node.getRoutingTable()) {
                    if (neb.getIp().equals(ip) && neb.getPort() == port) {
                        neighbourExist = true;
                        break;
                    }
                }
                if (!neighbourExist) {
                    node.getRoutingTable().add(neighbour);
                }

                joinResMessage = String.format("%04d", joinResMessage.length() + 5) + " " + joinResMessage;
                responseMsg = joinResMessage;
                log.info("Response message for JOIN req " + joinResMessage);
                break;

            case "LEAVE":
                ip = mes[2];
                port = Integer.parseInt(mes[3]);
                connectedNeighbourList.removeIf(neigh -> (neigh.getIp().equals(ip) && neigh.getPort() == port));
                node.removeDataFromRoutingTable(ip,port);
                String leaveResMessage = "LEAVEOK " + "0";
                leaveResMessage = String.format("%04d", leaveResMessage.length() + 5) + " " + leaveResMessage;
                responseMsg = leaveResMessage;
                break;
            case "SER":
                log.info("#MESSAGE#  #RECEIVED#");
                String fname = String.join(" ", Arrays.asList(mes).subList(4, mes.length - 1));
                String str = node.isFilePresent(fname);
                int count = str.split(" ").length;
                if (str.length() > 0) {
                    log.info("#MESSAGE#  #ANSWERED#");
                    String msg = " SEROK " + count + " " + nodeIP + " " + node.getRestServicePort() + " " + mes[mes.length - 1] + " " + str;
                    int length = msg.length() + 5;
                    msg = String.format("%04d", length) + msg;
                    service.send(msg, mes[2], Integer.parseInt(mes[3]));
                }
                mes[mes.length - 1] = String.valueOf(Integer.parseInt(mes[mes.length - 1]) - 1);
                if (!mes[mes.length - 1].equals("0")) {
                    log.info("#MESSAGE#  #FORWARDED#");
                    String joined = String.join(" ", mes);
                    for (Neighbour neighbour_ : node.getRoutingTable()) {
                        service.send(joined, neighbour_.getIp(), neighbour_.getPort());
                    }
                }
                break;
            case "SEROK":
                if (Integer.parseInt(mes[2]) > 0) {
                    String result = concatMsg(mes);
                    log.info("SEROK message received " + result);
                    Result foundResult = node.setResultObj(mes[3], Integer.parseInt(mes[4]), result.split("#"));
                    if (!node.resultExists(foundResult)) {
                        Instant end = Instant.now();
                        Duration diff = Duration.between(node.getStarttime(), end);
                        log.info("#PERF# Result found  Files: " + result + "Hops: " + (node.getHopCount() - Integer.parseInt(mes[5])));
                        log.info("#PERF TIME# Search String : "+ result + " Response latency = " + diff.toString() + " Hops count = " + (node.getHopCount()- Integer.parseInt(mes[5])));
                        node.addToResultObjList(foundResult);
                    } else {
                        log.info("########## Result already exists in the Obj list. Skipped!!!  ##########");
                    }
                }
                break;
            case "HEALTH":
                String healthRes = "HEALTHOK";
                String healthMessage = String.format("%04d", healthRes.length() + 5) + " " + healthRes;
                responseMsg = healthMessage;
                break;
            case "NODELIST":
                String nodeListRes = "NODEOK ";
                String nodeListResMsg = String.format("%04d", nodeListRes.length() + 5) + " " + nodeListRes;
                for (Neighbour neigh : connectedNeighbourList) {
                    nodeListResMsg = nodeListResMsg.concat(neigh.getIp() + " " + neigh.getPort() + " ");
                }
                responseMsg = nodeListResMsg;
            default:
                log.error("Message received by lister does not match any case " + message);
                break;
        }
        return responseMsg;

    }

    private String concatMsg(String[] message) throws IOException {

        String concatString = "";
        for (int i = 6; i < message.length; i++) {
            concatString = concatString.concat(message[i]).concat(" ");
        }
        return concatString.substring(0, concatString.length() - 1);
    }

}
