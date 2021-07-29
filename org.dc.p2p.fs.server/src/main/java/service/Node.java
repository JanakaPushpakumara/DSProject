package service;

import util.MessageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.Query;
import util.Result;
import util.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

public class Node {

    private int restServicePort;
    private final int serverPort;
    private int resultPort;
    public static int bsServerPort;
    public static int soTimeout;
    private int retryCount;
    private final int retryLimit;
    private final String serverName;
    private final String serverIP;
    private String resultIP;
    public static String bsServerIP;
    private String[] resultList;
    private Query query;
    private final Service service = new Service();
    private List<String> fileList;
    public static List<Neighbour> neighboursList = new ArrayList<>();
    public boolean retry;
    public static List<Neighbour> connectedNeighboursList = new ArrayList<>();
    public static Map<String, List<Neighbour>> neighbourMap = new HashMap<>();
    private final List<Result> resultObjList = new ArrayList<>();
    private static final Logger log = LoggerFactory.getLogger(Node.class);

    public Node(String ip, int port, String serverName, String bsServerIP, int bsServerPort, int soTimeout, int retryLimit, int restServicePort){
        this.serverIP = ip;
        this.serverName = serverName;
        this.serverPort = port;
        this.bsServerIP = bsServerIP;
        this.bsServerPort = bsServerPort;
        this.soTimeout = soTimeout;
        this.retryLimit = retryLimit;
        this.restServicePort = restServicePort;
        neighbourMap.put("Joined",neighboursList);
        neighbourMap.put("Connected", connectedNeighboursList);
        retry=true;
    }

    public boolean registerNode() throws Exception {

        boolean registerStatus = true;
        Neighbour[] neighbours = new Neighbour[0];
        String regMessage = "REG " + serverIP + " " + serverPort + " " + serverName;
        regMessage = String.format("%04d", regMessage.length() + 5) + " " + regMessage;
        log.info("Registering " + this.serverName +" with BS server: reg message " + regMessage);
        String bsResponse = service.sendToBS(regMessage, bsServerIP, bsServerPort, soTimeout);
        if (bsResponse != null) {
            neighbours = processBSResponse(bsResponse);
        } else {
            log.error("Error occurred while connecting to Boostrap Server.");
        }
        if (neighbours.length != 0) {
            boolean neighbourConnectStatus = processNeighbour(neighbours);
            if (!neighbourConnectStatus) {
                registerStatus = false;
                unRegisterNode();
            } else {
                retryCount = 0;
            }
        }
        return registerStatus;
    }

    public void unRegisterNode() throws Exception {

        String unRegMessage = "UNREG " + serverIP + " " + serverPort + " " + serverName;
        unRegMessage = String.format("%04d", unRegMessage.length() + 5) + " " + unRegMessage;
        String bsResponse = service.sendToBS(unRegMessage, bsServerIP, bsServerPort, soTimeout);
        log.error(serverName +" unregistered from the BS" +bsResponse);
        retryCount += 1;
        neighboursList.clear();
        if (retry){
            if (retryCount <= retryLimit) {
                log.warn("Attempt number "+retryCount +" to register with BS");
                Thread.sleep(100);
                registerNode();
            } else {
                log.error("Retry Limit reached");
            }}
    }

    public Neighbour[] processBSResponse(String message)
            throws Exception {

        String[] mes = message.split(" ");
        log.info(message);
        log.info("processBSResponse: " + mes[2]);
        int noNodes = Integer.parseInt(mes[2]);

        if (0 <= noNodes && noNodes <= 2) {
            Neighbour[] neighbour = new Neighbour[noNodes];
            for (int n = 0; n < noNodes; n++) {
                neighbour[n] = new Neighbour(mes[(n * 2) + 3],
                        Integer.parseInt(mes[(n * 2) + 4]));
            }
            return neighbour;
        } else {
            switch (noNodes) {
                case 9996:
                    throw new Exception("failed, can’t register." + " BS full");
                case 9997:
                    throw new Exception("failed, registered" + " to another user, try a different IP and port");
                case 9998:
                    log.error("Node already exists. Unregistering the node");
                    this.setRetry(false);
                    unRegisterNode();
                    throw new Exception("failed, already registered" + " to you, unregister first");
                case 9999:
                    throw new Exception("failed, there is some error" + " in the command");
            }
        }
        return null;
    }

    public boolean processNeighbour(Neighbour[] neighbours) throws Exception {

        boolean connectSuccess = false;
        for (Neighbour neighbour : neighbours) {
            connectSuccess = neighbour.NeighbourConnect(neighboursList, serverIP, serverPort, soTimeout);
            if (!connectSuccess)
                return false;
        }
        return connectSuccess;
    }

    public String isFilePresent(String fName) {

        StringBuilder fileStr = new StringBuilder();
        for (String element : fileList
        ) {
            if (element.toLowerCase().contains(fName.toLowerCase())) fileStr.append(element).append("#");
        }
        if (fileStr.toString().length()>0){
            return fileStr.substring(0,fileStr.toString().length()-1);}
        else {
        return fileStr.toString();}
    }

    public void searchFiles(String fName) throws IOException {
        resultObjList.clear();
        String str = isFilePresent(fName);
        if (str.length() > 0) {
            resultList = str.split("#");
            resultObjList.add(setResultObj(serverIP, restServicePort, resultList));
        } else {
            query = new Query(this.serverIP, this.serverPort, fName, 5);
            log.info("Query string for search files "+ query.getMsgString());

            for (Neighbour neighbour : neighboursList) {
                service.send(query.getMsgString(), neighbour.getIp(), neighbour.getPort());
            }

            for (Neighbour neighbour : connectedNeighboursList) {
                service.send(query.getMsgString(), neighbour.getIp(), neighbour.getPort());
            }

        }
        log.info("Searching done!");
    }

    private void decodeAndAct(String recQuery) throws IOException {

        String[] msgList = recQuery.split(" ");

        if (msgList[1].equals("SER")) {
            String str = isFilePresent(msgList[4]);
            if (str.length() > 0) {
                String msg = " SEROK " + serverIP + " " + serverPort + " " + str + " " + "1";
                int length = msg.length() + 5;
                msg = String.format("%04d", length) + msg;
                service.send(msg, msgList[2], Integer.parseInt(msgList[3]));
                //send
            } else {
                msgList[5] = String.valueOf(Integer.parseInt(msgList[5]) - 1);
                if (!msgList[5].equals("0")) {
                    String joined = String.join(" ", msgList);
                    for (Neighbour neighbour : neighboursList) {
                        service.send(joined, neighbour.getIp(), neighbour.getPort());
                    }
                }
            }
        } else if (msgList[1].equals("SEROK") && Integer.parseInt(msgList[2]) > 0) {
            resultIP = msgList[3];
            resultPort = Integer.parseInt(msgList[4]);
            resultList = Arrays.copyOfRange(msgList, 6, msgList.length);
        }

    }

    public void receiveFromNeighbours() throws IOException {

        String recQuery = service.receive(serverPort);
        decodeAndAct(recQuery);
    }

    public List<Result> getResultList() {
        return resultObjList;
    }

    public Result setResultObj(String resultIP, int resultPort, String[] resultList) {
        Result result = new Result();
        result.setResult(resultIP, resultPort, resultList);
        return result;
    }

    public void addToResultObjList(Result result) {
        resultObjList.add(result);
    }

    public String getResultIP() {

        return resultIP;
    }

    public int getResultPort() {

        return resultPort;
    }

    public void setFileList(List<String> fList) {

        fileList = fList;
    }

    public List<Neighbour> getNeighboursList() {

        return neighboursList;
    }

    public int getRestServicePort() {
        return restServicePort;
    }

    public static List<Neighbour> getConnectedNeighboursList() {

        return connectedNeighboursList;
    }

    public void startListner(){

        log.info("Listener Started on server port "+serverPort);
        Runnable runnable = new MessageProcessor(this, serverIP,serverPort);
        Thread thread = new Thread(runnable);
        thread.start();
    }

    public boolean getRetry() {

        return retry;
    }

    public void setRetry(boolean retry) {

        this.retry = retry;
    }
}
