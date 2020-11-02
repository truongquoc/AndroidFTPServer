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

class ServerSocketThread  extends Thread {

    private ServerSocket serverSocket;
    private ServerSocket dataServerSocket;
    private ObjectInputStream sInput;
    private ObjectOutputStream sOutput;
    private String dir;
    private final String root = Environment.getExternalStorageState();

    public ServerSocketThread()  {
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

            while (true) {
               try {
                   MainActivity.infoMsg.setText("FTP Server running on"+ getIPAddress(true)+ " PORT: "+ PORT);
                   Socket client = serverSocket.accept();
                   MainActivity.infoMsg.setText("Connection FTPClient from "+ client.getInetAddress()+ "PORT: "+client.getPort());
                   FtpServer ftpServer = new FtpServer(client, dataServerSocket);
                   ftpServer.start();
               } catch (Exception e) {
                   e.printStackTrace();
               }
            }
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
