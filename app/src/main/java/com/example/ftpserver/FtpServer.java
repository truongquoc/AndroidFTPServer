package com.example.ftpserver;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

public class FtpServer extends Thread {
    private ObjectInputStream sInput;
    private ObjectOutputStream sOutput;
    private Socket client, dataClient;
    private FileInputStream fis;
    private BufferedInputStream bis;
   private ServerSocket dataSocket;
    private String dir;
    public FtpServer(Socket clientSocket, ServerSocket dataSocket) {
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() );
        this.dir = file.getAbsolutePath();
        this.client = clientSocket;
        this.dataSocket = dataSocket;
        try {
            sInput = new ObjectInputStream(client.getInputStream());
            sOutput = new ObjectOutputStream(client.getOutputStream());
            sOutput.writeObject("Success");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }
    @Override
    public void run() {
               while (true) {
                  try {
                      String command = (String) sInput.readObject();
                      Log.d("command", command);
                      if(command.compareTo("LS") == 0) {
                        getFiles();
                      }
                      if(command.compareTo("CD") == 0) {
                          setCd();
                      }
                      if(command.compareTo("GET") == 0) {
                          sendFile();
                      }

                  } catch (Exception ex) {
                      ex.printStackTrace();
                      return;
                  }
               }

    }
    public void getFiles() {
        File fil=new File(this.dir);
//        Log.d("type", fil);
        File[] Files = fil.listFiles();
       try {
           sOutput.writeInt(Files.length);
               for (int count=0;count < Files.length;count ++){
                   sOutput.writeObject(Files[count].getName());
               }
       } catch (IOException ex) {
           ex.printStackTrace();
           return;
       }


//        for(File file : listFiles) {
//
//        }
    }

    public void setCd() {
        try {
            String path = (String) sInput.readObject();
            File newDir = new File(this.dir + "/" + path);
            if(newDir.exists()) {
                this.dir+= ("/" + path);
                sOutput.writeObject("true");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendFile() throws Exception{
        dataClient = dataSocket.accept();
        ObjectOutputStream outToClient = new ObjectOutputStream(dataClient.getOutputStream());
        String filename = (String) sInput.readObject();
        File file = new File(dir + "/" +filename);

        long length = file.length();
        try {
            if(file.exists()) {
                Log.d("length", String.valueOf(length));
                fis = new FileInputStream(file);
                outToClient.writeInt((int) file.length());
                byte[] buffer = new byte[100];
                Integer bytesRead = 0;
                while((bytesRead = fis.read(buffer)) > 0) {
                    outToClient.writeInt(bytesRead);
                    outToClient.writeObject(Arrays.copyOf(buffer, buffer.length));
                }
                dataClient.close();
            }
        } catch (Exception e) {
            Log.d("err", e.getMessage());
            e.printStackTrace();
        }
    }
}
