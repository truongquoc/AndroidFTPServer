package com.example.ftpserver;


import android.os.Environment;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.List;

public class ServerSocketThread  extends Thread {

    public static ServerSocket serverSocket;
    public static ServerSocket dataServerSocket;
    public static Socket client;
    private ObjectInputStream sInput;
    private ObjectOutputStream sOutput;
    private String dir;
    private final String root = Environment.getExternalStorageState();
    public static String username, password;
    private volatile boolean stopThread =  false;
    public static boolean isRunning = true;
    static boolean status = true;
    public ServerSocketThread(String username, String password)  {

        this.username = username;
        this.password = password;
        File file = new File(Environment.getExternalStorageState());
        this.dir = file.getAbsolutePath();
    }

    public static final int PORT = 3306;
//    public ServerSocketThread(Socket clientSocket, ServerSocket dataClient) {
//        this.client = clientSocket;
//        this.dataClient = dataClient;

//    }


    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(PORT);
            dataServerSocket = new ServerSocket(PORT - 1);
            while (isRunning) {
               try {
                   MainActivity.infoMsg.setText("FTP Server running on"+ getIPAddress(true)+ " PORT: "+ PORT);
                   client = serverSocket.accept();
                   MainActivity.infoMsg.setText("Connection FTPClient from "+ client.getInetAddress()+ "PORT: "+client.getPort());
                   FtpServer ftpServer = new FtpServer(client, dataServerSocket);
                   if(status) new Thread(ftpServer).start();
               } catch (Exception e) {
                   e.printStackTrace();
               }
            }
//            if(!isRunning) {
//                serverSocket.close();
//                dataServerSocket.close();
//            }
        } catch (Exception e) {
            e.printStackTrace();
           return;
        }
    }

    void updateMSg(String msg) {
        MainActivity.infoMsg.setText(msg);
    }

    /**
     * Get IP address from first non-localhost interface
     * @param useIPv4   true=return ipv4, false=return ipv6
     * @return  address or empty string
     */
    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) { } // for now eat exceptions
        return "";
    }
}
