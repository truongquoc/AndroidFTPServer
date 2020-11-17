package com.example.ftpserver;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

    private String dir, root;
    public FtpServer(Socket clientSocket, ServerSocket dataSocket) {
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() );
        this.dir = file.getAbsolutePath();
        this.root = dir;
        this.client = clientSocket;
        this.dataSocket = dataSocket;
        try {
            sInput = new ObjectInputStream(client.getInputStream());
            sOutput = new ObjectOutputStream(client.getOutputStream());
            String username = (String) sInput.readObject();
            String password = (String) sInput.readObject();
            Log.d("username", username);
            Log.d("pass", password);
            if(username.compareTo(ServerSocketThread.username) == 0 && password.compareTo(ServerSocketThread.password) == 0) {
                sOutput.writeObject("Success");
                ServerSocketThread.status = true;
            } else {
                ServerSocketThread.status = false;
                sOutput.writeObject("Failed");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
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
                        continue;
                      }
                      else if(command.compareTo("CD") == 0) {
                          setCd();
                      }
                      else if(command.compareTo("GET") == 0) {
                          sendFile();
                          continue;
                      }
                      else if(command.compareTo("PWD") == 0) {
                          getWorkingDirectory();
                          continue;
                      }
                      else if(command.compareTo("MKDIR") == 0) {
                          newDir();
                          continue;
                      }
                      else if(command.compareTo("PUT") == 0) {
                          receiveFiles();
                          continue;
                      }
                      else if(command.compareTo("GET_DIR") == 0) {
                          getDir();
                          continue;
                      }
                      else if(command.compareTo("RMDIR") == 0 ) {
                          Log.d("tag", "RMDIR");
                          deleteDir();
                          continue;
                      }
                      else if(command.compareTo("GET_FILES") == 0) {
                          getFilesOnly();
                      }
                      else if(command.compareTo("DELETE") == 0) {
                          deleteFile();
                      }
                  } catch (Exception ex) {
                      ex.printStackTrace();
                      return;
                  }
               }

    }
    public void getFiles() {
        File fil=new File(this.dir);
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

    }

    public void getWorkingDirectory() {
        try {
            sOutput.writeObject(dir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void newDir() {
        try {
            String dirName = (String) sInput.readObject();
            File newDir = new File(dir+"/"+dirName);
            if(!newDir.exists()) {
                newDir.mkdir();
                sOutput.writeObject("true");
                return;
            }
            sOutput.writeObject("false");
            return;
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public void deleteFile() throws Exception{
        String filename = (String) sInput.readObject();
        File file = new File(dir, filename);
        if(!file.exists()) {
            sOutput.writeObject("false");
            return;
        }
        file.delete();
        sOutput.writeObject("true");
    }
    public void deleteDir() {
        try {
            Log.d("tag", "delete");
            String dirName = (String) sInput.readObject();
            Log.d("dirName", dirName);
            File file = new File(dir, dirName);
            if(file.exists()) {
                deleteDirectory(file);
                Log.d("exist", file.getName());
                sOutput.writeObject("true");
            } else {
                sOutput.writeObject("false");
            }
            return;
        } catch (Exception e) {
            Log.d("error", e.getMessage());
            try {
                sOutput.writeObject("error"+e.getMessage());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            return;
        }
    }

    /**
     * @param file
     * Handle delete each file in Folder because Java isn't able to remove folder with data in it
     * You have delete all file before delete folder contain those files
     */
    public void deleteDirectory(File file ){
        File[] contents = file.listFiles();
        if(contents != null) {
            for(File f:contents) {
               deleteDirectory(f);
            }
        }
       file.delete();
    }
    public void setCd() {
        try {
            String path = (String) sInput.readObject();
            if(path.compareTo("..") == 0) {
                if(dir.compareTo(root) == 0) {
                    sOutput.writeObject(dir);
                    return;
                }
                 File parentDir = new File(dir).getParentFile();
                dir = parentDir.getAbsolutePath();
                sOutput.writeObject(dir);
                return;
            }
            File newDir = new File(this.dir + "/" + path);
            if(newDir.exists()) {
                this.dir+= ("/" + path);
                sOutput.writeObject(this.dir);
                return;
            }
            sOutput.writeObject("cd: "+path+" No such file or directory");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendFile() throws Exception {
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

    public void receiveFiles() throws  Exception {
        dataClient = dataSocket.accept();
        ObjectInputStream reciveData = new ObjectInputStream(dataClient.getInputStream());
        String filename = (String) sInput.readObject();
        Integer bytesRead;
        byte[] buffer = new byte[1024];
        File newfile = new File(dir+"/"+filename);
        FileOutputStream fos = new FileOutputStream(newfile);
        do {
            bytesRead = reciveData.readInt();
            Object obj = reciveData.readObject();
            buffer = (byte[]) obj;
            fos.write(buffer, 0, bytesRead);
        } while (bytesRead == 1024);
        dataClient.close();
        reciveData.close();
    }



    public void getDir()  {
     try {
         File currDir = new File(dir);
         File[] listDir = currDir.listFiles();
         int count=0;
         for(File file : listDir) {
             if(file.isDirectory()) {
                count++;
             }
         }
         sOutput.writeInt(count);

         for(File file : listDir) {
             if(file.isDirectory()) {
                 sOutput.writeObject(file.getName());
             }
         }
         return;
     } catch (IOException e) {
         Log.d("err", e.getMessage());
         return;
     }
    }

    public void getFilesOnly() throws Exception {
        File files = new File(dir);
        File[] listFiles = files.listFiles();
        int count = 0;
        for(File file : listFiles) {
            if(file.isFile()) count++;
        }
        sOutput.writeInt(count);
        for(File file : listFiles) {
            if(file.isFile()) sOutput.writeObject(file.getName());
        }
    }
}
